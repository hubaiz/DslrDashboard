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

import android.util.Log;

public class PtpSession {
	private static String TAG = "PtpSession";
	
    private int		sessionId;
    private int		xid;
    private boolean	active;

    public PtpSession () { }

    int getNextXID ()
	{ return (active ?  xid++ : 0); }

    int getNextSessionID ()
    {
	if (!active)
	    return ++sessionId;
	else
		return xid++;
    }

    public boolean isActive ()
	{ return active; }

    public void open ()
	{ xid = 1; active = true; Log.d(TAG, "**** sesion opened");}

    public void close ()
	{ active = false; }
    
    int getSessionId ()
	{ return sessionId; }

    // track objects and their info by handles;
    // hookup to marshaling system and event framework 
}
