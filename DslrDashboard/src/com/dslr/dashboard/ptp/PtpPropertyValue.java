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

public class PtpPropertyValue {
    int			typecode;
    Object		value;

    PtpPropertyValue (int tc)
	{ typecode = tc; }

    private PtpPropertyValue (int tc, Object obj)
    {
	typecode = tc;
	value = obj;

    }

    public Object getValue ()
	{ return value; }
    
    public int getTypeCode ()
	{ return typecode; }

    protected void parse (Buffer data)
    {
    	value = get(typecode, data);
    }

    public static void setNewPropertyValue(Buffer data, int valueType, Object value){
    	switch(valueType){
    	case u8:
    	case s8:
    		data.put8((Integer) value);
    		break;
    	case s16:
    	case u16:
    		data.put16((Integer)value);
    		break;
    	case s32:
    		data.put32((Integer)value);
    		break;
    	case u32:
    		data.put32((Long)value);
    		break;
    	case s64:
    	case u64:
    		data.put64((Long)value);
    		break;
    	case string:
    		data.putString(value.toString());
    		break;
    	}
    }
    static Object get (int code, Buffer buf)
    {

	switch (code) {
	    case s8:
		return new Integer (buf.nextS8 ());
	    case u8:
		return new Integer (buf.nextU8 ());
	    case s16:
		return new Integer (buf.nextS16 ());
	    case u16:
		return new Integer (buf.nextU16 ());
	    case s32:
		return new Integer (buf.nextS32 ());
	    case u32:
		return new Long (0x0ffFFffFFL & buf.nextS32 ());
	    case s64:
		return new Long (buf.nextS64 ());
	    case u64:
		// FIXME: unsigned masquerading as signed ...
		return new Long (buf.nextS64 ());

	    // case s128: case u128:

	    case s8array:
		return buf.nextS8Array ();
	    case u8array:
		return buf.nextU8Array ();
	    case s16array:
		return buf.nextS16Array ();
	    case u16array:
		return buf.nextU16Array ();
	    case u32array:
		// FIXME: unsigned masquerading as signed ...
	    case s32array:
		return buf.nextS32Array ();
	    case u64array:
		// FIXME: unsigned masquerading as signed ...
	    case s64array:
		return buf.nextS64Array ();
	    // case s128array: case u128array:

	    case string:
		return buf.nextString ();
	}
	throw new IllegalArgumentException ();
    }

    // code values, per 5.3 table 3

    public static final int s8 = 0x0001;
    /** Unsigned eight bit integer */
    public static final int u8 = 0x0002;
    public static final int s16 = 0x0003;
    /** Unsigned sixteen bit integer */
    public static final int u16 = 0x0004;
    public static final int s32 = 0x0005;
    /** Unsigned thirty two bit integer */
    public static final int u32 = 0x0006;
    public static final int s64 = 0x0007;
    /** Unsigned sixty four bit integer */
    public static final int u64 = 0x0008;
    public static final int s128 = 0x0009;
    /** Unsigned one hundred twenty eight bit integer */
    public static final int u128 = 0x000a;
    public static final int s8array = 0x4001;
    /** Array of unsigned eight bit integers */
    public static final int u8array = 0x4002;
    public static final int s16array = 0x4003;
    /** Array of unsigned sixteen bit integers */
    public static final int u16array = 0x4004;
    public static final int s32array = 0x4005;
    /** Array of unsigned thirty two bit integers */
    public static final int u32array = 0x4006;
    public static final int s64array = 0x4007;
    /** Array of unsigned sixty four bit integers */
    public static final int u64array = 0x4008;
    public static final int s128array = 0x4009;
    /** Array of unsigned one hundred twenty eight bit integers */
    public static final int u128array = 0x400a;
    /** Unicode string */
    public static final int string = 0xffff;


}
