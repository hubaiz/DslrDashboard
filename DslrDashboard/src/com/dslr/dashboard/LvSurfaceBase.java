// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import com.dslr.dashboard.ptp.PtpLiveViewObject;
import com.dslr.dashboard.ptp.PtpProperty;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LvSurfaceBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private static final String TAG = "LvSurfaceBase";
	
	private SurfaceHolder mHolder;
	private int mFrameWidth;
	private int mFrameHeight;
	private PtpLiveViewObject mLvo;
	private PtpProperty mLvZoom;
	private LayoutMain.Mode mMode;
	private int mMfDriveStep = 1;
	private Context _context;
	private int _focusCurrent;
	private int _focusMax;
	private int _lvOsdDisplay;
	
	private boolean mThreadRun;
	private final Object _syncRoot = new Object();
	
	public LvSurfaceBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		_context = context;
		mHolder = getHolder();
		mHolder.addCallback(this);
		Log.d(TAG, "Created new " + this.getClass());
	}

	public int getFrameWidth() {
		return mFrameWidth;
	}
	public int getFrameHeight() {
		return mFrameHeight;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "Surface changed");
		
		mFrameWidth = width;
		mFrameHeight = height;
		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Surface created");
		(new Thread(this)).start();
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Surface destroyed");
		mThreadRun = false;
	}

	public void processLvObject(PtpLiveViewObject lvo, PtpProperty lvZoom, LayoutMain.Mode mode, int mfDriveStep, int focusCurrent, int focusMax, int osdDisplay ) {
		synchronized (_syncRoot) {
			mLvo = lvo;
			mLvZoom = lvZoom;
			mMode = mode;
			mMfDriveStep = mfDriveStep;
			_focusCurrent = focusCurrent;
			_focusMax = focusMax;
			_lvOsdDisplay = osdDisplay;
			_syncRoot.notify();
		}
	}
	
	private Bitmap processLvImage(PtpLiveViewObject lvo){
		Bitmap bmp = BitmapFactory.decodeByteArray(lvo.data, lvo.imgPos, lvo.imgLen);
		android.graphics.Bitmap.Config bitmapConfig = bmp.getConfig();
		// set default bitmap config if none
		if(bitmapConfig == null) {
			bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
		}
		return bmp;
	}
	
	public void setDefaultImage() {
		synchronized (_syncRoot) {
			mLvo = null;
			_syncRoot.notify();
		}
	}
	public void run() {
		mThreadRun = true;
		Log.d(TAG, "Starting LV image processing thread");
		while (mThreadRun) {
			Bitmap bmp = null;
			synchronized (_syncRoot) {
				try {
					_syncRoot.wait();
					if (mLvo != null)
						bmp = processLvImage(mLvo);
					else
						bmp = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
				Canvas canvas = mHolder.lockCanvas();
				if (canvas != null) {
					if (mLvo != null && bmp != null){
						canvas.drawBitmap(bmp, new Rect(0, 0, mLvo.jpegImageSize.horizontal, mLvo.jpegImageSize.vertical), new RectF(0, 0, mFrameWidth, mFrameHeight), null);

						Paint paint = new Paint();
						paint.setColor(Color.GREEN);
						paint.setAntiAlias(true);
						paint.setStyle(Style.STROKE);
						paint.setStrokeWidth(2);

						canvas.drawRect(mLvo.afRects[0], paint);

						if ((_lvOsdDisplay & 1) == 1) {
							paint.setStrokeWidth(1);
							float thirdx = mFrameWidth / 3;
							float thirdy = mFrameHeight / 3;

							if (mLvZoom != null && (Integer)mLvZoom.getValue() == 0) {
								canvas.drawLine(thirdx, 0, thirdx, mFrameHeight, paint);
								canvas.drawLine(2 * thirdx, 0, 2 * thirdx, mFrameHeight, paint);
								canvas.drawLine(0, thirdy, mFrameWidth, thirdy, paint);
								canvas.drawLine(0, thirdy  * 2, mFrameWidth, thirdy * 2, paint);
							}	
						}

						paint.setColor(Color.YELLOW);
						paint.setStrokeWidth(2);

						for(int i = 1; i <= mLvo.faceDetectionPersonNo; i++){
							canvas.drawRect(mLvo.afRects[i], paint);
						}
						
						if ((_lvOsdDisplay & 2) == 2) {
							paint.setColor(Color.GREEN);
							paint.setTextSize(20);
							paint.setShadowLayer(3, 1, 1, Color.BLACK);
							canvas.drawText(String.format("%d s", mLvo.countDownTime), 100, 20, paint);
							canvas.drawText(String.format("f %.1f     %d / %d", (double)mLvo.apertureValue / 100, mLvo.shutterSpeedUpper, mLvo.shutterSpeedLower), 200, 20, paint);
							
							switch(mMode){
							case LVFOCUS:
								canvas.drawText("Manual focus", 50, 350, paint);
								canvas.drawText(String.format("Focus step: %d", mMfDriveStep), 50, 370, paint);
								break;
							case LVZOOM:
								canvas.drawText("Zoom", 50, 350, paint);
								break;
							}
							if (mLvo.focusDrivingStatus == 1)
								canvas.drawText("AF", 100, 45, paint);
							
							canvas.drawText(String.format("Focus %d / %d", _focusCurrent, _focusMax ), 80, 75, paint);
							
							if (mLvo.hasRolling)
								canvas.drawText(String.format("Rolling    %.2f", mLvo.rolling), 500, 50, paint);
							if (mLvo.hasPitching)
								canvas.drawText(String.format("Pitching  %.2f", mLvo.pitching), 500, 75, paint);
							if (mLvo.hasYawing)
								canvas.drawText(String.format("Yawing   %.2f", mLvo.yawing), 500, 100, paint);
							
							if (mLvo.movieRecording) {
								paint.setColor(Color.RED);
								canvas.drawText(String.format("REC remaining %d s", mLvo.movieRecordingTime / 1000), 500, 20, paint);
							}
							
							paint.clearShadowLayer();
							if (mLvo.focusingJudgementResult == 1){
								paint.setColor(Color.RED);
								paint.setStyle(Style.FILL_AND_STROKE);
								canvas.drawCircle(80, 40, 10, paint);
							} else if (mLvo.focusingJudgementResult == 2) {
								paint.setColor(Color.GREEN);
								paint.setStyle(Style.FILL_AND_STROKE);
								canvas.drawCircle(80, 40, 10, paint);
							} 
						}
					}
					else {
						canvas.drawColor(Color.BLACK);
					}
						
					mHolder.unlockCanvasAndPost(canvas);
				}
				if (bmp != null)
					bmp.recycle();
				
		}
	}
}
