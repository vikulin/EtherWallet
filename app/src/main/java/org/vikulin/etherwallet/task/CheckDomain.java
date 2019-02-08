package org.vikulin.etherwallet.task;

import android.app.Activity;

import org.vikulin.ethername.Domain;
import org.xbill.DNS.TextParseException;

import java.net.UnknownHostException;

import static org.vikulin.etherwallet.AccountListActivity.DOT_ETH;

/**
 * Created by vadym on 06.03.17.
 */

public abstract class CheckDomain implements Runnable {

    final String domainRegex = "^(([a-zA-Z]{1})|([a-zA-Z]{1}[a-zA-Z]{1})|([a-zA-Z]{1}[0-9]{1})|([0-9]{1}[a-zA-Z]{1})|([a-zA-Z0-9][a-zA-Z0-9-_]{1,61}[a-zA-Z0-9]))$";

    public CheckDomain (Activity activity, String value){
        this.activity = activity;
        this.value = value;
    }

    private Activity activity;
    private Domain.Status status;
    private String value;

    @Override
    public void run() {
        try {
            if(value.matches(domainRegex)) {
                status = Domain.isAvailable(value+DOT_ETH);
            }
        } catch (TextParseException e) {
            e.printStackTrace();
            return;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    refreshUI();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                } catch (TextParseException e) {
                    e.printStackTrace();
                    return;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
    }

    public Boolean isAvailable(){
        if(status==null){
            return null;
        }
        return status.isAvailable();
    }

    public String getValue(){
        return value;
    }

    public String getResolved() throws TextParseException, UnknownHostException {
        if(status==null){
            return null;
        }
        return status.getResolved();
    }

    public abstract void refreshUI() throws InterruptedException, TextParseException, UnknownHostException;
}