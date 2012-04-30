// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import com.dslr.dashboard.helper.CheckableImageView;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dslr.dashboard.helper.*;
import com.dslr.dashboard.ptp.PtpLiveViewObject;
import com.dslr.dashboard.ptp.PtpProperty;

public class MainActivity extends Activity {
	
    
    
	public final static int GALLERY_VIEW_IMAGE = 1;
	
	private static String TAG = "MainActivity";
	
	private final int MAINLAYOUTVIEW = 1;
	private final int LIVEVIEWLAYOUTVIEW = 3;
	private final int BRACKETINGLAYOUTVIEW = 2;
	private final int IMAGELAYOUTVIEW = 4;
	private final int PREFERENCESLAYOUTVIEW = 5;
	private final int ABOUTLAYOUTVIEW = 6;
	
	private CheckableImageView btn_modeSwitch;
	private LinearLayout layout_modeBar;
	private boolean isModeBarVisible = false;
	private RelativeLayout _parentLayout;
	
	private DslrHelper _dslrHelper;
	
	private CheckableImageView btnMainLayout, btnLiveViewLayout, btnBracketingLayout, btnImageLayout, btnPreferencesLayout, btnAboutLayout;
	private ImageView btnStopPtp;
	
	private LayoutMain mainLayout;
	private LayoutTimelapse layoutTimelapse;
	private LayoutBracketing bracketingLayout;
	private LayoutImagePreview imagePreviewLayout;
	private LayoutPreferences preferencesLayout;
	private LayoutAbout aboutLayout;
	
	private int visibleView = ABOUTLAYOUTVIEW;
	private DslrLayout _activeLayout = null;
	private boolean _checkForUsb = false;
	
	private static boolean _isTablet = false;
	
	public static boolean getIsTablet(){
		return _isTablet;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        int lastView = ABOUTLAYOUTVIEW;
        
        Log.d(TAG, "onCreate");
        
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        _parentLayout = (RelativeLayout)findViewById(R.id.mainLayout);

        // not working on all android devices
//        Configuration config = getResources().getConfiguration();
//        try {
//        	_isTablet = config.smallestScreenWidthDp >= 800;
//        } catch (NoSuchFieldError e){
//        	Display display = getWindowManager().getDefaultDisplay();
//        	Point size = new Point();
//        	int x = 800;
//        	try {
//        		display.getSize(size);
//        		x = size.x;
//        	} catch (NoSuchMethodError ex) {
//        		x = display.getWidth();
//        	}
//        	_isTablet = x >= 1280;
//        }
        Display display = getWindowManager().getDefaultDisplay();
        _isTablet = display.getWidth() >= 1280;
        Log.d(TAG, "WidthDp: " + display.getWidth() + "   HeightDp: " + display.getHeight());
        
//        Log.d(TAG, "SamllestScreenWidthDp: " + config.smallestScreenWidthDp);
        
        _dslrHelper = new DslrHelper(this);
        
       
        btn_modeSwitch = (CheckableImageView)findViewById(R.id.btn_modeswitch);
        btn_modeSwitch.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				toggleBarVisibility();
			}
		});
        layout_modeBar = (LinearLayout)findViewById(R.id.layout_modebar);
        
        if (!_isTablet)
        	layout_modeBar.setVisibility(View.GONE);
        
        btnMainLayout = (CheckableImageView)findViewById(R.id.btn_main);
        btnLiveViewLayout = (CheckableImageView)findViewById(R.id.btn_timelapse);
        btnBracketingLayout = (CheckableImageView)findViewById(R.id.btn_bracketing);
        btnImageLayout = (CheckableImageView)findViewById(R.id.btn_imagepreview);
        btnPreferencesLayout = (CheckableImageView)findViewById(R.id.btn_preferences);
        btnAboutLayout = (CheckableImageView)findViewById(R.id.btn_about);
        btnStopPtp = (ImageView)findViewById(R.id.img_stopptp);
        
        toggleLayoutBtnEnabled(false);
        
        btnMainLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(MAINLAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        btnLiveViewLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(LIVEVIEWLAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        btnBracketingLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(BRACKETINGLAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        btnImageLayout.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(IMAGELAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        btnPreferencesLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(PREFERENCESLAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        btnAboutLayout.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				handleLayoutChange(ABOUTLAYOUTVIEW);
				toggleBarVisibility();
			}
		});
        
        btnStopPtp.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!_ptpService.getIsUsbDevicePresent()) {
					_ptpService.searchForUsb();
					toggleBarVisibility();
				}
				else {
					doUnbindService(true);
					finish();
				}
			}
		});
        
        Log.d(TAG, "Creating layouts");
        
        mainLayout = new LayoutMain(this, null);
        layoutTimelapse = new LayoutTimelapse(this, null);
        if (_isTablet){
        	bracketingLayout = mainLayout.getBracketingLayout();
        	if (bracketingLayout == null) {
        		bracketingLayout = new LayoutBracketing(this, null);
        		_isTablet = false;
        	}
        }
        else
        	bracketingLayout = new LayoutBracketing(this, null);
        imagePreviewLayout = new LayoutImagePreview(this, null);
        preferencesLayout = new LayoutPreferences(this, null);
        aboutLayout = new LayoutAbout(this, null);
        
        Log.d(TAG, "Providing dslrHelper");
        
        mainLayout.setDslrHelper(_dslrHelper);
        layoutTimelapse.setDslrHelper(_dslrHelper);
        bracketingLayout.setDslrHelper(_dslrHelper);
        imagePreviewLayout.setDslrHelper(_dslrHelper);
        preferencesLayout.setDslrHelper(_dslrHelper);
        aboutLayout.setDslrHelper(_dslrHelper);
        
        mainLayout.setOnShowHIdeMainMenuListener(new LayoutMain.ShowHideMainMenuViews() {
			
			@Override
			public void onShowHideMainMenuViews(boolean show) {
				int hide = show ? View.VISIBLE : View.GONE;
				if (getIsTablet()){
					layout_modeBar.setVisibility(hide);
				}
				else {
					if (!show){
						layout_modeBar.setVisibility(View.GONE);
						btn_modeSwitch.setVisibility(View.GONE);
					}
					else {
						btn_modeSwitch.setVisibility(View.VISIBLE);
						btn_modeSwitch.setChecked(false);
					}
				}
			}
		});
        
    	showNewLayout(lastView);
    	toggleLayoutButtonChecked(lastView, true);
        
        
    }
    
    @Override
    protected void onDestroy() {
    	Log.d(TAG, "onDestroy");
    	super.onDestroy();
    }
    
    @Override
    protected void onStart() {
    	Log.d(TAG, "onStart");
    	checkUsbIntent(getIntent());
    	doBindService();
    	super.onStart();
    }
    
    @Override
    protected void onStop() {
    	Log.d(TAG, "onStop");
    	if (_ptpService != null)
    	{
    		if (!_ptpService.getIsUsbDeviceInitialized())
    			stopService(new Intent(MainActivity.this, PtpService.class));
    	}
    	doUnbindService();
    	super.onStop();
    }
    
    @Override
    protected void onResume() {
    	Log.d(TAG, "onResume");
    	super.onResume();
    	
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	checkUsbIntent(intent);
    	super.onNewIntent(intent);
    }
    
    private void checkUsbIntent(Intent intent) {
    	if (intent != null ) {
    		Bundle extras = intent.getExtras();
    		if (extras != null) {
    			if (extras.containsKey("UsbAttached"))
    				_checkForUsb = true;
    		}
    	}
    }
    
    @Override
    protected void onPause() {
    	Log.d(TAG, "onPause");
    	super.onPause();
    }
    
	private void handleLayoutChange(int newLayout)
	{
		if (newLayout != visibleView)
		{
			if (_activeLayout != null)
				_activeLayout.layoutDeactived();
			toggleLayoutButtonChecked(visibleView, false);
			_parentLayout.removeAllViews();
			
			showNewLayout(newLayout);
			toggleLayoutButtonChecked(newLayout, true);
			
			if (_activeLayout != null)
				_activeLayout.layoutActivated();
			
			visibleView = newLayout;
		}
	}
	
	private void toggleLayoutBtnEnabled(boolean isEnabled){
        btnMainLayout.setEnabled(isEnabled);
        btnLiveViewLayout.setEnabled(isEnabled);
        btnBracketingLayout.setEnabled(isEnabled);
		
	}
	private void showNewLayout(int view)
	{
		switch(view)
		{
		case MAINLAYOUTVIEW:
			_parentLayout.addView(mainLayout);
			_activeLayout = mainLayout;
			break;
		case LIVEVIEWLAYOUTVIEW:
			_parentLayout.addView(layoutTimelapse);
			_activeLayout = layoutTimelapse;
			break;
		case BRACKETINGLAYOUTVIEW:
			_parentLayout.addView(bracketingLayout);
			_activeLayout = bracketingLayout;
			break;
		case IMAGELAYOUTVIEW:
			_parentLayout.addView(imagePreviewLayout);
			_activeLayout = imagePreviewLayout;
			break;
		case PREFERENCESLAYOUTVIEW:
			_parentLayout.addView(preferencesLayout);
			_activeLayout = preferencesLayout;
			break;
		case ABOUTLAYOUTVIEW:
			_parentLayout.addView(aboutLayout);
			_activeLayout = aboutLayout;
			break;
		}
	}
	
	private void toggleLayoutButtonChecked(int view, boolean isChecked){
		switch(view)
		{
		case MAINLAYOUTVIEW:
			btnMainLayout.setChecked(isChecked);
			break;
		case LIVEVIEWLAYOUTVIEW:
			btnLiveViewLayout.setChecked(isChecked);
			break;
		case BRACKETINGLAYOUTVIEW:
			btnBracketingLayout.setChecked(isChecked);
			break;
		case IMAGELAYOUTVIEW:
			btnImageLayout.setChecked(isChecked);
			break;
		case PREFERENCESLAYOUTVIEW:
			btnPreferencesLayout.setChecked(isChecked);
			break;
		case ABOUTLAYOUTVIEW:
			btnAboutLayout.setChecked(isChecked);
			break;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
				finish();
				return true;
			default:
					return super.onKeyDown(keyCode, event);
		}
	}
	private void toggleBarVisibility()
	{
		if (!_isTablet){
		if (isModeBarVisible)
		{
			isModeBarVisible = false;
			btn_modeSwitch.setChecked(false);
			layout_modeBar.setVisibility(View.GONE);
		}
		else {
			isModeBarVisible = true;
			btn_modeSwitch.setChecked(true);
			layout_modeBar.setVisibility(View.VISIBLE);
		}
		}
	}
    

	PtpService.PtpServiceEventListener ptpServiceEventListener = new PtpService.PtpServiceEventListener() {
		
		@Override
		public void onServiceEvent(final PtpServiceEventType eventType, final Object eventData) {
			//Log.d(TAG, "PtpService event: " + eventType);
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					switch(eventType)
					{
						case DeviceInitialized:
							Log.i(TAG, "Device initialized");
							_dslrHelper.loadDslrProperties(_ptpService.getUsbDevice().getVendorId(), _ptpService.getUsbDevice().getProductId());
							toggleLayoutBtnEnabled(true);
							if (visibleView == ABOUTLAYOUTVIEW)
								handleLayoutChange(MAINLAYOUTVIEW);
							aboutLayout.ptpDeviceConnected();
							break;
						case DeviceClosed:
							handleLayoutChange(ABOUTLAYOUTVIEW);
							toggleLayoutBtnEnabled(false);
							aboutLayout.ptpDeviceDisConnected();
							break;
						case PropDescUpdated:
							mainLayout.updatePtpProperty((PtpProperty)eventData);
							layoutTimelapse.updatePtpProperty((PtpProperty)eventData);
							bracketingLayout.updatePtpProperty((PtpProperty)eventData);
							break;
						case LiveViewObject:
							if (visibleView == MAINLAYOUTVIEW)
								mainLayout.updateLiveViewObject((PtpLiveViewObject)eventData);
							break;
						case SoundMonitorIndicator:
							bracketingLayout.soundMonitorIndicatorEvent((Boolean)eventData);
							break;
						case SoundMonitorStarted:
							bracketingLayout.soundMonitorStarted();
							break;
						case SoundMonitorStoped:
							bracketingLayout.soundMonitorStoped();
							break;
						case SoundMonitorValue:
							bracketingLayout.soundMonitorValueEvent((Double)eventData);
							break;
						case GetObjectFromSdramStart:
							//handleLayoutChange(IMAGELAYOUTVIEW);
						case GetObjectFromSdramInfo:
						case GetObjectFromSdramThumb:
						case GetObjectFromSdramProgress:
						case GetObjectFromSdramFinished:
						case CaptureCompleteInSdram:
						case ObjectAdded:
						case ObjectInfosLoaded:
							imagePreviewLayout.handlePtpServiceEvent(eventType, eventData);
							break;
						case TimelapseStarted:
						case TimelapseStoped:
						case TimelapseEvent:
							layoutTimelapse.processTimelapseEvents(eventType, eventData);
							break;
						case CommandNotification:
							Toast.makeText(MainActivity.this, (CharSequence) eventData, 3).show(); 
							break;
						case MovieRecordingStart:
							mainLayout.updateMovieRecStatus();
							break;
						case MovieRecordingEnd:
							mainLayout.updateMovieRecStatus();
							break;
						case FocusMaxSet:
							mainLayout.focusMaxSet();
							break;
						case FocusBktNextImage:
							Toast.makeText(MainActivity.this, (String)eventData, 4).show();
							break;
					}
				}
			});
		}
	};
	
	private void updatePtpProperties(){
		if (_ptpService.getIsPtpDeviceInitialized()){
			// ptp device is initialized, update the ptp controls
			
			this.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Object[] properties = _ptpService.getPtpProperties();
					
					for(int i =0; i < properties.length; i++){
						mainLayout.updatePtpProperty((PtpProperty)properties[i]);
						layoutTimelapse.updatePtpProperty((PtpProperty)properties[i]);
						bracketingLayout.updatePtpProperty((PtpProperty)properties[i]);
					}
				}
			});
		}
	}
    //region Service binding/unbinding
    private boolean _mIsBound = false;
    private PtpService _ptpService = null;
    
	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because we want a specific service implementation that
	    // we know will be running in our own process (and thus won't be
	    // supporting component replacement by other applications).
		//Log.d(TAG, "doBindService");
	    bindService(new Intent(MainActivity.this, PtpService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	    _mIsBound = true;
	    
	}

	void doUnbindService() {
		doUnbindService(false);
	}
	void doUnbindService(Boolean stopService) {
		Log.d(TAG, "doUnbindService: " + _mIsBound);
		if (_mIsBound) {
			_ptpService.setIsUiBind(false);
			_ptpService.setOnPtpServiceEventListener(null);
			if (stopService)
				_ptpService.stopPtpService();
	        unbindService(serviceConnection);
	        _mIsBound = false;
	    }
	}
	private void checkIntent() {
    	Intent intent = getIntent();
    	if (intent != null && intent.hasExtra("UsbAttached")) {
    		_ptpService.searchForUsb();
    	}
	}
    private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Service disconnected");
			_ptpService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "Service connected");
			
			_ptpService = ((PtpService.MyBinder)service).getService();
			_ptpService.setIsUiBind(true);
			_dslrHelper.setPtpService(_ptpService);
			
			_ptpService.setOnPtpServiceEventListener(ptpServiceEventListener);
			
			// notify the view that we have a ptpservice
			mainLayout.ptpServiceSet(true);
			layoutTimelapse.ptpServiceSet(true);
			bracketingLayout.ptpServiceSet(true);
			imagePreviewLayout.ptpServiceSet(true);
			preferencesLayout.ptpServiceSet(true);
			aboutLayout.ptpServiceSet(true);

			updatePtpProperties();
			
			if (_dslrHelper.getPtpService().getIsPtpDeviceInitialized()) {
				if (visibleView == ABOUTLAYOUTVIEW)
					handleLayoutChange(MAINLAYOUTVIEW);
			}
			
			startService(new Intent(MainActivity.this, PtpService.class));

			if (_ptpService.getIsPtpDeviceInitialized()) {
				toggleLayoutBtnEnabled(true);
			}
			
			if (_checkForUsb)
				_ptpService.searchForUsb();
		}
	};
    //endregion
    
}