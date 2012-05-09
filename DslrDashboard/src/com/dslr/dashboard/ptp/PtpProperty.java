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

import java.util.Vector;

public class PtpProperty {
    int			propertyCode;
    int			dataType;
    boolean		writable;
    Object		factoryDefault;
    Object		currentValue;
    int			formType;
    Object		constraints;

    public PtpProperty () { }

    public void parse (Buffer data)
    {
		data.parse ();
	
		// per 13.3.3, tables 23, 24, 25
		propertyCode = data.nextU16 ();
		dataType = data.nextU16 ();
		writable = data.nextU8 () != 0;
	
		// FIXME use factories, as vendor hooks
		factoryDefault = PtpPropertyValue.get (dataType, data);
		currentValue = PtpPropertyValue.get (dataType, data);
	
		formType = data.nextU8 ();
		switch (formType) {
		    case 0:	// no more
			break;
		    case 1:	// range: min, max, step
			constraints = new PtpRange (dataType, data);
			break;
		    case 2:	// enumeration: n, value1, ... valueN
			constraints = parseEnumeration (data);
			break;
		    default:
			System.err.println ("ILLEGAL prop desc form, " + formType);
			formType = 0;
			break;
		}
    }

    public int getPropertyCode(){
    	return propertyCode;
    }
    public int getDataType(){
    	return dataType;
    }
    /** Returns true if the property is writable */
    public boolean isWritable ()
	{ return writable; }

    /** Returns the current value (treat as immutable!) */
    public Object getValue ()
	{ return currentValue; }

    /** Returns the factory default value (treat as immutable!) */
    public Object getDefault ()
	{ return factoryDefault; }


    // code values, per 13.3.5 table 26

    public static final int BatteryLevel = 0x5001;
    public static final int FunctionalMode = 0x5002;
    public static final int ImageSize = 0x5003;

    public static final int CompressionSetting = 0x5004;
    public static final int WhiteBalance = 0x5005;
    public static final int RGBGain = 0x5006;
    public static final int FStop = 0x5007;

    public static final int FocalLength = 0x5008;
    public static final int FocusDistance = 0x5009;
    public static final int FocusMode = 0x500a;
    public static final int ExposureMeteringMode = 0x500b;

    public static final int FlashMode = 0x500c;
    public static final int ExposureTime = 0x500d;
    public static final int ExposureProgramMode = 0x500e;
    public static final int ExposureIndex = 0x500f;

    public static final int ExposureBiasCompensation = 0x5010;
    public static final int DateTime = 0x5011;
    public static final int CaptureDelay = 0x5012;
    public static final int StillCaptureMode = 0x5013;

    public static final int Contrast = 0x5014;
    public static final int Sharpness = 0x5015;
    public static final int DigitalZoom = 0x5016;
    public static final int EffectMode = 0x5017;

    public static final int BurstNumber = 0x5018;
    public static final int BurstInterval = 0x5019;
    public static final int TimelapseNumber = 0x501a;
    public static final int TimelapseInterval = 0x501b;

    public static final int FocusMeteringMode = 0x501c;
    public static final int UploadURL = 0x501d;
    public static final int Artist = 0x501e;
    public static final int CopyrightInfo = 0x501f;

    public static final int WbTuneAuto					= 0xd017;
    public static final int WbTuneIncandescent			= 0xd018;
    public static final int WbTuneFluorescent			= 0xd019;
    public static final int WbTuneSunny					= 0xd01a;
    public static final int WbTuneFlash					= 0xd01b;
    public static final int WbTuneCloudy				= 0xd01c;
    public static final int WbTuneShade					= 0xd01d;
    public static final int WbPresetDataNo				= 0xd01f;
    public static final int WbPresetDataValue0			= 0xd025;
    public static final int WbPresetDataValue1			= 0xd026;
    public static final int ColorSpace					= 0xd032;
    public static final int ResetCustomSetting			= 0xd045;
    public static final int IsoAutocontrol				= 0xd054;
    public static final int ExposureEvStep				= 0xd056;
    public static final int AfAtLiveView				= 0xd05d;
    public static final int AeLockRelease				= 0xd05e;
    public static final int AeAfLockSetting				= 0xd05f;
    public static final int AutoMeterOffDelay			= 0xd062;
    public static final int SelfTimerDelay				= 0xd063;
    public static final int LcdPowerOff					= 0xd064;
    public static final int ImageConfirmTimeAfterPhoto	= 0xd065;
    public static final int AutoOffTime					= 0xd066;
    public static final int ExposureDelay				= 0xd06a;
    public static final int NoiseReduction				= 0xd06b;
    public static final int NumberingMode				= 0xd06c;
    public static final int NoiseReductionHiIso			= 0xd070;
    public static final int BracketingType				= 0xd078;
    public static final int FunctionButton				= 0xd084;
    public static final int CommandDialRotation			= 0xd085;
    public static final int EnableShutter				= 0xd08a;
    public static final int CommentString				= 0xd090;
    public static final int Enablecomment				= 0xd091;
    public static final int OrientationSensorMode		= 0xd092;
    public static final int MovieRecordScreenSize		= 0xd0a0;
    public static final int MovieRecordWithVoice		= 0xd0a1;
    public static final int MovieRecProhibitionCondition = 0xd0a4;
    public static final int LiveViewScreenDisplaySetting = 0xd0b2;
    public static final int EnableBracketing			= 0xd0c0;
    public static final int AeBracketingStep			= 0xd0c1;
    public static final int AeBracketingCount			= 0xd0c3;
    public static final int WbBracketingStep			= 0xd0c4;
    public static final int LensId						= 0xd0e0;
    public static final int LensSort					= 0xd0e1;
    public static final int LensType					= 0xd0e2;
    public static final int LensfocalMin				= 0xd0e3;
    public static final int LensFocalMax				= 0xd0e4;
    public static final int LensApatureMin				= 0xd0e5;
    public static final int LensApatureMax				= 0xd0e6;
    public static final int FinderIsoDisplay			= 0xd0f0;
    public static final int SelfTimerShootExpose		= 0xd0f5;
    public static final int AutoDistortion				= 0xd0f8;
    public static final int SceneMode					= 0xd0f9;
    public static final int ShutterSpeed				= 0xd100;
    public static final int ExternalDcIn				= 0xd101;
    public static final int WarningStatus				= 0xd102;
    public static final int RemainingExposure			= 0xd103;
    public static final int AfLockStatus				= 0xd104;
    public static final int AeLockStatus				= 0xd105;
    public static final int FocusArea					= 0xd108;
    public static final int FlexibleProgram				= 0xd109;
    public static final int RecordingMedia				= 0xd10b;
    public static final int UsbSpeed					= 0xd10c;
    public static final int CcdNumber					= 0xd10d;
    public static final int Orientation					= 0xd10e;
    public static final int ExternalSpeedLightExist		= 0xd120;
    public static final int ExternalSpeedLightStatus	= 0xd121;
    public static final int ExternalSpeedLightSort		= 0xd122;
    public static final int FlashCompensation			= 0xd124;
    public static final int NewExternalSpeedLightMode	= 0xd125;
    public static final int InternalFlashCompensation	= 0xd126;
    public static final int ActiveDLighting				= 0xd14e;
    public static final int WbTuneFluorescentType		= 0xd14f;
    public static final int Beep						= 0xd160;
    public static final int AfModeSelect				= 0xd161;
    public static final int AfSubLight					= 0xd163;
    public static final int IsoAutoShutterTimer			= 0xd164;
    public static final int InternalFlashMode			= 0xd167;
    public static final int IsoAutoSetting				= 0xd16a;
    public static final int RemoteControlDelay			= 0xd16b;
    public static final int GridDisplay					= 0xd16c;
    public static final int InternalFlashManual			= 0xd16d;
    public static final int DateImprintSetting			= 0xd170;
    public static final int DateCounterSelect			= 0xd171;
    public static final int DateCountData				= 0xd172;
    public static final int DateCountDisplaySetting		= 0xd173;
    public static final int RangeFinderSetting			= 0xd174;
    public static final int IsoautoHighLimit			= 0xd183;
    public static final int IndicatorDisplay			= 0xd18d;
    public static final int LiveViewStatus				= 0xd1a2;
    public static final int LiveViewImageZoomRatio		= 0xd1a3;
    public static final int LiveViewProhibitionCondition = 0xd1a4;
    public static final int ExposureDisplayStatus		= 0xd1b0;
    public static final int ExposureIndicateStatus		= 0xd1b1;
    public static final int InfoDisplayErrorStatus		= 0xd1b2;
    public static final int ExposureIndicateLightup		= 0xd1b3;
    public static final int InternalFlashPopup			= 0xd1c0;
    public static final int InternalFlashStatus			= 0xd1c1;
    public static final int ActivePicCtrlItem			= 0xd200;
    public static final int ChangePicCtrlItem			= 0xd201;
    public static final int SessionInitiatorVersionInfo	= 0xd406;
    public static final int PerceivedDeviceType			= 0xd407;

    /**
     * This class describes value ranges by minima, maxima,
     * and permissible increments.
     */
    public static final class PtpRange
    {
	private Object	min, max, step;

	PtpRange (int dataType, Buffer data)
	{
	    min = PtpPropertyValue.get (dataType, data);
	    max = PtpPropertyValue.get (dataType, data);
	    step = PtpPropertyValue.get (dataType, data);
	}

	/** Returns the maximum value of this range */
	public Object getMaximum () { return max; }

	/** Returns the minimum value of this range */
	public Object getMinimum () { return min; }

	/** Returns the increment of values in this range */
	public Object getIncrement () { return step; }
    }

    /** Returns any range constraints for this property's value, or null */
    public PtpRange getRange ()
    {
	if (formType == 1)
	    return (PtpRange) constraints;
	return null;
    }


    private Vector<Object> parseEnumeration (Buffer data)
    {
	int	len = data.nextU16 ();
	Vector<Object>	retval = new Vector<Object> (len);

	while (len-- > 0)
	    retval.addElement (PtpPropertyValue.get (dataType, data));
	return retval;
    }


    /** Returns any enumerated options for this property's value, or null */
    public Vector<?> getEnumeration ()
    {
	if (formType == 2)
	    return (Vector<?>) constraints;
	return null;
    }

}
