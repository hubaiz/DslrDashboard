// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.dslr.dashboard.ptp.*;
import com.dslr.dashboard.ptp.GetPartialObjectProcessor.GetPartialObjectProgress;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.method.MovementMethod;
import android.util.Log;
import android.widget.Toast;

public class PtpService extends Service {
	

	private static String TAG = "PtpService";
	
	private UsbManager _usbManager;
	private UsbDevice _usbDevice;
	private UsbDeviceConnection _usbConnection = null;
	private UsbInterface _usbIntf = null;
	private UsbEndpoint _usbWriteEp = null;
	private UsbEndpoint _usbReadEp = null;
	private UsbEndpoint _usbInterruptEp = null;
	
	private ExecutorService _executor;
	private boolean _isExecutorRunning = false;

	private LinkedList<PtpCommand> _waitingCommands;
	
	private EventThread _eventThread;
	private InterruptThread _interruptThread;
	
	private LiveViewThread _liveViewImgThread = null;
	private PtpLiveViewObject lvo;
	
	private PtpCommunicator _ptpCommunicator;
	private PtpSession _ptpSession;

	private PtpDeviceInfo _ptpDeviceInfo;
	private Hashtable<Integer, PtpProperty> _ptpProperties;
	private Hashtable<Integer, PtpStorageInfo> _ptpStorages;

	private boolean _ptpObjectsLoaded = false;
	
	private boolean isInterfaceClaimed = false;
	private boolean _isUsbDeviceInitialized = false;
	private boolean _isUsbDevicePresent = false;
	private boolean _isPtpDeviceInitialized = false;
	private boolean _isUiBind = false;
	
    private String _sdramSavingLocation = "";
    private int _sdramPictureNumbering = 1;
    private String _sdramPicturePrefix = "dslr";
    
    private SoundMeter _soundMeter;
    private double _soundShootingDenominator = 10.0;
    private double _soundShootingThreshold  = 10.0;
    private String _soundRecordingMedia = "Sdcard";
    private int _soundSamplingRate = 300;
    private boolean _isSoundShootingEnabled = false;
	private boolean _sound_indicator = false;
	private Handler smHandler = null;

    private int _bktStep = 1;
    private int _bktDirection = 2;
    private int _bktCount = 3;
    private boolean _bktEnabled = false;
    private boolean _builtInBracketing = false;

    private boolean _enterLiveViewAtStart = false;
    private boolean _isLiveViewEnabled = false;
    private boolean _isMovieRecordingEnabled = false;
    
    private LocationManager _locationManager;
    private Location _lastLocation = null;
    private boolean _addGpsLocation = true;
    private LinkedList<File> _gpsList;
    private boolean _isWaitingForGpsUpdate = false;
    private int _gpsSampleCount = 3;
    private int _gpsSampleInterval = 2;

    private int _focusMin = 1;
    private int _focusMax = 2000;
    private int _focusCurrent = 0;
    private boolean _focusMaxSet = false;
    private boolean _isFocusInPlace = false;
    
    private final Handler _gpsHandler = new Handler(){
		
		@Override
		public void handleMessage(android.os.Message msg) {
			if (!_isWaitingForGpsUpdate) {
				_isWaitingForGpsUpdate = true;
				_gpsUpdateCount = 0;
				Log.d(TAG, "Need GPS location update");
				_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			}
		};
	};
    
	private static final String ACTION_USB_PERMISSION = "com.dslr.dashboard.USB_PERMISSION";
	PendingIntent mPermissionIntent;  
	
	private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {

	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        Log.d(TAG, "Intent received " + action);
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) { 
	                	Log.d(TAG, "USB device permission granted");
	                	_usbDevice = usbDevice;
	                	_isUsbDevicePresent = true;
	                	initUsbConnection();
	                }
	            }
	        }
	    }
	};
	
	private final BroadcastReceiver mUsbDeviceDetached = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				
				Log.d(PtpService.TAG, "USB device detached");
				
				usbDeviceDetached();
			}
		}
	};
	
	private void usbDeviceDetached()
	{
		Log.d(TAG, "Usb device removed, stoping the service");
		closeUsbConnection();
		_isUsbDevicePresent = false;
		sendPtpServiceEvent(PtpServiceEventType.DeviceClosed, null);
		if (!_isUiBind)
			stopSelf();
		
	}
	
	private void startInForeground(){
		Notification note = new Notification(R.drawable.dslrlauncher48, "DslrDashboard", System.currentTimeMillis());
		Intent i = new Intent(this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
		note.setLatestEventInfo(this, "DslrDashboard", "PTP device connected", pi);
		note.flags |= Notification.FLAG_NO_CLEAR;
		startForeground(1337, note);
	}
	//region Service stoping
	private void stopThread(MyThreadBase thread)
	{
		if (thread != null){
	    boolean retry = true;
	    thread.setRunning(false);
	    while (retry) {
	        try {
	            thread.join();
	            retry = false;
	        } 
	        catch (InterruptedException e) {
	            Log.d(TAG, e.getMessage());
	        }
	        catch (Exception ex) {
	        	Log.d(TAG, ex.getMessage());
	        }
	    }
		}
	}
		
	private void stopThreads(){
		Log.d(TAG, "stoping liveview thread");
		stopThread(_liveViewImgThread);
		Log.d(TAG, "stoping interrupt thread");
		stopThread(_interruptThread);
		Log.d(TAG, "stoping event thread");
		stopThread(_eventThread);
	}

	private void stopExecutor(){
		if (_isExecutorRunning){
		Log.d(TAG, "stoping communicator thread");
		_executor.shutdown();
		   try {
			     // Wait a while for existing tasks to terminate
			     if (!_executor.awaitTermination(500, TimeUnit.MICROSECONDS)) {
			    	 _executor.shutdownNow(); // Cancel currently executing tasks
			       // Wait a while for tasks to respond to being cancelled
			       if (!_executor.awaitTermination(500, TimeUnit.MILLISECONDS))
			           System.err.println("Pool did not terminate");
			     }
			   } catch (InterruptedException ie) {
			     // (Re-)Cancel if current thread also interrupted
				   _executor.shutdownNow();
			     // Preserve interrupt status
			     Thread.currentThread().interrupt();
			   }			
		   _isExecutorRunning = false;
		}
	}
	
	private void closeUsbConnection()
	{
		_locationManager.removeUpdates(locationListener);
		stopSoundMeter();
		stopTimelapse();
		stopThreads();
		stopExecutor();
		stopForeground(true);
		
		// clear the storages and object infos loaded from sdcard
		_ptpStorages.clear();
		_ptpObjectsLoaded = false;
			
		_ptpSession.close();

		sendPtpServiceEvent(PtpServiceEventType.LiveViewEnd, null);
		sendPtpServiceEvent(PtpServiceEventType.DeviceClosed, null);
		
		if (_isPtpDeviceInitialized) {
			try
			{
				// 	clear the ptpproperties
				_ptpProperties.clear();
		
				if (isInterfaceClaimed)
				{
					_usbConnection.releaseInterface(_usbIntf);
					isInterfaceClaimed = false;
				}
				if (_usbConnection != null)
					_usbConnection.close();
				_usbIntf = null;
				_usbReadEp = null;
				_usbWriteEp = null;
				_usbInterruptEp = null;
				_usbConnection = null;
				_usbDevice = null;
				_isUsbDeviceInitialized = false;
			} catch (Exception e){
				Log.d(TAG, "CloseUsb exception: " + e.getMessage());
			}
		}
		
		_isPtpDeviceInitialized = false;
		
	}

	public void stopPtpService(){

		if (_isPtpDeviceInitialized) {
			if (_isLiveViewEnabled)
				sendCommandNew(new PtpCommand(PtpCommand.EndLiveView));
			
			sendCommandNew(new PtpCommand(PtpCommand.ChangeCameraMode)
					.addParam(0));
		}
		closeUsbConnection();
		stopSelf();
	}
	//endregion
	
	//region Service Binder
	
	public class MyBinder extends Binder{
		PtpService getService(){
			return PtpService.this;
		}
	}
	private final IBinder binder = new MyBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		//_isBind = true;
		return binder;
	}
	@Override
	public void onRebind(Intent intent) {
		Log.d(TAG, "onRebind");
		//_isBind = true;
		super.onRebind(intent);
	}
	@Override
	public boolean onUnbind(Intent intent) {
		Log.d(TAG, "onUnbind");
		//_isBind = false;
		return super.onUnbind(intent);
	}
	//endregion
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.d(TAG, "onCreate");
		
        IntentFilter usbDetachedFilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbDeviceDetached, usbDetachedFilter);
		
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, filter); 

        defaultApplicationPreferences();
        readApplicationPreferences();
        
        try
        {
        	_usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        }
        catch(Exception e){
        	Log.d(TAG, "UsbManager exception: " + e.getMessage());
        	_usbManager = null;
        }
        
        _ptpSession = new PtpSession();
        _ptpCommunicator = new PtpCommunicator(_ptpSession);
        _ptpDeviceInfo = new PtpDeviceInfo();
		_ptpProperties = new Hashtable<Integer, PtpProperty>();
		_ptpStorages = new Hashtable<Integer, PtpStorageInfo>();
        
        _waitingCommands = new LinkedList<PtpCommand>();
        
        _eventThread = new EventThread(this);
        _interruptThread = new InterruptThread(this);

        // sound shooting initialization
        _soundMeter = new SoundMeter();
        smHandler = new Handler();
        
        _locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        _gpsList = new LinkedList<File>();
        //_locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        
	}
	
	// Define a listener that responds to location updates
	private int _gpsUpdateCount = 0;
	LocationListener locationListener = new LocationListener() {
	    public void onLocationChanged(Location location) {
	    	
	    	Log.i(TAG, "New location latitude: " + location.getLatitude() + " longituted: " + location.getLongitude() + " altitude: " + location.getAltitude() + " count: " + _gpsUpdateCount);
	    	if (isBetterLocation(location, _lastLocation))
	    		_lastLocation = location;
	    	_gpsUpdateCount++;
	    	//Log.i(TAG, "GPS time: " + location.getTime());
	    	//Log.i(TAG, "Time: " + System.currentTimeMillis());
	    	// wait for 3 location updates
	    	if (_gpsUpdateCount == _gpsSampleCount) {
	    		_locationManager.removeUpdates(locationListener);
	    		//_gpsUpdateCount = 0;
	    		synchronized (_gpsList) {
	    		
	    			while(!_gpsList.isEmpty()){
	    				File file = _gpsList.poll();
	    				NativeMethods.getInstance().setGPSExifData(file.getAbsolutePath(), location.getLatitude(), location.getLongitude(), location.getAltitude());
	    				runMediaScanner(file);
	    			}
	    			_isWaitingForGpsUpdate = false;
	    		}
	    	}
	    }

	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	Log.i(TAG, "Status changes: " + status);
	    }

	    public void onProviderEnabled(String provider) {
	    	Log.i(TAG, "Provider enabled: " + provider);
	    }

	    public void onProviderDisabled(String provider) {
	    	Log.i(TAG, "Provider disable: " + provider);
	    }
	  };
	  
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
    	unregisterReceiver(mUsbDeviceDetached);
		unregisterReceiver(mUsbPermissionReceiver);
		
		saveApplicationPreferences();
		
		super.onDestroy();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart");
		super.onStart(intent, startId);
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		if (!_isExecutorRunning){
			Log.d(TAG, "Starting new executor");
			//_executor = Executors.newFixedThreadPool(1);
			_executor = Executors.newSingleThreadExecutor();
			_isExecutorRunning = true;
		}
		
		return START_STICKY;
	}
	
	public void searchForUsb() {
		try
		{
			if (_usbDevice == null) {
					Log.d(TAG, "Ptp service usb device not initialized, search for one");
			
					if (_usbManager != null)
					{
						HashMap<String, UsbDevice> devices = _usbManager.getDeviceList();
						Log.d(TAG, "Found USB devices count: " + devices.size());
						Iterator<UsbDevice> iterator = devices.values().iterator();
						while(iterator.hasNext())
						{
							UsbDevice usbDevice = iterator.next();
							Log.d(TAG, "USB Device: " + usbDevice.getDeviceName() + " Product ID: " + usbDevice.getProductId() + " Vendor ID: " + usbDevice.getVendorId() + " Interface count: " + usbDevice.getInterfaceCount());
							for(int i = 0; i < usbDevice.getInterfaceCount(); i++){
								UsbInterface intf = usbDevice.getInterface(i);
								Log.d(TAG, "Interface class: " + intf.getInterfaceClass() ); 
								if (intf.getInterfaceClass() == android.hardware.usb.UsbConstants.USB_CLASS_STILL_IMAGE)
								{
									_usbDevice = usbDevice;
									Log.d(TAG, "Ptp Service imaging usb device found requesting permission");
									_usbManager.requestPermission(_usbDevice, mPermissionIntent);
									break;
								}
							}
						}	 
					}
					else
						Log.d(TAG, "USB Manager is unavailable");
				
			}
			else {
				Log.d(TAG, "Ptp service usb imaging device already present, requesting permission");
				//_usbManager.requestPermission(_usbDevice, mPermissionIntent);
			}
		} catch (Exception e) {
			Log.d(TAG, "PtpService start exception: " + e.getMessage());
		}
		
	}
	//region Public properties
	public PtpDeviceInfo getPtpDeviceInfo(){
		return _ptpDeviceInfo;	
	}
	public synchronized PtpProperty getPtpProperty(final int propertyCode){
		PtpProperty property = (PtpProperty)_ptpProperties.get(propertyCode);
		return property;
	}
	public Object[] getPtpProperties(){
		return _ptpProperties.values().toArray();
	}
	public Hashtable<Integer, PtpStorageInfo> getPtpStorages() {
		return _ptpStorages;
	}
	public boolean getIsPtpDeviceInitialized(){
		return _isPtpDeviceInitialized;
	}
	public boolean getIsUsbDeviceInitialized(){
		return _isUsbDeviceInitialized;
	}
	public boolean getIsUsbDevicePresent(){
		return _isUsbDevicePresent;
	}
	public UsbDevice getUsbDevice(){
		return _usbDevice;
	}
	
	public int getBktStep(){
		return _bktStep;
	}
	public void setBktStep(int bktStep){
		_bktStep = bktStep;
	}
	public int getBktCount(){
		return _bktCount;
	}
	public void setBktCount(int bktCount){
		_bktCount = bktCount;
	}
	public int getBktDirection(){
		return _bktDirection;
	}
	public void setBktDirection(int bktDirection){
		_bktDirection = bktDirection;
	}
	public boolean getIsCustomBktEnabled(){
		return _bktEnabled;
	}
	public void setIsCustomBktEnabled(boolean isBktEnabled){
		_bktEnabled = isBktEnabled;
	}
	public boolean getIsBuiltInBktEnabled(){
		return _builtInBracketing;
	}
	public void setIsBuiltInBktEnabled(boolean isBuiltInBktEnabled){
		_builtInBracketing = isBuiltInBktEnabled;
	}
	public double getSoundDenominator(){
		return _soundShootingDenominator;
	}
	public void setSoundDenominator(double soundDenominator){
		_soundShootingDenominator = soundDenominator;
	}
	public double getSoundThreshold(){
		return _soundShootingThreshold;
	}
	public void setSoundThreshold(double soundThreshold){
		_soundShootingThreshold = soundThreshold;
	}
	public String getSoundRecordingMedia(){
		return _soundRecordingMedia;
	}
	public void setSoundRecordingMedia(String soundRecordingMedia){
		_soundRecordingMedia = soundRecordingMedia;
	}
	public int getSoundSamplingRate(){
		return _soundSamplingRate;
	}
	public void setSoundSamplingRate(int soundSamplingRate){
		_soundSamplingRate = soundSamplingRate;
	}
	public boolean getIsSoundShootingEnabled(){
		return _isSoundShootingEnabled;
	}
	public void setIsSoundShootingEnabled(boolean isSoundShootingEnabled){
		_isSoundShootingEnabled = isSoundShootingEnabled;
	}
	public boolean getIsLiveViewEnabled(){
		return _isLiveViewEnabled;
	}
	public boolean getIsMovieRecEnabled(){
		return _isMovieRecordingEnabled;
	}
	public String getSdramSavingLocation(){
		return _sdramSavingLocation;
	}
	public void setSdramSavingLocation(String path){
		
		_sdramSavingLocation = path;
	}
	public String getPicturePrefix(){
		return _sdramPicturePrefix;
	}
	public void setPicturePrefix(String prefix){
		_sdramPicturePrefix = prefix;
	}
	public int getPictureNumbering(){
		return _sdramPictureNumbering;
	}
	public void setPictureNumbering(int startNumber){
		_sdramPictureNumbering = startNumber;
	}
	
	public boolean getPtpObjectsLoaded(){
		return _ptpObjectsLoaded;
	}
	public boolean getIsUiBind(){
		return _isUiBind;
	}
	public void setIsUiBind(boolean value){
		_isUiBind = value;
	}
	public boolean getAddExifGps() {
		return _addGpsLocation;
	}
	public void setAddExifGps(boolean addGpsLocation) {
		_addGpsLocation = addGpsLocation;
	}
	public int getGpsSampleCount(){
		return _gpsSampleCount;
	}
	public void setGpsSampleCount(int gpsSampleCount){
		_gpsSampleCount = gpsSampleCount;
	}
	public int getGpsSampleInterval(){
		return _gpsSampleInterval;
	}
	public void setGpsSampleInterval(int gpsSampleInterval){
		_gpsSampleInterval = gpsSampleInterval;
	}
	
	public int getFocusMin(){
		return _focusMin;
	}
	public void setFocusMin(int focusMin) {
		_focusMin = focusMin;
	}
	public int getFocusMax(){
		return _focusMax;
	}
	public void setFocusMax(int focusMax) {
		_focusMax = focusMax;
		sendPtpServiceEvent(PtpServiceEventType.FocusMaxSet, (Integer)_focusMax);
	}
	public int getFocusCurrent() {
		return _focusCurrent;
	}
	public boolean getIsFocusMaxSet(){
		return _focusMaxSet;
	}
	
	public boolean getEnterLiveViewAtStart(){
		return _enterLiveViewAtStart;
	}
	public void setEnterLiveViewAtStart(boolean enterLiveViewAtStart) {
		_enterLiveViewAtStart = enterLiveViewAtStart;
	}
	//endregion
	
	//region PtpService events
	public interface PtpServiceEventListener {
		void onServiceEvent(PtpServiceEventType eventType, Object eventData);
	}
	
	private PtpServiceEventListener onPtpServiceEventListener =null;
	
	public void setOnPtpServiceEventListener(PtpServiceEventListener listener){
		onPtpServiceEventListener = listener;
	}
	
	private void notifyMainAboutDevice(){
		if (_isUsbDeviceInitialized){
			sendPtpServiceEvent(PtpServiceEventType.DeviceInitialized, null);
		}
		else {
			sendPtpServiceEvent(PtpServiceEventType.NoDeviceFound, null);
		}
		
	}
	
	private void sendPtpServiceEvent(PtpServiceEventType eventType, Object eventData){
		if (onPtpServiceEventListener != null)
			onPtpServiceEventListener.onServiceEvent(eventType, eventData);
	}
	
	//endregion
	
	//region Usb device initialization
	
	private void initUsbConnection()
	{
		try
		{
		if (_usbDevice != null)
		{
			sendPtpServiceEvent(PtpServiceEventType.DeviceFound, _usbDevice);
			if (_usbIntf == null)
			{
				for(int i = 0; i < _usbDevice.getInterfaceCount(); i++)
				{
					UsbInterface uintf = _usbDevice.getInterface(i);
					if (uintf.getInterfaceClass() == UsbConstants.USB_CLASS_STILL_IMAGE){
						// we have a still image interface
						//Log.d(MainActivity.TAG, "Imaging USB interface found");
						_usbIntf = uintf;
						break;
					}
				}
				if (_usbIntf != null)
				{
					// get the endpoints
					for(int i =0; i< _usbIntf.getEndpointCount(); i++)
					{
						UsbEndpoint ep = _usbIntf.getEndpoint(i);
						if (ep.getDirection() == UsbConstants.USB_DIR_OUT)
						{
							if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
							{
								_usbWriteEp = ep;
								//Log.d(MainActivity.TAG, "write endpoint found");
							}
						}
						else {
							switch(ep.getType())
							{
								case UsbConstants.USB_ENDPOINT_XFER_BULK:
									_usbReadEp = ep;
									//Log.d(MainActivity.TAG, "read endpoint found");
									break;
								case UsbConstants.USB_ENDPOINT_XFER_INT:
									_usbInterruptEp = ep;
									//Log.d(MainActivity.TAG, "interrupt endpoint found");
									break;
							}
						}
					}
				}
			}
			// if we have read and write endpoints then we good to go
			if (_usbReadEp != null && _usbWriteEp != null)
			{
				if (!_isUsbDeviceInitialized)
				{
					_usbConnection = _usbManager.openDevice(_usbDevice);
					_isUsbDeviceInitialized = _usbConnection != null;
				}
				if (_isUsbDeviceInitialized)
				{
					//Log.d(MainActivity.TAG, "----- Ptp Service connectio is opened");
					if (!isInterfaceClaimed)
					{
						isInterfaceClaimed = _usbConnection.claimInterface(_usbIntf, true);
						//Log.d(MainActivity.TAG, "----- Ptp Service device interface claimed: " + isInterfaceClaimed);
					}
					
					// start the communicator thread
					if (!_ptpCommunicator.isInitialized()){
						_ptpCommunicator.initCommunicator(_usbConnection, _usbWriteEp, _usbReadEp);
						initPtpDevice();
						startInForeground();
					}
					
				}
			}
			
		}
		notifyMainAboutDevice();
		} catch (Exception e){
			Log.d(TAG, "InitUsb exception: " + e.getMessage());
		}
	}
	
	//endregion
	
	//region Ptp device initialization
	
	private void initPtpDevice(){
		//sendPtpServiceEvent(PtpServiceEventType.DeviceInitialized, null);
		initializePtpDevice();
	}

	private void loadVendorProperties(){
		Log.i(TAG, "Load vendor properties");
		//try to get the vendor properties
		PtpCommand cmd = sendCommandNew(getVendorPropCodesCmd());
		// process the vendor properties
		if (cmd != null && cmd.isDataOk()){
			cmd.incomingData().parse();
			int[] vendorProps = cmd.incomingData().nextU16Array();
			for(int i = 0; i < vendorProps.length; i++){
				PtpCommand pCmd = sendCommandNew(getDevicePropDescCmd(vendorProps[i]));
				processLoadedProperty(pCmd);
			}	
		}
	}
	private void initializePtpDevice(){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
					// get the device info
					PtpCommand cmd = sendCommandNew(getDeviceInfoCmd());
					if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
						return;
					_ptpDeviceInfo.parse(cmd.incomingData());
					// open the session
					cmd = sendCommandNew(openSessionCmd());
					if (cmd == null)
						return;
					if (cmd.isResponseOk() || (cmd.hasResponse() && cmd.incomingResponse().getPacketCode() == PtpResponse.SessionAlreadyOpen)){
						// we have a session
						// load the supported properties
						for(int i = 0;i < _ptpDeviceInfo.getPropertiesSupported().length; i++){
							updateDevicePropDescCmd(_ptpDeviceInfo.getPropertiesSupported()[i]);
//							fCmd = sendCommand(getDevicePropDescCmd(_deviceInfo.getPropertiesSupported()[i]));
//							processLoadedProperty(fCmd);
						}
						
						loadVendorProperties();
						
						// check the warrning status
						// camera is in error status (no card inserted) but the warningstatus is still 0
						// must find another way
						
						Log.i(TAG, "Checking warrning status");
						boolean cont = true;
						
						
						PtpProperty prop = getPtpProperty(PtpProperty.WarningStatus);
						if (prop != null) {
							Log.i(TAG, "Got warrning status property");
							Integer val = (Integer)prop.getValue();
							Log.i(TAG, "Warrning status value: " + val);
							if (val != 0)
							{
								cont = false;
								// don't continue, display the error message
								sendPtpServiceEvent(PtpServiceEventType.WarrningMessage, val);
							}
						}
						
						if (cont) {
							// load the storage infos
							getStorageIds();
						
							// load the lens focus range
							loadLensFocusRange();
						
							// we are done
							Log.d(TAG, "Ptp Service initialized");
							_isPtpDeviceInitialized = true;
							startEventListener();
							sendPtpServiceEvent(PtpServiceEventType.PtpDeviceInitialized, null);

							enterLiveViewAtStart();
						}
					}
					
			}
		}).start();
	}
	
	//endregion
	
	//private Object _syncRoot = new Object();
	
	//region Send PtpCommand
	protected synchronized PtpCommand sendCommandNew(PtpCommand cmd){
		try {
			
			if (cmd.commandCode() != PtpCommand.GetDeviceInfo && cmd.commandCode() != PtpCommand.OpenSession)
			{
				if (!_ptpDeviceInfo.supportsOperation(cmd.commandCode()))
					return null;
			}
			// Log.d(MainActivity.TAG, "Before get task");
			FutureTask<PtpCommand> fCmd = cmd.getTask(_ptpCommunicator);
			//Log.d(MainActivity.TAG, "Before executing");
			_executor.execute(fCmd);
			//Log.d(MainActivity.TAG, "After executing");
		
			return fCmd.get();
		} catch (Exception e) {
			Log.e(TAG, "SendPtpCommand: " + e.getMessage());
		}
		return null;
	}

	private boolean isWaitingThreadRunning = false;
	private boolean waitingForCaptureFinish = false;
	
	private void captureFinishedExecuteWaitingCommands(){
		waitingForCaptureFinish = false;
		executeWaitingCommands();
	}
	
	private void executeWaitingCommands(){
		if (!_waitingCommands.isEmpty() && !isWaitingThreadRunning && !waitingForCaptureFinish)
		{
			isWaitingThreadRunning = true;
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					while(true){
							
							PtpCommand cmd = _waitingCommands.pollLast();
							if (cmd == null){
								isWaitingThreadRunning = false;
								checkNeedFocusStacking();
								break;
							}
							if (cmd.commandCode() == 0) {
								sendPtpServiceEvent(PtpServiceEventType.CommandNotification, cmd.getNotificatinMsg());
							}
							else {
								Log.d(TAG, String.format("WaitingCommands %#04x", cmd.commandCode()));
								cmd = sendCommandNew(cmd);
									if ((cmd == null) || (cmd != null && !cmd.isResponseOk())){
										Log.i(TAG, "Error executing waiting command");
										_waitingCommands.clear();
										isWaitingThreadRunning = false;
										break;
									}
									// if this is a capture command then break, the event will call again this function
									if (cmd.commandCode() == PtpCommand.InitiateCapture ||
											cmd.commandCode() == PtpCommand.InitiateCaptureRecInSdram ||
											cmd.commandCode() == PtpCommand.AfAndCaptureRecInSdram) {
										waitingForCaptureFinish = true;
										isWaitingThreadRunning = false;
										break;
									}
								}
							}
						}
				}).start();
		}
		else {
			checkNeedFocusStacking();
		}
	}
	
	private void checkNeedFocusStacking(){
		if (_isFocusStacking)
			nextFocusStackingImage();
		else
			checkReturnToLiveView();
		
	}
	//endregion
	
	//region Ptp events
	
	private void startEventListener(){
		if (!_ptpDeviceInfo.supportsOperation(PtpCommand.GetEvent)) {
			Log.d(TAG, "----- Using interrupt endpoint for events");
			if (!_interruptThread.isAlive()){
				_interruptThread.setRunning(true);
				_interruptThread.start();
			}
		}
		else {
			Log.d(TAG, "----- Using the GetEvent command for events");
			
			if(!_eventThread.isAlive()) {
				_eventThread.setRunning(true);
				_eventThread.start();
			}
		}
	}
	private synchronized void processEvent(int eventCode, int eventParam) {
		//Log.d(MainActivity.TAG, "+*+*+*+* event code: " + Integer.toHexString(eventCode) + " param: " + Integer.toHexString(eventParam));
		switch(eventCode){
		case PtpEvent.CancelTransaction:
			Log.d(TAG, "CancelTransaction: " + String.format("%#x", eventParam));
			break;
		case PtpEvent.ObjectAdded:
			Log.d(TAG, "ObjectAdded: " + String.format("%#x", eventParam));
			loadNewObjectInfo(eventParam);
			break;
		case PtpEvent.StoreAdded:
			Log.d(TAG, "StoreAdded: " + String.format("%#x", eventParam));
			getStorageInfo(eventParam);
			break;
		case PtpEvent.StoreRemoved:
			Log.d(TAG, "StoreRemoved: " + String.format("%#x", eventParam));
			// remove the store
			_ptpStorages.remove(eventParam);
			break;
		case PtpEvent.DevicePropChanged:
			Log.d(TAG, "DevicePropChanged: " + String.format("%#x", eventParam));
			updateDevicePropDescCmd(eventParam);
			//getDevicePropDescCommand(eventParam);
			break;
		case PtpEvent.DeviceInfoChanged:
			Log.d(TAG, "DeviceInfoChanged: " + String.format("%#x", eventParam));
			break;
		case PtpEvent.RequestObjectTransfer:
			Log.d(TAG, "RequestObjectTransfer: " + String.format("%#x", eventParam));
			break;
		case PtpEvent.StoreFull:
			Log.d(TAG, "StoreFull: " + String.format("%#x", eventParam));
			break;
		case PtpEvent.StorageInfoChanged:
			Log.d(TAG, "StorageInfoChanged: " + String.format("%#x", eventParam));
			getStorageInfo(eventParam);
			break;
		case PtpEvent.CaptureComplete:
			Log.d(TAG, "CaptureComplete: " + String.format("%#x", eventParam));
			captureFinishedExecuteWaitingCommands();
			
//			if (_isFocusStacking)
//				nextFocusStackingImage();
//			
//			checkReturnToLiveView();
			
			//executeWaitingCommands();
			break;
		case PtpEvent.ObjectAddedInSdram:
			Log.d(TAG, "ObjectAddedInSdram: " + String.format("%#x", eventParam));
			if (_eventThread != null)
				_eventThread.setPauseEventListener(true);
			sendPtpServiceEvent(PtpServiceEventType.GetObjectFromSdramStart, null);
			getPictureFromSdram(eventParam);
			
			break;
		case PtpEvent.CaptureCompleteRecInSdram:
			Log.d(TAG, "CaptureCompleteRecInSdram: " + String.format("%#x", eventParam));
			sendPtpServiceEvent(PtpServiceEventType.CaptureCompleteInSdram, null);
			captureFinishedExecuteWaitingCommands();
//			if (_isFocusStacking)
//				nextFocusStackingImage();
//			
//			checkReturnToLiveView();
			
			//executeWaitingCommands();
			break;
		case PtpEvent.PreviewImageAdded:
			Log.d(TAG, "PreviewImageAdded: " + String.format("%#x", eventParam));
			break;
		}
	}

	class EventThread extends MyThreadBase {
		
		private PtpService _service;
		private boolean pauseEventListener = false;
		private Object sync = new Object();
		
		public EventThread(PtpService ptpService){
			_service = ptpService;
			setName("Event thread");
		}
		public void setPauseEventListener(boolean pause){
			synchronized (sync) {
				pauseEventListener = pause;
			}
		}
		@Override
		public void run() {
			while(_run) {
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						Log.e(TAG, "Event thread interrupted");
					}
					synchronized (sync) {
						if (pauseEventListener)
							continue;
					}
					PtpCommand cmd = _service.sendCommandNew(_service.getEventCmd());
						if (cmd != null && cmd.isDataOk()){
							cmd.incomingData().parse();
							int numEvents = cmd.incomingData().nextU16();
							int eventCode, eventParam;
							for (int i = 1; i <= numEvents; i++){
								eventCode = cmd.incomingData().nextU16();
								eventParam = cmd.incomingData().nextS32();
								_service.processEvent(eventCode, eventParam);
							}
						}
			}
		}
	};
	

	class InterruptThread extends MyThreadBase
	{
		PtpService _service;
		
		public InterruptThread(PtpService service){
			_service = service;
			setName("Interrupt thread");
		}
		private void processInterrupPacket(byte[] data) throws InterruptedException, ExecutionException{
			Buffer buf = new Buffer(data);
			int eventCode = buf.getPacketCode();
			buf.parse();
			int eventParam = buf.nextS32();
			Log.d(TAG, "++=====++ New interrupt packet: " + buf.getPacketLength() +
					" type: " + buf.getPacketType() + " code:" + String.format("%#x", buf.getPacketCode()));
			_service.processEvent(eventCode, eventParam);
		}
		
		@Override
		public void run() {
			Buffer buf = new Buffer();
			byte[] tmpData = new byte[_usbInterruptEp.getMaxPacketSize()];
			byte[] data = null;
			boolean needMore = false;
			int counter = 0, size = 0, bytesRead = 0;
			while(_run){
				try {
					if (needMore){
						bytesRead = _usbConnection.bulkTransfer(_usbInterruptEp, tmpData, tmpData.length, 200);
						if (bytesRead > 0) {
							Log.d(TAG, "bytes read: " + bytesRead);
							System.arraycopy(tmpData, 0, data, counter, bytesRead);
							counter += bytesRead;
							if (counter >= size){
								needMore = false;
								processInterrupPacket(data);
							}
						}
					}
					else {
						bytesRead = _usbConnection.bulkTransfer(_usbInterruptEp, tmpData, tmpData.length, 200);
						if (bytesRead > 0){
							Log.d(TAG, "bytes read: " + bytesRead);
							buf.wrap(tmpData);
							size = buf.getPacketLength();
							data = new byte[size];
							System.arraycopy(tmpData, 0, data, 0, bytesRead);
							if (buf.getPacketLength() > bytesRead){
								needMore = true;
								counter = bytesRead;
							}
							else
								processInterrupPacket(data);
						}
					}
					Thread.sleep(400);
				} catch(ExecutionException e){
					
				}
				catch (InterruptedException e) {
					break;
				}
			}
		}
	}
		
	
	//endregion
	
	//region Ptp commands
	private PtpCommand getDeviceInfoCmd(){
		return new PtpCommand(PtpCommand.GetDeviceInfo);
	}
	private PtpCommand openSessionCmd(){
		return new PtpCommand(PtpCommand.OpenSession)
			.addParam(1);
	}
	private PtpCommand getDevicePropDescCmd(int propertyCode){
		return new PtpCommand(PtpCommand.GetDevicePropDesc)
			.addParam(propertyCode);
	}
	private void updateDevicePropDescCmd(int propertyCode) {
		processLoadedProperty(sendCommandNew(getDevicePropDescCmd(propertyCode)));
	}
	private void processLoadedProperty(PtpCommand cmd) 
	{
			if (cmd != null && cmd.isDataOk()){
				cmd.incomingData().parse();
				
				PtpProperty property;
				int propCode = cmd.incomingData().nextU16();
				if (!_ptpProperties.containsKey(propCode)){
					//Log.i(TAG, String.format("+++ Creating new property %#04x", propCode));
					property = new PtpProperty();
					_ptpProperties.put(Integer.valueOf(propCode), property);
				}
				else {
					property = _ptpProperties.get(propCode);
					//Log.i(TAG, String.format("+++ Property already in list %#04x", propCode));
				}
				property.parse(cmd.incomingData());
				sendPtpServiceEvent(PtpServiceEventType.PropDescUpdated, property);
				
				switch(property.getPropertyCode())
				{
					case PtpProperty.LiveViewStatus:
						_isLiveViewEnabled = (Integer)property.getValue() == 1;
						break;
				}
			}
	}
	
	private PtpCommand getVendorPropCodesCmd(){
		return new PtpCommand(PtpCommand.GetVendorPropCodes);
	}
	
	private PtpCommand getEventCmd(){
		return new PtpCommand(PtpCommand.GetEvent);
	}
	private PtpCommand setDevicePropValueCommand(int propertyCode, Object value){
		//Log.i(TAG, "setDevicePropValueCommand");
		PtpProperty property = _ptpProperties.get(propertyCode);
		if (property != null){
			Buffer buf = new Buffer(new byte[_usbWriteEp.getMaxPacketSize()]);
			PtpPropertyValue.setNewPropertyValue(buf, property.getDataType(), value);
			return new PtpCommand(PtpCommand.SetDevicePropValue)
				.addParam(propertyCode)
				.setCommandData(buf.getOfssetArray());
		}
		else
			return null;
	}
	private void setDevicePropValue(int propertyCode, Object value, boolean updateProperty) {
		
		final PtpCommand cmd = setDevicePropValueCommand(propertyCode, value);
		if (cmd != null){
			PtpCommand fCmd = sendCommandNew(cmd);
			if (fCmd != null && fCmd.isResponseOk()){
				//Log.d(TAG, "setDevicePropValue finished");
				if (updateProperty)
					updateDevicePropDescCmd(propertyCode);
			}
		}
//		else
//			Log.i(TAG, "property not found");
	}
	
	public void setDevicePropValueCmd(final int propertyCode, final Object value){
		setDevicePropValueCmd(propertyCode, value, true);
	}
	public void setDevicePropValueCmd(final int propertyCode, final Object value, final boolean updateProperty){
		if (_isPtpDeviceInitialized) {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						
						setDevicePropValue(propertyCode, value, updateProperty);
					}
				}).start();
		}
		
	}
	
	protected PtpCommand getThumbCmd(int objectId){
		return new PtpCommand(PtpCommand.GetThumb)
		.addParam(objectId);
	}
	protected PtpCommand getObjectInfoCmd(int objectId){
		return new PtpCommand(PtpCommand.GetObjectInfo)
		.addParam(objectId);
	}
	private void loadNewObjectInfo(final int objectId){
		// TODO : testing object info without thread
				PtpObjectInfo obj = getObjectInfo(objectId);
				sendPtpServiceEvent(PtpServiceEventType.ObjectAdded, obj);
	}
	
	private PtpCommand getStorageIdsCommand(){
		return new PtpCommand(PtpCommand.GetStorageIDs);
	}
	private PtpCommand getStorageInfoCommand(int storageId){
		return new PtpCommand(PtpCommand.GetStorageInfo)
			.addParam(storageId);
	}
	private PtpCommand getObjectHandlesCommand(int storageId){
		return new PtpCommand(PtpCommand.GetObjectHandles)
			.addParam(storageId);
	}
	private void getStorageIds() {
		PtpCommand cmd = sendCommandNew(getStorageIdsCommand());
		if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
			return;
		cmd.incomingData().parse();
		int count = cmd.incomingData().nextS32();
		for(int i = 1; i <= count; i++ ){
			getStorageInfo(cmd.incomingData().nextS32());
		}
		
	}
	private void getStorageIdsCmd() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				getStorageIds();
			}
		}).start();
	}
	private void getStorageInfo(final int storageId){
			PtpCommand cmd = sendCommandNew(getStorageInfoCommand(storageId));
			if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
				return;
			PtpStorageInfo storage;
			if (_ptpStorages.containsKey(storageId)){
				storage = _ptpStorages.get(storageId);
				storage.updateInfo(cmd.incomingData());
			}
			else {
				storage = new PtpStorageInfo(storageId, cmd.incomingData());
				_ptpStorages.put(storageId, storage);
			}
			
	}
	private void getObjectHandles(final int storageId){
			//Log.d(MainActivity.TAG, String.format("load object handles: %#04x", storageId));
			PtpCommand cmd = sendCommandNew(getObjectHandlesCommand(storageId));
			if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
				return;
			cmd.incomingData().parse();
			int count = cmd.incomingData().nextS32();
			for(int i = 1; i <= count; i++){
				getObjectInfo(cmd.incomingData().nextS32());
			}
	}
	
	private PtpObjectInfo getObjectInfo(final int objectId){
		PtpObjectInfo obj = null;
			PtpCommand cmd = sendCommandNew(getObjectInfoCmd(objectId));
			if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
				return null;
			cmd.incomingData().parse();
			int storageId = cmd.incomingData().nextS32();
			PtpStorageInfo storage = _ptpStorages.get(storageId);
			if (storage.objects.containsKey(objectId)) {
				obj = storage.objects.get(objectId);
				obj.parse(cmd.incomingData());
			}
			else {
				obj = new PtpObjectInfo(objectId, cmd.incomingData());
				storage.objects.put(objectId, obj);
			}
			switch (obj.objectFormatCode) {
			case 0x3000:
			case 0x3801:
				// TODO : save thumb
				File file = new File(_sdramSavingLocation + "/.dslrthumbs");
				if (!file.exists())
					file.mkdir();
				file = new File(file, obj.filename + ".jpg");
				//Log.d(TAG, "DSLR thumb location: " + file.getAbsolutePath());
				if (!file.exists()) {
					
					//Log.d(TAG, "No DSLR thumb found, downloading from camera: " + file.getAbsolutePath());
					cmd = sendCommandNew(getThumbCmd(objectId));
					if ((cmd == null) || (cmd != null && !cmd.isDataOk()))
						return null;
					obj.thumb = BitmapFactory.decodeByteArray(cmd.incomingData().data(), 12, cmd.incomingData().data().length - 12);

					//Log.d(TAG, "Saving DSLR image thumb: " + file.getAbsolutePath());
					FileOutputStream fOut;
					try {
						fOut = new FileOutputStream(file);
						obj.thumb.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
						fOut.flush();
						fOut.close();
						obj.thumb.recycle();
					} catch (Exception e) {
					}
					
				}
				//Log.d(MainActivity.TAG, "thumb loaded");
				break;

			}
			sendPtpServiceEvent(PtpServiceEventType.ObjectAdded, obj);
			//Log.d(MainActivity.TAG, String.format("object id: %#04x format: %#04x", objectId, obj.objectFormatCode));
		return obj;
	}
	public void loadObjectInfos(){
		Log.d(TAG, "load object infos");
		if (!_ptpObjectsLoaded){
			for(PtpStorageInfo info : _ptpStorages.values()){
				getObjectHandles(info.storageId);
			}
			_ptpObjectsLoaded = true;
			sendPtpServiceEvent(PtpServiceEventType.ObjectInfosLoaded, null);
		}
	}

	protected PtpCommand getPartialObjectCmd(PtpObjectInfo objectInfo, int maxSize){
		return getPartialObjectCmd(objectInfo, maxSize, null);
	}
	
	protected PtpCommand getPartialObjectCmd(PtpObjectInfo objectInfo, int maxSize, 
			GetPartialObjectProgress progressListener){
		GetPartialObjectProcessor processor = new GetPartialObjectProcessor(objectInfo);
		processor.setProgressListener(progressListener);
		return new PtpCommand(PtpCommand.GetPartialObject)
			.addParam(objectInfo.objectId)
			.addParam(0)
			.addParam(maxSize)
			.setFinalProcessor(processor);
	}
	private PtpCommand getLargeThumbCommand(int objectId){
		return new PtpCommand(PtpCommand.GetLargeThumb)
			.addParam(objectId);
	}
	public PtpCommand getLargeThumb(int objectId){
		PtpCommand cmd = sendCommandNew(getLargeThumbCommand(objectId));
		return cmd;
	}
	private PtpCommand getPreviewImageCommand(int objectId, int quality, int packetSize){
		return new PtpCommand(PtpCommand.GetPreviewImage)
			.addParam(objectId)
			.addParam(quality)
			.addParam(packetSize)
			.setFinalProcessor(new GetPreviewImageProcessor());
	}
	
	private void getPreviewImage(final int objectId, final int quality, final int packetSize){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
					PtpCommand cmd = sendCommandNew(getPreviewImageCommand(objectId, quality, packetSize));
					if (cmd != null && cmd.isDataOk()){
						
					}
			}
		}).start();
	}
	
	private File getObjectSaveFile(String objectName){
		//File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "DSLR");
		File folder = new File(_sdramSavingLocation);
		if (!folder.exists()) {
			Log.d(TAG, "Make dir: " + folder.mkdir());
		}
		
		
		
		int dotposition= objectName.lastIndexOf(".");
		String ext = objectName.substring(dotposition + 1, objectName.length());


		File f = new File(folder, String.format("%s%04d.%s", _sdramPicturePrefix, _sdramPictureNumbering, ext));
		
		_sdramPictureNumbering++;
		//saveApplicationPreferences();

		Log.d(TAG, "File name: " + f);
		
		return f;
	}
	
	private void getPictureFromSdram(int objectId) {
		try {
			final ImageObjectHelper helper = new ImageObjectHelper();
			helper.galleryItemType = ImageObjectHelper.PHONE_PICTURE;
			PtpCommand cmd = sendCommandNew(getObjectInfoCmd(objectId));
					if (cmd != null && cmd.isDataOk()){
						helper.objectInfo = new PtpObjectInfo(objectId, cmd.incomingData());
						helper.file = getObjectSaveFile(helper.objectInfo.filename);
						sendPtpServiceEvent(PtpServiceEventType.GetObjectFromSdramInfo, helper);
						cmd = sendCommandNew(getThumbCmd(objectId));
							if (cmd != null && cmd.isDataOk()){
								helper.saveThumb(cmd.incomingData());
								sendPtpServiceEvent(PtpServiceEventType.GetObjectFromSdramThumb, helper);
								cmd = sendCommandNew(getPartialObjectCmd(helper.objectInfo, 0x100000,
										new GetPartialObjectProcessor.GetPartialObjectProgress() {
											
											@Override
											public void onProgress(int offset) {
												helper.progress = offset;
												sendPtpServiceEvent(PtpServiceEventType.GetObjectFromSdramProgress, helper);
											}
										}));
									if (cmd != null && cmd.isDataOk()){
										GetPartialObjectProcessor processor = (GetPartialObjectProcessor)cmd.finalProcessor();
										if (helper.savePictureData(processor.pictureData())) {
											// TODO : gps
											if (_addGpsLocation) {
												addGpsLocation(helper.file);
											}
											else
												runMediaScanner(helper.file);
											
										}
										sendPtpServiceEvent(PtpServiceEventType.GetObjectFromSdramFinished, helper);
									}
								}
							}
						}
		finally {
			if (_eventThread != null){
				_eventThread.setPauseEventListener(false);
			}
		}
	}
	private void getPictureFromSdramCmd(final int objectId){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				getPictureFromSdram(objectId);
			}
		}).start();
	}
	
	private void runMediaScanner(File file) {
	    Uri contentUri = Uri.fromFile(file);
	    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
	    mediaScanIntent.setData(contentUri);
	    sendBroadcast(mediaScanIntent);		
	}
	public void changeCameraModeCmd(){
		if (_isPtpDeviceInitialized){
		PtpProperty property = getPtpProperty(PtpProperty.ExposureProgramMode);
		if (property != null)
			changeCameraModeCmd(property.isWritable());
		}
	}
	public void changeCameraModeCmd(boolean cameraMode) {
		if (_isPtpDeviceInitialized){
			int mode = cameraMode ? 0 : 1;
			PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.ChangeCameraMode)
			.addParam(mode));
			if (cmd != null && cmd.isResponseOk())
				updateDevicePropDescCmd(PtpProperty.ExposureProgramMode);
		}
	}
	public void changeCameraMode(final boolean cameraMode) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					changeCameraModeCmd(cameraMode);
				}
			}).start();
	}

	private boolean _isFocusStacking = false;
	private int _focusStackingNumber = 3;
	private int _focusStackingStep = 15;
	private int _focusStackingDirection = 1;
	private int _focusStackingPictureNo = 1;
	private boolean _focusSdcard = true;
	
	private void nextFocusStackingImage(){
		_focusStackingNumber -= 1;
		if (_focusStackingNumber > 0) {

			_focusStackingPictureNo += 1;
			// set focus to af-s
			Log.i(TAG, "Focus to af-s");
			setDevicePropValue(PtpProperty.AfModeSelect, 0, true);
			// enter live view
			Log.i(TAG, "Start live view");
			startLiveView();
			// move focus
			Log.i(TAG, "MFDrive");
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
			}
			seekFocus(_focusStackingDirection, _focusStackingStep);
			
//			if (_focusStackingDirection == 2)
//				seekFocus(_focusCurrent + _focusStackingStep);
//			else
//				seekFocus(_focusCurrent - _focusStackingStep);
			// end live view
			Log.i(TAG, "Stop live view");
			endLiveView();
			
			// set MF focus
			setDevicePropValue(PtpProperty.AfModeSelect, 0x0004, true);

			sendPtpServiceEvent(PtpServiceEventType.FocusBktNextImage, "Focus bracketing " + _focusStackingPictureNo);
			
			// capture image
			if (_focusSdcard) {
				initiateCaptureCmd(false);
				//sendCommandNew(initiateCaptureCommand());
			}
			else {
				initiateCaptureRecInSdramCmd(false);
				//sendCommandNew(initiateCaptureRecInSdramCommand());
			}
			
		}
		else {
			_isFocusStacking = false;
			// set focus to af-s
			setDevicePropValue(PtpProperty.AfModeSelect, 0, true);
			// enter live view
			startLiveViewDisplay();
		}
	}
	
	public void focusBracketing(final int number, final int focusStep, final int direction, final boolean sdCard) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				focusStacking(number, focusStep, direction, sdCard);
			}
		}).start();
	}
	
	private void focusStacking(int number, int focusStep, int direction, boolean sdCard) {
		_isFocusStacking = true;
		
		_focusStackingNumber = number;
		_focusStackingDirection = direction;
		_focusStackingStep = focusStep;
		_focusSdcard = sdCard;
		_focusStackingPictureNo = 1;
		
		Log.i(TAG, "Focus stacking, number: " + number + " step: " + focusStep + " direction: " + direction);
		// todo : focus stacking
		// end the live view
		Log.i(TAG, "Stop live view");
		endLiveViewDisplay();
		sendPtpServiceEvent(PtpServiceEventType.FocusBktNextImage, "Focus bracketing " + _focusStackingPictureNo);
		
			// set focus mode to MF
			Log.i(TAG, "Focus to MF");
			setDevicePropValue(PtpProperty.AfModeSelect, 0x0004, true);
			// take picture
			Log.i(TAG, "Capture image");

			if (_focusSdcard) {
				initiateCaptureCmd();
				//sendCommandNew(initiateCaptureCommand());
			}
			else {
				initiateCaptureRecInSdramCmd();
				//sendCommandNew(initiateCaptureRecInSdramCommand());
			}
//			if (_focusSdcard)
//				initiateCaptureCmd();
//			else
//				initiateCaptureRecInSdramCmd();
			
	}
	
	public void enterLiveViewAtStart(){
		if (_enterLiveViewAtStart) {
			Log.i(TAG, "Entering live view at start");
			//PtpProperty property = getPtpProperty(PtpProperty.RecordingMedia);
			//if (property != null) {
				Log.i(TAG, "Setting recording media to sdram");
				setDevicePropValue(PtpProperty.RecordingMedia, 0x0001, true);
				
				Log.i(TAG, "Starting live view");
				switch (startLiveView()){
					case PtpResponse.OK:
						createLiveViewThread();
						sendPtpServiceEvent(PtpServiceEventType.LiveViewStart, null);
						break;
			}
			//}
		}
	}
	
	private int startLiveView() {
		PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.StartLiveView));
		if (cmd != null){
			boolean again = false;
			do {
				cmd = sendCommandNew(new PtpCommand(PtpCommand.DeviceReady));
				if (cmd != null) {
					switch(cmd.getResponseCode()) {
						case PtpResponse.OK:
							Log.i(TAG, "Live view start ok");
							return PtpResponse.OK;
						case PtpResponse.DeviceBusy:
							Log.i(TAG, "Live view start busy");
							try {
								Thread.sleep(20);
							} catch (InterruptedException e) {
							}
							again = true;
							break;
						default:
							Log.i(TAG, "Live view start error");
							return 0;
					}
				}
			} while (again);
		}
		else
			Log.i(TAG, "Live view start error");
		return 0;
	}
	
	private void startLiveViewDisplay(){
		switch (startLiveView()){
		case PtpResponse.OK:
			createLiveViewThread();
			sendPtpServiceEvent(PtpServiceEventType.LiveViewStart, null);
			break;
	}
	}
	public void startLiveViewCmd(){
		if (_isPtpDeviceInitialized){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					startLiveViewDisplay();
				}
			}).start();
		}
	}
	private int endLiveView(){
		PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.EndLiveView));
		if (cmd != null){
			boolean again = false;
			do {
				cmd = sendCommandNew(new PtpCommand(PtpCommand.DeviceReady));
				if (cmd != null) {
					switch(cmd.getResponseCode()) {
						case PtpResponse.OK:
							Log.i(TAG, "Live view end ok");
							return PtpResponse.OK;
						case PtpResponse.DeviceBusy:
							Log.i(TAG, "Live view end busy");
							try {
								Thread.sleep(20);
							} catch (InterruptedException e) {
							}
							again = true;
							break;
						default:
							Log.i(TAG, "Live view end error");
							return 0;
					}
				}
			}while(again);
		}
		else
			Log.i(TAG, "Live view end error");
		return 0;
	}
	private void endLiveViewDisplay() {
		switch( endLiveView()) {
		case PtpResponse.OK:
			if (_liveViewImgThread != null && _liveViewImgThread.isAlive()){
				stopThread(_liveViewImgThread);
				_liveViewImgThread = null;
			}
			_isMovieRecordingEnabled = false;
			sendPtpServiceEvent(PtpServiceEventType.LiveViewEnd, null);
			sendPtpServiceEvent(PtpServiceEventType.MovieRecordingEnd, null);
			break;
	}
	}
	public void endLiveViewCmd(){
		if (_isPtpDeviceInitialized){
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					endLiveViewDisplay();
				}
			}).start();
		}
	}
	public void startMovieRecCmd() {
		if (!_isMovieRecordingEnabled){
			PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.StartMovieRecInCard));
			if (cmd != null && cmd.isResponseOk()) {
				_isMovieRecordingEnabled = true;
				sendPtpServiceEvent(PtpServiceEventType.MovieRecordingStart, null);
			}
		}
	}
	public void stopMovieRecCmd() {
		PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.EndMovieRec));
		if (cmd != null && cmd.isResponseOk()) {
			_isMovieRecordingEnabled = false;
			sendPtpServiceEvent(PtpServiceEventType.MovieRecordingEnd, null);
		}
		
	}
	public void startMfDrive(final boolean up, final int amount){
		if (_isPtpDeviceInitialized){
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					PtpCommand mfDriveCmd = new PtpCommand(PtpCommand.MfDrive);
					if (up)
						mfDriveCmd.addParam(0x00000002);
					else
						mfDriveCmd.addParam(0x00000001);
					if (amount < 0)
						mfDriveCmd.addParam(0);
					else if (amount > 32767)
						mfDriveCmd.addParam(32767);
					else
						mfDriveCmd.addParam(amount);
					PtpCommand cmd = sendCommandNew(mfDriveCmd);
							if (cmd != null && cmd.isDataOk()){
								
							}
				}
			}).start();
		}
	}
	private PtpCommand getMfDriveCommand(int direction, int amount) {
		return new PtpCommand(PtpCommand.MfDrive)
			.addParam(direction)
			.addParam(amount);
	}
	private int driveFocusToEnd(int direction, int step) {
		boolean again = true;
		PtpCommand cmd;
			cmd = sendCommandNew(getMfDriveCommand(direction, step));
			if (cmd != null) {
				switch(cmd.getResponseCode()) {
					case PtpResponse.OK:
						// do a deviceready command
						again = true;
						while(again) {
							cmd = sendCommandNew(new PtpCommand(PtpCommand.DeviceReady));
							if (cmd != null) {
								switch (cmd.getResponseCode()){
									case PtpResponse.DeviceBusy:
										// we do another deviceready
										// do some pause
										try {
											Thread.sleep(20);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										break;
									default:
										// for all other cases assume there is an error
										return cmd.getResponseCode();
								}
							}
							else
								return 0;
						}
						break;
					default:
						return cmd.getResponseCode();
				}
			}
			else return 0;
		return 0;
	}
	
	private int mfDriveMeassure(){
		Log.i(TAG, "Driving to one end");
		_focusMin = 1;
		int max = 0;
		int res;
		boolean again = true;

		while (again) {
			res = driveFocusToEnd(0x00000001, 4000);
			switch(res) {
			case 0:
				Log.i(TAG, "Undefined error");
				return 0;
			case PtpResponse.OK:
				Log.i(TAG, "OK, doing another run");
				// do another run
				break;
			case PtpResponse.MfDriveStepEnd:
				Log.i(TAG, "MfDriveStepEnd reached");
				// reached the end, stop the loop
				again = false;
				break;
			case PtpResponse.MfDriveStepInsufficiency:
				// stop
				Log.i(TAG, "Drive to end MfDriveStepInsufficiency");
				again = false;
				break;
			default:
				Log.i(TAG, "Other errors: " + res);
				return 0;
			}
		}
		
		again = true;
		int step = 50;
		int retry = 0;
		Log.i(TAG, "Starting first meassure run");
		while (again) {
			Log.i(TAG, "Step " + step);
			res = driveFocusToEnd(0x00000002, step);
			switch(res) {
			case 0:
				Log.i(TAG, "Undefined error");
				return 0;
			case PtpResponse.OK:
				retry = 0;
				max += step;
				Log.i(TAG, "OK, doing another run value: " + max);
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// do another run
				break;
			case PtpResponse.MfDriveStepEnd:
				retry += 1;
				Log.i(TAG, "MfDriveStepEnd reached value: " + max + " retrys: " + retry);
				if (retry > 20) {
					// reached the end, stop the loop
					Log.i(TAG, "MfDriveStepEnd reached value: " + max + " retrys: " + retry);
					again = false;
				}
				break;
			case PtpResponse.MfDriveStepInsufficiency:
				// stop
				if (retry > 0) { 
					Log.i(TAG, "Drive to end MfDriveStepInsufficiency value: " + max + " step: " + step + " retrys: " + retry);
					again = false;
				}
				else {
					Log.i(TAG, "Drive to end MfDriveStepInsufficiency value: " + max + " step: " + step);
					step += 5;
					if (step > 100)
						again = false;
				}
				break;
			default:
				Log.i(TAG, "Other errors: " + res);
				return 0;
			}
		}
		
		again = true;
		// just to be sure
		res = driveFocusToEnd(0x00000002, 32767);
		
		// move the focus in oposite direction for max value and calculate the rest after
		res = driveFocusToEnd(0x00000001, max - 200);
		max -= 200;
		step = _focusMin;
		retry = 0;
		if (res == PtpResponse.OK || res == PtpResponse.MfDriveStepEnd) {
			Log.i(TAG, "Starting second meassure run");
			while (again) {
				res = driveFocusToEnd(0x00000001, step);
				switch(res) {
				case 0:
					Log.i(TAG, "Undefined error");
					return 0;
				case PtpResponse.OK:
					retry = 0;
					max += step;
					Log.i(TAG, "OK, doing another run value: " + max);
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// do another run
					break;
				case PtpResponse.MfDriveStepEnd:
					retry += 1;
					Log.i(TAG, "MfDriveStepEnd reached value: " + max + " retrys: " + retry);
					if (retry > 20) {
						Log.i(TAG, "MfDriveStepEnd reached value: " + max);
						// reached the end, stop the loop
						again = false;
					}
					break;
				case PtpResponse.MfDriveStepInsufficiency:
					// stop
					if (retry > 0) {
						Log.i(TAG, "Drive to end MfDriveStepInsufficiency value: " + max + " step: " + step + " retrys: " + retry);
						again = false;
					}
					else {
						step += 1;
						_focusMin = step;
						Log.i(TAG, "Drive to end MfDriveStepInsufficiency value: " + max + " step: " + step);
						if (step > 50)
							again = false;
					}
					break;
				default:
					Log.i(TAG, "Other errors: " + res);
					return 0;
				}
			}
		}
		else return 0;
		
//		cmd = sendCommandNew(new PtpCommand(PtpCommand.DeviceReady));
//		if (cmd != null) {
//			Log.i(TAG, "Device ready: " + cmd.getResponseCode());
//		}
		Log.i(TAG, "Focus min: " + _focusMin + " max: " + max);
		
		return max;

	}
	public void meassureMfDrive(){
		Log.i(TAG, "Starting focus meassure");
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (_liveViewImgThread != null)
					_liveViewImgThread.setPause(true);
				
				int focusMax = mfDriveMeassure();
				
				if (_liveViewImgThread != null)
					_liveViewImgThread.setPause(false);
				
				if (focusMax != 0) {
					_focusMax = focusMax;
					_focusMaxSet = true;
					_focusCurrent = 0;
					_isFocusInPlace = true;
					saveLensFocusRange();
					
					sendPtpServiceEvent(PtpServiceEventType.FocusMaxSet, _focusMax);
				}
				
			}
		}).start();
	}
	
	public void seekFocusMin(){
		if (_focusMaxSet) {
			int res = driveFocusToEnd(0x00000001, 32767);
			_focusCurrent = 0;
			_isFocusInPlace = true;
		}
	}
	public void seekFocusMax() {
		if (_focusMaxSet) {
			int res = driveFocusToEnd(0x00000002, 32767);
			_focusCurrent = _focusMax;
			_isFocusInPlace = true;
		}
		
	}
	
	public int seekFocus(int direction, int amount) {
		Log.i(TAG, "Seek focus direction: " + direction + " step: " + amount);
		boolean again = false;
		int retry = 0;
		int res = 0;
		do {
			res = driveFocusToEnd(direction, amount);
			switch(res) {
			case PtpResponse.OK:
				Log.i(TAG, "Seek focus OK");
				again = false;
				break;
			case PtpResponse.MfDriveStepEnd:
				retry += 1;
				Log.i(TAG, "Seek focus MfDriveStepEnd retry: " + retry);
				if (retry < 5)
					again = true;
				else
					again = false;
				break;
			case PtpResponse.MfDriveStepInsufficiency:
				Log.i(TAG, "Seek focus MfDriveStepInsufficiency retry: " + retry);
				if (retry > 0) {
					again = false;
				}
				else {
					retry += 1;
					again = true;
				}
				break;
			default:
				Log.i(TAG, "Seek focus other: " + res);
				again = false;
				break;
			}
			
		} while (again) ;
		
		return res;
	}
	
	public void seekFocus(int position){
		boolean again = false;
		int retry = 0;
		
		do {
			Log.i(TAG, "Seek focus current: " + _focusCurrent + " new focus: " + position);
			if (_focusMaxSet && _isFocusInPlace) {
				// 0x00000002 == 0
				// 0x00000001 == max
				int res;
				int dif = _focusCurrent - position;
				if (Math.abs(dif) > _focusMin) {
					if (dif > 0) {
						res = driveFocusToEnd(0x00000001, Math.abs(dif)); 
					}
					else {
						res = driveFocusToEnd(0x00000002, Math.abs(dif));
					}
					switch(res) {
						case PtpResponse.OK:
							Log.i(TAG, "Ok");
							_focusCurrent = position;
							again = false;
							break;
						case PtpResponse.MfDriveStepEnd:
							Log.i(TAG, "DriveStepEnd");
							retry += 1;
							if (retry < 5)
								again = true;
							else
								again = false;
							break;
						case PtpResponse.MfDriveStepInsufficiency:
							Log.i(TAG, "DriveStepInsufficiency");
							if (retry > 0) {
								if (_focusCurrent > position)
									_focusCurrent = 0;
								else
									_focusCurrent = _focusMax;
								again = false;
							}
							break;
						default:
							again = false;
							break;
					}
				}
				
			}
		} while (again);
	}
	
	private void getLiveViewImageCmd() {
		if (_isPtpDeviceInitialized) {
			PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.GetLiveViewImage));
				if (cmd != null && cmd.isDataOk()){
					if (lvo == null)
						lvo = new PtpLiveViewObject(_usbDevice);
					lvo.setBuffer(cmd.incomingData());
					sendPtpServiceEvent(PtpServiceEventType.LiveViewObject, lvo);
				}
			}
	}
	public void changeAfAreaCmd(int x, int y){
		PtpCommand cmd = sendCommandNew(new PtpCommand(PtpCommand.ChangeAfArea)
				.addParam(x)
				.addParam(y));
		if (cmd != null && cmd.isDataOk()){
			
		}
	}
	
	private boolean _returnToLiveView = false;
	
	private PtpCommand initiateCaptureCommand(){
		
		return new PtpCommand(PtpCommand.InitiateCapture);
	}
	
	private void checkReturnToLiveView() {
		Log.i(TAG, "Check return to live view: " + _returnToLiveView);
		if (_waitingCommands.isEmpty() ) {
			if (_returnToLiveView) {
				Log.i(TAG, "Need return to live view");
				startLiveViewDisplay();
				_returnToLiveView = false;
			}
		}
	}
	
	private void checkNeedReturnToLiveView() {
		Log.i(TAG, "Check if we need to return to live view after capture");
		PtpProperty property = getPtpProperty(PtpProperty.LiveViewStatus);
		if (property != null) {
			_returnToLiveView = (Integer)property.getValue() != 0;
			Log.i(TAG, "Need to return to live view: " + _returnToLiveView);
			if (_returnToLiveView) {
				// stop live view
				endLiveViewDisplay();
			}
		}
		else
			_returnToLiveView = false;
	}
	
	public void initiateCaptureCmd() {
		initiateCaptureCmd(true);
	}
	public void initiateCaptureCmd(boolean checkNeedReturnToLV){
		if (checkNeedReturnToLV)
			checkNeedReturnToLiveView();
		if (_bktEnabled)
			//builtinBracketingSerie(_bktStep, _bktDirection, _bktCount, false);
			bracketingSerie(_bktStep, _bktDirection, _bktCount, false);
		else 
			sendCommandNew(initiateCaptureCommand());
	}
	private PtpCommand initiateCaptureRecInSdramCommand(){
		return new PtpCommand(PtpCommand.InitiateCaptureRecInSdram)
			.addParam(0xffffffff);
	}
	public void initiateCaptureRecInSdramCmd(){
		initiateCaptureRecInSdramCmd(true);
	}
	public void initiateCaptureRecInSdramCmd(boolean checkNeedReturnToLV){
		if (checkNeedReturnToLV)
			checkNeedReturnToLiveView();
		if (_bktEnabled)
			//builtinBracketingSerie(_bktStep, _bktDirection, _bktCount, true);
			bracketingSerie(_bktStep, _bktDirection, _bktCount, true);
		else
			sendCommandNew(initiateCaptureRecInSdramCommand());
	}
	private PtpCommand afAndCaptureRecInSdramCommand(){
		return new PtpCommand(PtpCommand.AfAndCaptureRecInSdram);
	}
	public void afAndCaptureRecInSdramCmd(){
		sendCommandNew(afAndCaptureRecInSdramCommand());
	}
	public void startAfDriveCmd(){
		sendCommandNew(new PtpCommand(PtpCommand.AfDrive));
	}
	private PtpCommand getShootCommand(boolean toSdram){
		return toSdram ? initiateCaptureRecInSdramCommand() : initiateCaptureCommand();
	}
	
	private Integer previousBurstNumber = 1;
	private Integer previousStillCaptureMode = 1;
	
	public void toggleBracketing(){
		PtpProperty property = getPtpProperty(PtpProperty.EnableBracketing);
		if (property != null){
			int burstNumber;
			int stillCaptureMode;
			int enableBracketing;
			if ((Integer)property.getValue() == 0){
				// enable bracketing
				burstNumber = 3; // take 3 pictures
				stillCaptureMode = 2; // continous shooting
				enableBracketing = 1;
				// save the previous values
				property = getPtpProperty(PtpProperty.BurstNumber);
				if (property != null)
					previousBurstNumber = (Integer)property.getValue();
				property = getPtpProperty(PtpProperty.StillCaptureMode);
				if (property != null)
					previousStillCaptureMode = (Integer)property.getValue();
			} 
			else {
				// disable bracketing
				enableBracketing = 0;
				burstNumber = previousBurstNumber;
				stillCaptureMode = previousStillCaptureMode;
			}
			setDevicePropValueCmd(PtpProperty.EnableBracketing, enableBracketing);
			setDevicePropValueCmd(PtpProperty.BurstNumber, burstNumber);
			setDevicePropValueCmd(PtpProperty.StillCaptureMode, stillCaptureMode);
		}
	}
	
	//endregion

	//region LiveView
	class LiveViewThread extends MyThreadBase
	{
		private PtpService _service;
		private boolean _pause = false;
		public LiveViewThread(PtpService service){
			_service = service;
			setName("LiveView Thread");
		}
		public void setPause(boolean pause) {
			_pause = pause;
		}
		@Override
		public void run() {
			while(_run){
					if (!_pause) {
						if (_service.getIsUiBind())
							_service.getLiveViewImageCmd();
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
	}
	private void createLiveViewThread(){
		if (_liveViewImgThread == null){
			_liveViewImgThread = new LiveViewThread(this);
			_liveViewImgThread.setRunning(true);
			_liveViewImgThread.start();
		}
	}
	
	//endregion
	
	//region Preferences
	
	private void loadLensFocusRange() {
		
		PtpProperty property = getPtpProperty(PtpProperty.LensSort);
		if (property != null) {
			if (((Integer)property.getValue()) == 1) {
				property = getPtpProperty(PtpProperty.LensId);
    			String lensFocusMaxName = "lens" + property.getValue().toString();
    	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	    	_focusMax = Integer.parseInt(prefs.getString(lensFocusMaxName + "max", "0"));
    	    	_focusMin = Integer.parseInt(prefs.getString(lensFocusMaxName + "min", "0"));
	    		Log.i(TAG, "Focus range loaded: " + lensFocusMaxName + " max: " + _focusMax + " min step: " +_focusMin );
    	    	if (_focusMax > 0) {
    	    		_focusMaxSet = true;
    	    	}
    	    	else {
    	    		_focusMin = 6;
    	    		_focusMax = 2000;
    	    		_focusMaxSet = true;
    	    		_focusCurrent = 0;
    	    		_isFocusInPlace = false;
    	    	}
			}
		}
	}
	
    public void readApplicationPreferences(){
    	Log.d(TAG, "Read preferences");
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //getPreferences(Context.MODE_PRIVATE);
    	
    	_sdramSavingLocation = prefs.getString("sdram_saving_location", _sdramSavingLocation);
    	
    	NumberFormat df = DecimalFormat.getInstance();
    	
    	_sdramPicturePrefix = prefs.getString("sdram_picture_prefix", _sdramPicturePrefix);
    	try{
    		_sdramPictureNumbering =  Integer.parseInt(prefs.getString("sdram_picture_numbering", "1")); //prefs.getInt("sdram_picture_numbering", 1); //
    	} catch (NumberFormatException ex){
    		_sdramPictureNumbering = 1;
    	}
    	
    	try{
				_soundShootingDenominator = df.parse(prefs.getString("sound_shooting_denominator", "10")).doubleValue();
			} catch (ParseException e) {
	    		_soundShootingDenominator = 10;
			}
    	try{
    		_soundShootingThreshold = df.parse(prefs.getString("sound_shooting_threshold", "10")).doubleValue();
    	} catch (ParseException ex){
    		_soundShootingThreshold = 10;
    	}
    	_soundRecordingMedia = prefs.getString("sound_recording_media", _soundRecordingMedia);
    	try{
    		_soundSamplingRate = Integer.parseInt(prefs.getString("sound_sampling_rate", "300"));
    	} catch (NumberFormatException ex){
    		_soundSamplingRate = 300;
    	}
    	
    	try{
    		_bktCount = Integer.parseInt(prefs.getString("custom_bracketing_count", "3"));
    	} catch (NumberFormatException ex){
    		_bktCount = 3;
    	}
    	try{
    		_bktStep = Integer.parseInt(prefs.getString("custom_bracketing_step", "1"));
    	} catch (NumberFormatException ex){
    		_bktStep = 1;
    	}
    	try {
    		_bktDirection = Integer.parseInt(prefs.getString("custom_bracketing_direction", "2"));
    	} catch (NumberFormatException ex){
    		_bktDirection = 2;
    	}
    	_bktEnabled = Boolean.parseBoolean(prefs.getString("custom_bracketing_enabled", "false"));
    	_builtInBracketing = Boolean.parseBoolean(prefs.getString("custom_builtin_brakceting", "false"));
    	
    	_enterLiveViewAtStart = Boolean.parseBoolean(prefs.getString("liveview_at_start", "false"));
    	
    	try {
    		_addGpsLocation = Boolean.parseBoolean(prefs.getString("exif_add_gps", "false"));
    	} catch (Exception e) {
    		
    	}
    	try{
    		_gpsSampleCount = Integer.parseInt(prefs.getString("gps_sample_count", "3"));
    	} catch (NumberFormatException ex){
    		_gpsSampleCount = 3;
    	}
    	try{
    		_gpsSampleInterval = Integer.parseInt(prefs.getString("gps_sample_interval", "2"));
    	} catch (NumberFormatException ex){
    		_gpsSampleInterval = 2;
    	}
    
    	//Log.d(TAG, String.format("%f %f", _soundShootingDenominator, _soundShootingThreshold));
    }
    
    public void defaultApplicationPreferences()
    {
    	File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "DSLR");
    	_sdramSavingLocation = file.getPath();
    	_sdramPicturePrefix = "dslr";
    	_sdramPictureNumbering =  1;
    	
    	_soundShootingDenominator = 10;
    	_soundShootingThreshold = 10;
    	_soundRecordingMedia = "Sdcard";
    	_soundSamplingRate = 300;
    	
    	_bktCount = 3;
    	_bktStep = 1;
    	_bktDirection = 2;
    	_bktEnabled = false;
    	_builtInBracketing = false;
    	
    	_addGpsLocation = false;
    	_gpsSampleInterval = 2;
    	_gpsSampleCount = 3;
    	
    	_enterLiveViewAtStart = false;
    	
    	_focusMax = 2000;
    	_focusMin = 6;
    	_focusMaxSet = true;
    	_focusCurrent = 0;
    	_isFocusInPlace = false;
    }

    private void saveLensFocusRange(){
    	if (_focusMaxSet) {
    		// get the lens id
    		PtpProperty property = getPtpProperty(PtpProperty.LensId);
    		if (property != null) {
    			String lensFocusMaxName = "lens" + property.getValue().toString();
	    		Log.i(TAG, "Saving focus range: " + lensFocusMaxName + " value: " + _focusMax );
    	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //getPreferences(Context.MODE_PRIVATE);
    	    	Editor editor = prefs.edit();
    	    	editor.putString(lensFocusMaxName + "max", String.format("%d", _focusMax));
    	    	editor.putString(lensFocusMaxName + "min", String.format("%d", _focusMin));
    	    	editor.commit();
    		}
		}
    	
    }
    
    public void saveApplicationPreferences(){
    	Log.d(TAG, "Save preferences");
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); //getPreferences(Context.MODE_PRIVATE);
    	Editor editor = prefs.edit();
    	
    	editor.putString("sdram_saving_location", _sdramSavingLocation);
    	editor.putString("sdram_picture_prefix", _sdramPicturePrefix);
    	editor.putString("sdram_picture_numbering", String.format("%d", _sdramPictureNumbering));
    	
    	editor.putString("custom_bracketing_count", String.format("%d", _bktCount));
    	editor.putString("custom_bracketing_step", String.format("%d", _bktStep));
    	editor.putString("custom_bracketing_direction", String.format("%d", _bktDirection));
    	editor.putString("custom_bracketing_enabled", _bktEnabled ? "true" : "false");
    	editor.putString("custom_builtin_brakceting", _builtInBracketing ? "true" : "false");
    	
    	editor.putString("sound_shooting_denominator", String.format("%.1f", _soundShootingDenominator));
    	editor.putString("sound_shooting_threshold", String.format("%.1f", _soundShootingThreshold));
    	editor.putString("sound_sampling_rate", String.format("%d", _soundSamplingRate));
    	editor.putString("sound_recording_media", _soundRecordingMedia);
    	
    	editor.putString("exif_add_gps", _addGpsLocation ? "true" : "false");
    	editor.putString("gps_sample_count", String.format("%d", _gpsSampleCount));
    	editor.putString("gps_sample_interval", String.format("%d", _gpsSampleInterval));
    	
    	editor.putString("liveview_at_start", _enterLiveViewAtStart ? "true" : "false");
    	
    	editor.commit();
    }
    
	//endregion
    
    //region Custom bracketing
    public void builtinBracketingSerie(int step, int direction, int count, boolean toSdram){
		int bktStep = 1;
		setDevicePropValueCmd(PtpProperty.EnableBracketing, 1);
		setDevicePropValueCmd(PtpProperty.BracketingType, 1);
		setDevicePropValueCmd(PtpProperty.StillCaptureMode, 2);
		setDevicePropValueCmd(PtpProperty.BurstNumber, 3);
		
    	PtpProperty prop = getPtpProperty(PtpProperty.ExposureEvStep);
    	if (prop != null){
    		int evStep = (Integer) prop.getValue();
    		switch(step)
    		{
        		case 1:
        			bktStep = evStep == 0 ? 0 : 1;
        			break;
        		case 2:
        			bktStep = evStep == 0 ? 2 : 3;
        			break;
        		case 3:
        			bktStep = evStep == 0 ? 3 : 5;
        			break;
        		case 4:
        			bktStep = evStep == 0 ? 4 : 7;
        			break;
        		case 5:
        			bktStep = 6;
        			break;
        		case 6:
        			bktStep = 7;
        			break;
    		}
    		setDevicePropValueCmd(PtpProperty.AeBracketingStep, bktStep);
    	}

		prop = getPtpProperty(PtpProperty.ExposureBiasCompensation);
		if (prop != null){
			final Vector<?> enums = prop.getEnumeration();
			int currentEv = enums.indexOf(prop.getValue());
			int evIndex = currentEv;

			
			switch(direction) {
			case 0: // negative
				switch(count)
				{
				case 3:
					if ((currentEv - step) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				case 5:
					if ((currentEv - step) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, first run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv - (4 * step)) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - (4 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				case 7:
				case 9:
					if ((currentEv - step) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, first run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv - (4 * step)) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - (4 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv - (7 * step)) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, third run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - (7 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				}
				break;
			case 1: // positive
				switch(count)
				{
				case 3:
					if ((currentEv + step) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				case 5:
					if ((currentEv + step) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, first run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv + (4 * step)) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + (4 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				case 7:
				case 9:
					if ((currentEv + step) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, first run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + step)));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv + (4 * step)) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + (4 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv + (7 * step)) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, third run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + (7 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				}
				break;
			case 2: // both
				Log.d(TAG, "EV: " + enums.get(evIndex));
				switch(count){
				case 3:
					_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, total: %d", count)));
					_waitingCommands.push(getShootCommand(toSdram));
					break;
				case 5:
					_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, first run total: %d", count)));
					_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + step)));
					_waitingCommands.push(getShootCommand(toSdram));
					_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
					_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - step)));
					_waitingCommands.push(getShootCommand(toSdram));
					break;
				case 7:
					_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, total: %d", count)));
					_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv)));
					_waitingCommands.push(getShootCommand(toSdram));
					if ((currentEv + (2 * step)) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + (2 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv - (2 * step)) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, third run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - (2 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				case 9:
					_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, total: %d", count)));
					_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv)));
					_waitingCommands.push(getShootCommand(toSdram));
					if ((currentEv + (3 * step)) < enums.size()){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, second run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + (3 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					if ((currentEv - (3 * step)) >= 0){
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing with builtin, third run total: %d", count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - (3 * step))));
						_waitingCommands.push(getShootCommand(toSdram));
					}
					break;
				}
				break;
			}
			_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, prop.getValue()));
			_waitingCommands.push(new PtpCommand("Custom bracketing with builtin finished"));
		}
		executeWaitingCommands();
    }
    
	public void bracketingSerie(int step, int direction, int count, boolean toSdram){
		if (_builtInBracketing)
			builtinBracketingSerie(step, direction, count, toSdram);
		else {
			PtpProperty prop = getPtpProperty(PtpProperty.ExposureBiasCompensation);
			PtpProperty afProp = getPtpProperty(PtpProperty.AfModeSelect);
			
			// set focus mode to MF
			Log.i(TAG, "Focus to MF");
			setDevicePropValue(PtpProperty.AfModeSelect, 0x0004, true);
			
			if (prop != null){
				final Vector<?> enums = prop.getEnumeration();
				int currentEv = enums.indexOf(prop.getValue());
				int evIndex = currentEv;
				int evCounter = 0;
				switch(direction) {
				case 0: // negative
					do {
						Log.d(TAG, "EV: " + enums.get(evIndex));
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing count: %d total: %d", evCounter+1, count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(evIndex)));
						_waitingCommands.push(getShootCommand(toSdram));
						evCounter++;
						evIndex -= step;
					} while (evIndex >= 0 && evCounter < count);
					break;
				case 1: // positive
					do {
						Log.d(TAG, "EV: " + enums.get(evIndex));
						_waitingCommands.push(new PtpCommand(String.format("Custom bracketing count: %d total: %d", evCounter+1, count)));
						_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(evIndex)));
						_waitingCommands.push(getShootCommand(toSdram));
						evCounter++;
						evIndex += step;
					} while (evIndex < enums.size() && evCounter < count);
					break;
				case 2: // both
					Log.d(TAG, "EV: " + enums.get(evIndex));
					_waitingCommands.push(getShootCommand(toSdram));
					evCounter++;
					int counter = 1;
					do {
						evIndex = counter * step;
						if ((currentEv - evIndex) >= 0 && (currentEv + evIndex) <= enums.size()){
							Log.d(TAG, "EV: " + enums.get(currentEv + evIndex));
							Log.d(TAG, "EV: " + enums.get(currentEv - evIndex));
							_waitingCommands.push(new PtpCommand(String.format("Custom bracketing count: %d total: %d", evCounter+1, count)));
							_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv + evIndex)));
							_waitingCommands.push(getShootCommand(toSdram));
							_waitingCommands.push(new PtpCommand(String.format("Custom bracketing count: %d total: %d", evCounter+2, count)));
							_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, enums.get(currentEv - evIndex)));
							_waitingCommands.push(getShootCommand(toSdram));
						}
						counter++;
						evCounter += 2;
					} while (evCounter < count);
					break;
				}
				_waitingCommands.push(setDevicePropValueCommand(PtpProperty.ExposureBiasCompensation, prop.getValue()));
				_waitingCommands.push(setDevicePropValueCommand(PtpProperty.AfModeSelect, afProp.getValue()));
				_waitingCommands.push(new PtpCommand("Custom bracketing finished"));
			}
			executeWaitingCommands();
		}
	}

    //endregion
	
	//region Sound shooting

	public Runnable smTask = new Runnable() {
		
		@Override
		public void run() {
			double amp = _soundMeter.getAmplitude();
			//double ampEMA = _soundMeter.getAmplutideEMA();
			//Log.d(TAG, String.format("amplitude: %+.1f %+.1f", amp, ampEMA));
			
			_sound_indicator = !_sound_indicator;
			sendPtpServiceEvent(PtpServiceEventType.SoundMonitorIndicator, _sound_indicator);
			sendPtpServiceEvent(PtpServiceEventType.SoundMonitorValue, amp);
			
			if (amp > _soundShootingThreshold){
				if (_soundRecordingMedia.equals("Sdcard"))
					initiateCaptureCmd();
				else
					initiateCaptureRecInSdramCmd();
			}
			if (_isSoundShootingEnabled)
				smHandler.postDelayed(smTask, _soundSamplingRate);
			else
				sendPtpServiceEvent(PtpServiceEventType.SoundMonitorStoped, null);
		}
	};

	public void startSoundMeter()
	{
		if (!_isSoundShootingEnabled) {
			try {
				_soundMeter.start(_soundShootingDenominator);
				_isSoundShootingEnabled = true;
				smHandler.postDelayed(smTask, _soundSamplingRate);
				sendPtpServiceEvent(PtpServiceEventType.SoundMonitorStarted, null);
			} catch (IllegalStateException e) {
			} catch (IOException e) {
			}
		}
	}
	
	public void stopSoundMeter(){
		if (_isSoundShootingEnabled){
			_isSoundShootingEnabled = false;
			_soundMeter.stop();
		}
	}
	
	//endregion
	
	//region Timelapse
	private Handler _timelapseHandler = new Handler();
	private boolean _timelapseRunning = false;
	private boolean _timelapseSdramCapture = false;
	private int _timelapseInterval = 10; // interval in seconds
	private int _timelapseIterations = 10;
	private int _timelapseRemainingIterations = 10;
	
	public boolean getIsTimelapseRunning(){
		return _timelapseRunning;
	}
	public boolean getIsTimelapseSdramCapture(){
		return _timelapseSdramCapture;
	}
	public int getTimelapseInterval(){
		return _timelapseInterval;
	}
	public void setTimelapseInterval(int value){
		_timelapseInterval = value;
	}
	public int getTimelapseIterations(){
		return _timelapseIterations;
	}
	public void setTimelapseIterations(int value){
		_timelapseIterations = value;
	}
	public int getTimelapseRemainingIterations(){
		return _timelapseRemainingIterations;
	}
	private Runnable timelapseTask = new Runnable() {
		
		@Override
		public void run() {
			

			if (_bktEnabled)
				bracketingSerie(_bktStep, _bktDirection, _bktCount, _timelapseSdramCapture);
			else {
				_waitingCommands.push(getShootCommand(_timelapseSdramCapture));
				executeWaitingCommands();
			}
			_timelapseRemainingIterations -= 1;
			sendPtpServiceEvent(PtpServiceEventType.TimelapseEvent, _timelapseRemainingIterations);
			if (_timelapseRemainingIterations == 0){
				if (!_waitingCommands.isEmpty()){
					executeWaitingCommands();
				}
				stopTimelapse();
				return;
			}
			Log.d(TAG, "----- Timelapse event ----- iterations remain: " + _timelapseRemainingIterations);
			
			_timelapseHandler.removeCallbacks(timelapseTask);
			_timelapseHandler.postDelayed(this, _timelapseInterval * 1000);
			//_timelapseHandler.postAtTime(this, start + (((minutes * 60) + seconds + 1) * 1000));
		}
	};
	
	public void startTimelapse(boolean sdramCapture){
		Log.d(TAG, "---- Timelapse started ----");
		_timelapseRunning = true;
		_timelapseSdramCapture = sdramCapture;
		_timelapseRemainingIterations = _timelapseIterations;
		
		_timelapseHandler.removeCallbacks(timelapseTask);
		_timelapseHandler.postDelayed(timelapseTask, 100);
		sendPtpServiceEvent(PtpServiceEventType.TimelapseStarted, null);
	}
	
	public void stopTimelapse(){
		Log.d(TAG, "---- Timelapse stoped ----");
		_timelapseHandler.removeCallbacks(timelapseTask);
		_timelapseRunning = false;
		sendPtpServiceEvent(PtpServiceEventType.TimelapseStoped, null);
	}
	//endregion
	
	
	private int getSampleInterval(){
		return 1000 * 60 * _gpsSampleInterval;
	}
	
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > getSampleInterval();
	    boolean isSignificantlyOlder = timeDelta < -getSampleInterval();
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}
	
	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	private boolean needLocationUpdate() {
		if (_lastLocation != null) {
			long utcTime = System.currentTimeMillis();
			if ((utcTime - _lastLocation.getTime()) < getSampleInterval())
					return false;
		}
		return true;
	}
	

	private void addGpsLocation(File file) {
		
		synchronized (_gpsList) {
			Log.d(TAG, "Getting exif gps data");
			if (needLocationUpdate()) {
				_gpsList.push(file);
				_gpsHandler.sendEmptyMessage(0);
		        
			}
			else {
		    	NativeMethods.getInstance().setGPSExifData(file.getAbsolutePath(), _lastLocation.getLatitude(), _lastLocation.getLongitude(), _lastLocation.getAltitude());
		    	runMediaScanner(file);
			}
		}
	}
}
