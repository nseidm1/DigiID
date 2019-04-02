package com.noahseidman.digiid.models;

import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import com.noahseidman.digiid.R;
import com.noahseidman.digiid.adapter.LayoutBinding;

public class SiteViewModel extends BaseObservable implements LayoutBinding {

    private String url;

    public SiteViewModel(String url) {
        this.url = url;
    }

    @Bindable
    public String getUrl() {
        return url;
    }

    @Override
    public int getLayoutId() {
        return R.layout.site;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        SiteViewModel model = (SiteViewModel) obj;
        return model != null && model.url.equals(url);
    }
}
