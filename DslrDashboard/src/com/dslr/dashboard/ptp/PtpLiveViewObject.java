// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.ptp;

import android.graphics.RectF;
import android.hardware.usb.UsbDevice;

public class PtpLiveViewObject {

	public static final int FIXED_POINT = 16;
    public static final int ONE = 1 << FIXED_POINT;
    
	private UsbDevice usbDevice;
	
	public LvSizeInfo jpegImageSize;
	LvSizeInfo wholeSize;
	LvSizeInfo displayAreaSize;
	LvSizeInfo displayCenterCoordinates;
	LvSizeInfo afFrameSize;
	LvSizeInfo afFrameCenterCoordinates;
	int noPersons = 0;
	LvSizeInfo[] personAfFrameSize = null;//= new SizeInfo[5];
	LvSizeInfo[] personAfFrameCenterCoordinates = null; // = new SizeInfo[5];
	public RectF[] afRects = null; // = new RectF[6];
	public int selectedFocusArea;
	public int rotationDirection;
	public int focusDrivingStatus;
	public int shutterSpeedUpper;
	public int shutterSpeedLower;
	public int apertureValue;
	public int countDownTime;
	public int focusingJudgementResult;
	public int afDrivingEnabledStatus;
	public int levelAngleInformation;
	public int faceDetectionAfModeStatus;
	public int faceDetectionPersonNo;
	public int afAreaIndex;
	public byte[] data;
	
	// d7000 properties
	public double rolling;
	public boolean hasRolling = false;
	public double pitching;
	public boolean hasPitching = false;
	public double yawing;
	public boolean hasYawing = false;
	
	public int movieRecordingTime;
	public boolean movieRecording = false;
	
	public float ox, oy, dLeft, dTop;
	
	public int imgLen;
	public int imgPos;
	
	public PtpLiveViewObject(UsbDevice device){
		usbDevice = device;
		switch (usbDevice.getProductId()){
		case 0x0429: // d5100
		case 0x0428: // d7000
			noPersons = 35;
			break;
		case 0x0423: // d5000
		case 0x0421: // d90
			noPersons = 5;
			break;
		case 0x041a: // d300
		case 0x0422: // d700
		case 0x0425: // d300s
			noPersons = 0;
			break;
		}
		personAfFrameSize = new LvSizeInfo[noPersons];
		personAfFrameCenterCoordinates = new LvSizeInfo[noPersons];
		afRects = new RectF[noPersons + 1];
		for (int i = 0; i < noPersons; i++) {
			personAfFrameSize[i] = new LvSizeInfo();
			personAfFrameCenterCoordinates[i] = new LvSizeInfo();
		}
		jpegImageSize = new LvSizeInfo();
		wholeSize = new LvSizeInfo();
		displayAreaSize = new LvSizeInfo();
		displayCenterCoordinates = new LvSizeInfo();
		afFrameSize = new LvSizeInfo();
		afFrameCenterCoordinates = new LvSizeInfo();
	}

	public float sDw, sDh;
	
	private Buffer buf = null;
	public void setBuffer(Buffer buffer){
		buf = buffer;
	}
	public void parse(int sWidth, int sHeight){
		if (buf != null){
			buf.parse();
			jpegImageSize.setSize(buf.nextU16(true), buf.nextU16(true));
			
			sDw = (float)sWidth / (float)jpegImageSize.horizontal;
			sDh = (float)sHeight / (float)jpegImageSize.vertical;
			
			//Log.d(MainActivity.TAG, "++ Width: " + jpegImageSize.horizontal + " height: " + jpegImageSize.vertical);
			wholeSize.setSize(buf.nextU16(true), buf.nextU16(true));
			displayAreaSize.setSize(buf.nextU16(true), buf.nextU16(true));
			displayCenterCoordinates.setSize(buf.nextU16(true), buf.nextU16(true));
			afFrameSize.setSize(buf.nextU16(true), buf.nextU16(true));
			afFrameCenterCoordinates.setSize(buf.nextU16(true), buf.nextU16(true));
			
			buf.nextS32(); // reserved
			selectedFocusArea = buf.nextU8();
			rotationDirection = buf.nextU8();
			focusDrivingStatus = buf.nextU8();
			buf.nextU8(); // reserved
			shutterSpeedUpper = buf.nextU16(true);
			shutterSpeedLower = buf.nextU16(true);
			apertureValue = buf.nextU16(true);
			countDownTime = buf.nextU16(true);
			focusingJudgementResult = buf.nextU8();
			afDrivingEnabledStatus = buf.nextU8();
			buf.nextU16(); // reserved
	
	        ox = (float)jpegImageSize.horizontal / (float)displayAreaSize.horizontal;
	        oy = (float)jpegImageSize.vertical / (float)displayAreaSize.vertical;
	        dLeft = ((float)displayCenterCoordinates.horizontal - ((float)displayAreaSize.horizontal / 2));
	        dTop = ((float)displayCenterCoordinates.vertical - ((float)displayAreaSize.vertical / 2));
			
			switch (usbDevice.getProductId()){
			case 0x041a: // d300
			case 0x0422: // d700
			case 0x0425: // d300s
		        CalcCoord(0, dLeft, dTop, afFrameCenterCoordinates, afFrameSize);
		        imgPos = 64 + 12;
				break;
			case 0x0429: // d5100
			case 0x0428: // d7000
				hasRolling = true;
				rolling = ((double)buf.nextS32(true)) / ONE;
				hasPitching = true;
				pitching = ((double)buf.nextS32(true)) / ONE;
				hasYawing = true;
				yawing = ((double)buf.nextS32(true)) / ONE;
				
				movieRecordingTime = buf.nextS32();
				movieRecording = buf.nextU8() == 1;
				faceDetectionAfModeStatus = buf.nextU8();
				faceDetectionPersonNo = buf.nextU8();
				afAreaIndex = buf.nextU8();
				
		        CalcCoord(0, dLeft, dTop, afFrameCenterCoordinates, afFrameSize);
	
				for(int i = 0; i < noPersons; i++){
					personAfFrameSize[i].setSize(buf.nextU16(true), buf.nextU16(true));
					personAfFrameCenterCoordinates[i].setSize(buf.nextU16(true), buf.nextU16(true));
					
					if ((i+1) <= faceDetectionPersonNo)
						CalcCoord(i + 1, dLeft, dTop, personAfFrameCenterCoordinates[i], personAfFrameSize[i]);
				}
				imgPos = 384 + 12;
		        
				break;
			case 0x0423: // d5000
			case 0x0421: // d90
				levelAngleInformation = buf.nextS32(true);
				faceDetectionAfModeStatus = buf.nextU8();
				buf.nextU8(); // reserved
				faceDetectionPersonNo = buf.nextU8();
				afAreaIndex = buf.nextU8();
	
		        CalcCoord(0, dLeft, dTop, afFrameCenterCoordinates, afFrameSize);
				
				for(int i = 0; i < noPersons; i++){
					personAfFrameSize[i].setSize(buf.nextU16(true), buf.nextU16(true));
					personAfFrameCenterCoordinates[i].setSize(buf.nextU16(true), buf.nextU16(true));
					
					if ((i+1) <= faceDetectionPersonNo)
						CalcCoord(i + 1, dLeft, dTop, personAfFrameCenterCoordinates[i], personAfFrameSize[i]);
				}
				imgPos = 128+12;
				break;
			}
			imgLen = buf.data().length - imgPos;
			data = buf.data();
		}
		//System.arraycopy(buf.data(), imgPos, imgData, 0, imgLen);
		
	}
	
    private void CalcCoord(int coord, float dLeft, float dTop, LvSizeInfo afCenter, LvSizeInfo afSize)
    {
    	float left, top, right, bottom;
        left = (((float)afCenter.horizontal - ((float)afSize.horizontal / 2)) - dLeft) * ox;
        top = (((float)afCenter.vertical - ((float)afSize.vertical / 2)) - dTop) * ox;
        if (left < 0)
            left = 0;
        if (top < 0)
            top = 0;
        right = left + ((float)afSize.horizontal * ox);
        bottom = top + ((float)afSize.vertical * oy);
        if (right > jpegImageSize.horizontal)
        	right = jpegImageSize.horizontal;
        if (bottom > jpegImageSize.vertical)
        	bottom = jpegImageSize.vertical;
        //Log.d(MainActivity.TAG, "++ Left: " + left + " top: " + top +" right: " + right + " bottom: " + bottom);
        afRects[coord] = new RectF(left * sDw, top * sDh, right * sDw, bottom * sDh);
    }
	
}
