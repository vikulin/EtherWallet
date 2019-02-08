package org.vikulin.etherwallet.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupManager;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;

/**
 * Created by vadym on 13.09.17.
 */

public class SharedPreferencesBackupAgent extends BackupAgentHelper {
    //здесь мы просто, через запятую указываем названия файлов,
    // в которых хранятся ваши настройки.
    // Обратите внимание на последний параметр,
    // в файле под таким названием хранит данные дефолтный PreferenceManager(PreferenceManager.getDefaultSharedPreferences(context))

    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = null;
        helper = new SharedPreferencesBackupHelper(this, getApplicationContext().getPackageName()+"_preferences");
        addHelper("prefs", helper);
    }

    //метод для запроса бэкапа. Согласно документации следует вызывать этот метод всякий раз, когда данные изменились.
    public static void requestBackup(Context context) {
        BackupManager bm = new BackupManager(context);
        bm.dataChanged();
    }

}