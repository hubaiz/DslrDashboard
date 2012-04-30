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

import android.graphics.Bitmap;

public class PtpObjectInfo {
	public int		objectId;
    public int		storageId;			// 8.1
    public int		objectFormatCode;		// 6.2
    public int		protectionStatus;		// 0 r/w, 1 r/o
    public int		objectCompressedSize;

    public int		thumbFormat;			// 6.2
    public int		thumbCompressedSize;
    public int		thumbPixWidth;
    public int		thumbPixHeight;

    public int		imagePixWidth;
    public int		imagePixHeight;
    public int		imageBitDepth;
    public int		parentObject;

    public int		associationType;		// 6.4
    public int		associationDesc;		// 6.4
    public int		sequenceNumber;			// (ordered associations)
    public String	filename;			// (sans path)

    public String	captureDate;			// DateTime string
    public String	modificationDate;		// DateTime string
    public String	keywords;

    public PtpObjectInfo(int objectIdentification, Buffer buf){
    	objectId = objectIdentification;
    	parse(buf);
    }
    
    public Bitmap thumb = null;
    
    public void parse (Buffer buf)
    {
    	buf.parse();
		storageId = buf.nextS32 ();
		objectFormatCode = buf.nextU16 ();
		protectionStatus = buf.nextU16 ();
		objectCompressedSize = /* unsigned */ buf.nextS32 ();
	
		thumbFormat = buf.nextU16 ();
		thumbCompressedSize = /* unsigned */ buf.nextS32 ();
		thumbPixWidth = /* unsigned */ buf.nextS32 ();
		thumbPixHeight = /* unsigned */ buf.nextS32 ();
	
		imagePixWidth = /* unsigned */ buf.nextS32 ();
		imagePixHeight = /* unsigned */ buf.nextS32 ();
		imageBitDepth = /* unsigned */ buf.nextS32 ();
		parentObject = buf.nextS32 ();
	
		associationType = buf.nextU16 ();
		associationDesc = buf.nextS32 ();
		sequenceNumber = /* unsigned */ buf.nextS32 ();
		filename = buf.nextString ();
	
		captureDate = buf.nextString ();
		modificationDate = buf.nextString ();
		keywords = buf.nextString ();
    }

    /** ObjectFormatCode:  unrecognized non-image format */
    public static final int Undefined = 0x3000;
    /** ObjectFormatCode:  associations include folders and panoramas */
    public static final int Association = 0x3001;
    /** ObjectFormatCode: */
    public static final int Script = 0x3002;
    /** ObjectFormatCode: */
    public static final int Executable = 0x3003;

    /** ObjectFormatCode: */
    public static final int Text = 0x3004;
    /** ObjectFormatCode: */
    public static final int HTML = 0x3005;
    /** ObjectFormatCode: */
    public static final int DPOF = 0x3006;
    /** ObjectFormatCode: */
    public static final int AIFF = 0x3007;

    /** ObjectFormatCode: */
    public static final int WAV = 0x3008;
    /** ObjectFormatCode: */
    public static final int MP3 = 0x3009;
    /** ObjectFormatCode: */
    public static final int AVI = 0x300a;
    /** ObjectFormatCode: */
    public static final int MPEG = 0x300b;

    /** ObjectFormatCode: */
    public static final int ASF = 0x300c;

    /** ObjectFormatCode: QuickTime video */
    public static final int QuickTime = 0x300d;

    /** ImageFormatCode: unknown image format */
    public static final int UnknownImage = 0x3800;
    /**
     * ImageFormatCode: EXIF/JPEG version 2.1, the preferred format
     * for thumbnails and for images.
     */
    public static final int EXIF_JPEG = 0x3801;
    /**
     * ImageFormatCode: Uncompressed TIFF/EP, the alternate format
     * for thumbnails.
     */
    public static final int TIFF_EP = 0x3802;
    /** ImageFormatCode: FlashPix image format */
    public static final int FlashPix = 0x3803;

    /** ImageFormatCode: MS-Windows bitmap image format */
    public static final int BMP = 0x3804;
    /** ImageFormatCode: Canon camera image file format */
    public static final int CIFF = 0x3805;
    // 3806 is reserved
    /** ImageFormatCode: Graphics Interchange Format (deprecated) */
    public static final int GIF = 0x3807;

    /** ImageFormatCode: JPEG File Interchange Format */
    public static final int JFIF = 0x3808;
    /** ImageFormatCode: PhotoCD Image Pac*/
    public static final int PCD = 0x3809;
    /** ImageFormatCode: Quickdraw image format */
    public static final int PICT = 0x380a;
    /** ImageFormatCode: Portable Network Graphics */
    public static final int PNG = 0x380b;

    /** ImageFormatCode: Tag Image File Format */
    public static final int TIFF = 0x380d;
    /** ImageFormatCode: TIFF for Information Technology (graphic arts) */
    public static final int TIFF_IT = 0x380e;
    /** ImageFormatCode: JPEG 2000 baseline */
    public static final int JP2 = 0x380f;

    /** ImageFormatCode: JPEG 2000 extended */
    public static final int JPX = 0x3810;


    /**
     * Returns true for format codes that have the image type bit set.
     */
    public boolean isImage ()
    {
	return (objectFormatCode & 0xf800) == 0x3800;
    }

    /**
     * Returns true for some recognized video format codes.
     */
    public boolean isVideo ()
    {
	switch (objectFormatCode) {
	case AVI:
	case MPEG:
	case ASF:
	case QuickTime:
	    return true;
	}
	return false;
    }


}
