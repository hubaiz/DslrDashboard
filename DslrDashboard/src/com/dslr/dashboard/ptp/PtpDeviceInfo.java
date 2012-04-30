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

public class PtpDeviceInfo {
    int		standardVersion;
    int		vendorExtensionId;
    int		vendorExtensionVersion;
    String	vendorExtensionDesc;

    int		functionalMode; 		// may change; 
    int		operationsSupported [];		// 10.2
    int		eventsSupported [];		// 12.5
    int		propertiesSupported [];		// 13.3.5

    int		captureFormats [];		// 6
    int		imageFormats [];		// 6
    String	manufacturer;
    String	model;

    String	deviceVersion;
    String	serialNumber;
    
    boolean isInitialized = false;

    public String getManufacturer(){
    	return manufacturer;
    }
    public String getModel() {
    	return model;
    }
    public String getDeviceVersion(){
    	return deviceVersion;
    }
    public String getSerialNumber(){
    	return serialNumber;
    }
    // FIXME add formal vendor hooks, which we'd consult for string
    // mappings ... we don't have any here.

    // Command, Response, ObjectInfo, Event, and DevicePropDesc can
    // all be subclassed; but we won't have instances here.  And
    // there's also the vendor extension stuff here.


    // input -- we can't know buffer size yet
    public PtpDeviceInfo ()
	{ 
    	isInitialized = false;
	}

    public PtpDeviceInfo(Buffer data)
    {
    	parse(data);
    }
    
    public boolean getIsInitialized(){
    	return isInitialized;
    }
    
    private boolean supports (int supported [], int code)
    {
	for (int i = 0; i < supported.length; i++) {
	    if (code == supported [i])
		return true;
	}
	return false;
    }

    public int[] getPropertiesSupported()
    {
    	return propertiesSupported;
    }

    /** Returns true iff the device supports this operation */
    public boolean supportsOperation (int opCode)
    {
	return supports (operationsSupported, opCode);
    }

    /** Returns true iff the device supports this event */
    public boolean supportsEvent (int eventCode)
    {
	return supports (eventsSupported, eventCode);
    }

    /** Returns true iff the device supports this property */
    public boolean supportsProperty (int propCode)
    {
	return supports (propertiesSupported, propCode);
    }

    /** Returns true iff the device supports this capture format */
    public boolean supportsCaptureFormat (int formatCode)
    {
	return supports (captureFormats, formatCode);
    }

    /** Returns true iff the device supports this image format */
    public boolean supportsImageFormat (int formatCode)
    {
	return supports (imageFormats, formatCode);
    }



    public void parse (Buffer data)
    {
	 data.parse();

	 standardVersion = data.nextU16 ();
	 vendorExtensionId = /* unsigned */ data.nextS32 ();
	 vendorExtensionVersion = data.nextU16 ();
	 vendorExtensionDesc = data.nextString ();

	 functionalMode = data.nextU16 ();
	 operationsSupported = data.nextU16Array ();
	 eventsSupported = data.nextU16Array ();
	 propertiesSupported = data.nextU16Array ();

	 captureFormats = data. nextU16Array ();
	 imageFormats = data.nextU16Array ();
	 manufacturer = data.nextString ();
	 model = data.nextString ();

	 deviceVersion = data.nextString ();
	 serialNumber = data.nextString ();
	 
	 isInitialized = true;
    }
}
