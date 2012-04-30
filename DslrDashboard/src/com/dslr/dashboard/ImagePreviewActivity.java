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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import com.dslr.dashboard.helper.ExifDataHelper;
import com.dslr.dashboard.imgzoom.DynamicZoomControl;
import com.dslr.dashboard.imgzoom.ImageZoomView;
import com.dslr.dashboard.imgzoom.LongPressZoomListener;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ImagePreviewActivity extends Activity {

	private static String TAG = "ImagePreviewActivity";
	private ListView _exifList;
	private Boolean _isExifVisible = false;
	
	private String[] _exifNames;
	
    /** Image zoom view */
    private ImageZoomView mZoomView;
    private TextView txtLoading;

    /** Zoom control */
    private DynamicZoomControl mZoomControl;

    /** Decoded bitmap image */
    private Bitmap mBitmap = null;

    /** On touch listener for zoom view */
    private LongPressZoomListener mZoomListener;
    private String _imgPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		if (savedInstanceState != null) {
			mBitmap = savedInstanceState.getParcelable("bitmap");
			_imgPath = savedInstanceState.getString("imgPath");
		}
		setContentView(R.layout.imagepreview);

        mZoomControl = new DynamicZoomControl();


        mZoomListener = new LongPressZoomListener(this);
        mZoomListener.setZoomControl(mZoomControl);
        
        mZoomView = (ImageZoomView)findViewById(R.id.zoomview);
        mZoomView.setZoomState(mZoomControl.getZoomState());
        mZoomView.setOnTouchListener(mZoomListener);
        mZoomControl.setAspectQuotient(mZoomView.getAspectQuotient());
        
        mZoomView.setVisibility(View.GONE);
        
        txtLoading = (TextView)findViewById(R.id.txtLoading);
        
        _exifList = (ListView)findViewById(R.id.exifList);
        
        _exifNames = getResources().getStringArray(R.array.exifNames);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		if (mBitmap != null)
			loadImage(null);
		else {
			Intent intent = getIntent();
			if (intent != null){
				Log.d(TAG, "Image from intent");
				Uri data = intent.getData();
				if (data != null) {
					String path = data.getEncodedPath();
					Log.d(TAG, "Image path " + path);
					_imgPath = path;
					loadImage(path);
				}
				else if (intent.hasExtra("data")){
					Log.d(TAG, "Image from bitmap ");
				    mBitmap = BitmapFactory.decodeByteArray(
				            intent.getByteArrayExtra("data"),0,getIntent().getByteArrayExtra("data").length);
				    loadImage(null);
				}
				else {
					Log.d(TAG, "No data in intent");
					loadImage(null);
				}
			}
			else {
				Log.d(TAG, "No Intent");
				loadImage(null);
			}
		}
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}
	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mBitmap != null)
			outState.putParcelable("bitmap", mBitmap);
		if (_imgPath != null && !_imgPath.isEmpty())
			outState.putString("imgPath", _imgPath);
		Log.d(TAG, "onSaveInstanceState");
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
				if (mBitmap != null) {
					mBitmap.recycle();					
					//mBitmap = null;
				}
				finish();
				return true;
			case KeyEvent.KEYCODE_MENU:
				if (_imgPath != null && !_imgPath.isEmpty()){
					
					if (!_isExifVisible) {
						ArrayList<ExifDataHelper> exifs = NativeMethods.getInstance().getImageExif(_exifNames, _imgPath);
						ExifAdapter adapter = new ExifAdapter(this, exifs);
						_exifList.setAdapter(adapter);
					}
					_isExifVisible = !_isExifVisible;
					_exifList.setVisibility(_isExifVisible ? View.VISIBLE : View.GONE);
				}
				return true;
			default:
				return super.onKeyDown(keyCode, event);
		}
	}
	
	private Bitmap logJpeg(String path)   {
	    Log.i(TAG,"loading:"+path);
		final int IMAGE_MAX_SIZE = 8000000; // 1.2MP
		
	    Bitmap bm = null;
	    File file=new File(path);
	    
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try {
        	FileInputStream fis = new FileInputStream(file);
        	BitmapFactory.decodeStream(fis, null, o);
        	fis.close();
        } catch (IOException e) {
        	return null;
        }
        
        int scale = 1;
        while ((o.outWidth * o.outHeight) * (1 / Math.pow(scale, 2)) > IMAGE_MAX_SIZE) {
            scale++;
        }
        Log.i(TAG, "Sample size: " + scale);
        
	    BitmapFactory.Options bfOptions=new BitmapFactory.Options();
	    bfOptions.inSampleSize = scale;
	    bfOptions.inDither=false;                     //Disable Dithering mode
	    bfOptions.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
	    bfOptions.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
	    bfOptions.inTempStorage=new byte[32 * 1024]; 


	    FileInputStream fs=null;
	    try {
	        fs = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
	        //TODO do something intelligent
	        e.printStackTrace();
	    }

	    try {
	        if(fs!=null) 
	        	bm=BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);
	    } catch (IOException e) {
	    	
	    } finally{ 
	        if(fs!=null) {
	            try {
	                fs.close();
	            } catch (IOException e) {
	            	
	            }
	        }
	    }
	    return bm;
	}	
	private void loadImage(final String path){
        new Thread(new Runnable() {
			
			@Override
			public void run() {
				if (path != null && !path.isEmpty()) {
					File f = new File(path);
					if (f.exists()) {
						if (path.substring((path.lastIndexOf(".") + 1), path.length()).toLowerCase().equals("nef"))
							mBitmap = (Bitmap)NativeMethods.getInstance().loadRawImage(path);
						else {
							mBitmap = logJpeg(path);
							
						}
					}
				}
				if (mBitmap != null) {
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							txtLoading.setVisibility(View.GONE);
							mZoomView.setImage(mBitmap);
							mZoomView.setVisibility(View.VISIBLE);
					        resetZoomState();
						}
					});
				}
			}
		}).start();
	}
	
	
    private void resetZoomState() {
        mZoomControl.getZoomState().setPanX(0.5f);
        mZoomControl.getZoomState().setPanY(0.5f);
        mZoomControl.getZoomState().setZoom(1f);
        mZoomControl.getZoomState().notifyObservers();
    }
	
}
