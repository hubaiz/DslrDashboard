// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

public class ItemBean 
{
	String title;
	int image;
	private Object _value;
	private int _nameId;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getImage() {
		return image;
	}

	public void setImage(int image) {
		this.image = image;
	}	
	
	
	
	public ItemBean(){
		
	}
	public ItemBean(Object propValue, int propNameId, int propImgId){
		_value = propValue;
		_nameId = propNameId;
		image = propImgId;
	}
	public Object value(){
		return _value;
	}
	public void setValue(Object value){
		_value = value;
	}
	public int nameId(){
		return _nameId;
	}
	
}