package org.vikulin.etherwallet;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.multidex.MultiDexApplication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;
import org.json.JSONObject;
import org.vikulin.etherpush.Code;
import org.vikulin.etherpush.Data;
import org.vikulin.etherpush.Message;


/**
 * Created by vadym on 09.12.16.
 */
@ReportsCrashes(
        formUri = "https://vikulin.cloudant.com/acra-etherwallet/_design/acra-storage/_update/report",
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUriBasicAuthLogin = "aretsendayeadestillsight",
        formUriBasicAuthPassword = "ab06513cb8105fbbc83c8661fb126d4373a6922b",
        //formKey = "", // This is required for backward compatibility but not used
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.STACK_TRACE,
                ReportField.DUMPSYS_MEMINFO,
                ReportField.TOTAL_MEM_SIZE
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.toast_crash
)

public class MainApplication extends MultiDexApplication {

    private Activity mCurrentActivity = null;

    public Activity getCurrentActivity(){
        return mCurrentActivity;
    }

    public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
    }

    private Gson gson  = new GsonBuilder().create();

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);


        OneSignal.startInit(this).inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification).
                setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler(){

                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        JSONObject additionalData = result.notification.payload.additionalData;
                        if(additionalData==null || additionalData.length()==0){
                            final Intent mainIntent = new Intent(MainApplication.this.getBaseContext(), DrawerActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                            MainApplication.super.startActivity(mainIntent);
                            return;
                        }
                        String jsonPush = additionalData.toString();
                        Data data = gson.fromJson(jsonPush, Data.class);
                        if(data.getType().equals(Data.Encryption.NOT_ENCRYPTED.name())) {
                            String jsonData = data.getObject();
                            Message message = gson.fromJson(jsonData, Message.class);
                            int code = message.getCode();
                            switch (code) {
                                /*
                                case Code.INVITE:
                                    final Intent showDialogIntent = new Intent(MainApplication.this.getBaseContext(), MessengerActivity.class);
                                    showDialogIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    showDialogIntent.putExtra(INVITE, jsonData);
                                    //MainApplication.super.startActivity(showDialogIntent);

                                    final int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
                                    final PendingIntent pendingIntent = PendingIntent.getActivity(MainApplication.this.getBaseContext(), uniqueInt, showDialogIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                                    try {
                                        pendingIntent.send();
                                    } catch (PendingIntent.CanceledException e) {
                                        e.printStackTrace();
                                    }
                                    break;*/
                                case Code.TRANSACTION_IN:
                                    final Intent transactionIntent = new Intent(MainApplication.this.getBaseContext(), TransactionListActivity.class);
                                    transactionIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    MainApplication.super.startActivity(transactionIntent);
                                    break;
                            }
                        }
                    }
                }).
                init();
    }
}
