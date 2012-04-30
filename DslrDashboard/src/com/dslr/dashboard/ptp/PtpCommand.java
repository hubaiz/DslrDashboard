package com.dslr.dashboard.ptp;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import android.util.Log;

public class PtpCommand implements Callable<PtpCommand> {

	private static String TAG = "PtpCommand";
	
	public interface OnCommandFinishedListener {
		public void onCommandFinished(PtpCommand command);
	}
	
	private OnCommandFinishedListener _cmdFinishedListener = null;
	
	private int _commandCode;
	protected ArrayList<Integer> _params;
	private int _sessionId = 0;
	private boolean _hasSessionId = false;
	private Buffer _buffer;
	private byte[] _commandData = null;
	private boolean _hasSendData = false;
	private PtpCommunicator _communicator;
	private PtpCommandFinalProcessor _finalProcessor = null;
	private String _notificationMsg;
	
	public int commandCode(){
		return _commandCode;
	}
	public PtpCommandFinalProcessor finalProcessor(){
		return _finalProcessor;
	}
	public ArrayList<Integer> params(){
		return _params;
	}
	public int sessionId(){
		return _sessionId;
	}
	public boolean hasSessionId(){
		return _hasSessionId;
	}
	public boolean hasSendData(){
		return _hasSendData;
	}
	public PtpCommunicator communicator(){
		return _communicator;
	}
	public String getNotificatinMsg(){
		return _notificationMsg;
	}
	
	public PtpCommand addParam(int param) {
		_params.add(param);
		return this;
	}
	public PtpCommand setCommandData(byte[] commandData){
		_commandData = commandData;
		_hasSendData = true;
		return this;
	}
	public PtpCommand setOnCommandFinishedListener(OnCommandFinishedListener listener){
		_cmdFinishedListener = listener;
		return this;
	}
	public PtpCommand setFinalProcessor(PtpCommandFinalProcessor processor){
		_finalProcessor = processor;
		return this;
	}
	public FutureTask<PtpCommand> getTask(PtpCommunicator communicator){
		_communicator = communicator;
		return new FutureTask<PtpCommand>(this);
	}
	
	public PtpCommand(int commandCode){
		_commandCode = commandCode;
		_params = new ArrayList<Integer>();
		_buffer = new Buffer();
	}
	
	public PtpCommand(String notification){
		_notificationMsg = notification;
		_commandCode = 0;
	}
	@Override
	public PtpCommand call() throws Exception {
		//Log.d(TAG, "Before sending command: " + _communicator);
		try
		{
		_communicator.processCommand(this);
		}
		catch (Exception e)
		{
			Log.d(TAG, e.getMessage());
			throw e; 
		}
		if (_cmdFinishedListener != null)
			_cmdFinishedListener.onCommandFinished(this);
		//Log.d(TAG, String.format("Command finished: %#04x", _commandCode));
		return this;
	}

	
	protected byte[] getCommandPacket(int sessionId){
		_sessionId = sessionId;
		_hasSessionId = true;
		int bufLen = 12 + (4 * _params.size());
		byte[] data = new byte[bufLen];
		_buffer.wrap(data);
		_buffer.put32(bufLen); // size of the packet
		_buffer.put16(1); // this is a command packet
		_buffer.put16(_commandCode); // command code
		_buffer.put32(_sessionId); // session id
		for(int i = 0; i < _params.size(); i++){
			_buffer.put32(_params.get(i));
		}
		return data;
	}
	
	protected byte[] getCommandDataPacket(){
		if (_hasSessionId && _hasSendData){
			_buffer.wrap(new byte[12 + _commandData.length]);
			_buffer.put32(_buffer.length()); // size will be later set;
			_buffer.put16(2); // this is a data packet
			_buffer.put16(_commandCode); // the command code
			_buffer.put32(_sessionId); // session id
			// copy the data byte[] to the packet
			System.arraycopy(_commandData, 0, _buffer.data(), 12, _commandData.length);
			return _buffer.data();
		}
		else 
			return null;
	}
	

	private boolean _hasData = false;
	private boolean _hasResponse = false;
	private Buffer _incomingData = null;
	private Buffer _incomingResponse = null;
	
	private boolean _needMoreBytes = false;
	private int _bytesCount = 0;
	private int _packetLen = 0;

	private byte[] _tmpData = null;
	
	public boolean hasData(){
		return _hasData;
	}
	public boolean hasResponse(){
		return _hasResponse;
	}
	public boolean isResponseOk(){
		return _hasResponse ? _incomingResponse.getPacketCode() == PtpResponse.OK : false;
	}
	public int getResponseCode() {
		return _hasResponse ? _incomingResponse.getPacketCode() : 0;
	}
	public boolean isDataOk(){
		return _hasData ? isResponseOk() : false;
	}
	public Buffer incomingData(){
		return _incomingData;
	}
	public Buffer incomingResponse(){
		return _incomingResponse;
	}
	public int responseParam(){
		if (_hasResponse){
			_incomingResponse.parse();
			return _incomingResponse.nextS32();
		}
		return 0;
	}
	
	protected boolean newPacket(byte[] packet, int size){
		if (_needMoreBytes){
			System.arraycopy(packet, 0, _tmpData, _bytesCount, size);
			_bytesCount += size;
			if (_bytesCount >= _packetLen){
				_needMoreBytes = false;
				processPacket();
				return false;
			}
			else
				return true;
		}
		else {
			_buffer.wrap(packet);
			_packetLen = _buffer.getPacketLength();
			
			int packetType = _buffer.getPacketType();
			switch(packetType){
				case 2: // data
					_incomingData = new Buffer(new byte[_packetLen]);
					_tmpData = _incomingData.data();
					break;
				case 3: // response
					_incomingResponse = new Buffer(new byte[_packetLen]);
					_tmpData = _incomingResponse.data();
					break;
			}
			System.arraycopy(packet, 0, _tmpData, 0, size);
			
			if (_packetLen > size) {// we need more bytes
				_needMoreBytes = true;
				_bytesCount = size;
				return true;
			}
			else {
				processPacket();
				return false;
			}
		}
	}
	
	protected void processPacket(){
		_buffer.wrap(_tmpData);
		switch (_buffer.getPacketType()) {
		case 2:
			//Log.d(TAG, "--- Incoming data packet");
			_hasData = true;
			break;
		case 3:
			//Log.d(TAG, "--- Incoming response packet");
			_hasResponse = true;
			//Log.d(TAG, "--- Response code " + Integer.toHexString(_incomingResponse.getPacketCode()));
			break;
		default:
			break;
		}
	}

	private void reset(){
		_hasSessionId = false;
		_hasData = false;
		_hasResponse = false;
		_needMoreBytes = false;
		_incomingData = null;
		_incomingResponse = null;
		_tmpData = null;
	}
	
	public boolean weFinished()
	{
		boolean result = doFinalProcessing();
		if (!result){
			// we finished
		}
		else {
			// we need another run, reset evrything
			reset();
		}
		return result;
	}
	protected boolean doFinalProcessing(){
		return _finalProcessor == null ? false : _finalProcessor.doFinalProcessing(this);
	}
	
	
    public static final int GetDeviceInfo               = 0x1001;
    public static final int OpenSession                 = 0x1002;
    public static final int CloseSession                = 0x1003;
    public static final int GetStorageIDs               = 0x1004;
    public static final int GetStorageInfo              = 0x1005;
    public static final int GetNumObjects               = 0x1006;
    public static final int GetObjectHandles            = 0x1007;
    public static final int GetObjectInfo               = 0x1008;
    public static final int GetObject                   = 0x1009;
    public static final int GetThumb                    = 0x100a;
    public static final int DeleteObject                = 0x100b;
    public static final int SendObjectInfo              = 0x100c;
    public static final int SendObject                  = 0x100d;
    public static final int InitiateCapture             = 0x100e;
    public static final int FormatStore                 = 0x100f;
    public static final int ResetDevice                 = 0x1010;
    public static final int SelfTest                    = 0x1011;
    public static final int SetObjectProtection         = 0x1012;
    public static final int PowerDown                   = 0x1013;
    public static final int GetDevicePropDesc           = 0x1014;
    public static final int GetDevicePropValue          = 0x1015;
    public static final int SetDevicePropValue          = 0x1016;
    public static final int ResetDevicePropValue        = 0x1017;
    public static final int TerminateOpenCapture        = 0x1018;
    public static final int MoveObject                  = 0x1019;
    public static final int CopyObject                  = 0x101a;
    public static final int GetPartialObject            = 0x101b;
    public static final int InitiateOpenCapture         = 0x101c;
    
    public static final int InitiateCaptureRecInSdram	= 0x90c0;
    public static final int AfDrive						= 0x90c1;
    public static final int ChangeCameraMode			= 0x90c2;
    public static final int DeleteImagesInSdram			= 0x90c3;
    public static final int GetLargeThumb				= 0x90c4;
    public static final int GetEvent					= 0x90c7;
    public static final int DeviceReady					= 0x90c8;
    public static final int SetPreWbData				= 0x90c9;
    public static final int GetVendorPropCodes			= 0x90ca;
    public static final int AfAndCaptureRecInSdram		= 0x90cb;
    public static final int GetPicCtrlData				= 0x90cc;
    public static final int SetPicCtrlData				= 0x90cd;
    public static final int DeleteCustomPicCtrl			= 0x90ce;
    public static final int GetPicCtrlCapability		= 0x90cf;
    public static final int GetPreviewImage				= 0x9200;
    public static final int StartLiveView				= 0x9201;
    public static final int EndLiveView					= 0x9202;
    public static final int GetLiveViewImage			= 0x9203;
    public static final int MfDrive						= 0x9204;
    public static final int ChangeAfArea				= 0x9205;
    public static final int AfDriveCancel				= 0x9206;
    public static final int InitiateCaptureRecInMedia   = 0x9207;
    public static final int StartMovieRecInCard		    = 0x920a;
    public static final int EndMovieRec                 = 0x920b;
    
    public static final int MtpGetObjectPropsSupported  = 0x9801;
    public static final int MtpGetObjectPropDesc        = 0x9802;
    public static final int MtpGetObjectPropValue       = 0x9803;
    public static final int MtpSetObjectPropValue       = 0x9804;
    public static final int MtpGetObjPropList           = 0x9805;
	
}
