// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.ptp;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

public class PtpCommunicator {

	private static String TAG = "PtpCommunicator";
	
	private final PtpSession _session;
	private UsbDeviceConnection _connection;
	private UsbEndpoint _writeEp;
	private UsbEndpoint _readEp;
	private boolean _isInitialized = false;
	
	public void initCommunicator(
			UsbDeviceConnection connection,
			UsbEndpoint writeEp,
			UsbEndpoint readEp){
		_connection = connection;
		_writeEp = writeEp;
		_readEp = readEp;
		_isInitialized = true;
	}
	public PtpCommunicator(PtpSession session){ 
		_session = session;
	}
	
	public boolean isInitialized(){
		return _isInitialized;
	}
	
	protected synchronized void processCommand(PtpCommand cmd) throws Exception{
		if (_isInitialized)
		{
			boolean needAnotherRun = false;
			do {
				//Log.d(TAG, "+++ Sending command to device");
				int bytesCount = 0;
				int retry = 0;
				byte[] data;
				synchronized (_session) {
					data = cmd.getCommandPacket(_session.getNextSessionID());
				}
				while(true) {
					bytesCount = _connection.bulkTransfer(_writeEp, data, data.length , 200);
					if (bytesCount != data.length) {
						Log.d(TAG, "+++ Command packet sent, bytes: " + bytesCount);
						retry += 1;
						if (retry > 2)
							throw new Exception("writen length != packet length");
					}
					else
						break;
				}
				if (cmd.hasSendData()){
					data = cmd.getCommandDataPacket();
					bytesCount = _connection.bulkTransfer(_writeEp, data, data.length, 200);
					//Log.d(TAG, "+++ Command data packet sent, bytes: " + bytesCount);
					if (bytesCount != data.length)
						throw new Exception("writen length != packet length");
					// give the device a bit time to process the data
					Thread.sleep(100);
				}
				data = new byte[_readEp.getMaxPacketSize()];
				while (true){
					bytesCount = _connection.bulkTransfer(_readEp, data, data.length, 200);
	//				if (bytesCount < 1)
	//					Log.d(TAG, "+++ Packet received, bytes: " + bytesCount);
					if (bytesCount > 0)
					{
						if (!cmd.newPacket(data, bytesCount)){
							//data = null;
							if (cmd.hasResponse()) {
								needAnotherRun = cmd.weFinished();
								break;
							}
						}
					}
				}
	//			if (needAnotherRun)
	//				Thread.sleep(300);
			} while (needAnotherRun);
		}
	}
}