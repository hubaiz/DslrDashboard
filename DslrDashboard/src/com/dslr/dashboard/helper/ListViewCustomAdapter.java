// Copyright 2012 by Zoltan Hubai <hubaiz@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version as long the source code includes
// this copyright notice.
// 

package com.dslr.dashboard.helper;

import java.util.ArrayList;

import com.dslr.dashboard.*;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ListViewCustomAdapter extends BaseAdapter{
	
	ArrayList<ItemBean> itemList;
    public Context context;
    public LayoutInflater inflater;
    int _selectedItem;
 
    public ListViewCustomAdapter(Context context, ArrayList<ItemBean> itemList, int selectedItem){
        super();
        
        _selectedItem = selectedItem;
        this.context = context;
        this.itemList = itemList;
        
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    
    @Override
    public int getCount() {
        return itemList.size();
    }
 
    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return 0;
    }
 
    public static class ViewHolder
    {
        ImageView imgViewLogo;
        TextView txtViewTitle;
    }
 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
 
        ViewHolder holder;
        if(convertView==null)
        {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.items, null);
 
            holder.imgViewLogo = (ImageView) convertView.findViewById(R.id.imgViewLogo);
            holder.txtViewTitle = (TextView) convertView.findViewById(R.id.txtViewTitle);
 
            convertView.setTag(holder);
        }
        else
            holder=(ViewHolder)convertView.getTag();
 
        ItemBean bean = (ItemBean) itemList.get(position);
        
       
        if (bean.getImage() > 0)
        	holder.imgViewLogo.setImageResource(bean.getImage());
        else
        	holder.imgViewLogo.setImageDrawable(null);
        
        if (bean.nameId() > 0)
        	holder.txtViewTitle.setText(bean.nameId());
        else
        	holder.txtViewTitle.setText(bean.getTitle());
 
        return convertView;
    }
    
   
 
}
