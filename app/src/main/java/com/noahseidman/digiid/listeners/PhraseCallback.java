package com.noahseidman.digiid.listeners;

import com.noahseidman.digiid.adapter.MultiTypeDataBoundAdapter;

public interface PhraseCallback extends MultiTypeDataBoundAdapter.ActionCallback {
    void onClick(String string);
}
