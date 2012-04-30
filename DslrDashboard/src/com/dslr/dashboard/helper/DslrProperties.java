// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

import java.util.Hashtable;

public class DslrProperties {
	
	private Hashtable<Integer, DslrProperty> _properties;
	private int _vendorId, _productId;
	
	public DslrProperties(int vendorId, int productId){
		_vendorId = vendorId;
		_productId = productId;
		_properties = new Hashtable<Integer, DslrProperty>();
	}
	
	public Hashtable<Integer, DslrProperty> properties(){
		return _properties;
	}
	
	public DslrProperty addProperty(int propertyCode){
		DslrProperty property = new DslrProperty(propertyCode);
		_properties.put(propertyCode, property);
		return property;
	}
	
	public boolean containsProperty(int propertyCode){
		return _properties.containsKey(propertyCode);
	}
	public DslrProperty getProperty(int propertyCode){
		return _properties.get(propertyCode);
	}
	
	public int getVendorId(){
		return _vendorId;
	}
	public int getProductId(){
		return _productId;
	}
}
