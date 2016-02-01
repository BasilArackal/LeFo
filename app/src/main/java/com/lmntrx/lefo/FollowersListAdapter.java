package com.lmntrx.lefo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by livin on 31/1/16.
 */
public class FollowersListAdapter extends ArrayAdapter<String> {


    public FollowersListAdapter(Context context, String[] values) {
        super(context, R.layout.followers_list_item, values);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.followers_list_item, parent, false);

        String followerDetails = getItem(position);
        String follower, status;
        try {
            follower = followerDetails.substring(0, followerDetails.indexOf(':'));
            status = followerDetails.substring(followerDetails.indexOf(':') + 1);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
            view.setVisibility(View.INVISIBLE);
            return view;
        }
        TextView textView = (TextView) view.findViewById(R.id.list_item_field);
        textView.setText(follower);
        ImageView imageView = (ImageView) view.findViewById(R.id.statusImgView);
        switch (status) {
            case "true":
                imageView.setImageResource(R.drawable.online);
                break;
            case "false":
                imageView.setImageResource(R.drawable.offline);
                break;
            default:
                imageView.setImageResource(R.drawable.mr_ic_pause_light);
                break;
        }



        return view;
    }
}
