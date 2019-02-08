package org.vikulin.etherwallet.adapter;

/**
 * Created by vadym on 19.03.17.
 */

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amazonaws.services.s3.model.S3VersionSummary;

import org.vikulin.etherwallet.R;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by vadym on 01.12.16.
 */

public class S3ObjectVersionAdapter<L extends List<?>> extends ArrayAdapter<S3VersionSummary> {

    private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

    private Context context;
    private int layoutResourceId;
    protected List<S3VersionSummary> data = null;

    public S3ObjectVersionAdapter(Context context, int layoutResourceId, List<S3VersionSummary> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        S3ObjectVersionHolder holder = null;
        S3VersionSummary object = data.get(position);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new S3ObjectVersionHolder();
            holder.version = (TextView)row.findViewById(R.id.version);
            row.setTag(holder);
        } else {
            holder = (S3ObjectVersionHolder)row.getTag();
        }
        String key = (position+1)+". "+fmt.format(object.getLastModified())+" ("+object.getSize()+"b)";

        holder.version.setText(key);
        return row;
    }


    static class S3ObjectVersionHolder
    {
        TextView version;
    }
}