package com.snj.furlencotaskjava.utils;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.snj.furlencotaskjava.R;

public class UiUtils {

    public static void showSnackbar(Activity activity, String msg) {
        View parentLayout = activity.findViewById(android.R.id.content);
        Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
                .setAction(activity.getResources().getText(R.string.close), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //do nothing
                    }
                })
                .setActionTextColor(activity.getResources().getColor(android.R.color.holo_red_light))
                .show();
    }
}
