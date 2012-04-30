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

public class PtpResponse {
    /** ResponseCode: */
    public static final int Undefined = 0x2000;
    /** ResponseCode: */
    public static final int OK = 0x2001;
    /** ResponseCode: */
    public static final int GeneralError = 0x2002;
    /** ResponseCode: */
    public static final int SessionNotOpen = 0x2003;

    /** ResponseCode: */
    public static final int InvalidTransactionID = 0x2004;
    /** ResponseCode: */
    public static final int OperationNotSupported = 0x2005;
    /** ResponseCode: */
    public static final int ParameterNotSupported = 0x2006;
    /** ResponseCode: */
    public static final int IncompleteTransfer = 0x2007;

    /** ResponseCode: */
    public static final int InvalidStorageID = 0x2008;
    /** ResponseCode: */
    public static final int InvalidObjectHandle = 0x2009;
    /** ResponseCode: */
    public static final int DevicePropNotSupported = 0x200a;
    /** ResponseCode: */
    public static final int InvalidObjectFormatCode = 0x200b;

    /** ResponseCode: */
    public static final int StoreFull = 0x200c;
    /** ResponseCode: */
    public static final int ObjectWriteProtected = 0x200d;
    /** ResponseCode: */
    public static final int StoreReadOnly = 0x200e;
    /** ResponseCode: */
    public static final int AccessDenied = 0x200f;


    /** ResponseCode: */
    public static final int NoThumbnailPresent = 0x2010;
    /** ResponseCode: */
    public static final int SelfTestFailed = 0x2011;
    /** ResponseCode: */
    public static final int PartialDeletion = 0x2012;
    /** ResponseCode: */
    public static final int StoreNotAvailable = 0x2013;

    /** ResponseCode: */
    public static final int SpecificationByFormatUnsupported = 0x2014;
    /** ResponseCode: */
    public static final int NoValidObjectInfo = 0x2015;
    /** ResponseCode: */
    public static final int InvalidCodeFormat = 0x2016;
    /** ResponseCode: */
    public static final int UnknownVendorCode = 0x2017;

    /** ResponseCode: */
    public static final int CaptureAlreadyTerminated = 0x2018;
    /** ResponseCode: */
    public static final int DeviceBusy = 0x2019;
    /** ResponseCode: */
    public static final int InvalidParentObject = 0x201a;
    /** ResponseCode: */
    public static final int InvalidDevicePropFormat = 0x201b;

    /** ResponseCode: */
    public static final int InvalidDevicePropValue = 0x201c;
    /** ResponseCode: */
    public static final int InvalidParameter = 0x201d;
    /** ResponseCode: */
    public static final int SessionAlreadyOpen = 0x201e;
    /** ResponseCode: */
    public static final int TransactionCanceled = 0x201f;

    /** ResponseCode: */
    public static final int SpecificationOfDestinationUnsupported = 0x2020;
    
    public static final int HardwareError = 0xa001;
    public static final int OutOfFocus = 0xa002;
    public static final int ChangeCameraModeFailed = 0xa003;
    public static final int InvalidStatus = 0xa004;
    public static final int SetPropertyNotSupport = 0xa005;
    public static final int WbPresetError = 0xa006;
    public static final int DustRefenreceError = 0xa007;
    public static final int ShutterSpeedBulb = 0xa008;
    public static final int MirrorUpSquence = 0xa009;
    public static final int CameraModeNotAdjustFnumber = 0xa00a;
    public static final int NotLiveView = 0xa00b;
    public static final int MfDriveStepEnd = 0xa00c;
    public static final int MfDriveStepInsufficiency = 0xa00e;
    public static final int InvalidObjectPropCode = 0xa801;
    public static final int InvalidObjectPropFormat = 0xa802;
    public static final int ObjectPropNotSupported = 0xa80a;
    
    

}
