package org.vikulin.etherwallet.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.ValueCallback;

import org.vikulin.etherwallet.FSObjectPicker;

import im.delight.android.webview.AdvancedWebView;

/**
 * Created by vadym on 19.11.16.
 */

public class UploadWebView extends AdvancedWebView {

    public UploadWebView(Context context) {
        super(context);
    }

    public UploadWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UploadWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("NewApi")
    protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond, final boolean allowMultiple) {
        if (mFileUploadCallbackFirst != null) {
            mFileUploadCallbackFirst.onReceiveValue(null);
        }
        mFileUploadCallbackFirst = fileUploadCallbackFirst;

        if (mFileUploadCallbackSecond != null) {
            mFileUploadCallbackSecond.onReceiveValue(null);
        }
        mFileUploadCallbackSecond = fileUploadCallbackSecond;

        if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11) {
            Intent intent = new Intent(mFragment.get().getActivity(), FSObjectPicker.class);
            intent.putExtra(FSObjectPicker.ONLY_DIRS, false);
            intent.putExtra(FSObjectPicker.ASK_READ, true);
            intent.putExtra(FSObjectPicker.START_DIR, "/sdcard");
            mFragment.get().startActivityForResult(intent, mRequestCodeFilePicker);
        }
        else if (mActivity != null && mActivity.get() != null) {
            Intent intent = new Intent(mActivity.get(), FSObjectPicker.class);
            intent.putExtra(FSObjectPicker.ONLY_DIRS, false);
            intent.putExtra(FSObjectPicker.ASK_READ, true);
            intent.putExtra(FSObjectPicker.START_DIR, "/sdcard");
            mActivity.get().startActivityForResult(intent, mRequestCodeFilePicker);
        }
    }

    /*
    private float x1,x2;
    static final int MIN_DISTANCE = 150;

    public boolean onTouchEvent(MotionEvent event){
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;
                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // Left to Right swipe action
                    // Left to Right swipe action
                    if (x2 > x1) {
                        onLeftToRightSwipe();
                        if (currentMenuItemIndex>0){
                            //loadUrl(defaultURL+menuItems[currentMenuItemIndex--]);
                            loadUrl("javascript:document.getElementById('"+(menuItemsID[currentMenuItemIndex--])+"').click();");
                        }
                        Toast.makeText(context, "Left to Right swipe [Next]"+currentMenuItemIndex, Toast.LENGTH_SHORT).show ();
                    }
                    // Right to left swipe action
                    else {
                        onRightToLeftSwipe();
                        if (currentMenuItemIndex<menuItemsID.length-1){
                            //loadUrl(defaultURL+menuItems[currentMenuItemIndex++]);
                            loadUrl("javascript:document.getElementById('"+(menuItemsID[currentMenuItemIndex++])+"').click();");
                        }
                        Toast.makeText(context, "Right to Left swipe [Previous] index="+currentMenuItemIndex, Toast.LENGTH_SHORT).show ();
                    }
                }
                else {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }*/
}
