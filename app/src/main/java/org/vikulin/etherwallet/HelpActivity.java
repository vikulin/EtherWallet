package org.vikulin.etherwallet;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HelpActivity extends FullScreenActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        TextView msg=(TextView)findViewById(R.id.help);
        InputStream helpFileInputStream = getResources().openRawResource(R.raw.index);
        msg.setText(Html.fromHtml(readTextFile(helpFileInputStream)));
    }

    public String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int len;
        String out = null;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            out = outputStream.toString();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }
}
