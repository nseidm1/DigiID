package com.noahseidman.digiid.models

import com.noahseidman.digiid.R
import com.noahseidman.digiid.adapter.LayoutBinding

class PhraseViewModel(val phrase: String) : LayoutBinding {
    override fun getLayoutId(): Int {
        return R.layout.phrase_view
    }
}
