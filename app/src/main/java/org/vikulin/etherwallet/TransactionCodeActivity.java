package org.vikulin.etherwallet;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.vikulin.etherwallet.adapter.SellItemListAdapter;

import java.util.Date;

import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;

/**
 * Created by vadym on 10.12.16.
 */

public class TransactionCodeActivity extends FullScreenActivity {


    public static final String TRANSACTION_HASH = "transaction_hash";
    public static final String ADDRESS_FROM = "address_from";
    public static final String ADDRESS_TO = "address_to";
    public static final String VALUE = "value";
    public static final String TRANSACTION_ID = "input_data";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            setContentView(R.layout.activity_transaction_code_h);
        } else {
            // portrait
            setContentView(R.layout.activity_transaction_code_w);
        }

        final Bundle extras = getIntent().getExtras();
        final ImageView imageView = (ImageView) findViewById(R.id.qrCodeImage);

        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int w = size.x;
                int h = size.y;
                if(extras!=null) {
                    String transactionHash = extras.getString(TRANSACTION_HASH);
                    try {
                        Bitmap bitmap = encodeAsBitmap(transactionHash, TransactionCodeActivity.this);
                        imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }
        });
        TextView date = (TextView) findViewById(R.id.date);
        TextView addressFrom = (TextView) findViewById(R.id.addressFrom);
        TextView addressTo = (TextView) findViewById(R.id.addressTo);
        TextView transactionId = (TextView) findViewById(R.id.transactionId);
        TextView value = (TextView) findViewById(R.id.value);
        java.text.DateFormat dateFormat = DateFormat.getLongDateFormat(getBaseContext());
        date.setText(dateFormat.format(new Date()));
        String addressFromExtras = extras.getString(ADDRESS_FROM).substring(0,30)+"...";
        String addressToExtras = extras.getString(ADDRESS_TO).substring(0,30)+"...";
        String transactionIdExtras = extras.getString(TRANSACTION_ID).substring(0,30)+"...";
        Double valueExtras = extras.getDouble(VALUE);
        addressFrom.setText(addressFromExtras);
        addressTo.setText(addressToExtras);
        transactionId.setText(transactionIdExtras);
        value.setText(SellItemListAdapter.REAL_FORMATTER.format(valueExtras)+" ETH");
    }
}
