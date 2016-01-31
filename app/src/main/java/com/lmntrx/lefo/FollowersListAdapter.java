package com.lmntrx.lefo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by livin on 31/1/16.
 */
public class FollowersListAdapter extends ArrayAdapter<String> {


    public FollowersListAdapter(Context context, String[] values ) {
        super(context, R.layout.followers_list_item,values);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater= LayoutInflater.from(getContext());


        //Here...........

        View view=inflater.inflate(R.layout.followers_list_item, parent, false);

        String follower = getItem(position);

        TextView textView=(TextView)view.findViewById(R.id.list_item_field);
        textView.setText(follower);
        ImageView imageView=(ImageView)view.findViewById(R.id.statusImgView);
        imageView.setImageResource(R.drawable.online);




        return super.getView(position, convertView, parent);
    }
}
