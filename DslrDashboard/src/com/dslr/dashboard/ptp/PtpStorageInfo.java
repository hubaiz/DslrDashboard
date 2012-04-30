// Copyright 2000 by David Brownell <dbrownell@users.sourceforge.net>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 

package com.dslr.dashboard.ptp;

import java.util.Hashtable;

import android.util.Log;

public class PtpStorageInfo {
	private static String TAG = "PtpStorageInfo";
	
	public int storageId;
	public int storageType;
	public int filesystemType;
	public int accessCapability;
	public long maxCapacity;
	public long freeSpaceInBytes;
	public int freeSpaceInImages;
	public int storageDescription;
	public String volumeLabel;
	public boolean isObjectsLoaded = false;
	
	public Hashtable<Integer, PtpObjectInfo> objects;
	
	public PtpStorageInfo(int id, Buffer buf)
	{
		objects = new Hashtable<Integer, PtpObjectInfo>();
		
		storageId = id;
		updateInfo(buf);
	}
	
	public void updateInfo(Buffer buf){
		buf.parse();
		storageType = buf.nextU16();
		filesystemType = buf.nextU16();
		accessCapability = buf.nextU16();
		maxCapacity = buf.nextS64();
		freeSpaceInBytes = buf.nextS64();
		freeSpaceInImages = buf.nextS32();
		storageDescription = buf.nextU8();
		volumeLabel = buf.nextString();
		
		Log.d(TAG, String.format("Storage id: %#04x images: %d", storageId, freeSpaceInImages));
	}
}
