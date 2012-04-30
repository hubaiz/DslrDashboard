package com.dslr.dashboard.helper;

import java.util.ArrayList;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.dslr.dashboard.*;

/**
 * 
 * Create custom Dialog windows for your application
 * Custom dialogs rely on custom layouts wich allow you to 
 * create and use your own look & feel.
 * 
 * Sample from <a href="http://blog.androgames.net">Android tutorial blog</a>
 * Under GPL v3 : http://www.gnu.org/licenses/gpl-3.0.html
 * 
 * @author antoine vianey
 *
 */
public class CustomDialog extends Dialog {
 
	
    public CustomDialog(Context context, int theme) {
        super(context, theme);
    }
 
    public CustomDialog(Context context) {
        super(context);
    }
 
    public void proba(int selectedItem){
    	ListView list = (ListView)this.findViewById(R.id.list);
    	list.setItemChecked(selectedItem, true);
    	setListviewSelection(list, selectedItem);
    }
    private static void setListviewSelection(final ListView list, final int pos) {
        list.post(new Runnable() {
            @Override
            public void run() {
                list.setSelection(pos);
                View v = list.getChildAt(pos);
                if (v != null) {
                    v.requestFocus();
                }
            }
        });
    }
    /**
     * Helper class for creating a custom dialog
     */
    public static class Builder {
 
        private Context context;
        private String title;
        private String message;
        private String positiveButtonText;
        private String negativeButtonText;
        private View contentView;
        ListViewCustomAdapter adapter;
        private ArrayList<ItemBean> itemList;
        private int selectedItem = -1;
        
      
        private DialogInterface.OnClickListener 
                        positiveButtonClickListener,
                        negativeButtonClickListener,
                        listOnClickListener;
 
        public Builder(Context context) {
            this.context = context;
        }
 
        /**
         * Set the Dialog message from String
         * @param title
         * @return
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }
 
        /**
         * Set the Dialog message from resource
         * @param title
         * @return
         */
        public Builder setMessage(int message) {
            this.message = (String) context.getText(message);
            return this;
        }
 
        /**
         * Set the Dialog title from resource
         * @param title
         * @return
         */
        public Builder setTitle(int title) {
            this.title = (String) context.getText(title);
            return this;
        }
 
        /**
         * Set the Dialog title from String
         * @param title
         * @return
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
 
        /**
         * Set a custom content view for the Dialog.
         * If a message is set, the contentView is not
         * added to the Dialog...
         * @param v
         * @return
         */
        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }
 
        public Builder setListitems(ArrayList<ItemBean> items) {
        	return this.setListItems(items, -1);
        }
        public Builder setListItems(ArrayList<ItemBean> items, int selected){
        	this.itemList = items;
        	this.selectedItem = selected;
        	return this;
        }
        public Builder setListOnClickListener(DialogInterface.OnClickListener listener){
        	this.listOnClickListener = listener;
        	return this;
        }
        /**
         * Set the positive button resource and it's listener
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(int positiveButtonText,
                DialogInterface.OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }
 
        /**
         * Set the positive button text and it's listener
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(String positiveButtonText,
                DialogInterface.OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }
 
        /**
         * Set the negative button resource and it's listener
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(int negativeButtonText,
                DialogInterface.OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }
 
        /**
         * Set the negative button text and it's listener
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(String negativeButtonText,
                DialogInterface.OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }
 
        /**
         * Create the custom dialog
         */
        public CustomDialog create() {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // instantiate the dialog with the custom Theme
            final CustomDialog dialog = new CustomDialog(context, 
            		R.style.Dialog);
            View layout = inflater.inflate(R.layout.dialog, null);
            dialog.addContentView(layout, new LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            // set the dialog title
            ((TextView) layout.findViewById(R.id.title)).setText(title);
            
            ListView list = (ListView)layout.findViewById(R.id.list);
            if (itemList != null){
    	        adapter = new ListViewCustomAdapter(context, itemList, selectedItem);
    	        list.setAdapter(adapter);
    	        list.setItemChecked(selectedItem, true);
    	        list.setSelection(selectedItem);
    	        //setListviewSelection(list, selectedItem);
    	        	list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    	    			@Override
    	    			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
    	    				// user clicked a list item, make it "selected"
    	    				//adapter.setSelectedItem(position);
    	    				
    	    				if (listOnClickListener != null)
    	    					listOnClickListener.onClick(dialog, position);
    	    				dialog.dismiss();
    	    			}
    	            });
            }
            else
            	list.setVisibility(View.GONE);
            
            // set the confirm button
            if (positiveButtonText != null) {
                ((Button) layout.findViewById(R.id.positiveButton))
                        .setText(positiveButtonText);
                if (positiveButtonClickListener != null) {
                    ((Button) layout.findViewById(R.id.positiveButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    positiveButtonClickListener.onClick(
                                    		dialog, 
                                            DialogInterface.BUTTON_POSITIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.positiveButton).setVisibility(
                        View.GONE);
            }
            // set the cancel button
            if (negativeButtonText != null) {
                ((Button) layout.findViewById(R.id.negativeButton))
                        .setText(negativeButtonText);
                if (negativeButtonClickListener != null) {
                    ((Button) layout.findViewById(R.id.negativeButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                	negativeButtonClickListener.onClick(
                                    		dialog, 
                                            DialogInterface.BUTTON_NEGATIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.negativeButton).setVisibility(
                        View.GONE);
            }
            // set the content message
            if (message != null) {
                ((TextView) layout.findViewById(
                		R.id.message)).setText(message);
            } else {
            	((TextView)layout.findViewById(R.id.message)).setVisibility(View.GONE);
            	if (contentView != null) {
            
                // if no message set
                // add the contentView to the dialog body
                ((LinearLayout) layout.findViewById(R.id.content))
                        .removeAllViews();
                ((LinearLayout) layout.findViewById(R.id.content))
                        .addView(contentView, 
                                new LayoutParams(
                                        LayoutParams.FILL_PARENT, 
                                        LayoutParams.WRAP_CONTENT));
            	}
            }
            dialog.setContentView(layout);
            
            return dialog;
        }
 
    }

   
    
 
}
