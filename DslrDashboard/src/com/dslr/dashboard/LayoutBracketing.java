// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import com.dslr.dashboard.helper.CheckableImageView;
import com.dslr.dashboard.helper.CustomDialog;
import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.helper.ItemBean;
import com.dslr.dashboard.ptp.PtpProperty;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LayoutBracketing extends RelativeLayout implements DslrLayout{

	private DslrHelper _dslrHelper;
	
	private ImageView btnShoot, btnShootSdram, btnAf;
	private CheckableImageView chkBkt;
	private ImageView btnBracketingType;
	private TextView txtAeBracketingCount;
	private LinearLayout layoutAeBracketing, layoutWbBracketing, layoutAdlBracketing;
	private TextView txtAeBracketingStep;
	private CheckableImageView chkCustomBracketing, chkBultInCustomBracketing;
	private TextView txtCustomBktStep, txtCustomBktDirection, txtCustomBktCount;
	private CheckableImageView chkSoundShooting;
	private TextView txtSoundDenominator, txtSoundThreshold, txtSoundRate, txtSoundRecordingMedia, txtSoundValue;
	private ImageView imgSoundIndicator;
	
	public LayoutBracketing(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		LayoutInflater.from(context).inflate(R.layout.layoutbracketing, this, true);
		
		btnShoot = (ImageView)findViewById(R.id.img_bktshoot);
		btnShootSdram = (ImageView)findViewById(R.id.img_bktshootsdram);
		btnAf = (ImageView)findViewById(R.id.img_bktaf);
		
		chkBkt = (CheckableImageView)findViewById(R.id.chk_bkt);
		btnBracketingType = (ImageView)findViewById(R.id.img_bracketingtype);
		txtAeBracketingCount = (TextView)findViewById(R.id.txt_aebracketingcount);
		
		layoutAeBracketing = (LinearLayout)findViewById(R.id.aebktlayout);
		layoutWbBracketing = (LinearLayout)findViewById(R.id.wbbktlayout);
		layoutAdlBracketing = (LinearLayout)findViewById(R.id.adlbktlayout);

		txtAeBracketingStep = (TextView)findViewById(R.id.txt_aebracketingstep);
		
		chkCustomBracketing = (CheckableImageView)findViewById(R.id.chk_bkt_custom);
		chkBultInCustomBracketing = (CheckableImageView)findViewById(R.id.chk_builtin_custom_bkt);
		txtCustomBktStep = (TextView)findViewById(R.id.txt_custombktstep);
		txtCustomBktDirection = (TextView)findViewById(R.id.txt_custombktdirection);
		txtCustomBktCount = (TextView)findViewById(R.id.txt_custombktcount);
		
		chkSoundShooting = (CheckableImageView)findViewById(R.id.chk_sound_shooting);
		txtSoundDenominator = (TextView)findViewById(R.id.txt_sound_denominator);
		txtSoundThreshold = (TextView)findViewById(R.id.txt_sound_threshold);
		txtSoundRate = (TextView)findViewById(R.id.txt_sound_rate);
		txtSoundRecordingMedia = (TextView)findViewById(R.id.txt_sound_recording_media);
		txtSoundValue = (TextView)findViewById(R.id.txt_sound_value);
		imgSoundIndicator = (ImageView)findViewById(R.id.img_sound_indicator);
		
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
		chkBkt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().toggleBracketing();
			}
		});
		btnBracketingType.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.BracketingType, "Bracketing type");
			}
		});
		txtAeBracketingStep.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.createDslrDialog(PtpProperty.AeBracketingStep, "AE Bracketing step");
			}
		});
		
		txtCustomBktStep.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				customBktStepDialog();
			}
		});
		txtCustomBktDirection.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				customBktDirektionDialog();
			}
		});
		txtCustomBktCount.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				customBktCountDialog();
			}
		});
		chkCustomBracketing.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().setIsCustomBktEnabled(!_dslrHelper.getPtpService().getIsCustomBktEnabled());
				chkCustomBracketing.setChecked(_dslrHelper.getPtpService().getIsCustomBktEnabled());
				_dslrHelper.getPtpService().saveApplicationPreferences();
			}
		});
		chkBultInCustomBracketing.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				_dslrHelper.getPtpService().setIsBuiltInBktEnabled(!_dslrHelper.getPtpService().getIsBuiltInBktEnabled());
				chkBultInCustomBracketing.setChecked(_dslrHelper.getPtpService().getIsBuiltInBktEnabled());
				_dslrHelper.getPtpService().saveApplicationPreferences();
			}
		});
		
		txtSoundDenominator.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showSoundDenominatorDialog();
			}
		});
		txtSoundRate.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showSoundRateDialog();
			}
		});
		txtSoundThreshold.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showSoundThresholdDialog();
			}
		});
		txtSoundRecordingMedia.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				showSoundRecordingMediaDialog();
			}
		});
		chkSoundShooting.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (_dslrHelper.getPtpService().getIsSoundShootingEnabled())
					_dslrHelper.getPtpService().stopSoundMeter();
				else
					_dslrHelper.getPtpService().startSoundMeter();
				chkSoundShooting.setChecked(_dslrHelper.getPtpService().getIsSoundShootingEnabled());
			}
		});
	}

	public void soundMonitorIndicatorEvent(boolean value){
		imgSoundIndicator.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
	}
	public void soundMonitorValueEvent(double value){
		txtSoundValue.setText(String.format("%.2f", value));
	}
	public void soundMonitorStarted(){
		imgSoundIndicator.setVisibility(View.INVISIBLE);
		txtSoundValue.setVisibility(View.VISIBLE);
		
	}
	public void soundMonitorStoped(){
		imgSoundIndicator.setVisibility(View.GONE);
		txtSoundValue.setVisibility(View.GONE);
		
	}
	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		_dslrHelper = dslrHelper;
	}

	@Override
	public void ptpServiceSet(boolean isSet) {
		setCustomValues();
	}
	
	private void setCustomValues(){
		if (_dslrHelper != null && _dslrHelper.getIsPtpServiceSet()){
			txtCustomBktDirection.setText(getCustomBktDirection(_dslrHelper.getPtpService().getBktDirection()));
			txtCustomBktCount.setText((String.format("%d", _dslrHelper.getPtpService().getBktCount())));
			//txtCustomBktStep.setText((String.format("%d", _dslrHelper.getPtpService().getBktStep())));
			chkCustomBracketing.setChecked(_dslrHelper.getPtpService().getIsCustomBktEnabled());
			chkBultInCustomBracketing.setChecked(_dslrHelper.getPtpService().getIsBuiltInBktEnabled());
		
			txtSoundDenominator.setText(String.format("%.1f", _dslrHelper.getPtpService().getSoundDenominator()));
			txtSoundThreshold.setText(String.format("%.1f", _dslrHelper.getPtpService().getSoundThreshold()));
			txtSoundRate.setText(String.format("%d", _dslrHelper.getPtpService().getSoundSamplingRate()));
			txtSoundRecordingMedia.setText(_dslrHelper.getPtpService().getSoundRecordingMedia());
			chkSoundShooting.setChecked(_dslrHelper.getPtpService().getIsSoundShootingEnabled());
		}
	}

	@Override
	public void updatePtpProperty(PtpProperty property) {
		switch(property.getPropertyCode()){
			case PtpProperty.EnableBracketing:
				chkBkt.setVisibility(View.VISIBLE);
				chkBkt.setChecked((Integer)property.getValue() != 0);
				_dslrHelper.setDslrImg(chkBkt, property);
				break;
		case PtpProperty.BracketingType:
			layoutAeBracketing.setVisibility(View.GONE);
			layoutWbBracketing.setVisibility(View.GONE);
			layoutAdlBracketing.setVisibility(View.GONE);
			
			btnBracketingType.setVisibility(View.VISIBLE);
			_dslrHelper.setDslrImg(btnBracketingType, property);
			
			switch ((Integer)property.getValue()) {
			case 1:
				layoutAeBracketing.setVisibility(View.VISIBLE);
				break;
			case 3:
				layoutWbBracketing.setVisibility(View.VISIBLE);
				break;
			case 4:
				layoutAdlBracketing.setVisibility(View.VISIBLE);
				break;
			}
			break;
		case PtpProperty.AeBracketingStep:
			_dslrHelper.setDslrTxt(txtAeBracketingStep, property);
			break;
		case PtpProperty.AeBracketingCount:
			txtAeBracketingCount.setVisibility(View.VISIBLE);
			txtAeBracketingCount.setText(property.getValue().toString());
			txtAeBracketingCount.setEnabled(property.isWritable());
			break;
		case PtpProperty.ExposureEvStep:
			if ((Integer)property.getValue() == 0)
				txtCustomBktStep.setText(String.format("%.1f EV", _dslrHelper.getPtpService().getBktStep() * (1f / 3f)));
			else
				txtCustomBktStep.setText(String.format("%.1f EV", _dslrHelper.getPtpService().getBktStep() * (1f / 2f)));
			break;
		}
	}

    private String getCustomBktDirection(int direktion){
    	switch(direktion){
    	case 0:
    		return "Negative";
    	case 1:
    		return "Positive";
    	case 2:
    		return "Both";
    		default:
    			return "Both";
    	}
    }
    private void customBktCountDialog(){
    	final ArrayList<ItemBean> items = new ArrayList<ItemBean>();
    	int selectedItem = -1;
    	for(int i = 0; i <= 3; i++){
    		int current = 3 + (i * 2);
    		if (current == _dslrHelper.getPtpService().getBktCount())
    			selectedItem = i;
    		ItemBean item = new ItemBean();
    		item.setImage(-1);
    		item.setValue(current);
    		item.setTitle(String.format("%d images", current));
    		items.add(item);
    	}
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Custom bracketing count")
            .setListItems(items, selectedItem)
            .setListOnClickListener(new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which >= 0 && which < items.size()){
						_dslrHelper.getPtpService().setBktCount((Integer) items.get(which).value());
						((TextView)findViewById(R.id.txt_custombktcount)).setText(items.get(which).getTitle());
						_dslrHelper.getPtpService().saveApplicationPreferences();
					}
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
    }
    private void customBktDirektionDialog(){
    	final ArrayList<ItemBean> items = new ArrayList<ItemBean>();
    	for(int i = 0; i <= 2; i++){
    		ItemBean item = new ItemBean();
    		item.setImage(-1);
    		item.setValue(i);
    		item.setTitle(getCustomBktDirection(i));
    		items.add(item);
    	}
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Custom bracketing direktion")
            .setListItems(items, _dslrHelper.getPtpService().getBktDirection())
            .setListOnClickListener(new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which >= 0 && which < items.size()){
						_dslrHelper.getPtpService().setBktDirection((Integer) items.get(which).value());
						((TextView)findViewById(R.id.txt_custombktdirection)).setText(items.get(which).getTitle());
						_dslrHelper.getPtpService().saveApplicationPreferences();
					}
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
    }
    private void customBktStepDialog(){
    	PtpProperty prop = _dslrHelper.getPtpService().getPtpProperty(PtpProperty.ExposureEvStep);
    	if (prop != null){
    		int evStep = (Integer) prop.getValue();
    		final ArrayList<ItemBean> items = new ArrayList<ItemBean>();
    		switch(evStep)
    		{
    		case 0: // 1/3
    			for(int i = 1; i <= 6; i++){
    				ItemBean item = new ItemBean();
    				item.setImage(-1);
    				item.setTitle(String.format("%.1f EV", i * (1f / 3f)));
    				item.setValue(i);
    				items.add(item);
    			}
    			break;
    		case 1: // 1/2
    			if (_dslrHelper.getPtpService().getBktStep() > 4)
    				 _dslrHelper.getPtpService().setBktStep(1);
    			for(int i = 1; i <= 4; i++){
    				ItemBean item = new ItemBean();
    				item.setImage(-1);
    				item.setTitle(String.format("%.1f EV", i * (1f / 2f)));
    				item.setValue(i);
    				items.add(item);
    			}
    			break;
    		}
    		
            CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
            customBuilder.setTitle("Custom bracketing EV step")
                .setListItems(items, _dslrHelper.getPtpService().getBktStep() - 1)
                .setListOnClickListener(new DialogInterface.OnClickListener() {
    				
    				@Override
    				public void onClick(DialogInterface dialog, int which) {
    					if (which >= 0 && which < items.size()){
    						_dslrHelper.getPtpService().setBktStep((Integer) items.get(which).value());
    						((TextView)findViewById(R.id.txt_custombktstep)).setText(items.get(which).getTitle());
    					}
    				}
    			});
        CustomDialog dialog = customBuilder.create();
        dialog.show();
    		
    	}
    }
	    
    private void showSoundDenominatorDialog(){
    	final EditText tv = new EditText(_dslrHelper.getContext());
    	tv.setText(String.format("%.1f", _dslrHelper.getPtpService().getSoundDenominator()));
    	tv.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    	CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
    	customBuilder.setTitle("Amplitude denominator")
    		.setContentView(tv)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					NumberFormat nf = DecimalFormat.getInstance();
					try {
						_dslrHelper.getPtpService().setSoundDenominator(nf.parse(tv.getText().toString()).doubleValue());
						setCustomValues();
					} catch (ParseException e) {
						
					}
					dialog.dismiss();
				}
			});
    	CustomDialog dialog = customBuilder.create();
    	dialog.show();
    }
    
    private void showSoundThresholdDialog(){
    	final EditText tv = new EditText(_dslrHelper.getContext());
    	tv.setText(String.format("%.1f", _dslrHelper.getPtpService().getSoundThreshold()));
    	tv.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
    	CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
    	customBuilder.setTitle("Amplitude threshold")
    		.setContentView(tv)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					NumberFormat nf = DecimalFormat.getInstance();
					try {
						_dslrHelper.getPtpService().setSoundThreshold(nf.parse(tv.getText().toString()).doubleValue());
						setCustomValues();
					} catch (ParseException e) {
					}
					dialog.dismiss();
				}
			});
    	CustomDialog dialog = customBuilder.create();
    	dialog.show();
    }
    
    private void showSoundRateDialog(){
    	final EditText tv = new EditText(_dslrHelper.getContext());
    	tv.setText(String.format("%d", _dslrHelper.getPtpService().getSoundSamplingRate()));
    	tv.setInputType(InputType.TYPE_CLASS_NUMBER);
    	CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
    	customBuilder.setTitle("Amplitude sampling rate")
    		.setContentView(tv)
    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try{
						_dslrHelper.getPtpService().setSoundSamplingRate(Integer.parseInt(tv.getText().toString()));
						setCustomValues();
					} catch (NumberFormatException ex){
						
					}
					dialog.dismiss();
				}
			});
    	CustomDialog dialog = customBuilder.create();
    	dialog.show();
    }
    
    private void showSoundRecordingMediaDialog(){
		final ArrayList<ItemBean> items = new ArrayList<ItemBean>();
		ItemBean item = new ItemBean();
		item.setImage(-1);
		item.setTitle("Sdcard");
		items.add(item);
		item = new ItemBean();
		item.setImage(-1);
		item.setTitle("Sdram");
		items.add(item);
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
        customBuilder.setTitle("Sound shooting recording media")
            .setListItems(items, _dslrHelper.getPtpService().getSoundRecordingMedia().equals("Sdcard") ? 0 : 1)
            .setListOnClickListener(new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which >= 0 && which < items.size()){
						_dslrHelper.getPtpService().setSoundRecordingMedia(items.get(which).getTitle());
						setCustomValues();
					}
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
    }

	@Override
	public void layoutActivated() {
		
	}

	@Override
	public void layoutDeactived() {
		
	}

	
}
