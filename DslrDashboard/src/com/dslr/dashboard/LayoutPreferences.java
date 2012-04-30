// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.ptp.PtpProperty;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class LayoutPreferences extends RelativeLayout implements DslrLayout{

	private DslrHelper _dslrHelper;
	private EditText txtSavingLocation, txtPrefix, txtNumbering, txtGpsSampleCount, txtGpsSampleInterval, txtFocusMin, txtFocusMax;
	private CheckBox chkAddExifGps;
	private CheckBox chkLiveViewAtStart;
	
	
	public LayoutPreferences(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.layoutpreferences, this, true); 
		
		txtSavingLocation = (EditText)findViewById(R.id.txt_savinglocation);
		txtPrefix = (EditText)findViewById(R.id.txt_imageprefix);
		txtNumbering = (EditText)findViewById(R.id.txt_numbering);
		chkAddExifGps = (CheckBox)findViewById(R.id.chk_add_exif_gps);
		txtGpsSampleCount = (EditText)findViewById(R.id.txt_gps_sample_count);
		txtGpsSampleInterval = (EditText)findViewById(R.id.txt_gps_sample_interval);
		chkLiveViewAtStart = (CheckBox)findViewById(R.id.chk_liveview_at_start);
		txtFocusMin = (EditText)findViewById(R.id.txt_focus_min);
		txtFocusMax = (EditText)findViewById(R.id.txt_focus_max);
	}

	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		_dslrHelper = dslrHelper;
	}

	@Override
	public void updatePtpProperty(PtpProperty property) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ptpServiceSet(boolean isSet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void layoutActivated() {
		txtSavingLocation.setText(_dslrHelper.getPtpService().getSdramSavingLocation());
		txtPrefix.setText(_dslrHelper.getPtpService().getPicturePrefix());
		txtNumbering.setText(String.valueOf(_dslrHelper.getPtpService().getPictureNumbering()));
		chkAddExifGps.setChecked(_dslrHelper.getPtpService().getAddExifGps());
		txtGpsSampleCount.setText(String.valueOf(_dslrHelper.getPtpService().getGpsSampleCount()));
		txtGpsSampleInterval.setText(String.valueOf(_dslrHelper.getPtpService().getGpsSampleInterval()));
		chkLiveViewAtStart.setChecked(_dslrHelper.getPtpService().getEnterLiveViewAtStart());
		txtFocusMin.setText(String.valueOf(_dslrHelper.getPtpService().getFocusMin()));
		txtFocusMax.setText(String.valueOf(_dslrHelper.getPtpService().getFocusMax()));
	}

	@Override
	public void layoutDeactived() {
		_dslrHelper.getPtpService().setSdramSavingLocation(txtSavingLocation.getText().toString());
		_dslrHelper.getPtpService().setPicturePrefix(txtPrefix.getText().toString());
		_dslrHelper.getPtpService().setPictureNumbering(Integer.parseInt(txtNumbering.getText().toString()));
		_dslrHelper.getPtpService().setAddExifGps(chkAddExifGps.isChecked());
		_dslrHelper.getPtpService().setGpsSampleCount(Integer.parseInt(txtGpsSampleCount.getText().toString()));
		_dslrHelper.getPtpService().setGpsSampleInterval(Integer.parseInt(txtGpsSampleInterval.getText().toString()));
		_dslrHelper.getPtpService().setEnterLiveViewAtStart(chkLiveViewAtStart.isChecked());
		_dslrHelper.getPtpService().saveApplicationPreferences();
		_dslrHelper.getPtpService().setFocusMin(Integer.parseInt(txtFocusMin.getText().toString()));
		_dslrHelper.getPtpService().setFocusMax(Integer.parseInt(txtFocusMax.getText().toString()));
	}

}
