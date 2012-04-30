// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard;

import java.util.ArrayList;

import com.dslr.dashboard.ImageGalleryAdapter.ViewHolder;
import com.dslr.dashboard.helper.ExifDataHelper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class ExifAdapter extends BaseAdapter {

	private ArrayList<ExifDataHelper> _items;
	
	public ArrayList<ExifDataHelper> items(){
		return _items;
	}
    public Context context;
    public LayoutInflater inflater;
	
    public ExifAdapter(Context context, ArrayList<ExifDataHelper> arrayList){
        super();
        
        this.context = context;
        this._items = arrayList;
        
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }    
    
    public void changeItems(ArrayList<ExifDataHelper> arrayList){
    	_items = arrayList;
    	notifyDataSetChanged();
    }
    
	@Override
	public int getCount() {
        return _items.size();
	}

	@Override
	public Object getItem(int position) {
        return _items.get(position);
	}

	@Override
	public long getItemId(int position) {
        return position;
	}

	@Override
	public boolean hasStableIds() {
			return true;
	}
	
	@Override
	public int getItemViewType(int position) {
		return IGNORE_ITEM_VIEW_TYPE;
	}
	@Override
	public int getViewTypeCount() {
		return 1;
	}

    public static class ViewHolder
    {
        TextView txtExifName;
        TextView txtExifValue;
        
    }
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView==null)
        {
            holder = new ViewHolder();
            
            convertView = inflater.inflate(R.layout.exif_list_item, null);
 
            holder.txtExifName = (TextView) convertView.findViewById(R.id.txtexifdescription);
            holder.txtExifValue = (TextView) convertView.findViewById(R.id.txtexifvalue);
            
            convertView.setTag(holder);
        }
        else
            holder=(ViewHolder)convertView.getTag();
 
        ExifDataHelper helper = _items.get(position);
        holder.txtExifName.setText(helper.mExifDescription);
        holder.txtExifValue.setText(helper.mExifValue);
        
        return convertView;
	}

}
