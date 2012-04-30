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

public class Buffer {
    static final int	HDR_LEN = 12;
	
    private byte _data [] = null;
    private int  _offset = 0;

    public Buffer(){
    	
    }
    
    public Buffer(byte[] data) {
    	_data = data;
    	_offset = 0;
    }
    public void wrap(byte[] data){
    	_data = data;
    	_offset = 0;
    }
    
    public boolean hasData(){
    	return _data != null;
    }
    public byte[] data(){
    	return _data;
    }
    public byte[] getOfssetArray(){
    	byte[] buf = new byte[_offset];
    	System.arraycopy(_data, 0, buf, 0, _offset);
    	return buf;
    }
    public int length(){
    	if (_data != null)
    		return _data.length;
    	else
    		return 0;
    }
    public int offset(){
    	return _offset;
    }
    public void moveOffset(int moveBy){
    	_offset += moveBy;
    }
    public void parse(){
    	_offset = HDR_LEN;
    }
    public void setPacketLength(){
    	int tmpOffset = _offset;
    	_offset = 0;
    	put32(tmpOffset);
    	_offset = tmpOffset;
    }
    /**
     * Return the packet length
     * @return
     */
    public int getPacketLength(){
    	return getS32(0);
    }
    /**
     * Return the packet type (1 command, 2 data)
     * @return
     */
    public int getPacketType(){
    	return getU16(4);
    }
    /**
     * Return the packet code
     * @return
     */
    public int getPacketCode(){
    	return getU16(6);
    }
    /** 
     * Returns the session id
     * @return
     */
    public int getSessionId(){
    	return getS32(8);
    }
    /** Unmarshals a signed 8 bit integer from a fixed buffer offset. */
    public final int getS8 (int index)
    {
    	return _data [index];
    }

    /** Unmarshals an unsigned 8 bit integer from a fixed buffer offset. */
    public final int getU8 (int index)
    {
    	return 0xff & _data [index];
    }

    /** Marshals an 8 bit integer (signed or unsigned) */
    protected final void put8 (int value)
    {
    	_data [_offset++] = (byte) value;
    }

    /** Unmarshals the next signed 8 bit integer */
    public final int nextS8 ()
    {
    	return _data [_offset++];
    }

    /** Unmarshals the next unsigned 8 bit integer */
    public final int nextU8 ()
    {
    	return 0xff & _data [_offset++];
    }

    /** Unmarshals an array of signed 8 bit integers */
    public final int [] nextS8Array ()
    {
		int len = /* unsigned */ nextS32 ();
		int retval [] = new int [len];
		for (int i = 0; i < len; i++)
		    retval [i] = nextS8 ();
		return retval;
    }


    /** Unmarshals an array of 8 bit integers */
    public final int [] nextU8Array ()
    {
		int len = /* unsigned */ nextS32 ();
		int retval [] = new int [len];
		for (int i = 0; i < len; i++)
		    retval [i] = nextU8 ();
		return retval;
    }


    /** Unmarshals a signed 16 bit integer from a fixed buffer offset. */
    public final int getS16 (int index)
    {
		int retval;
	
		retval = 0xff & _data [index++];
		retval |= _data [index] << 8;
		return retval;
    }
    
    /** Unmarshals an unsigned 16 bit integer from a fixed buffer offset. */
    public final int getU16 (int index)
    {
		return getU16(index, false);
    }
    
    public final int getU16(int index, boolean reverse)
    {
    	int retval;
    	if (reverse)
    	{
    		retval = 0xff00 & (_data[index++] << 8);
    		retval |= 0xff & _data[index];
    	}
    	else
    	{
    		retval = 0xff & _data [index++];
    		retval |= 0xff00 & (_data [index] << 8);
    	}
    	return retval;
    }
    
    /** Marshals a 16 bit integer (signed or unsigned) */
    protected final void put16 (int value)
    {
		_data [_offset++] = (byte) value;
		_data [_offset++] = (byte) (value >> 8);
    }

    /** Unmarshals the next signed 16 bit integer */
    public final int nextS16 ()
    {
		int retval = getS16 (_offset);
		_offset += 2;
		return retval;
    }

    /** Unmarshals the next unsinged 16 bit integer */
    public final int nextU16()
    {
    	return nextU16(false);
    }
    public final int nextU16 (boolean reverse)
    {
		int retval = getU16 (_offset, reverse);
		_offset += 2;
		return retval;
    }

    /** Unmarshals an array of signed 16 bit integers */
    public final int [] nextS16Array ()
    {
		int len = /* unsigned */ nextS32 ();
		int retval [] = new int [len];
		for (int i = 0; i < len; i++)
		    retval [i] = nextS16 ();
		return retval;
    }

    /** Unmarshals an array of unsigned 16 bit integers */
    public final int [] nextU16Array ()
    {
		int len = /* unsigned */ nextS32 ();
		int retval [] = new int [len];
		for (int i = 0; i < len; i++)
		    retval [i] = nextU16 ();
		return retval;
    }


    /** Unmarshals a signed 32 bit integer from a fixed buffer offset. */
    public final int getS32 (int index)
    {
    	return getS32(index, false);
    }

    public final int getS32(int index, boolean reverse)
    {
    	int retval;
    	if (reverse)
    	{
    		retval = _data[index++] << 24;
    		retval |= (0xff & _data[index++]) << 16;
    		retval |= (0xff & _data[index++]) << 8;
    		retval |= (0xff & _data[index]);
    	}
    	else
    	{
    		retval  = (0xff & _data [index++]) ;
    		retval |= (0xff & _data [index++]) << 8;
    		retval |= (0xff & _data [index++]) << 16;
    		retval |=         _data [index  ]  << 24;
    	}
    	return retval;
    }
    /** Marshals a 32 bit integer (signed or unsigned) */
    protected final void put32 (int value)
    {
		_data [_offset++] = (byte) value;
		_data [_offset++] = (byte) (value >> 8);
		_data [_offset++] = (byte) (value >> 16);
		_data [_offset++] = (byte) (value >> 24);
    }
    protected final void put32(long value)
    {
		_data [_offset++] = (byte) value;
		_data [_offset++] = (byte) (value >> 8);
		_data [_offset++] = (byte) (value >> 16);
		_data [_offset++] = (byte) (value >> 24);
    }

    /** Unmarshals the next signed 32 bit integer */
    public final int nextS32 (boolean reverse)
    {
		int retval = getS32 (_offset, reverse);
		_offset += 4;
		return retval;
    }

    public final int nextS32 ()
    {
    	return nextS32(false);
    }
    /** Unmarshals an array of signed 32 bit integers. */
    public final int [] nextS32Array ()
    {
		int len = /* unsigned */ nextS32 ();
		int retval [] = new int [len];
		for (int i = 0; i < len; i++) {
		    retval [i] = nextS32 ();
		}
		return retval;
    }



    /** Unmarshals a signed 64 bit integer from a fixed buffer offset */
    public final long getS64 (int index)
    {
		long retval = 0xffffffff & getS32 (index);
	
		retval |= (getS32 (index + 4) << 32);
		return retval;
    }

    /** Marshals a 64 bit integer (signed or unsigned) */
    protected final void put64 (long value)
    {
		put32 ((int) value);
		put32 ((int) (value >> 32));
    }

    /** Unmarshals the next signed 64 bit integer */
    public final long nextS64 ()
    {
		long retval = getS64 (_offset);
		_offset += 8;
		return retval;
    }

    /** Unmarshals an array of signed 64 bit integers */
    public final long [] nextS64Array ()
    {
		int len = /* unsigned */ nextS32 ();
		long retval [] = new long [len];
		for (int i = 0; i < len; i++)
		    retval [i] = nextS64 ();
		return retval;
    }

    // Java doesn't yet support 128 bit integers,
    // needed to support primitives like these:

    // getU128
    // putU128
    // nextU128
    // nextU128Array

    // getS128
    // putS128
    // nextS128
    // nextS128Array


    /** Unmarshals a string (or null) from a fixed buffer offset. */
    public final String getString (int index)
    {
		int	savedOffset = _offset;
		String	retval;
	
		_offset = index;
		retval = nextString ();
		_offset = savedOffset;
		return retval;
    }

    /** Marshals a string, of length at most 254 characters, or null. */
    protected void putString (String s)
    {
		if (s == null) {
		    put8 (0);
		    return;
		}
	
		int len = s.length ();
	
		if (len > 254)
		    throw new IllegalArgumentException ();
		put8 (len + 1);
		for (int i = 0; i < len; i++)
		    put16 ((int) s.charAt (i));
		put16 (0);
    }

    /** Unmarshals the next string (or null). */
    public String nextString ()
    {
		int		len = nextU8 ();
		StringBuffer	str;
	
		if (len == 0)
		    return null;
	
		str = new StringBuffer (len);
		for (int i = 0; i < len; i++)
		    str.append ((char) nextU16 ());
		// drop terminal null
		str.setLength (len - 1);
		return str.toString ();
    }
    
}
