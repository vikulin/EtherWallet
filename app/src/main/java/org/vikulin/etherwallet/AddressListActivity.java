package org.vikulin.etherwallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.ethername.Domain;
import org.vikulin.etherwallet.adapter.AddressAdapter;
import org.vikulin.etherwallet.icon.Blockies;
import org.web3j.utils.Numeric;
import org.xbill.DNS.TextParseException;

import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Set;

import static org.vikulin.etherwallet.DrawerActivity.KEY_FILE_PICKER;
import static org.vikulin.etherwallet.MainActivity.REQUEST_PASSWORD;

/**
 * Created by vadym on 25.12.16.
 */

public abstract class AddressListActivity extends FullScreenActivity {

    public static final String DEFAULT_SELL_ADDRESS = "default_sell_address";
    public static final String DEFAULT_BUY_ADDRESS = "default_buy_address";
    private PopupWindow popupAddress;
    private AddressAdapter adapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public PopupWindow getAddressListPopup(){
        return popupAddress;
    }

    public abstract TextView getWalletListView();

    public Adapter getAdapter(){
        return adapter;
    }

    public void setIcon(String address){
        getWalletListView().setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(getResources(), Blockies.createIcon(8, Numeric.prependHexPrefix(address), 8)), null, null, null);
    }

    public void resolveAddress(String domain){
        try {
            String address = Domain.resolve(domain).getResolved();
            getWalletListView().setText(address.replaceAll("\"",""));
        } catch (TextParseException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            getWalletListView().setError(e.getMessage());
        }
    }

    //TODO implement domains in address list
    public void onClickAddressList(View v){
        int height = -1 * v.getHeight();
        getAddressListPopup().showAsDropDown(v, -2, height);
        //String value = getWalletListView().getText().toString();
        //if(height<0 && value!=null && !value.startsWith("0x")){
        //    resolveAddress(value);
        //}
    }

    public PopupWindow getPopupWindow(int textViewResourceId, String[] objects, TextView editText, String hint) {
        // initialize a pop up window type
        PopupWindow popupWindow = new PopupWindow(this);
        // the drop down list is a list view
        ListView listView = new ListView(this);
        listView.setDividerHeight(0);
        View header = getLayoutInflater().inflate(R.layout.address_header, null);
        TextView headerTitle = (TextView)header.findViewById(R.id.headerTitle);
        headerTitle.setText(hint);
        listView.addHeaderView(header);
        // set our adapter and pass our pop up window contents
        adapter = new AddressAdapter(this, textViewResourceId, objects, popupWindow , editText);
        listView.setAdapter(adapter);
        // set the item click listener
        listView.setOnItemClickListener(adapter);
        // some other visual settings
        popupWindow.setFocusable(true);
        //popupWindow.setWidth(400);
        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        popupWindow.setWidth(display.getWidth()-30);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // set the list view as pop up window content
        popupWindow.setContentView(listView);
        this.popupAddress = popupWindow;
        return popupWindow;
    }

    protected void importKey(){
        Intent intent = new Intent(this, FSObjectPicker.class);
        intent.putExtra(FSObjectPicker.ONLY_DIRS, false);
        intent.putExtra(FSObjectPicker.ASK_READ, true);
        intent.putExtra(FSObjectPicker.START_DIR, "/sdcard");
        this.startActivityForResult(intent, KEY_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == DrawerActivity.KEY_FILE_PICKER && resultCode == RESULT_OK) {
            Uri path = intent.getData();
            Intent passwordActivity = new Intent(this, KeyFilePasswordActivity.class);
            passwordActivity.setData(path);
            startActivityForResult(passwordActivity, REQUEST_PASSWORD);
            return;
        }
        if (requestCode == REQUEST_PASSWORD && resultCode == RESULT_OK) {
            //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
            Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
            String[] addresses = new String[savedKeys.size()];
            for(int i=0; i<savedKeys.size();i++){
                try {
                    addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(addresses.length==1){
                getWalletListView().setText(addresses[0]);
                getPopupWindow(R.layout.spinner_address_item, addresses, getWalletListView(), getString(R.string.choose_address));
            }
            return;
        }
    }

    public void onClickChooseAddress(View view) {
        if(getAdapter().getCount()==0){
            importKey();
        }
    }
}
