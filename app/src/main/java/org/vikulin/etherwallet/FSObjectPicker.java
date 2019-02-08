package org.vikulin.etherwallet;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 Copyright (C) 2011 by Brad Greco <brad@bgreco.net>
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

public class FSObjectPicker extends ListActivity {

    public static final String START_DIR = "startDir";
    public static final String ONLY_DIRS = "onlyDirs";
    public static final String ASK_WRITE = "askWrite";
    public static final String ASK_READ = "askRead";
    public static final String SHOW_HIDDEN = "showHidden";
    public static final String CHOSEN_FSOBJECT = "chosenFSObject";
    public static final int PICK_FSOBJECT = 2049;
    public static final String TITLE = "title";
    private File dir;
    private boolean showHidden = false;
    private boolean onlyDirs = true;
    private boolean askReadPermission = false ;
    private boolean askWritePermission = false ;
    private static String TAG = "PermissionRequest";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_READ_STORAGE = 111;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        dir = Environment.getExternalStorageDirectory();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String preferredStartDir = extras.getString(START_DIR);
            showHidden = extras.getBoolean(SHOW_HIDDEN, false);
            askReadPermission = extras.getBoolean(ASK_READ, false);
            askWritePermission = extras.getBoolean(ASK_WRITE, false);
            onlyDirs = extras.getBoolean(ONLY_DIRS, true);
            if(preferredStartDir != null) {
                File startDir = new File(preferredStartDir);
                if(startDir.isDirectory()) {
                    dir = startDir;
                }
            }
        }

        setContentView(R.layout.fsobject_picker_activity);
        setTitle(dir.getAbsolutePath());
        Button btnChoose = (Button) findViewById(R.id.btnChoose);
        if(askWritePermission){
            askWritePermission();
        }
        if(askReadPermission){
            askReadPermission();
        }
        if (onlyDirs) {
            String name = dir.getName();
            if (name.length() == 0)
                name = "/";
            btnChoose.setText(name);
            btnChoose.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    returnFile(dir);
                }
            });
        } else {
            btnChoose.setVisibility(View.GONE);
        }
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        if(!dir.canRead()) {
            dir = new File("/");
            if (!dir.canRead()) {
                Context context = getApplicationContext();
                String msg = "Could not read folder contents.";
                Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        }
        final ArrayList<File> files = filter(dir.listFiles(), onlyDirs, showHidden);
        String[] names = names(files);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.list_language_item, names));
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!files.get(position).isDirectory() & !onlyDirs){
                    returnFile(files.get(position));
                    return;
                }
                String path = files.get(position).getAbsolutePath();
                Intent intent = new Intent(FSObjectPicker.this, FSObjectPicker.class);
                intent.putExtra(FSObjectPicker.START_DIR, path);
                intent.putExtra(FSObjectPicker.SHOW_HIDDEN, showHidden);
                intent.putExtra(FSObjectPicker.ONLY_DIRS, onlyDirs);
                startActivityForResult(intent, PICK_FSOBJECT);
            }
        });
    }

    private void askWritePermission(){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to access the SD-CARD is required for this app.").setTitle("Permission required");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "Clicked");
                        makeWriteRequest();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                makeWriteRequest();
            }
        }
    }

    private void askReadPermission(){
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission to record denied");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to access the SD-CARD is required for this app.").setTitle("Permission required");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "Clicked");
                        makeReadRequest();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                makeReadRequest();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user");
                    finish();
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                    recreate();
                }
                return;
            }
            case REQUEST_READ_STORAGE:{
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user");
                    finish();
                } else {
                    Log.i(TAG, "Permission has been granted by user");
                    recreate();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FSOBJECT && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            String path = (String) extras.get(FSObjectPicker.CHOSEN_FSOBJECT);
            returnFile(new File(path));
        }
    }

    private void returnFile(File file) {
        Intent result = new Intent();
        result.putExtra(CHOSEN_FSOBJECT, file.getAbsolutePath());
        result.setData(Uri.fromFile(file));
        setResult(RESULT_OK, result);
        finish();
    }

    public ArrayList<File> filter(File[] file_list, boolean onlyDirs, boolean showHidden) {
        ArrayList<File> files = new ArrayList<File>();
        for(File file: file_list) {
            if(onlyDirs && !file.isDirectory())
                continue;
            if(!showHidden && file.isHidden())
                continue;
            if(!file.canWrite() && onlyDirs){
                continue;
            }
            files.add(file);
        }
        Collections.sort(files);
        return files;
    }

    public String[] names(ArrayList<File> files) {
        String[] names = new String[files.size()];
        int i = 0;
        for(File file: files) {
            names[i] = file.getName();
            i++;
        }
        return names;

    }

    private void makeWriteRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }

    private void makeReadRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_READ_STORAGE);
    }
}