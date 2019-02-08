package org.vikulin.etherwallet.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.cache.TokenIconCache;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by vadym on 04.12.16.
 */

public class TokenAdapter extends ArrayAdapter<Token> implements AdapterView.OnItemClickListener {

    private final Context context;
    private final List<Token> objects;
    private final PopupWindow popup;
    private final TextView editText;

    public TokenAdapter(Context context, int textViewResourceId, List<Token> objects, PopupWindow popup, TextView editText) {
        super(context, textViewResourceId, objects);
        this.context = context;
        this.objects = objects;
        this.popup = popup;
        this.editText = editText;
    }

    public List<Token> getData(){
        return objects;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {

        if (convertView==null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_token, parent, false);
        }
        TextView sub=(TextView)convertView.findViewById(R.id.tokenSymbol);
        TextView name=(TextView)convertView.findViewById(R.id.name);
        String tokenSymbol = objects.get(position).getTokenInfo().getSymbol();
        String tokenName = objects.get(position).getTokenInfo().getName();
        sub.setText(tokenSymbol);
        name.setText(tokenName);
        ImageView icon = (ImageView)convertView.findViewById(R.id.idIcon);
        if(tokenSymbol.equals("ETH")){
            icon.setImageDrawable(this.context.getResources().getDrawable(R.drawable.ether));
            return convertView;
        }
        try {
            icon.setImageBitmap(getTokenIcon(context, tokenSymbol));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return convertView;
    }

    public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
        // get the context and main activity to access variables
        // add some animation when a list item was clicked
        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        fadeInAnimation.setDuration(10);
        v.startAnimation(fadeInAnimation);
        // dismiss the pop up
        popup.dismiss();
        // get the text and set it as the button text
        View text = v.findViewById(R.id.tokenSymbol);
        if(text==null){
            return;
        }
        int tokenIndex = position - 1;
        String tokenSymbol =  objects.get(tokenIndex).getTokenInfo().getSymbol();
        editText.setTag(tokenIndex);
        editText.setText(tokenSymbol);
    }

    private Bitmap getTokenIcon(final Context context, String tokenSymbol) throws IOException {
        if(TokenIconCache.getInstance(context).contains(tokenSymbol)) {
            return TokenIconCache.getInstance(context).get(tokenSymbol);
        }
        return null;
    }
}