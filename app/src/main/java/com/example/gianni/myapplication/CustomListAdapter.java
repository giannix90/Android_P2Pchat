package com.example.gianni.myapplication;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


/**
 *
 * This class is used to set the style of listview's row
 *
 * **/

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final List<String> itemname;
    private final List<String> addr;
    private final Integer[] imgid;

    public CustomListAdapter(Activity context, List<String> itemname,List<String> addr ,Integer[] imgid) {
        super(context, R.layout.mylist, itemname);

        //R.layout.mylist is the structure of the element of list view
        // TODO Auto-generated constructor stub

        this.context=context;
        this.itemname=itemname;
        this.imgid=imgid;
        this.addr=addr;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.mylist, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.txtview);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView extratxt = (TextView) rowView.findViewById(R.id.infotxtview);

        txtTitle.setText(itemname.get(position));
        extratxt.setText("\n Ip address: "+ addr.get(position));
        return rowView;

    };
}