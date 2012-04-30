// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import android.view.KeyEvent;

import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.ptp.PtpProperty;

public interface DslrLayout {
	void setDslrHelper(DslrHelper dslrHelper);
	void updatePtpProperty(PtpProperty property);
	void ptpServiceSet(boolean isSet);
	void layoutActivated();
	void layoutDeactived();
}
