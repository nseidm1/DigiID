package com.noahseidman.digiid.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

public class FragmentSignalViewModel extends BaseObservable {

    private String title;
    private String description;
    private int icon;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    @Bindable
    public String getDescription() {
        return description;
    }

    @Bindable
    public int getIcon() {
        return icon;
    }
}
