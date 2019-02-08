package org.vikulin.etherwallet.listener;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by DDD on 28.03.2017.
 */

public abstract class InterruptableTextWatcher implements TextWatcher {

    private boolean isStopped;

    private InterruptableTextWatcher shouldStop;

    public void setInterruptableTextWatcher(InterruptableTextWatcher watcher){
        shouldStop = watcher;
    }

    @Override
    public final void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public final void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if(isStopped){
            return;
        }
        shouldStop.stop();
        try {
            run(s);
        } catch (NumberFormatException e){
            s.clear();
        }
        shouldStop.resume();
    }

    public abstract void run(Editable s);

    public void stop(){
        this.isStopped = true;
    }

    public void resume(){
        this.isStopped = false;
    }
}
