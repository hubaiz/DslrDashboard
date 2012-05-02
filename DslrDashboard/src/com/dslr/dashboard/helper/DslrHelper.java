// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import org.xmlpull.v1.XmlPullParserException;
import com.dslr.dashboard.PtpService;
import com.dslr.dashboard.R;
import com.dslr.dashboard.ptp.PtpProperty;
import com.dslr.dashboard.ptp.PtpProperty.PtpRange;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class DslrHelper {
	private static String TAG="DslrHelper";
	
	private Context _context;
	
	private PtpService _ptpService;
	private boolean _isPtpServiceSet = false;
	
	private DslrProperties _dslrProperties;
	
	public DslrHelper(Context context){
		_context = context;
	}
	
	public void setPtpService(PtpService ptpService){
		if (ptpService != null){
			_isPtpServiceSet = true;
			_ptpService = ptpService;
			if (_ptpService.getIsPtpDeviceInitialized())
			{
				try
				{
					loadDslrProperties(_ptpService.getUsbDevice().getVendorId(), _ptpService.getUsbDevice().getProductId());
				}
				catch(Exception e)
				{
					
				}
			}
		}
		else{
			_ptpService = null;
			_isPtpServiceSet = false;
		}
	}
	
	public PtpService getPtpService(){
		return _ptpService;
	}
	public boolean getIsPtpServiceSet(){
		return _isPtpServiceSet;
	}
	public DslrProperties getDslrProperties(){
		return _dslrProperties;
	}
	public Context getContext(){
		return _context;
	}
	public void loadDslrProperties(int vendorId, int productId){
		
		_dslrProperties = new DslrProperties(vendorId, productId);
		
		Resources res = _context.getResources();
		
    	XmlResourceParser devices = _context.getResources().getXml(R.xml.propertyvalues);
    	
    	int eventType = -1;
    	
    	DslrProperty dslrProperty = null;
    	
    	while (eventType != XmlResourceParser.END_DOCUMENT) {
    		if(eventType == XmlResourceParser.START_DOCUMENT) {
    		}
    		else if(eventType == XmlResourceParser.START_TAG) {
    			
    			String strName = devices.getName();
    			
    			if(strName.equals("device")) {
    				
    			} else if(strName.equals("ptpproperty")) {
    				
    				int propertyCode = Integer.parseInt(devices.getAttributeValue(null, "id"), 16);
    				
    				dslrProperty = _dslrProperties.addProperty(propertyCode);
    				
    			} else if (strName.equals("item")){
    				
    				int valueId = devices.getAttributeResourceValue(null, "value", 0);
    				int nameId = devices.getAttributeResourceValue(null, "name", 0);
    				int resId = devices.getAttributeResourceValue(null, "img", 0);
    				
    				Object value = null;
    				
    				String valueType = res.getResourceTypeName(valueId);
    				if (valueType.equals("string")){
    					value = res.getString(valueId);
    				} else if (valueType.equals("integer")){
        				value = res.getInteger(valueId);
    				}
    				
    				dslrProperty.addPropertyValue(value, nameId, resId);
    				
    			}
    		}
    		try {
				eventType = devices.next();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	Log.d(TAG, "Document End");    	 		
	}
	
	public void createDslrDialog(int propertyCode, String dialogTitle){
		PtpProperty property = _ptpService.getPtpProperty(propertyCode);
		if (property == null)
			return;
		DslrProperty dslrProperty = _dslrProperties.getProperty(propertyCode);
		if (dslrProperty == null)
			return;
		ArrayList<ItemBean> items = null;
		int selectedItem = -1;
		Vector<?> propValues = property.getEnumeration();
		if (propValues != null){
			items = new ArrayList<ItemBean>();
			selectedItem = propValues.indexOf(property.getValue());
			for(int i = 0; i < propValues.size(); i++){
				ItemBean item = dslrProperty.getPropertyByValue(propValues.get(i));
				if (item != null)
					items.add(item);
			}
		}
		else {
			items = dslrProperty.valueNames();
			selectedItem = dslrProperty.indexOfValue(property.getValue());
		}
		createDialog(property, dialogTitle, items, selectedItem);
	}
	
	public void setDslrImg(ImageView view, PtpProperty property){
		setDslrImg(view, property, true);
	}
	public void setDslrImg(ImageView view, PtpProperty property, boolean setEnableStatus){
		DslrProperty prop = _dslrProperties.getProperty(property.getPropertyCode());
		if (prop == null)
			return;
		if (setEnableStatus)
			view.setEnabled(property.isWritable());
		view.setImageResource(prop.getImgResourceId(property.getValue()));
	}
	
	public void setDslrTxt(TextView view, PtpProperty property){
		DslrProperty prop = _dslrProperties.getProperty(property.getPropertyCode());
		if (prop == null)
			return;
		view.setEnabled(property.isWritable());
		view.setText(prop.getnameResourceId(property.getValue()));
	}
	
	public void createDialog(PtpProperty property, String dialogTitle, final ArrayList<ItemBean> items, int selectedItem){
		final int propertyCode = property.getPropertyCode();
        CustomDialog.Builder customBuilder = new CustomDialog.Builder(_context);
        customBuilder.setTitle(dialogTitle)
            .setListItems(items, selectedItem)
            .setListOnClickListener(new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which >= 0 && which < items.size())
						_ptpService.setDevicePropValueCmd(propertyCode, items.get(which).value());
					//Toast.makeText(MainActivity.this, String.format("You selected: %d", which), Toast.LENGTH_SHORT).show();
				}
			});
    CustomDialog dialog = customBuilder.create();
    dialog.show();
    //dialog.proba(selectedItem);
    
	}
	
	public void showApertureDialog() {
		PtpProperty property = _ptpService.getPtpProperty(PtpProperty.FStop);
		if (property != null){
			final Vector<?> enums = property.getEnumeration();
			int selectedItem = enums.indexOf(property.getValue());
			if (enums != null){
				ArrayList<ItemBean> items = new ArrayList<ItemBean>();
				for (int i = 0; i < enums.size(); i++){
					Integer value = (Integer) enums.get(i);
					Double val = ((double)value / 100);
					ItemBean item = new ItemBean();
					item.image = -1;
					item.title = val.toString();
					item.setValue(enums.get(i));
					items.add(item);
				}
				createDialog(property, "Aperture", items, selectedItem);
			}
				
		}
	}
	public void showExposureCompensationDialog() {
		PtpProperty property = _ptpService.getPtpProperty(PtpProperty.ExposureBiasCompensation);
		if (property != null){
			final Vector<?> enums = property.getEnumeration();
			int selectedItem = enums.indexOf(property.getValue());
			if (enums != null){
				ArrayList<ItemBean> items = new ArrayList<ItemBean>();
				for (int i = 0; i < enums.size(); i++){
					Integer value = (Integer) enums.get(i);
					ItemBean item = new ItemBean();
					item.image = -1;
					item.title = String.format("%+.1f EV", (double)value/1000);
					item.setValue(enums.get(i));
					items.add(item);
				}
				createDialog(property, "Exposure compensation", items, selectedItem);
			}
				
		}
	}
	public void showInternalFlashCompensationDialog() {
		PtpProperty property = _ptpService.getPtpProperty(PtpProperty.InternalFlashCompensation);
		if (property != null){
			PtpRange range = property.getRange();
			if (range != null){
			int selectedItem = -1;
			int i = 0;
			ArrayList<ItemBean> items = new ArrayList<ItemBean>();
			for (int r = (Integer)range.getMinimum(); r <= (Integer)range.getMaximum(); r += (Integer)range.getIncrement()){
				if (r == (Integer)property.getValue())
					selectedItem = i;
			
					ItemBean item = new ItemBean();
					item.image = -1;
					item.title = String.format("%+.1f EV", (double)r/6);
					item.setValue(r);
					items.add(item);
					i++;
				}
				createDialog(property, "Internal flash compensation", items, selectedItem);
			}		
		}
	}
	public void showShutterDialog(){
		PtpProperty property = _ptpService.getPtpProperty(PtpProperty.ExposureTime);
		if (property != null){
			final Vector<?> enums = property.getEnumeration();
			int selectedItem = enums.indexOf(property.getValue());
			if (enums != null){
				ArrayList<ItemBean> items = new ArrayList<ItemBean>();
				for (int i = 0; i < enums.size(); i++){
					String str;
					Long nesto = (Long)enums.get(i);
					if (nesto == 0xffffffffL)
						str = "Bulb";
					else {
						if (nesto >= 10000) 
							str = String.format("%.1f \"",  (double)nesto / 10000);
						else
							str = String.format("1/%.1f" , 10000 / (double)nesto);
//						double value = 1 / ((double)nesto / 10000);
//						if (value < 1)
//							str = Math.round(1/value) + "\"";
//						else
//							str = "1/" + Math.round(value);
					}
					ItemBean item = new ItemBean();
					item.image = -1;
					item.title = str;
					item.setValue(enums.get(i));
					items.add(item);
					
				}
				createDialog(property, "Exposure time", items, selectedItem);
			}
				
		}
	}
	
}
