package org.vikulin.etherwallet.adapter;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.SellItem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadym on 01.12.16.
 */

public class SellItemListAdapter<L extends List<?>> extends ArrayAdapter<SellItem> {

    public static DecimalFormat REAL_FORMATTER = new DecimalFormat("0.00###");
    private final TextView totalAmount;

    private Context context;
    private int layoutResourceId;
    private List<SellItem> data = null;
    private double total;

    public SellItemListAdapter(Context context, int layoutResourceId, List<SellItem> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.totalAmount = (TextView) ((Activity)context).findViewById(R.id.totalAmount);

    }

    public SellItemListAdapter(Context context, int layoutResourceId) {
        this(context, layoutResourceId, new ArrayList<SellItem>());
    }

    public List<SellItem> getData(){
        return data;
    }

    public Object[][] getDataArray(){
        Object[][] dataArray = new Object[2][data.size()];
            for(int j=0;j<data.size();j++){
                dataArray[0][j] =data.get(j).getName();
                dataArray[1][j] =data.get(j).getPrice();
            }
        return null;
    }

    public void setSellItems(JSONArray jsonArray) throws JSONException {
        int l = jsonArray.length();
        data.clear();
        for(int i=0;i<l;i++){
            JSONObject si = (JSONObject) jsonArray.get(i);
            double price = si.getDouble("p");
            String name = new String(Base64.decode(si.getString("n").getBytes()));
            SellItem s = new SellItem();
            s.setName(name);
            s.setPrice(price);
            data.add(s);
        }
    }

    /**
     * This method returns JSON encoded string
     * @return
     */
    public JSONObject getSellItems() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("t",total);
        JSONArray jsonArray = new JSONArray();
        for(SellItem si:data){
            JSONObject sellItem = new JSONObject();
            sellItem.put("p",si.getPrice());
            sellItem.put("n", Base64.toBase64String(si.getName().getBytes()));
            jsonArray.put(sellItem);
        }
        return json.put("i",jsonArray);
    }

    public void add(int index, SellItem o){
        data.add(index, o);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        SellItemHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new SellItemHolder();
            holder.itemNumber = (TextView)row.findViewById(R.id.itemNumber);
            holder.itemName = (TextView)row.findViewById(R.id.itemName);
            holder.itemPrice = (TextView)row.findViewById(R.id.itemPrice);

            row.setTag(holder);
        }  else {
            holder = (SellItemHolder)row.getTag();
        }

        SellItem sellItem = data.get(position);
        holder.itemNumber.setText(String.valueOf(position+1));
        holder.itemName.setText(sellItem.getName());
        holder.itemPrice.setText(REAL_FORMATTER.format(sellItem.getPrice()));

        return row;
    }

    private final static class SellItemHolder
    {
        TextView itemNumber;
        TextView itemName;
        TextView itemPrice;
    }

    @Override
    public void notifyDataSetChanged(){
        super.notifyDataSetChanged();
        total = getTotal();
        totalAmount.setText(REAL_FORMATTER.format(total)+" ETH");
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {

        }
    }

    public void notifyDataSetChangedSilently(){
        super.notifyDataSetChanged();
        total = getTotal();
        totalAmount.setText(REAL_FORMATTER.format(total)+" ETH");
    }

    public double getTotal(){
        double total=0;
        for(SellItem si:data){
            total+=si.getPrice();
        }
        return total;
    }
}