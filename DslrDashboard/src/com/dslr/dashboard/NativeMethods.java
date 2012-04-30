// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.util.ArrayList;

import com.dslr.dashboard.helper.ExifDataHelper;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class NativeMethods {
	private final static String TAG = "NativeMethods";
	
    static {
        System.loadLibrary("dslrdashboard");
    }
	
    public native Object loadRawImage(String imgPath);
    public native boolean loadRawImageThumb(String imgPath, String thumbPath);
    //public native String[] getExifInfo(String imgPath);
    public native int getExifData(String path, int count, Object obj);
    public native int setGPSExifData(String path, double latitude, double longitude, double altitude);
    
    private static NativeMethods _instance = null;
    
    public static NativeMethods getInstance() {
    	if (_instance == null)
    		_instance = new NativeMethods();
    	return _instance;
    }
    
    public ArrayList<ExifDataHelper> getImageExif(String[] exifNames, String path){
    	ArrayList<ExifDataHelper> helper = new ArrayList<ExifDataHelper>();
    	for(String name : exifNames) {
    		String[] tmp = name.split("@");
    		helper.add(new ExifDataHelper(tmp[0], tmp[1]));
    	}
    	
    	getExifData(path, helper.size(), helper);
    	return helper;
    }
    
	public void exifValueCallback(String test, int index, Object obj) {
		ArrayList<ExifDataHelper> helper = (ArrayList<ExifDataHelper>)obj;
		//Log.d(TAG, "Exif value callback " + test + " index: " + index + " helper test: " + helper.size());
		helper.get(index).mExifValue = test;
	}
	public String exifNameCallback(int index, Object obj) {
		ArrayList<ExifDataHelper> helper = (ArrayList<ExifDataHelper>)obj;
		//Log.d(TAG, "Name callback " + helper.get(index).mExifName + " index: " + index + " helper test: " + helper.size());
		return helper.get(index).mExifName;
	}
    
}
