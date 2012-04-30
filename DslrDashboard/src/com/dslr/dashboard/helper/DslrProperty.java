// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

import java.util.ArrayList;

public class DslrProperty {
	private int _propertyCode;
	private ArrayList<String> _values;
	private ArrayList<ItemBean> _propertyValues;
	
	public DslrProperty(int ptpPropertyCode){
		_propertyCode = ptpPropertyCode;
		_propertyValues = new ArrayList<ItemBean>();
		_values = new ArrayList<String>();
	}
	
	public int propertyCode(){
		return _propertyCode;
	}
	
	public ArrayList<ItemBean> valueNames(){
		return _propertyValues;
	}
	public ArrayList<String> values(){
		return _values;
	}
	public int indexOfValue(Object value){
		return _values.indexOf(value.toString());
	}
	public int getImgResourceId(Object value){
		ItemBean prop = getPropertyByValue(value);
		if (prop == null)
			return 0;
		return prop.getImage();
	}
	public int getnameResourceId(Object value){
		ItemBean prop = getPropertyByValue(value);
		if (prop == null)
			return 0;
		return prop.nameId();
	}
	public ItemBean getPropertyByValue(Object value){
		int index = indexOfValue(value);
		if (index == -1)
			return null;
		return _propertyValues.get(index);
	}
	public ItemBean addPropertyValue(Object value, int nameId, int imgId){
		ItemBean item = new ItemBean(value, nameId, imgId);
		_propertyValues.add(item);
		_values.add(value.toString());
		return item;
	}
}
