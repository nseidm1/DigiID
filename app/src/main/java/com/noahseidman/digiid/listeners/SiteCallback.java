package com.noahseidman.digiid.listeners;

import com.noahseidman.digiid.adapter.MultiTypeDataBoundAdapter;

public interface SiteCallback extends MultiTypeDataBoundAdapter.ActionCallback {
    void onClick(String urlVariation);
}
