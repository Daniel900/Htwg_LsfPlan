package com.hstrobel.lsfplan.classes;

import android.graphics.drawable.Drawable;

import com.hstrobel.lsfplan.frags.MainListFragment;

public class EventItem {
    public final MainListFragment fragment;
    public final Drawable icon;       // the drawable for the ListView item ImageView
    public final Drawable icon2;       // the drawable for the ListView item ImageView
    public final String title;        // the text for the ListView item title
    public final String description;  // the text for the ListView item description

    public EventItem(Drawable icon, Drawable icon2, String title, String description, MainListFragment fragment) {
        this.icon2 = icon2;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.fragment = fragment;
    }
}