package com.lmntrx.lefo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

/**
 * Created by ACJLionsRoar on 1/31/16.
 */
public class Queen extends Fragment {

    public static class fragment1_follow_entercode extends Queen{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parentViewGroup,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment1_follow_entercode, parentViewGroup, false);
            return rootView;
        }

    }

    public static class fragment2_follow_scanqr extends Queen{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parentViewGroup,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment2_follow_scanqr, parentViewGroup, false);
            return rootView;
        }

    }

    public static class fragment3_livetrack_you extends Queen {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parentViewGroup,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment3_livetrack_you, parentViewGroup, false);

           // ToggleButton switch1 = (ToggleButton) rootView.findViewById(R.id.liveTrackSwitch) ;


            return rootView;



        }

    }
    public static class fragment4_livetrack_others extends Queen{
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parentViewGroup,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment4_livetrack_others, parentViewGroup, false);
            return rootView;
        }

    }
}
