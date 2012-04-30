// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.dslr.dashboard.ptp.Buffer;
import com.dslr.dashboard.ptp.PtpObjectInfo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageObjectHelper {
	
    public final static int DSLR_PICTURE = 0;
    public final static int PHONE_PICTURE = 1;
	
	private static String TAG = "ImageObjectHelper";
	
	public PtpObjectInfo objectInfo = null;
	public File file = null;
	public int progress = 0;
	public boolean isChecked = false;
	public int galleryItemType = 0;

	public ImageObjectHelper(){
		
	}
	public String getFileExt(String FileName)
    {       
         String ext = FileName.substring((FileName.lastIndexOf(".") + 1), FileName.length());
         return ext.toLowerCase();
    }
	
	
	public boolean tryLoadThumb(String ext) {
		File thumbFile = getThumbFilePath(ext);
		if (thumbFile.exists()){
			if (!ext.equals("ppm")) {
				
				return true;
			}
			else
				return true;
		}
		return false;
	}
	public File getThumbFilePath( String ext){
		File f = new File(file.getParent() + "/.thumb");
		if (!f.exists())
			f.mkdir();
		String fname = file.getName();
		if (!ext.isEmpty())
			return new File(f, fname + "." + ext);
		else
			return new File(f, fname);
	}
	

	public void saveThumb(Buffer data){
		Bitmap bmp = null;
		switch(objectInfo.objectFormatCode){
		case PtpObjectInfo.EXIF_JPEG:
			bmp = BitmapFactory.decodeByteArray(data.data(), 12, data.data().length - 12);
			break;
		case PtpObjectInfo.Undefined:
			bmp = createThumbFromRaw(data);
			break;
		}
		if (bmp != null) {
			FileOutputStream fOut;
			try {
				fOut = new FileOutputStream(getThumbFilePath("jpg"));
				bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
				fOut.flush();
				fOut.close();
				bmp.recycle();
			} catch (Exception e) {
			}
		}
		
	}

	public void deleteImage(){
		if (file.exists())
			file.delete();
		File tf = getThumbFilePath("png");
		if (tf.exists())
			tf.delete();
		tf = getThumbFilePath("jpg");
		if (tf.exists())
			tf.delete();
	}
	
	private Bitmap createThumbFromRaw(Buffer data){
		int stride = ((160 * 24 + 25) & ~25) / 8;
		int[] colors = createColors(160,120,stride, data);
		return Bitmap.createBitmap(colors, 0, stride, 160, 120, Bitmap.Config.ARGB_8888);
	}
	
    private static int[] createColors(int width, int height, int stride, Buffer data) {
    	data.parse();
        int[] colors = new int[stride * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
            	int r = data.nextU8();
            	int g = data.nextU8();
            	int b = data.nextU8();
                int a = 255;
                colors[y * stride + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return colors;
    }
	
	public boolean savePictureData(byte[] data){
		if (file != null){
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(file, false));
				out.write(data);
				return true;
			} catch (Exception e) {
				Log.d(TAG, "File open error");
				return false;
			}
			finally {
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						Log.d(TAG, "Error closing stream");
					}
			}
		}
		else {
			Log.d(TAG, "Error file name not set!");
			return false;
		}
	}
}