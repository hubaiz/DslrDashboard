// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.ptp;

import android.util.Log;

public class GetPreviewImageProcessor extends PtpCommandFinalProcessor {
	private static String TAG = "GetPreviewImageProcessor";
	
	@Override
	public boolean doFinalProcessing(PtpCommand cmd) {
		Log.d(TAG, "get preview image: " + cmd.responseParam());
		return cmd.responseParam() == 0 ? false : true;
	}
}
