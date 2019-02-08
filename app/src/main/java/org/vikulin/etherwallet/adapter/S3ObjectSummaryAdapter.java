package org.vikulin.etherwallet.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.vikulin.etherwallet.R;

import java.util.List;

/**
 * Created by vadym on 01.12.16.
 */

public class S3ObjectSummaryAdapter extends ArrayAdapter {

    private Context context;
    private int layoutResourceId;
    protected List<S3ObjectSummary> data = null;

    public S3ObjectSummaryAdapter(Context context, int layoutResourceId, List<S3ObjectSummary> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        S3ObjectHolder holder = null;
        S3ObjectSummary object = data.get(position);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new S3ObjectHolder();
            holder.key = (TextView)row.findViewById(R.id.key);
            row.setTag(holder);
        } else {
            holder = (S3ObjectHolder)row.getTag();
        }
        String address = object.getKey();
        holder.key.setText(address);
        return row;
    }

    static class S3ObjectHolder
    {
        TextView key;
    }
}