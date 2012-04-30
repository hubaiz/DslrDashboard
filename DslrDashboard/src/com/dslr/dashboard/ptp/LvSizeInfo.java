// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.ptp;

public class LvSizeInfo {
	public int horizontal;
	public int vertical;
	
	public LvSizeInfo(){
		horizontal = 0;
		vertical = 0;
	}
	public LvSizeInfo(int horSize, int vertSize){
		horizontal = horSize;
		vertical = vertSize;
	}
	
	public void setSize(int hor, int vert){
		horizontal = hor;
		vertical = vert;
	}
	
	public void setSize(LvSizeInfo sizeInfo){
		horizontal = sizeInfo.horizontal;
		vertical = sizeInfo.vertical;
	}
}
