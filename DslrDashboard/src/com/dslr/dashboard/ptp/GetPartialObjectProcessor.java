// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.ptp;

import android.util.Log;

public class GetPartialObjectProcessor extends PtpCommandFinalProcessor{

	private static String TAG = "GetPartialObjectProcessor";
	
	public interface GetPartialObjectProgress {
		public void onProgress(int offset);
	}
	
	private GetPartialObjectProgress _progressListener;
	public void setProgressListener(GetPartialObjectProgress listener){
		_progressListener = listener;
	}
	private PtpObjectInfo _objectInfo;
    private int _offset = 0;
    private int _maxSize = 0x100000;
	private byte[] _objectData;
	
	public byte[] pictureData(){
		return _objectData;
	}
	public PtpObjectInfo objectInfo(){
		return _objectInfo;
	}
	public int maxSize(){
		return _maxSize;
	}
	public GetPartialObjectProcessor(PtpObjectInfo objectInfo){
		this(objectInfo, 0x100000);
	}
	public GetPartialObjectProcessor(PtpObjectInfo objectInfo, int maxSize){
		_objectInfo = objectInfo;
		_maxSize = maxSize;
		_objectData = new byte[_objectInfo.objectCompressedSize];
	}
	
	@Override
	public boolean doFinalProcessing(PtpCommand cmd) {
		Log.d(TAG, "GetPartialObject processing");
        boolean result = false;
        
        int count = cmd.incomingData().getPacketLength() - 12;
        
        if (cmd.isResponseOk())
        {
            Log.d(TAG, "Offset: " +_offset + " count: " + count + " size: " + _objectInfo.objectCompressedSize);
            
        	System.arraycopy(cmd.incomingData().data(), 12, _objectData, _offset, count);
            if ((_offset + count) < _objectInfo.objectCompressedSize)
            {
                _offset += count;
                cmd.params().set(1, _offset);
                if ((_offset + _maxSize) > _objectInfo.objectCompressedSize)
                	cmd.params().set(2, _objectInfo.objectCompressedSize - _offset);
                else
                	cmd.params().set(2, _maxSize);
                
                result = true;
            }
            else
            	_offset += count;
            if (_progressListener != null){
            	_progressListener.onProgress(_offset);
            }
        }
        return result;
    }			
}
