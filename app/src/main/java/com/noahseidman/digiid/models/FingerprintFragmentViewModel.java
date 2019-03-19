package com.noahseidman.digiid.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import com.noahseidman.digiid.BR;

public class FingerprintFragmentViewModel extends BaseObservable {

    private String title;
    private String message;
    private String cancelButtonLabel;

    public void setMessage(String message) {
        this.message = message;
        notifyPropertyChanged(BR.message);
    }

    @Bindable
    public String getMessage() {
        return message;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    public void setCancelButtonLabel(String cancelButtonLabel) {
        this.cancelButtonLabel = cancelButtonLabel;
        notifyPropertyChanged(BR.cancelButtonLabel);
    }

    @Bindable
    public String getCancelButtonLabel() {
        return cancelButtonLabel;
    }

}
