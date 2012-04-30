// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public enum BitmapManager {
	INSTANCE;

	private final String TAG = "ThumbLoader";
	
	private final Map<String, SoftReference<Bitmap>> cache;
	private final ExecutorService pool;
	private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	private Bitmap placeholder;

	BitmapManager() {
		cache = new HashMap<String, SoftReference<Bitmap>>();
		pool = Executors.newFixedThreadPool(5);
	}

	public void setPlaceholder(Bitmap bmp) {
		placeholder = bmp;
	}

	public Bitmap getBitmapFromCache(String url) {
			if (cache.containsKey(url)) {
				return cache.get(url).get();
			}
		return null;
	}

	public void clearCach(){
		cache.clear();
		imageViews.clear();
	}
	public void queueJob(final String url, final ImageView imageView) {
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String tag = imageViews.get(imageView);
				if (tag != null && tag.equals(url)) {
					Bitmap bmp = (Bitmap)msg.obj;
					if (bmp != null) {
						imageView.setImageBitmap(bmp);
						cache.put(url, new SoftReference<Bitmap>(bmp));
					} else {
						imageView.setImageBitmap(placeholder);
					}
				}
			}
		};

		pool.submit(new Runnable() {
			@Override
			public void run() {
				final Bitmap bmp = downloadBitmap(url);
				Message message = Message.obtain();
				
				message.obj = bmp;
				//Log.d(TAG, "Item downloaded: " + url);

				handler.sendMessage(message);
			}
		});
	}

	public void loadBitmap(final String url, final ImageView imageView) {
		
		Bitmap bitmap = getBitmapFromCache(url);

		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		} else {
			imageViews.put(imageView, url);
			imageView.setImageBitmap(placeholder);
			queueJob(url, imageView);
		}
	}

	private File getThumbFilePath( String fileName, String ext){
		File file = new File(fileName);
		File f = new File(file.getParent() + "/.thumb");
		if (!f.exists())
			f.mkdir();
		String fname = file.getName();
		if (!ext.isEmpty())
			return new File(f, fname + "." + ext);
		else
			return new File(f, fname);
	}
	
	private Bitmap downloadBitmap(String url) {
		String thumbPath = "";
		Bitmap bitmap = null;
		if (url.contains(".dslrthumbs"))
			thumbPath = url;
		else {
			File thumbFile = getThumbFilePath(url, "png");
			if (thumbFile.exists())
				thumbPath = thumbFile.getAbsolutePath();
			else{
				thumbFile = getThumbFilePath(url, "jpg");
				if (thumbFile.exists())
					thumbPath = thumbFile.getAbsolutePath();
			}
		}
		if (!thumbPath.equals(""))
		{
			final int IMAGE_MAX_SIZE = 30000; // 1.2MP
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			Bitmap tmp = BitmapFactory.decodeFile(thumbPath, options);

	        int scale = 1;
	        while ((options.outWidth * options.outHeight) * (1 / Math.pow(scale, 2)) > IMAGE_MAX_SIZE) {
	            scale++;
	        }
	        tmp = null;
	        if (scale > 1) {
	            scale--;
		        options = new BitmapFactory.Options();
		        options.inSampleSize = scale;
		        bitmap = BitmapFactory.decodeFile(thumbPath, options);
	        }
	        else
	        	bitmap = BitmapFactory.decodeFile(thumbPath);
		}
        return bitmap;
	}
}