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
import com.dslr.dashboard.helper.CustomDialog;
import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.ptp.PtpLiveViewObject;
import com.dslr.dashboard.ptp.PtpProperty;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class LayoutMain extends RelativeLayout implements DslrLayout {

	public interface ShowHideMainMenuViews {
		void onShowHideMainMenuViews(boolean show);
	}
	
	private ShowHideMainMenuViews onShowHideMainMenuListener =null;
	
	public void setOnShowHIdeMainMenuListener(ShowHideMainMenuViews listener){
		onShowHideMainMenuListener = listener;
	}
	private Context _context;
	private final static String TAG = "LayoutMain";
	private final static String TAGLV = "LayoutLiveView";
	private DslrHelper _dslrHelper;
	
	private CheckableImageView chkLiveViewOverlay, chkLiveViewFocusOverlay;
	private ImageView btnCameraMode, btnShoot, btnShootSdram, btnAf;
	private ImageView btnCompression, btnExposureMetering;
	private TextView txtIso, txtBurstNumber;
	private ImageView btnImageSize, btnWb, btnFocusMetering, btnFocusMode, btnStillCaptureMode;
	private ImageView btnSceneMode;
	private CheckableImageView chkCameraMode;
	private TextView txtExposureCompensation, txtExposureEvStep, txtInternalFlashCompensation, txtRecordingMedia;
	private ImageView btnFlashMode, btnActivePicCtrlItem, btnActiveDLighting;
	private TextView txtApertureExpStatus, txtAperture, txtExpIndicateStatus, txtShutter, txtShutterExpStatus;
	private ProgressBar batteryInfo;
	
	private LinearLayout leftLayout1, topLayout1, topLayout2, rightLayout1, rightLayout2, bottomLayout1, bottomLayout2, lvFocusLayout1, lvFocusLayout2, lvFocusLayout3;

	private boolean _isLvOverlayVisible = true;
	private boolean _isLvFocusOverlayVisible = false;
	private LvSurfaceBase _lvSurface;
	
	// live view
	private boolean _isLiveViewEnabled = false;
	private CheckableImageView chkLiveView, chkMovieRec;
//	private ImageView imgLiveView;
	private ImageView btnLvAfMode;
	private int _lvOsdDisplay = 3;
	
	// live view focus
	private int _focusStep = 1;
	private ImageView focusLeft, focusRight, focusPlus, focusMinus, lvZoomOut, lvZoomIn, focusMeassure, focusBkt;
	private ImageView focusMin, focusMax, lvOsd;
	private TextView txtFocusStep;
	private SeekBar _focusSeekBar;
	
	private LayoutBracketing _layoutBracketing = null;
	
	public LayoutMain(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		_context = context;
		
		LayoutInflater.from(context).inflate(R.layout.layoutmain, this, true); 
		
		try
		{
			if (MainActivity.getIsTablet())
				_layoutBracketing = (LayoutBracketing)findViewById(R.id.bracketinglayouttablet);
		}
		catch(Exception e){
			_layoutBracketing = null;
		}
		
		leftLayout1 = (LinearLayout)findViewById(R.id.leftlayout1);
		topLayout1 = (LinearLayout)findViewById(R.id.toplayout1);
		topLayout2 = (LinearLayout)findViewById(R.id.toplayout2);
		bottomLayout1 = (LinearLayout)findViewById(R.id.bottomlayout1);
		bottomLayout2 = (LinearLayout)findViewById(R.id.bottomlayout2);
		rightLayout1 = (LinearLayout)findViewById(R.id.rightlayout1);
		rightLayout2 = (LinearLayout)findViewById(R.id.rightlayout2);
		lvFocusLayout1 = (LinearLayout)findViewById(R.id.lvfocuslayout1);
		lvFocusLayout2 = (LinearLayout)findViewById(R.id.lvfocuslayout2);
		lvFocusLayout3 = (LinearLayout)findViewById(R.id.lvfocuslayout3);
	
		batteryInfo = (ProgressBar)findViewById(R.id.batteryinfo);
		
		chkLiveViewOverlay = (CheckableImageView)findViewById(R.id.btn_lvoverlay);
		chkLiveViewFocusOverlay = (CheckableImageView)findViewById(R.id.btn_lvfocusoverlay);
		
		btnCameraMode = (ImageView)findViewById(R.id.img_cameramode);
		btnShoot = (ImageView)findViewById(R.id.img_shoot);
		btnShootSdram = (ImageView)findViewById(R.id.img_shootsdram);
		btnAf = (ImageView)findViewById(R.id.img_af);
		
		btnCompression = (ImageView)findViewById(R.id.img_compression);
		btnExposureMetering = (ImageView)findViewById(R.id.img_exposuremetering);
		txtIso = (TextView)findViewById(R.id.txt_iso);
		txtBurstNumber = (TextView)findViewById(R.id.txt_burstnumber);
		
		btnImageSize = (ImageView)findViewById(R.id.img_imagesize);
		btnWb = (ImageView)findViewById(R.id.img_wb);
		btnFocusMetering = (ImageView)findViewById(R.id.img_focusmetering);
		btnFocusMode = (ImageView)findViewById(R.id.img_focusmodebtn);
		btnStillCaptureMode = (ImageView)findViewById(R.id.img_stillcapturemode);
		
		btnSceneMode = (ImageView)findViewById(R.id.img_scenemode);
		
		chkCameraMode = (CheckableImageView)findViewById(R.id.chk_cameramode);
		
		txtExposureCompensation = (TextView)findViewById(R.id.txt_exposurecompensation);
		txtExposureEvStep = (TextView)findViewById(R.id.txt_exposureevstep);
		txtInternalFlashCompensation = (TextView)findViewById(R.id.txt_internalflashcompensation);
		txtRecordingMedia = (TextView)findViewById(R.id.txt_recordingmedia);
		
		btnFlashMode = (ImageView)findViewById(R.id.img_flash);
		btnActivePicCtrlItem = (ImageView)findViewById(R.id.img_activepicctrlitem);
		btnActiveDLighting = (ImageView)findViewById(R.id.img_activedlighting);

		txtApertureExpStatus = (TextView)findViewById(R.id.txt_apertureexpstatus);
		txtAperture = (TextView)findViewById(R.id.txt_aperture);
		txtExpIndicateStatus = (TextView)findViewById(R.id.txt_expindicatestatus);
		txtShutter = (TextView)findViewById(R.id.txt_shutter);
		txtShutterExpStatus = (TextView)findViewById(R.id.txt_shutterexpstatus);
		
		txtAperture.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.showApertureDialog();
			}
		});
		txtShutter.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.showShutterDialog();
			}
		});
		txtBurstNumber.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				PtpProperty property = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.BurstNumber);
				if (property != null){
			        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
			        
			        InputFilter[] filterArray = new InputFilter[1];
			        filterArray[0] = new InputFilter.LengthFilter(2);
	
			        final EditText txt = new EditText(_dslrHelper.getContext());
			        txt.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			        txt.setText(property.getValue().toString());
			        txt.setInputType(InputType.TYPE_CLASS_NUMBER);
			        txt.setFilters(filterArray);
			        customBuilder.setTitle("Enter burst number")
			        	.setContentView(txt)
			        	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								String strNum = txt.getText().toString().trim();
								if (!strNum.isEmpty()) {
									int brNum = Integer.parseInt(strNum);
									Toast.makeText(_dslrHelper.getContext(), "Burst number: " + strNum + " num: " + brNum, Toast.LENGTH_SHORT).show();
									_dslrHelper.getPtpService().setDevicePropValueCmd(PtpProperty.BurstNumber, brNum);
								}
							}
						})
						.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
			        CustomDialog dialog = customBuilder.create();
			        dialog.show();
				}
			}
		});
		btnCompression.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.CompressionSetting, "Image quality");
			}
		});
		btnImageSize.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ImageSize, "Image size");
			}
		});
		txtIso.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ExposureIndex, "ISO");
			}
		});
		btnWb.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.WhiteBalance, "White balance");
			}
		});
		btnExposureMetering.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ExposureMeteringMode, "Exposure metering mode");
			}
		});
		btnFocusMetering.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.FocusMeteringMode, "Focus metering mode");
			}
		});
		
		btnFocusMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.AfModeSelect, "Focus mode");
			}
		});
		
		btnStillCaptureMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.StillCaptureMode, "Still capture mode");
			}
		});
		btnCameraMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ExposureProgramMode, "Program mode");
			}
		});
		btnSceneMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.SceneMode, "Scene mode");
			}
		});
		btnFlashMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.FlashMode, "Flash mode");
			}
		});
		txtExposureCompensation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.showExposureCompensationDialog();
			}
		});
		txtExposureEvStep.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ExposureEvStep, "Exposure EV step");
				
			}
		});
		txtInternalFlashCompensation.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.showInternalFlashCompensationDialog();
			}
		});
		txtRecordingMedia.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.RecordingMedia, "Recording media");
			}
		});
		btnActivePicCtrlItem.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ActivePicCtrlItem, "Active picture control");
			}
		});
		btnActiveDLighting.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.ActiveDLighting, "Active D-Lighting");
			}
		});
		chkCameraMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().changeCameraModeCmd();
			}
		});
		btnShoot.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().initiateCaptureCmd();
			}
		});
		btnShootSdram.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().initiateCaptureRecInSdramCmd();
			}
		});
		btnAf.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().startAfDriveCmd();
			}
		});
	
		chkLiveViewOverlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_isLiveViewEnabled){
					showHideLvOverlay(!_isLvOverlayVisible);
				}
			}
		});
		chkLiveViewFocusOverlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_isLiveViewEnabled) {
					showHideLvFocusOverlay(!_isLvFocusOverlayVisible);
				}
			}
		});
		
        mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScaledMaximumFlingVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mVibrator = (Vibrator)context.getSystemService("vibrator");
		
		chkLiveView = (CheckableImageView)findViewById(R.id.chk_liveview);
		chkMovieRec = (CheckableImageView)findViewById(R.id.chk_movierec);
		btnLvAfMode = (ImageView)findViewById(R.id.img_lvafmode);
		
		_lvSurface = (LvSurfaceBase)findViewById(R.id.lvsurface);

		focusLeft = (ImageView)findViewById(R.id.img_focusleft);
		focusRight = (ImageView)findViewById(R.id.img_focusright);
		focusMinus = (ImageView)findViewById(R.id.img_focusminus);
		focusPlus = (ImageView)findViewById(R.id.img_focusplus);
		txtFocusStep = (TextView)findViewById(R.id.txt_focusstep);
		
		lvZoomOut = (ImageView)findViewById(R.id.img_lvzoomout);
		lvZoomIn = (ImageView)findViewById(R.id.img_lvzoomin);
		
		focusMeassure = (ImageView)findViewById(R.id.img_focusmeassure);
		focusBkt = (ImageView)findViewById(R.id.img_focusbkt);
		
		_focusSeekBar = (SeekBar)findViewById(R.id.focus_seekbar);
		focusMin = (ImageView)findViewById(R.id.img_focusmin);
		focusMax = (ImageView)findViewById(R.id.img_focusmax);
		lvOsd = (ImageView)findViewById(R.id.img_lvosd);
		
		_focusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if (fromUser) {
					_dslrHelper.getPtpService().seekFocus(progress);
				}
				
			}
		});
		focusMin.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().seekFocusMin();
				_focusSeekBar.setProgress(_dslrHelper.getPtpService().getFocusCurrent());
			}
		});
		focusMax.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().seekFocusMax();
				_focusSeekBar.setProgress(_dslrHelper.getPtpService().getFocusCurrent());
			}
		});
		focusMinus.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_focusStep > 1)
					_focusStep -= 1;
				txtFocusStep.setText(String.format("%d", _focusStep));
			}
		});
		focusPlus.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_focusStep < 100)
					_focusStep += 1;
				txtFocusStep.setText(String.format("%d", _focusStep));
			}
		});
		focusLeft.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_dslrHelper.getPtpService().getIsFocusMaxSet()) {
					if ((_dslrHelper.getPtpService().getFocusCurrent() - _focusStep) >= 0) {
						_dslrHelper.getPtpService().seekFocus(_dslrHelper.getPtpService().getFocusCurrent() - _focusStep);
					}
					else {
						_dslrHelper.getPtpService().seekFocus(0);
					}
					_focusSeekBar.setProgress(_dslrHelper.getPtpService().getFocusCurrent());
				}
				else
					_dslrHelper.getPtpService().startMfDrive(true, _focusStep);
			}
		});
		focusRight.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_dslrHelper.getPtpService().getIsFocusMaxSet()){
					if ((_dslrHelper.getPtpService().getFocusCurrent() + _focusStep) <= _dslrHelper.getPtpService().getFocusMax()) {
						_dslrHelper.getPtpService().seekFocus(_dslrHelper.getPtpService().getFocusCurrent() + _focusStep);
					}
					else {
						_dslrHelper.getPtpService().seekFocus(_dslrHelper.getPtpService().getFocusMax());
					}
					_focusSeekBar.setProgress(_dslrHelper.getPtpService().getFocusCurrent());
				}
				else
					_dslrHelper.getPtpService().startMfDrive(false, _focusStep);			
			}
		});
		focusMeassure.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().meassureMfDrive();
			}
		});
		focusBkt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				focusStackingLayout();
			}
		});
		
		lvZoomOut.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				zoomLiveView(false);
			}
		});
		lvZoomIn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				zoomLiveView(true);
				
			}
		});
		
		chkLiveView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_dslrHelper.getPtpService() != null && _dslrHelper.getPtpService().getIsPtpDeviceInitialized()) {
				PtpProperty property = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.LiveViewStatus);
				if (property != null){
					if ((Integer)property.getValue() == 0) {
						PtpProperty lvProhibition = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.LiveViewProhibitionCondition);
						if (lvProhibition != null){
							long prohibition = (Long)lvProhibition.getValue();
							if (prohibition != 0){
								liveViewProhibitionMessage(prohibition);
								return;
							}
						}
						_dslrHelper.getPtpService().startLiveViewCmd();
					}
					else
						_dslrHelper.getPtpService().endLiveViewCmd();
				}
				}
			}
		});
		chkMovieRec.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

				PtpService service = _dslrHelper.getPtpService();
				if (service != null && service.getIsPtpDeviceInitialized()) {
					if (!service.getIsMovieRecEnabled()) {
						PtpProperty movieRecProhibition = service.getPtpProperty(PtpProperty.MovieRecProhibitionCondition);
						if (movieRecProhibition != null){
							long prohibition = (Long)movieRecProhibition.getValue();
							if (prohibition != 0){
								movieRecordingProhibitionMessage(prohibition);
								return;
							}
						}
						service.startMovieRecCmd();
						return;
					}
					service.stopMovieRecCmd();
				}
			}
		});
		lvOsd.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_lvOsdDisplay == 3)
					_lvOsdDisplay = 0;
				else
					_lvOsdDisplay += 1;
			}
		});
		
		_lvSurface.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (_dslrHelper.getPtpService().getIsLiveViewEnabled()){
		        final int action = event.getAction();
		        final float x = event.getX();
		        final float y = event.getY();

		        if (mVelocityTracker == null) {
		            mVelocityTracker = VelocityTracker.obtain();
		        }
		        mVelocityTracker.addMovement(event);

		        switch (action) {
		            case MotionEvent.ACTION_DOWN:
		                v.postDelayed(mLongPressRunnable, mLongPressTimeout);
		                mDownX = x;
		                mDownY = y;
		                calculateMfDriveStep(v.getHeight(), y);
		                break;

		            case MotionEvent.ACTION_MOVE: {
		                switch(mMode)
		                {
		                case TOUCH:
		                    final float scrollX = mDownX - x;
		                    final float scrollY = mDownY - y;

		                    final float dist = (float)Math.sqrt(scrollX * scrollX + scrollY * scrollY);

		                    if (dist >= mScaledTouchSlop) {
		                        v.removeCallbacks(mLongPressRunnable);
		                        mMode = Mode.PAN;
		                    }
		                	break;
		                case PAN:
		                	break;
		                case LVZOOM:
			                int mdy = Math.round(((y - mDownY) / v.getHeight()) * 10);
							if (mdy > mZoomY){
								zoomLiveView(true);
							}
							else if (mdy < mZoomY){
								zoomLiveView(false);
							}
							mZoomY = mdy;
		                	break;
		                case LVFOCUS:
		                	calculateMfDriveStep(v.getHeight(), y);
		                	if (x >= (focusX + 20)){
		                		_dslrHelper.getPtpService().startMfDrive(true, mfDriveStep);
		                		focusX = x;
		                	}
		                	else if (x <= (focusX - 20)){
		                		_dslrHelper.getPtpService().startMfDrive(false, mfDriveStep);
		                		focusX = x;
		                	}
		                	break;
		                }

		                break;
		            }

		            case MotionEvent.ACTION_UP:
		            	switch(mMode)
		            	{
		            	case LVZOOM:
		            		break;
		            	case LVFOCUS:
		            		break;
		            	case PAN:
		                    mVelocityTracker.computeCurrentVelocity(1000, mScaledMaximumFlingVelocity);
		            		break;
		            	case TOUCH:
    						lvAfx = x;
    						lvAfy = y;
		            		long currentTime = System.currentTimeMillis();
		            		if ((currentTime - lastTime) < 200) {
		            			lastTime = -1;
		            			v.removeCallbacks(mSingleTapRunnable);
		            			lvSurfaceDoubleTap();
		            		} else {
		            			lastTime = currentTime;
		            			v.postDelayed(mSingleTapRunnable, 200);
		            		}
		            		
		            		break;
		            	}
		                mVelocityTracker.recycle();
		                mVelocityTracker = null;
		                v.removeCallbacks(mLongPressRunnable);
		                mMode = Mode.TOUCH;
		                break;

		            default:
		                mVelocityTracker.recycle();
		                mVelocityTracker = null;
		                v.removeCallbacks(mLongPressRunnable);
		                mMode = Mode.TOUCH;
		                break;

		        }
				}
		        return true;
		        	
			}
		});
		
		
		btnLvAfMode.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.AfAtLiveView, "Live view AF");
			}
		});
	}

	private long lastTime = -1;
	private boolean isLvFullScreen = false;
	private LayoutParams oldParams;
	
	private void lvSurfaceDoubleTap(){
		if (!isLvFullScreen) {
			oldParams = (LayoutParams) _lvSurface.getLayoutParams();
			
			LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			leftLayout1.setVisibility(View.GONE);
			showHideLvFocusOverlay(false);
			showHideLvOverlay(false);
			showHideLayoutsTablet(false);
			_lvSurface.setLayoutParams(params);
		} else {
			showHideLayoutsTablet(true);
			leftLayout1.setVisibility(View.VISIBLE);
			_lvSurface.setLayoutParams(oldParams);
		}
		if (onShowHideMainMenuListener != null)
			onShowHideMainMenuListener.onShowHideMainMenuViews(isLvFullScreen);
		isLvFullScreen = !isLvFullScreen;
	}
	
	private final Runnable clearLvSurface = new Runnable() {
		
		@Override
		public void run() {
			_lvSurface.setDefaultImage();
		}
	};
	public LayoutBracketing getBracketingLayout(){
		return _layoutBracketing;
	}
	
	private void showHideLayoutsTablet(boolean show){
		if (MainActivity.getIsTablet()){
			showHideLayouts(show);
			_layoutBracketing.setVisibility(show ? View.VISIBLE : View.GONE);
			lvFocusLayout1.setVisibility(show ? (_isLiveViewEnabled ? View.VISIBLE : View.GONE) : View.GONE);
			lvFocusLayout2.setVisibility(show ? (_isLiveViewEnabled ? View.VISIBLE : View.GONE) : View.GONE);
			lvFocusLayout3.setVisibility(show ? (_isLiveViewEnabled ? View.VISIBLE : View.GONE) : View.GONE);
		}
		
	}
	private void showHideLayouts(boolean show){
		int visible = show ? View.VISIBLE : View.GONE;
		
			topLayout1.setVisibility(visible);
			topLayout2.setVisibility(visible);
			rightLayout1.setVisibility(visible);
			rightLayout2.setVisibility(visible);
			bottomLayout1.setVisibility(visible);
			bottomLayout2.setVisibility(visible);
	}
	
	private void showHideLvFocusOverlay(boolean show){
		if (!MainActivity.getIsTablet()){
			if (show)
				showHideLvOverlay(false);
			_isLvFocusOverlayVisible = show;
			if (_isLiveViewEnabled)
			{
				chkLiveViewFocusOverlay.setVisibility(View.VISIBLE);
				chkLiveViewFocusOverlay.setChecked(show);
				lvFocusLayout1.setVisibility(show ? View.VISIBLE : View.GONE);
				lvFocusLayout2.setVisibility(show ? View.VISIBLE : View.GONE);
				lvFocusLayout3.setVisibility(show ? View.VISIBLE : View.GONE);
			}
			else {
				chkLiveViewFocusOverlay.setChecked(false);
				chkLiveViewFocusOverlay.setVisibility(View.GONE);
				lvFocusLayout1.setVisibility(View.GONE);
				lvFocusLayout2.setVisibility(View.GONE);
				lvFocusLayout3.setVisibility(View.GONE);
			}
		}
		else {
			lvFocusLayout1.setVisibility(show ? View.VISIBLE : View.GONE);
			lvFocusLayout2.setVisibility(show ? View.VISIBLE : View.GONE);
			lvFocusLayout3.setVisibility(show ? View.VISIBLE : View.GONE);
		}
	}
	
	private void showHideLvOverlay(boolean show){
		if (!MainActivity.getIsTablet())
		{
			if (show)
				showHideLvFocusOverlay(false);
			_isLvOverlayVisible = show;
			if (_isLiveViewEnabled)
				chkLiveViewOverlay.setChecked(show);
			else
				chkLiveViewOverlay.setChecked(false);

			showHideLayouts(show);
		}
	}
	
	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		_dslrHelper = dslrHelper;
	}


	@Override
	public void updatePtpProperty(PtpProperty ptpProperty){
		final PtpProperty property = ptpProperty;
				switch(property.getPropertyCode()){
				case PtpProperty.BatteryLevel:
					batteryInfo.setMax(100);
					batteryInfo.setProgress((Integer)property.getValue());
					break;
				case PtpProperty.FStop:
					int fStop = (Integer)property.getValue();
					txtAperture.setVisibility(View.VISIBLE);
					txtAperture.setText("f " + (double)fStop / 100);
					txtAperture.setEnabled(property.isWritable()); 			
					break;
				case PtpProperty.ExposureTime:
					Long nesto = (Long)property.getValue();
					Log.i(TAG, "Exposure " + nesto);
					//double value = 1 / ((double)nesto / 10000);
					txtShutter.setVisibility(View.VISIBLE);
					if (nesto == 4294967295L)
						txtShutter.setText("Bulb");
					else {
						if (nesto >= 10000)
							txtShutter.setText(String.format("%.1f \"", (double)nesto / 10000));
						else
							txtShutter.setText(String.format("1/%.1f" , 10000 / (double)nesto));
//						if (value < 1)
//							txtShutter.setText(Math.round(1/value) + "\"");
//						else
//							txtShutter.setText("1/" + Math.round(value));
					}
					txtShutter.setEnabled(property.isWritable()); 			
					break;
				case PtpProperty.ExposureProgramMode:
					_dslrHelper.setDslrImg(btnCameraMode, property);
					btnCameraMode.setVisibility(View.VISIBLE);
					chkCameraMode.setVisibility(View.VISIBLE);
					chkCameraMode.setChecked(property.isWritable());
					chkCameraMode.setImageResource(chkCameraMode.isChecked() ? R.drawable.hostmode : R.drawable.cameramode );	
					break;
				case PtpProperty.SceneMode:
					btnSceneMode.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnSceneMode, property);
					break;
				case PtpProperty.CompressionSetting:
					btnCompression.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnCompression, property);
					break;
				case PtpProperty.ImageSize:
					btnImageSize.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnImageSize, property);
					break;
				case PtpProperty.WhiteBalance:
					btnWb.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnWb, property);
					break;
				case PtpProperty.ExposureIndex:
					txtIso.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrTxt(txtIso, property);
					break;
				case PtpProperty.FocusMeteringMode:
					btnFocusMetering.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnFocusMetering, property);
					break;
				case PtpProperty.AfModeSelect:
					btnFocusMode.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnFocusMode, property);
					break;
				case PtpProperty.ExposureMeteringMode:
					btnExposureMetering.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnExposureMetering, property);
					break;
				case PtpProperty.StillCaptureMode:
					btnStillCaptureMode.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnStillCaptureMode, property);
					break;
				case PtpProperty.BurstNumber:
					txtBurstNumber.setVisibility(View.VISIBLE);
					txtBurstNumber.setText(property.getValue().toString());
					txtBurstNumber.setEnabled(property.isWritable());
					break;
				case PtpProperty.ExposureBiasCompensation:
					txtExposureCompensation.setVisibility(View.VISIBLE);
					txtExposureCompensation.setEnabled(property.isWritable());
					int ev = (Integer)property.getValue();
					txtExposureCompensation.setText(String.format("%+.1f EV", (double)ev/1000));
					break;
				case PtpProperty.ExposureEvStep:
					txtExposureEvStep.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrTxt(txtExposureEvStep, property);
					
					break;
				case PtpProperty.FlashMode:
					btnFlashMode.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnFlashMode, property);
					//setPropertyViewImage(img_flashmode, property);
					break;
				case PtpProperty.InternalFlashCompensation:
					txtInternalFlashCompensation.setVisibility(View.VISIBLE);
					txtInternalFlashCompensation.setEnabled(property.isWritable());
					int fev = (Integer)property.getValue();
					txtInternalFlashCompensation.setText(String.format("%+.1f EV", (double)fev/6));
					break;
				case PtpProperty.RecordingMedia:
					txtRecordingMedia.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrTxt(txtRecordingMedia, property);
					break;
				case PtpProperty.ActivePicCtrlItem:
					btnActivePicCtrlItem.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnActivePicCtrlItem, property);
					break;
				case PtpProperty.ActiveDLighting:
					btnActiveDLighting.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnActiveDLighting, property);
					break;
				case PtpProperty.ExposureDisplayStatus:
					int expStatus = (Integer)property.getValue();
					switch(expStatus){
					case 0:
					case 3:
					case 6:
						txtShutterExpStatus.setText("");
						break;
					case 1:
					case 4:
					case 7:
						txtShutterExpStatus.setText("Lo");
						break;
					case 2:
					case 5:
					case 8:
						txtShutterExpStatus.setText("Hi");
						break;
					}
					switch(expStatus){
					case 0:
					case 1:
					case 2:
						txtApertureExpStatus.setText("");
						break;
					case 3:
					case 4:
					case 5:
						txtApertureExpStatus.setText("Lo");
						break;
					case 6:
					case 7:
					case 8:
						txtApertureExpStatus.setText("Hi");
						break;
					}
					txtApertureExpStatus.setVisibility(View.VISIBLE);
					txtShutterExpStatus.setVisibility(View.VISIBLE);
					break;
				case PtpProperty.ExposureIndicateStatus:
					txtExpIndicateStatus.setVisibility(View.VISIBLE);
					int iev = (Integer)property.getValue();
					txtExpIndicateStatus.setText(String.format("%+.1f EV", (double)iev/6));
					break;
				// live view
				case PtpProperty.LiveViewStatus:
					chkLiveView.setVisibility(View.VISIBLE);
					_isLiveViewEnabled = (Integer)property.getValue() != 0;
					chkLiveView.setChecked(_isLiveViewEnabled);
					if (MainActivity.getIsTablet())
						showHideLvFocusOverlay(_isLiveViewEnabled);
					else {
						chkLiveViewFocusOverlay.setVisibility(_isLiveViewEnabled ? View.VISIBLE : View.GONE);
					}
					
					_dslrHelper.setDslrImg(chkLiveView, property, false);
					showHideLvOverlay(!_isLiveViewEnabled);
					if (!_isLiveViewEnabled)
						this.postDelayed(clearLvSurface, 500);
					if (_dslrHelper.getPtpService().getIsFocusMaxSet()) {
						_focusSeekBar.setMax(_dslrHelper.getPtpService().getFocusMax());
						_focusSeekBar.setProgress(_dslrHelper.getPtpService().getFocusCurrent());
					}
					
					break;
				case PtpProperty.AfAtLiveView:
					btnLvAfMode.setVisibility(View.VISIBLE);
					_dslrHelper.setDslrImg(btnLvAfMode, property);
					break;
				case PtpProperty.MovieRecProhibitionCondition:
					break;
				};
				
	}

	@Override
	public void ptpServiceSet(boolean isSet) {
		if (isSet && _dslrHelper.getPtpService().getIsPtpDeviceInitialized()){
			showHideLvOverlay(true);
		}
	}

	@Override
	public void layoutActivated() {
		this.postDelayed(clearLvSurface, 500);
	}

	@Override
	public void layoutDeactived() {
	}
	
	//region live view
	
    public enum Mode {
        TOUCH, PAN, LVZOOM, LVFOCUS
    }
    
    private int mfDriveStep = 200;

    /** Time of tactile feedback vibration when entering zoom mode */
    private static final long VIBRATE_TIME = 50;

    /** Current listener mode */
    private Mode mMode = Mode.TOUCH;

    /** X-coordinate of latest down event */
    private float mDownX;

    /** Y-coordinate of latest down event */
    private float mDownY;

    private int mZoomY;
    private float focusX;
    
    /** Velocity tracker for touch events */
    private VelocityTracker mVelocityTracker;

    /** Distance touch can wander before we think it's scrolling */
    private final int mScaledTouchSlop;

    /** Duration in ms before a press turns into a long press */
    private final int mLongPressTimeout;

    /** Vibrator for tactile feedback */
    private final Vibrator mVibrator;

    /** Maximum velocity for fling */
    private final int mScaledMaximumFlingVelocity;
	
    private final Runnable mLongPressRunnable = new Runnable() {
        public void run() {
        	if (mDownX > 550)
        	{
        		mMode = Mode.LVZOOM;
        		mZoomY = 0;
                mVibrator.vibrate(VIBRATE_TIME);
        	}
        	else {
        		mMode = Mode.LVFOCUS;
        		focusX = mDownX;
                mVibrator.vibrate(VIBRATE_TIME);
        	}
        }
    };
    
    private final Runnable mSingleTapRunnable = new Runnable() {
		
		@Override
		public void run() {
			synchronized (_syncRoot) {
				_liveViewNeedFocusChanged = true;
			}
		}
	};
	
    private void calculateMfDriveStep(int height, float y){
    	float inv = height - y;
    	float step = inv * 0.205f;
    	int testy = Math.round(inv * step);
    	if (testy < 1)
    		testy = 1;
    	else if (testy > 32767)
    		testy = 32767;
    	mfDriveStep = testy;
    }
    
    public void focusMaxSet() {
    	_focusSeekBar.setMax(_dslrHelper.getPtpService().getFocusMax());
    	_focusSeekBar.setProgress(0);
    }

	private void zoomLiveView(boolean up){
		if (_dslrHelper.getPtpService().getIsPtpDeviceInitialized()){
			// check if liveview is enabled
			PtpProperty lvStatus = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.LiveViewStatus);
		
			if (lvStatus != null && (Integer)lvStatus.getValue() == 1){
				// get the zoom factor
				PtpProperty zoom = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.LiveViewImageZoomRatio);
				if (zoom != null){
					int zValue = (Integer)zoom.getValue();
					if (up){
						if (zValue < 5)
							_dslrHelper.getPtpService().setDevicePropValueCmd(PtpProperty.LiveViewImageZoomRatio, zValue + 1);
					}
					else {
						if (zValue > 0)
							_dslrHelper.getPtpService().setDevicePropValueCmd(PtpProperty.LiveViewImageZoomRatio, zValue - 1);
					}
				}
			}
		}
		
	}
    
	private Object _syncRoot = new Object();
	private boolean _liveViewNeedFocusChanged = false;
    private float lvAfx, lvAfy;
	
	public void updateLiveViewObject(final PtpLiveViewObject lvo){
		
		PtpService service = _dslrHelper.getPtpService();
		
		lvo.parse(_lvSurface.getFrameWidth(), _lvSurface.getFrameHeight());
		if (_liveViewNeedFocusChanged){
				float x = ((lvAfx / lvo.sDw) / lvo.ox) + lvo.dLeft;
				float y = ((lvAfy / lvo.sDh) / lvo.oy) + lvo.dTop;
				service.changeAfAreaCmd((int)x, (int)y);
			_liveViewNeedFocusChanged = false;
		}
		
	    PtpProperty lvZoom = service.getPtpProperty(PtpProperty.LiveViewImageZoomRatio);
	    int focusCurrent = service.getFocusCurrent();
	    int focusMax = service.getFocusMax();
		_lvSurface.processLvObject(lvo, lvZoom, mMode, mfDriveStep, focusCurrent, focusMax, _lvOsdDisplay);
		
		}
	
	private void liveViewProhibitionMessage(long prohibitionCode){
		StringBuilder sb = new StringBuilder();
		int bit = 1;
		for(int i = 0; i < 24; i++){
			int value = (int) (prohibitionCode & bit);
				switch (value){
				case 1: // bit 0
					sb.append("The recording destination is the SD card. \n\n");
					break;
				case 2: // bit 1
					sb.append(" \n");
					break;
				case 4: // bit 2
					sb.append("Sequence error \n\n");
					break;
				case 8: // bit 3
					sb.append(" \n");
					break;
				case 16: // bit 4
					sb.append("Fully pressed button error \n\n");
					break;
				case 32: // bit 5
					sb.append("The aperture value is being set by the lens aperture ring. \n\n");
					break;
				case 64: // bit 6
					sb.append("Bulb error \n\n");
					break;
				case 128: // bit 7
					sb.append("During mirror-up operation \n\n");
					break;
				case 256: // bit 8
					sb.append("During insufficiency of battery \n\n");
					break;
				case 512: // bit 9
					sb.append("TTL error \n\n");
					break;
				case 1024: // bit 10
					sb.append("While the aperture value operation by the lens aperture ring is valid \n\n");
					break;
				case 2048: // bit 11
					sb.append("A non-CPU lens is mounted and the exposure mode is not M. \n\n");
					break;
				case 4096: // bit 12
					sb.append("There is an image whose recording destination is SDRAM \n\n");
					break;
				case 8192: // bit 13
					sb.append("The release mode is [Mirror-up]. \n\n");
					break;
				case 16384: // bit 14
					sb.append("The recording destination is the card or the card & SDRAM, and the card is not inserted with the release disabled without a card. \n\n");
					break;
				case 32768: // bit 15
					sb.append("During processing by the shooting command/n" +
							"* When the recording destination is the card, it indicates the time until the CaptureComplete event is passed./n" +
							"* When the recording destination is the SDRAM, it indicates the time until the CaptureCompleteRecInSdram event is passed./n"+
							"* When the recording destinations are the card and the SDRAM, it indicates the time until the CaptureComplete and the CaptureCompleteRecInSdram events are passed. \n\n");
					break;
				case 65536: // bit 16
					sb.append("The shooting mode is EFFECTS. \n\n");
					break;
				case 131072: // bit 17
					sb.append("The Live view cannot be started when the temperature rises.  \n\n");
					break;
				case 262144: // bit 18
					sb.append("Card protected \n\n");
					break;
				case 524288: // bit 19
					sb.append("Card error \n\n");
					break;
				case 1048576: // bit 20
					sb.append("Card unformatted \n\n");
					break;
				}
			bit = bit << 1;
		}
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Liveview prohibition")
        	.setMessage(sb.toString())
        	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
	}

	private void movieRecordingProhibitionMessage(long prohibitionCode){
		StringBuilder sb = new StringBuilder();
		int bit = 1;
		for(int i = 0; i < 24; i++){
			int value = (int) (prohibitionCode & bit);
				switch (value){
				case 1: // bit 0
					sb.append("No card inserted. \n\n");
					break;
				case 2: // bit 1
					sb.append("Card error \n\n");
					break;
				case 4: // bit 2
					sb.append("Card not formatted \n\n");
					break;
				case 8: // bit 3
					sb.append("No free area in the card \n\n");
					break;
				case 16: // bit 4
					sb.append("\n");
					break;
				case 32: // bit 5
					sb.append("\n");
					break;
				case 64: // bit 6
					sb.append("\n");
					break;
				case 128: // bit 7
					sb.append("There is data whose recording destination is a card in the buffer. \n\n");
					break;
				case 256: // bit 8
					sb.append("There is data whose recording destination is the PC in the buffer. \n\n");
					break;
				case 512: // bit 9
					sb.append("There is movie data in the buffer. \n\n");
					break;
				case 1024: // bit 10
					sb.append("During movie file recording \n\n");
					break;
				case 2048: // bit 11
					sb.append("Card protected \n\n");
					break;
				case 4096: // bit 12
					sb.append("During enlarged display of Live view \n\n");
					break;
				case 8192: // bit 13
					sb.append("\n");
					break;
				case 16384: // bit 14
					sb.append("\n");
					break;
				case 32768: // bit 15
					sb.append("\n");
					break;
				case 65536: // bit 16
					sb.append("\n");
					break;
				case 131072: // bit 17
					sb.append("\n");
					break;
				case 262144: // bit 18
					sb.append("\n");
					break;
				case 524288: // bit 19
					sb.append("\n");
					break;
				case 1048576: // bit 20
					sb.append("\n");
					break;
				}
			bit = bit << 1;
		}
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Liveview prohibition")
        	.setMessage(sb.toString())
        	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
	}
	
	//endregion
	
	public void updateMovieRecStatus(){
		if (_dslrHelper.getPtpService() != null && _dslrHelper.getPtpService().getIsPtpDeviceInitialized()) {
			chkMovieRec.setChecked(_dslrHelper.getPtpService().getIsMovieRecEnabled());
		}
	}
	
	private void focusStackingLayout() {
        LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.focus_stacking_layout, null);
        
        final EditText txtNumber = (EditText)view.findViewById(R.id.txtImageNumber);
        final EditText txtStep = (EditText)view.findViewById(R.id.txtFocusStep);
        final RadioButton rbDirection = (RadioButton)view.findViewById(R.id.focusRadioDown);
        final RadioButton rbMedia = (RadioButton)view.findViewById(R.id.radioSdcard);
        
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Focus stacking")
        	.setContentView(view)
        	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
        	.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						int number = Integer.parseInt(txtNumber.getText().toString());
						int step = Integer.parseInt(txtStep.getText().toString());
						int direction = rbDirection.isChecked() ? 1 : 2;
						boolean sdcard = rbMedia.isChecked(); 
						_dslrHelper.getPtpService().focusBracketing(number, step, direction, sdcard);
					} catch (NumberFormatException e) {
						
					}
					dialog.dismiss();
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
	}
	
}
