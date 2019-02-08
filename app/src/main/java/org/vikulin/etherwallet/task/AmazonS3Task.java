package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import org.vikulin.etherwallet.FullScreenActivity;

/**
 * Created by vadym on 19.03.17.
 */

public abstract class AmazonS3Task extends AsyncTask<Void,Void,Void> {

    public AmazonS3Task(Activity activity){
        this.activity = activity;
    }

    private Activity activity;
    @Override
    protected Void doInBackground(Void... voids) {
        final AWSCredentials awsCredentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return "AKIAJAVVTS6H6CD5LKCA";
            }

            @Override
            public String getAWSSecretKey() {
                return "TkOkspSkuUsit31ZrAPf7SOLIKJPVV80dq+S3IJ5";
            }
        };
        AmazonS3 s3 = new AmazonS3Client(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return awsCredentials;
            }

            @Override
            public void refresh() {

            }
        });
        try {
            run(s3);
        } catch (AmazonS3Exception e){
            ((FullScreenActivity)activity).showAlertDialogOnUiThread("",e.getMessage());
        } catch (AmazonClientException e){
            ((FullScreenActivity)activity).showAlertDialogOnUiThread("",e.getMessage());
        }
        return null;
    }

    public abstract void run(AmazonS3 s3);
}
