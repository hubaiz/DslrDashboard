// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

public class ExifDataHelper {
	public String mExifName;
	public String mExifDescription;
	public String mExifValue;
	
	public ExifDataHelper(String exifName, String exifDescription) {
		mExifName = exifName;
		mExifDescription = exifDescription;
	}
}
