package org.vikulin.etherwallet.adapter;

/**
 * Created by vadym on 21.05.17.
 */

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.icon.Blockies;

import java.util.List;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 01.12.16.
 */

public class ContactAdapter<L extends List<?>> extends ArrayAdapter<JSONObject> {

    private Context context;
    private int layoutResourceId;
    protected List<JSONObject> data = null;

    public ContactAdapter(LayoutInflater inflater, int layoutResourceId, List<JSONObject> data) {
        super(inflater.getContext(), layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = inflater.getContext();
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        KeyHolder holder = null;
        JSONObject wallet = data.get(position);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new KeyHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.idIcon);
            holder.address = (TextView)row.findViewById(R.id.address);
            row.setTag(holder);
        } else {
            holder = (KeyHolder)row.getTag();
        }
        try {
            String address = prependHexPrefix(wallet.getString("address"));
            boolean isNamePresent=false;
            try {
                isNamePresent = wallet.getString("key_name").length()>0;
            } catch (JSONException e) {
                isNamePresent=false;
            }

            if(isNamePresent) {
                holder.address.setText(wallet.getString("key_name"));
                holder.address.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            } else {
                holder.address.setText(address);
            }

            boolean isPublicKey=false;
            try {
                isPublicKey = wallet.getString("pgpPublicKey").length()>0;
            } catch (JSONException e) {
                isPublicKey=false;
            }
            if(isPublicKey) {
                holder.imgIcon.setImageBitmap(Blockies.createIcon(8, address, 12));
            } else {
                holder.imgIcon.setImageDrawable(this.context.getResources().getDrawable(R.drawable.question));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return row;
    }


    static class KeyHolder
    {
        ImageView imgIcon;
        TextView address;
    }
}