// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
//

#include <string.h>
#include <jni.h>
#include <cstring>
#include <sstream>
#include <iostream>
#include <math.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <exiv2/exiv2.hpp>
#include <libraw/libraw.h>
#include <netinet/in.h>

#define LOG_TAG "dslrdashboard_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct
{
	uint8_t red;
	uint8_t green;
	uint8_t blue;
	uint8_t alpha;
} argb;

typedef Exiv2::ExifData::const_iterator (*EasyAccessFct)(const Exiv2::ExifData& ed);


extern "C" {
JNIEXPORT jobject JNICALL Java_com_dslr_dashboard_NativeMethods_loadRawImage(
	JNIEnv* env, jobject thiz, jstring imgPath );

JNIEXPORT jboolean JNICALL Java_com_dslr_dashboard_NativeMethods_loadRawImageThumb(
	JNIEnv* env, jobject thiz, jstring imgPath, jstring thumbPath);

//JNIEXPORT jobjectArray JNICALL Java_com_dslr_dashboard_NativeMethods_getExifInfo(
//	JNIEnv* env, jobject thiz, jstring imgPath);

JNIEXPORT jint JNICALL Java_com_dslr_dashboard_NativeMethods_getExifData(
	JNIEnv* env, jobject thiz, jstring imgPath, jint count, jobject callback);

JNIEXPORT jint JNICALL Java_com_dslr_dashboard_NativeMethods_setGPSExifData(
	JNIEnv* env, jobject thiz, jstring imgPath, jdouble latitude, jdouble longitude, jdouble altitude);
}

// no error reporting, only params check
void write_ppm(libraw_processed_image_t *img, const char *basename)
{
    if(!img) return;
    // type SHOULD be LIBRAW_IMAGE_BITMAP, but we'll check
    if(img->type != LIBRAW_IMAGE_BITMAP) return;
    // only 3-color images supported...
    if(img->colors != 3) return;

    char fn[1024];
    snprintf(fn,1024,"%s.ppm",basename);
    FILE *f = fopen(fn,"wb");
    if(!f) return;
    fprintf (f, "P6\n%d %d\n%d\n", img->width, img->height, (1 << img->bits)-1);
/*
  NOTE:
  data in img->data is not converted to network byte order.
  So, we should swap values on some architectures for dcraw compatibility
  (unfortunately, xv cannot display 16-bit PPMs with network byte order data
*/
#define SWAP(a,b) { a ^= b; a ^= (b ^= a); }
    if (img->bits == 16 && htons(0x55aa) != 0x55aa)
        for(unsigned i=0; i< img->data_size; i+=2)
            SWAP(img->data[i],img->data[i+1]);
#undef SWAP

    fwrite(img->data,img->data_size,1,f);
    fclose(f);
}

void write_thumb(libraw_processed_image_t *img, const char *basename)
{
    if(!img) return;

    if(img->type == LIBRAW_IMAGE_BITMAP)
        {
            char fnt[1024];
            snprintf(fnt,1024,"%s.thumb",basename);
            write_ppm(img,fnt);
        }
    else if (img->type == LIBRAW_IMAGE_JPEG)
        {
            char fn[1024];
            snprintf(fn,1024,"%s.jpg",basename);
            FILE *f = fopen(fn,"wb");
            if(!f) return;
            fwrite(img->data,img->data_size,1,f);
            fclose(f);
        }
}


JNIEXPORT jobject JNICALL
Java_com_dslr_dashboard_NativeMethods_loadRawImage( JNIEnv* env,
                                                  jobject thiz, jstring imgPath )
{
    const char *naziv = env->GetStringUTFChars(imgPath, NULL);

    jobject myBitmap = NULL;
    void*              pixelscolor;
    AndroidBitmapInfo  infocolor;
    int ret;

    // Creation of image processing object
    LibRaw RawProcessor;

    // The date in TIFF is written in the local format; let us specify the timezone for compatibility with dcraw
    putenv ((char*)"TZ=UTC");

    // Let us define variables for convenient access to fields of RawProcessor

	#define P1  RawProcessor.imgdata.idata
	#define S   RawProcessor.imgdata.sizes
	#define C   RawProcessor.imgdata.color
	#define T   RawProcessor.imgdata.thumbnail
	#define P2  RawProcessor.imgdata.other
	#define OUT RawProcessor.imgdata.params

    OUT.output_tiff = 0; // Let us output TIFF
    OUT.half_size = 1;
    OUT.output_bps = 8;
    OUT.document_mode = 0;
    OUT.use_auto_wb = 0;
    OUT.use_camera_matrix = 1;
    OUT.use_camera_wb = 1;
    OUT.output_color = 1;
    OUT.user_qual = 3;
    OUT.no_auto_bright = 1;
    OUT.fbdd_noiserd = 0;
    OUT.green_matching = 1;

    LOGI("Image path %s",naziv);
    // Let us open the file
    if( (ret = RawProcessor.open_file(naziv,20000000L)) != LIBRAW_SUCCESS)
       {

           LOGE("Cannot open %s: %s\n",naziv,libraw_strerror(ret));
           RawProcessor.recycle();
           goto end;
       }

    if( (ret = RawProcessor.unpack() ) != LIBRAW_SUCCESS)
       {
           LOGE("Cannot unpack %s: %s\n",naziv,libraw_strerror(ret));

           if(LIBRAW_FATAL_ERROR(ret))
                     goto end;
           // if there has been a non-fatal error, we will try to continue
       }

    // Let us unpack the thumbnail
    if( (ret = RawProcessor.unpack_thumb() ) != LIBRAW_SUCCESS)
       {
            // error processing is completely similar to the previous case
             LOGE("Cannot unpack_thumb %s: %s\n",naziv,libraw_strerror(ret));
             if(LIBRAW_FATAL_ERROR(ret))
                     goto end;
      }
    else // We have successfully unpacked the thumbnail, now let us write it to a file
      {
      }

    if(OUT.document_mode)
            ret = RawProcessor.dcraw_document_mode_processing();
    else
            ret = RawProcessor.dcraw_process();


     if(LIBRAW_SUCCESS != ret ) // error at the previous step
           {
                LOGE("Cannot do postprocessing on %s: %s\n",naziv,libraw_strerror(ret));
                if(LIBRAW_FATAL_ERROR(ret))
                         goto end;
           }
    else  // Successful document processing
      {

 	   libraw_processed_image_t *image = RawProcessor.dcraw_make_mem_image(&ret);
 	   if (image)
 	   {
 		   LOGI("Creating bitmap width: %d ;  height: %d", image->width, image->height);

			jclass bitmapConfig = env->FindClass("android/graphics/Bitmap$Config");
			jfieldID rgb888FieldID = env->GetStaticFieldID(bitmapConfig, "ARGB_8888",
			    "Landroid/graphics/Bitmap$Config;");
			jobject rgb888Obj = env->GetStaticObjectField(bitmapConfig, rgb888FieldID);

			jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
			jmethodID createBitmapMethodID = env->GetStaticMethodID(bitmapClass,"createBitmap",
			    "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

			myBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID,
			    image->width, image->height, rgb888Obj);

			if (myBitmap)
			{
			    if ((ret = AndroidBitmap_getInfo(env, myBitmap, &infocolor)) == 0)
			    {
				    if ((ret = AndroidBitmap_lockPixels(env, myBitmap, &pixelscolor)) == 0)
				    {
				    	LOGI("Pixels locked, transfering image");
				    	uint8_t *raw_data = (uint8_t*)image->data;
				        for (int y=0;y<infocolor.height;y++)
				        {
				        	argb * line = (argb *) pixelscolor;
				        	for (int x=0;x<infocolor.width;x++)
				        	{
				        		//line[x].alpha = 0;
				        		line[x].red = raw_data[0];
				        		line[x].green = raw_data[1];
				        		line[x].blue = raw_data[2];
				        		raw_data += 3;
				        	}

				        	pixelscolor = (char *)pixelscolor + infocolor.stride;
				        }
				        LOGI("Pixels transfered, unlocking");
				        AndroidBitmap_unlockPixels(env, myBitmap);

				    }
				    else
				        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);

			    }
			    else
			        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
			}


 		   LibRaw::dcraw_clear_mem(image);
 	   }
 	   else
 	   {
            LOGE("Make mem image failed on %s: %s\n",naziv,libraw_strerror(ret));
 	   }
      }



	end:
	env->ReleaseStringUTFChars(imgPath, naziv);
	RawProcessor.recycle();
	return myBitmap; //env->NewStringUTF(c_result);
}

JNIEXPORT jboolean JNICALL
Java_com_dslr_dashboard_NativeMethods_loadRawImageThumb( JNIEnv* env,
                                                  jobject thiz, jstring imgPath, jstring thumbPath)
{
    const char *naziv = env->GetStringUTFChars(imgPath, NULL);
    const char *thumbNaziv = env->GetStringUTFChars(thumbPath, NULL);

    jobject myBitmap = NULL;
    void*              pixelscolor;
    AndroidBitmapInfo  infocolor;
    int ret;
    char thumbfn[1024];

    bool rezultat = false;

    // Creation of image processing object
    LibRaw RawProcessor;

    // The date in TIFF is written in the local format; let us specify the timezone for compatibility with dcraw
    putenv ((char*)"TZ=UTC");

    // Let us define variables for convenient access to fields of RawProcessor

	#define P1  RawProcessor.imgdata.idata
	#define S   RawProcessor.imgdata.sizes
	#define C   RawProcessor.imgdata.color
	#define T   RawProcessor.imgdata.thumbnail
	#define P2  RawProcessor.imgdata.other
	#define OUT RawProcessor.imgdata.params

    LOGI("Image path %s",naziv);
    // Let us open the file
    if( (ret = RawProcessor.open_file(naziv,20000000L)) != LIBRAW_SUCCESS)
       {
           // recycle() is needed only if we want to free the resources right now.
           // If we process files in a cycle, the next open_file()
           // will also call recycle(). If a fatal error has happened, it means that recycle()
           // has already been called (repeated call will not cause any harm either).

           LOGE("Cannot open %s: %s\n",naziv,libraw_strerror(ret));
           RawProcessor.recycle();
           goto end;
       }
	   LOGI("Thumb image width: %d ;  height: %d", T.twidth, T.theight);


    // Let us unpack the thumbnail
    if( (ret = RawProcessor.unpack_thumb() ) != LIBRAW_SUCCESS)
       {
            // error processing is completely similar to the previous case
             LOGE("Cannot unpack_thumb %s: %s\n",naziv,libraw_strerror(ret));
             if(LIBRAW_FATAL_ERROR(ret))
                     goto end;
      }
    else // We have successfully unpacked the thumbnail, now let us write it to a file
      {



    	        snprintf(thumbfn,sizeof(thumbfn),"%s.%s",thumbNaziv,T.tformat == LIBRAW_THUMBNAIL_JPEG ? "jpg" : "ppm");
    	        LOGE("Saving thumb: ", thumbfn);

    	        if( LIBRAW_SUCCESS != (ret = RawProcessor.dcraw_thumb_writer(thumbfn)))
    	          {
    	                  LOGE("Cannot write %s: %s\n",thumbfn,libraw_strerror(ret));

    	                  // in the case of fatal error, we should terminate processing of the current file
    	                  if(LIBRAW_FATAL_ERROR(ret))
    	                            goto end;
    	          }
    	        else rezultat = true;
      }

	end:
	env->ReleaseStringUTFChars(imgPath, naziv);
	env->ReleaseStringUTFChars(thumbPath, thumbNaziv);
	RawProcessor.recycle();
	return rezultat;
}

JNIEXPORT jint JNICALL Java_com_dslr_dashboard_NativeMethods_getExifData(
	JNIEnv* env, jobject thiz, jstring imgPath, jint count, jobject callback)
{
	jclass cls = env->GetObjectClass(thiz);
	jmethodID mid = env->GetMethodID(cls, "exifValueCallback", "(Ljava/lang/String;ILjava/lang/Object;)V");
	  if (mid == 0)
	    return -1;
	jmethodID vid = env->GetMethodID(cls, "exifNameCallback", "(ILjava/lang/Object;)Ljava/lang/String;");
	if (vid == 0)
		return -1;

	const char *naziv = env->GetStringUTFChars(imgPath, NULL);
	LOGI("Exif image: %s", naziv);
    Exiv2::Image::AutoPtr image = Exiv2::ImageFactory::open(naziv);
	env->ReleaseStringUTFChars(imgPath, naziv);

    if (image.get() != 0) {
    	image->readMetadata();
    	Exiv2::ExifData& ed = image->exifData();

    	for(int i = 0; i < count; i++) {
    		jstring obj = (jstring)env->CallObjectMethod(thiz, vid, i, callback);

    		const char *nesto = env->GetStringUTFChars(obj, NULL);
    		LOGI("Callback value: %s", nesto);
    		Exiv2::ExifMetadata::iterator it = ed.findKey(Exiv2::ExifKey(nesto));
    		env->ReleaseStringUTFChars(obj, nesto);

    		if (it != ed.end())
    		{
            	std::string value = it->print(&ed);
        		env->CallVoidMethod(thiz, mid, env->NewStringUTF(value.c_str()), i, callback);
    		}
    	}
    	return 0;
    }
    return -1;
}

JNIEXPORT jint JNICALL Java_com_dslr_dashboard_NativeMethods_setGPSExifData(
	JNIEnv* env, jobject thiz, jstring imgPath, jdouble latitude, jdouble longitude, jdouble altitude)
{
	char scratchBufLatitude[100];
	long int degLatitude, minLatitude, secLatitude;
	char scratchBufLongitude[100];
	long int degLongitude, minLongitude, secLongitude;

	const char *naziv = env->GetStringUTFChars(imgPath, NULL);
	LOGI("Exif image: %s", naziv);
    Exiv2::Image::AutoPtr image = Exiv2::ImageFactory::open(naziv);
	env->ReleaseStringUTFChars(imgPath, naziv);

    if (image.get() != 0) {
    	image->readMetadata();
    	Exiv2::ExifData& ed = image->exifData();

        if ( ed.findKey(Exiv2::ExifKey("Exif.GPSInfo.GPSVersionID")) == ed.end())
        {
                Exiv2::Value::AutoPtr value = Exiv2::Value::create(Exiv2::unsignedByte);
                value->read("2 2 0 0");
                ed.add(Exiv2::ExifKey("Exif.GPSInfo.GPSVersionID"), value.get());
                ed["Exif.GPSInfo.GPSMapDatum"] = "WGS-84";
        }
        ed["Exif.GPSInfo.GPSLatitudeRef"] = (latitude < 0 ) ? "S" : "N";

        degLatitude = (int)floor(fabs(latitude));
        minLatitude = (int)floor((fabs(latitude) - degLatitude)*60);
        secLatitude = (int)floor(((fabs(latitude) - degLatitude)*60 - minLatitude )*60);
        snprintf(scratchBufLatitude, 100, "%ld/1 %ld/1 %ld/1", degLatitude, minLatitude, secLatitude);
        LOGI("Latitude %s", scratchBufLatitude);
        ed["Exif.GPSInfo.GPSLatitude"] = scratchBufLatitude;

        ed["Exif.GPSInfo.GPSLongitudeRef"] = (longitude < 0 ) ? "W" : "E";
        degLongitude = (int)floor(fabs(longitude));
        minLongitude = (int)floor((fabs(longitude) - degLongitude)*60);
        secLongitude = (int)floor(((fabs(longitude) - degLongitude)*60 - minLongitude )*60);
        snprintf(scratchBufLongitude, 100, "%ld/1 %ld/1 %ld/1", degLongitude, minLongitude, secLongitude);
        LOGI("Longitude %s", scratchBufLongitude);
        ed["Exif.GPSInfo.GPSLongitude"] = scratchBufLongitude;

        ed["Exif.GPSInfo.GPSAltitudeRef"] = (altitude >= 0) ? 0 : 1;

        snprintf(scratchBufLongitude, 100, "%ld/100", (int)floor(fabs(altitude * 100)));
        LOGI("Altitude %s", scratchBufLongitude);
        ed["Exif.GPSInfo.GPSAltitude"] = scratchBufLongitude;


        time_t datetime = time(NULL);
        ctime(&datetime);

    	struct tm *tmp = gmtime(&datetime);
    	tmp->tm_isdst = 0;


    	snprintf(scratchBufLongitude, 100, "%d/1 %d/1 %d/1",
    			tmp->tm_hour, tmp->tm_min,
    			tmp->tm_sec);
        LOGI("Time %s", scratchBufLongitude);
        ed["Exif.GPSInfo.GPSTimeStamp"] = scratchBufLongitude;

    	snprintf(scratchBufLongitude, 100, "%d:%02d:%02d",
    			tmp->tm_year + 1900,
    			tmp->tm_mon + 1,
    			tmp->tm_mday);
        LOGI("Date %s", scratchBufLongitude);
        ed["Exif.GPSInfo.GPSDateStamp"] = scratchBufLongitude;
        ed["Exif.GPSInfo.GPSProcessingMethod"] = "GPS";

        image->setExifData(ed);
        image->writeMetadata();
    }
    return 0;
}

