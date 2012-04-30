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
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LayoutAbout extends RelativeLayout implements DslrLayout{

	private TextView txtAbout, txtNoCamera;
	
	public LayoutAbout(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.layoutabout, this, true); 
		
		txtAbout = (TextView)findViewById(R.id.txt_about);
		txtAbout.setText(Html.fromHtml(context.getString(R.string.about)));
		txtAbout.setMovementMethod(LinkMovementMethod.getInstance());
		txtNoCamera = (TextView)findViewById(R.id.txt_nocamera);
		txtNoCamera.setText(Html.fromHtml(context.getString(R.string.nocamera)));
	}

	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		
	}

	@Override
	public void updatePtpProperty(PtpProperty property) {
		
	}

	@Override
	public void ptpServiceSet(boolean isSet) {
	}

	public void ptpDeviceConnected(){
		txtNoCamera.setVisibility(View.GONE);
	}
	public void ptpDeviceDisConnected(){
		txtNoCamera.setVisibility(View.VISIBLE);
	}
	@Override
	public void layoutActivated() {
		
	}

	@Override
	public void layoutDeactived() {
		
	}

}
