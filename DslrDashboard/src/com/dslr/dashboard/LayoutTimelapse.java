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
import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.ptp.PtpProperty;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LayoutTimelapse extends RelativeLayout implements DslrLayout{

	private final static String TAG	= "LayoutLiveView";
	
	private DslrHelper _dslrHelper;
	
	private CheckableImageView btnShoot, btnShootSdram;
	private ImageView btnAf;
	private ImageView btnIMinusm, btnIMinuss, btnIPlusm, btnIPluss;
	private TextView txtInterval;
	private EditText txtIterations;
	private ProgressBar timelapseProgress;
	
	private int _interval = 10;
	private int _iterations = 10;
    
	public LayoutTimelapse(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.layouttimelapse, this, true);
		
		btnShoot = (CheckableImageView)findViewById(R.id.chk_tlshoot);
		btnShootSdram = (CheckableImageView)findViewById(R.id.chk_tlshootsdram);
		btnAf = (ImageView)findViewById(R.id.img_tlaf);
		
		btnIMinusm = (ImageView)findViewById(R.id.img_iminusm);
		btnIMinuss = (ImageView)findViewById(R.id.img_iminuss);
		btnIPlusm = (ImageView)findViewById(R.id.img_iplusm);
		btnIPluss = (ImageView)findViewById(R.id.img_ipluss);
		txtInterval = (TextView)findViewById(R.id.txtinterval);
		txtIterations = (EditText)findViewById(R.id.txt_timelapseiterations);
		timelapseProgress = (ProgressBar)findViewById(R.id.timelapse_progress);
		
		btnShoot.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startStopTimelapse(false);
			}
		});
		btnShootSdram.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startStopTimelapse(true);
			}
		});
		btnAf.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().startAfDriveCmd();
			}
		});
		
		btnIMinusm.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_interval > 60){
					_interval -= 60;
					displayInterval();
				}
			}
		});
		btnIMinuss.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_interval > 1){
					_interval -= 1;
					displayInterval();
				}
			}
		});
		btnIPlusm.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_interval += 60;
				displayInterval();
			}
		});
		btnIPluss.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_interval += 1;
				displayInterval();
			}
		});
	}

	private void startStopTimelapse(boolean toSdram){
//		if (_dslrHelper.getPtpService().getIsPtpDeviceInitialized()){
			if (!_dslrHelper.getPtpService().getIsTimelapseRunning()){
				_dslrHelper.getPtpService().setTimelapseInterval(_interval);
				_dslrHelper.getPtpService().setTimelapseIterations(getIterations());
				_dslrHelper.getPtpService().startTimelapse(toSdram);
			}
			else {
				if (_dslrHelper.getPtpService().getIsTimelapseSdramCapture() == toSdram)
					_dslrHelper.getPtpService().stopTimelapse();
			}
//		}
	}
	public void processTimelapseEvents(PtpServiceEventType event, Object eventData){
		switch(event){
			case TimelapseStarted:
				setButtonStatus();
				initTimelapseProgress(true);
				break;
			case TimelapseStoped:
				setButtonStatus();
				initTimelapseProgress(false);
				break;
			case TimelapseEvent:
				timelapseProgress.setProgress((Integer)eventData);
				break;
		}
	}

	private void initTimelapseProgress(boolean show){
		if (show){
			timelapseProgress.setVisibility(View.VISIBLE);
			timelapseProgress.setMax(_dslrHelper.getPtpService().getTimelapseIterations());
			timelapseProgress.setProgress(_dslrHelper.getPtpService().getTimelapseRemainingIterations());
		}
		else
			timelapseProgress.setVisibility(View.GONE);
	}
	
	private void setButtonStatus(){
		btnShoot.setChecked(false);
		btnShootSdram.setChecked(false);
		if (_dslrHelper.getPtpService().getIsTimelapseRunning())
		{
			if (_dslrHelper.getPtpService().getIsTimelapseSdramCapture()){
				btnShootSdram.setChecked(true);
			}
			else{
				btnShoot.setChecked(true);
			}
			initTimelapseProgress(true);
		}
		else
			initTimelapseProgress(false);
	}
	private int getIterations(){
		int iterations;
		try {
			iterations = Integer.parseInt(txtIterations.getText().toString());
		} catch (NumberFormatException ex){
			iterations = 10;
		}
		return iterations;
	}
	private void displayInterval(){
		int min = _interval / 60;
		int sec = _interval % 60;
		txtInterval.setText(min + ":" + sec);
	}
	private void displayIterations(){
		txtIterations.setText(String.format("%d", _iterations));
	}
	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		_dslrHelper = dslrHelper;
	}

	@Override
	public void updatePtpProperty(PtpProperty property) {
	}


	@Override
	public void ptpServiceSet(boolean isSet) {
		if (isSet){
			setButtonStatus();
		}
	}

	@Override
	public void layoutActivated() {
		_interval = _dslrHelper.getPtpService().getTimelapseInterval();
		_iterations = _dslrHelper.getPtpService().getTimelapseIterations();
		displayInterval();
		displayIterations();
	}

	@Override
	public void layoutDeactived() {
		// TODO Auto-generated method stub
		
	}
	
}
