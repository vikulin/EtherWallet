package org.vikulin.etherwallet.push;

import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationDisplayedResult;
import com.onesignal.OSNotificationReceivedResult;

import org.json.JSONObject;
import org.vikulin.ethername.Domain;
import org.vikulin.etherpush.Code;
import org.vikulin.etherpush.Data;
import org.vikulin.etherpush.Message;
import org.vikulin.etherpush.Transaction;
import org.vikulin.etherwallet.R;
import org.web3j.utils.Convert;
import org.xbill.DNS.TextParseException;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Created by vadym on 22.03.17.
 */

public class PushNotificationHandler extends NotificationExtenderService {

    private Gson gson  = new GsonBuilder().create();

    private DecimalFormat REAL_FORMATTER ;

    @Override
    protected boolean onNotificationProcessing(final OSNotificationReceivedResult receivedResult) {

        final NotificationExtenderService.OverrideSettings overrideSettings = new NotificationExtenderService.OverrideSettings();

        overrideSettings.extender = new NotificationCompat.Extender() {
            @Override
            public NotificationCompat.Builder extend(final NotificationCompat.Builder builder) {
                JSONObject additionalData = receivedResult.payload.additionalData;
                builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.eth));
                if(additionalData==null || additionalData.length()==0){
                    return builder;
                }
                DecimalFormatSymbols ds = new DecimalFormatSymbols(getResources().getConfiguration().locale);
                ds.setDecimalSeparator('.');
                ds.setGroupingSeparator(',');
                REAL_FORMATTER = new DecimalFormat("#,###,###,##0.00###", ds);
                String jsonPush = additionalData.toString();
                Data data = gson.fromJson(jsonPush, Data.class);
                if(data.getType().equals(Data.Encryption.NOT_ENCRYPTED.name())) {
                    String jsonData = data.getObject();
                    Message message = null;
                    try {
                        message = gson.fromJson(jsonData, Message.class);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    int code = message.getCode();
                    switch (code) {
                        case Code.TRANSACTION_IN:
                            Transaction transaction = gson.fromJson(jsonData, Transaction.class);
                            String value = transaction.getValue();
                            String ether = REAL_FORMATTER.format(Convert.fromWei(new BigInteger(value, 16).toString(), Convert.Unit.ETHER));
                            builder.setContentTitle(getString(R.string.received)+" "+ether+"ETH");
                            builder.setContentText("⇦"+transaction.getFrom());
                            builder.setStyle(new NotificationCompat.BigTextStyle().bigText("⇦"+transaction.getFrom()));
                            builder.setSubText("⇨"+transaction.getTo());
                            break;
                        case Code.INVITE:

                            break;
                    }
                } else {
                    builder.setContentTitle("Error");
                    builder.setContentText("walletList has unknown type:"+data.getType());
                }

                // Sets the background notification color to blue on Android 5.0+ devices.

                //builder.setSmallIcon(R.mipmap.eth);
                //builder.setContentTitle("Received ether");
                //builder.setContentText(walletList.getObject());

                //builder.setContentInfo("test content2");

                return builder.setColor(getResources().getColor(R.color.headerColor));
            }


            private void resolveDomain(final String address, final OnAddressResolved listener){
                Thread thread = new Thread(new Runnable() {
                    String v = null;
                    @Override
                    public void run() {
                        try {
                            v = Domain.resolve(address).getResolved();
                            if(v!=null) {
                                listener.onResolved(v);
                            }
                        } catch (TextParseException e) {
                            e.printStackTrace();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
            }
        };
        OSNotificationDisplayedResult displayedResult = displayNotification(overrideSettings);

        return true;
    }

    private interface OnAddressResolved{
        void onResolved(String resolvedAddress);
    }

}
