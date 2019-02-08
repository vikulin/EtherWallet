package org.vikulin.etherwallet;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import static org.vikulin.etherwallet.ConfigurationActivity.languages;
import static org.vikulin.etherwallet.DrawerActivity.LANGUAGE;

/**
 * Created by vadym on 20.12.15.
 */
public class CustomDialog extends Dialog implements AdapterView.OnItemClickListener {

    public DrawerActivity c;
    private ListView language;

    public CustomDialog(DrawerActivity a) {
        super(a);
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_configuration);
        language = (ListView) findViewById(android.R.id.list);
        language.setOnItemClickListener(this);
        ArrayAdapter adapter = new ArrayAdapter<>(c, R.layout.list_language_item, languages);
        language.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String language = languages[i];
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(c.getBaseContext());
        preferences.edit().putString(LANGUAGE, language).commit();
        //c.getIntent().putExtra(LANGUAGE, language);
        //c.changeLanguage(language);
        c.updateConfiguration();
        c.updateMenu();
        dismiss();
    }
}