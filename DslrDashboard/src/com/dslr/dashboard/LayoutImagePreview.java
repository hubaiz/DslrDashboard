// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.dslr.dashboard.helper.CheckableImageView;
import com.dslr.dashboard.helper.CustomDialog;
import com.dslr.dashboard.helper.DslrHelper;
import com.dslr.dashboard.ptp.PtpCommand;
import com.dslr.dashboard.ptp.PtpObjectInfo;
import com.dslr.dashboard.ptp.PtpProperty;
import com.dslr.dashboard.ptp.PtpStorageInfo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class LayoutImagePreview extends RelativeLayout implements DslrLayout{

	private final static String TAG = "LayoutImagePreview";

	private int _galleryMode = -1;
	
	private Context _context;
	private DslrHelper _dslrHelper;
    private GridView imageGallery;
    private ImageGalleryAdapter imageGalleryAdapter;
    private ArrayList<ImageObjectHelper> imagesFromPhone;
    private ArrayList<ImageObjectHelper> imagesFromDslr;
    private String _imageSaveLocation;
	private ImageView btnSelectAll, btnDelete;
	
    private CheckableImageView chkPhone, chkCamera;
    

    
    private void switchGalleryMode(int galleryMode){
    	if (_galleryMode != galleryMode){
    		_galleryMode = galleryMode;
    		chkPhone.setChecked(galleryMode == ImageObjectHelper.PHONE_PICTURE);
    		chkCamera.setChecked(galleryMode == ImageObjectHelper.DSLR_PICTURE);

    		imageGallery.setVisibility(View.GONE);
    		
    		if (_galleryMode == ImageObjectHelper.PHONE_PICTURE){
    			imageGalleryAdapter.changeItems(imagesFromPhone);
    		}
    		else {
    			imageGalleryAdapter.changeItems(imagesFromDslr);
    		}
    		
    		imageGallery.setVisibility(View.VISIBLE);
    		imageGallery.invalidateViews();
    		
    		try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Log.e(TAG, e.getMessage());
			}
    		imageGallery.requestFocusFromTouch();
    		imageGallery.setSelection(0);
    	}
    }
    
	public LayoutImagePreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		_context = context;
		
		LayoutInflater.from(context).inflate(R.layout.layoutimagepreview, this, true);

		
        imagesFromDslr = new ArrayList<ImageObjectHelper>();
        imagesFromPhone = new ArrayList<ImageObjectHelper>();
        
        imageGalleryAdapter = new ImageGalleryAdapter(context, imagesFromPhone);

        imageGallery = (GridView)findViewById(R.id.img_gallery);
        imageGallery.setAdapter(imageGalleryAdapter);
        
		chkPhone = (CheckableImageView)findViewById(R.id.chk_phonePictures);
		chkPhone.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switchGalleryMode(ImageObjectHelper.PHONE_PICTURE);
			}
		});
		chkCamera = (CheckableImageView)findViewById(R.id.chk_dslrPictures);
		chkCamera.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				switchGalleryMode(ImageObjectHelper.DSLR_PICTURE);
				if (_dslrHelper.getPtpService().getIsPtpDeviceInitialized()){
					if (_dslrHelper.getPtpService().getPtpObjectsLoaded()) {
						loadObjectInfosFromCamera();
					}
					else {
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								_dslrHelper.getPtpService().loadObjectInfos();
							}
						}).start();
					}
				}
			}
		});

		switchGalleryMode(ImageObjectHelper.PHONE_PICTURE);
		
        imageGalleryAdapter.setOnSelectionChanged(new ImageGalleryAdapter.SelectionChangedListener() {
			
			@Override
			public void onSelectionChanged(ArrayList<ImageObjectHelper> selectedItems) {
			}
		});
        imageGalleryAdapter.setOnImageItemClicked(new ImageGalleryAdapter.ImageItemClickedListener() {
			
			@Override
			public void onImageItemClicked(ImageObjectHelper obj) {

				switch(obj.galleryItemType){
					case ImageObjectHelper.PHONE_PICTURE:
						Intent ipIntent = new Intent(_context, ImagePreviewActivity.class);
						ipIntent.setAction(Intent.ACTION_VIEW);
						Uri uri = Uri.fromFile(obj.file);
						ipIntent.setData(uri);
						_context.startActivity(ipIntent);
						break;
					case ImageObjectHelper.DSLR_PICTURE:
						Log.d(TAG, "sdcard image clicked");
						PtpCommand cmd = _dslrHelper.getPtpService().getLargeThumb(obj.objectInfo.objectId);
						if (cmd != null && cmd.isDataOk()){

							Bitmap bmp = BitmapFactory.decodeByteArray(cmd.incomingData().data(), 12, cmd.incomingData().data().length - 12);
							if (bmp != null) {
								Log.d(TAG, "Display DSLR image");
								Intent dslrIntent = new Intent(_context, ImagePreviewActivity.class);
								
								ByteArrayOutputStream bs = new ByteArrayOutputStream();
								bmp.compress(Bitmap.CompressFormat.JPEG, 85, bs);
								dslrIntent.putExtra("data", bs.toByteArray());								
							
								_context.startActivity(dslrIntent);
							}
						}
						break;
				}
			}
		});
        imageGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        		switch(_galleryMode){
        		case ImageObjectHelper.DSLR_PICTURE:
        			break;
        		case ImageObjectHelper.PHONE_PICTURE:
        			break;
        		}
        	}
		});
        
        btnDelete = (ImageView)findViewById(R.id.btn_gallerydelete);
        btnSelectAll = (ImageView)findViewById(R.id.btn_galleryselectall);
        btnSelectAll.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				imageGalleryAdapter.selectAll();
			}
		});
        btnSelectAll.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				imageGalleryAdapter.invert();
				return true;
			}
		});
        
        btnDelete = (ImageView)findViewById(R.id.btn_gallerydelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		    	CustomDialog.Builder customBuilder = new CustomDialog.Builder(_dslrHelper.getContext());
		    	customBuilder.setTitle("Image deletion")
		    		.setMessage("Delete selected images)") 
		    		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
		    		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							switch(_galleryMode)
							{
								case ImageObjectHelper.PHONE_PICTURE:
									for(int i = imageGalleryAdapter.getCount()-1; i >= 0; i--){
										ImageObjectHelper item = imageGalleryAdapter.items().get(i);
										if (item.isChecked) {
											item.deleteImage();
											imageGalleryAdapter.items().remove(i);
										}
									}
									imageGalleryAdapter.notifyDataSetChanged();
									break;
								case ImageObjectHelper.DSLR_PICTURE:
									
									break;
							}
						}
					});
		    	CustomDialog dialog = customBuilder.create();
		    	dialog.show();
			}
		});
	}

	
	private void loadImagesFromPhone(){
		Log.d(TAG, "LoadImagesFromPhone");
		imagesFromPhone.clear();
		File f = new File(_imageSaveLocation);
		if (f.exists()){
		File[] phoneFiles = f.listFiles();
		for(int i = 0; i < phoneFiles.length; i++){
			if (phoneFiles[i].isFile()){
				final ImageObjectHelper helper = new ImageObjectHelper();
				helper.file = phoneFiles[i];
				helper.galleryItemType = ImageObjectHelper.PHONE_PICTURE;
				
				if (!tryLoadThumb(helper))
				{
					String fExt = helper.getFileExt(helper.file.toString());
					if (fExt.equals("jpg") || fExt.equals("png")) {
						Bitmap thumb = null;
						final int IMAGE_MAX_SIZE = 30000; // 1.2MP
						
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;
						thumb = BitmapFactory.decodeFile(helper.file.getAbsolutePath(), options);
		
				        int scale = 1;
				        while ((options.outWidth * options.outHeight) * (1 / Math.pow(scale, 2)) > IMAGE_MAX_SIZE) {
				            scale++;
				        }
				        Log.d(TAG, "scale = " + scale + ", orig-width: " + options.outWidth       + ", orig-height: " + options.outHeight);
		
				        if (scale > 1) {
				            scale--;
					        options = new BitmapFactory.Options();
					        options.inSampleSize = scale;
					        thumb = BitmapFactory.decodeFile(helper.file.getAbsolutePath(), options);
				        }
				        else
				        	thumb = BitmapFactory.decodeFile(helper.file.getAbsolutePath());
				        if (thumb != null) {
							FileOutputStream fOut;
							try {
								fOut = new FileOutputStream(helper.getThumbFilePath("jpg"));
								thumb.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
								fOut.flush();
								fOut.close();
								thumb.recycle();
							} catch (Exception e) {
							}				        	
				        }
					}
					else {
						// try jni to create a thumb
						String proba = helper.getThumbFilePath("").getAbsolutePath();
						if (NativeMethods.getInstance().loadRawImageThumb(helper.file.getAbsolutePath(), proba ))
						{
							
						}
					}
				}
				imagesFromPhone.add(helper);
			}
		}
		}
		Log.d(TAG, "Images from phone - NotifyDataSetChanged");
		imageGalleryAdapter.notifyDataSetChanged();
	}
	
	private boolean tryLoadThumb(ImageObjectHelper helper){
		boolean rezultat = helper.tryLoadThumb("png");
		if (!rezultat){
			rezultat = helper.tryLoadThumb("jpg");
			if (!rezultat)
				rezultat = helper.tryLoadThumb("ppm");
		}
			
		return rezultat;
	}
	private void addPictureToGallery(File f){
	    Uri contentUri = Uri.fromFile(f);
	    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
	    mediaScanIntent.setData(contentUri);
	    _context.sendBroadcast(mediaScanIntent);		
}
	
	@Override
	public void setDslrHelper(DslrHelper dslrHelper) {
		_dslrHelper = dslrHelper;
	}

	@Override
	public void updatePtpProperty(PtpProperty property) {
		
	}

	@Override
	public void ptpServiceSet(boolean isSet) {
		if (isSet){
			_imageSaveLocation = _dslrHelper.getPtpService().getSdramSavingLocation();
		}
	}

	private void handleSdramEvent(PtpServiceEventType event, Object eventData){
		ImageObjectHelper helper = null;
		if (eventData != null)
			helper = (ImageObjectHelper)eventData;
		switch(event){
			case GetObjectFromSdramStart:
				break;
			case GetObjectFromSdramInfo:
				((ProgressBar)findViewById(R.id.img_gallery_progress)).setMax(helper.objectInfo.objectCompressedSize);
				break;
			case GetObjectFromSdramThumb:
				break;
			case GetObjectFromSdramProgress:
				((ProgressBar)findViewById(R.id.img_gallery_progress)).setProgress(helper.progress);
				break;
			case GetObjectFromSdramFinished:
				// we have the picture and it is saved
				// add it to gallery
				imagesFromPhone.add(helper);
				if (_galleryMode == ImageObjectHelper.PHONE_PICTURE){
					imageGalleryAdapter.notifyDataSetChanged();
					imageGallery.setSelection(imageGalleryAdapter.items().size()-1);
				}
				break;
		}
	}
	
	private void loadObjectInfosFromCamera(){
		imagesFromDslr.clear();
		for(PtpStorageInfo store : _dslrHelper.getPtpService().getPtpStorages().values()){
			for(PtpObjectInfo obj : store.objects.values()){
				addObjectFromCamera(obj);
			}
		}
		imageGalleryAdapter.notifyDataSetChanged();
	}
	
	private void addObjectFromCamera(PtpObjectInfo obj){
		switch(obj.objectFormatCode){
			case 0x3000:
			case 0x3801:
				ImageObjectHelper imgObj = new ImageObjectHelper();
				imgObj.objectInfo = obj;
				imgObj.galleryItemType = ImageObjectHelper.DSLR_PICTURE;
				imgObj.file = new File(_dslrHelper.getPtpService().getSdramSavingLocation() + "/.dslrthumbs/" + obj.filename + ".jpg");
				imagesFromDslr.add(imgObj);
				if (_galleryMode == ImageObjectHelper.DSLR_PICTURE)
					imageGalleryAdapter.notifyDataSetChanged();
				break;
		}
	}
	public void handlePtpServiceEvent(PtpServiceEventType event, Object eventData){
		switch(event){
			case GetObjectFromSdramStart:
			case GetObjectFromSdramInfo:
			case GetObjectFromSdramThumb:
			case GetObjectFromSdramProgress:
			case GetObjectFromSdramFinished:
			case CaptureCompleteInSdram:
				handleSdramEvent(event, eventData);
				break;
			case ObjectAdded:
				if (eventData != null){
					PtpObjectInfo obj = (PtpObjectInfo)eventData;
					addObjectFromCamera(obj);		
				}
				break;
			case ObjectInfosLoaded:
				break;
		}
	}

	@Override
	public void layoutActivated() {
		loadImagesFromPhone();
		if (_dslrHelper.getPtpService().getIsPtpDeviceInitialized()){
			chkCamera.setEnabled(true);
		}
		else{
			chkCamera.setEnabled(false);
		}
	}

	@Override
	public void layoutDeactived() {
		
	}
}
