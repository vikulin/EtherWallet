package org.vikulin.etherwallet.adapter;

/**
 * Created by vadym on 12.03.17.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.EtherscanTransaction;
import org.vikulin.etherwallet.adapter.pojo.TransactionType;
import org.vikulin.etherwallet.icon.Blockies;
import org.web3j.utils.Convert;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

public class TransactionListAdapter<L extends List<?>> extends ArrayAdapter<EtherscanTransaction> {

    private DecimalFormat REAL_FORMATTER;
    private Context context;
    private int layoutResourceId;
    private List<EtherscanTransaction> data = null;
    private String walletAddress;

    public TransactionListAdapter(Context context, int layoutResourceId, List<EtherscanTransaction> data, String walletAddress) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.walletAddress = walletAddress;
        DecimalFormatSymbols ds = new DecimalFormatSymbols(context.getResources().getConfiguration().locale);
        ds.setDecimalSeparator('.');
        ds.setGroupingSeparator(',');
        this.REAL_FORMATTER = new DecimalFormat("#,###,###,##0.00###", ds);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        TransactionItemHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new TransactionItemHolder();
            holder.icon = (ImageView)row.findViewById(R.id.icon);
            holder.address = (TextView)row.findViewById(R.id.address);
            holder.date = (TextView)row.findViewById(R.id.date);
            holder.inOut = (ImageView) row.findViewById(R.id.inOut);
            holder.value = (TextView)row.findViewById(R.id.value);

            row.setTag(holder);
        }  else {
            holder = (TransactionItemHolder)row.getTag();
        }

        EtherscanTransaction transactionItem = data.get(position);
        String from = transactionItem.getFrom();
        String to = transactionItem.getTo();
        String contract  = transactionItem.getContractAddress();
        if(contract!=null && contract.length()>0){
            transactionItem.setTransactionType(TransactionType.CONTRACT);
            holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), Blockies.createIcon(8,contract, 9)));
            holder.address.setText(contract);
            holder.inOut.setImageDrawable(context.getResources().getDrawable(R.drawable.contract));
        } else {
            if (walletAddress.equalsIgnoreCase(from)) {
                transactionItem.setTransactionType(TransactionType.OUT);
                holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), Blockies.createIcon(8, to, 9)));
                holder.address.setText(to);
                holder.inOut.setImageDrawable(context.getResources().getDrawable(R.drawable.out));
            } else {
                transactionItem.setTransactionType(TransactionType.IN);
                holder.icon.setImageDrawable(new BitmapDrawable(context.getResources(), Blockies.createIcon(8, from, 9)));
                holder.address.setText(from);
                holder.inOut.setImageDrawable(context.getResources().getDrawable(R.drawable.in));
            }
        }

        long delta = System.currentTimeMillis()/1000-Long.parseLong(transactionItem.getTimeStamp());
        String diff = formatTime(delta);
        String confirmations =transactionItem.getConfirmations();
        holder.date.setText(diff+", confirmed "+confirmations+" times");
        holder.value.setText(REAL_FORMATTER.format(Convert.fromWei(transactionItem.getValue(), Convert.Unit.ETHER))+" ETH");

        return row;
    }

    private final static class TransactionItemHolder
    {
        ImageView icon;
        TextView address;
        TextView date;
        ImageView inOut;
        TextView value;
    }

    private String formatTime(long diffInSeconds){
        long diff[] = new long[] { 0, 0, 0, 0 };

    /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
    /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
    /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
            long days = diffInSeconds / 24;
            diff[0] = days;
            if(days>=1){
                return String.format(
                        "%dd%dh ago",
                        diff[0],
                        diff[1]
                        );
            } else {
                if(diff[1]>=1){
                    return String.format(
                            "%dh%dm ago",
                            diff[1],
                            diff[2]
                            );
                } else {
                    if(diff[2]>=1){
                        return String.format(
                                "%dm ago",
                                diff[2]
                                );
                    } else {
                        return String.format(
                                "%dsec ago",
                                diff[3]
                                );
                    }
                }
            }
    }

    public static void main(String args[]){

        final AWSCredentials awsCredentials = new AWSCredentials(){
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAJAVVTS6H6CD5LKCA";
            }
            @Override
            public String getAWSSecretKey() {
                return "TkOkspSkuUsit31ZrAPf7SOLIKJPVV80dq+S3IJ5";
            }
        };

        AmazonS3 s3 = new AmazonS3Client(new AWSCredentialsProvider(){
            @Override
            public AWSCredentials getCredentials() {
                return awsCredentials;
            }
            @Override
            public void refresh() {

            }
        });

        File fileToUpload = new File("/etc/hosts");
        //(Replace "MY-BUCKET" with your S3 bucket name, and "MY-OBJECT-KEY" with whatever you would            like to name the file in S3)
        PutObjectRequest putRequest = new PutObjectRequest("bar-code.hyperborian.org", "new bar-code file",
                fileToUpload);
        PutObjectResult putResponse = s3.putObject(putRequest);
    }

}
