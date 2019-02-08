package org.vikulin.etherwallet.cache;

import android.app.Activity;

import com.google.gson.reflect.TypeToken;
import com.jakewharton.disklrucache.DiskLruCache;
import com.vincentbrison.openlibraries.android.dualcache.Builder;
import com.vincentbrison.openlibraries.android.dualcache.JsonSerializer;
import com.vincentbrison.openlibraries.android.dualcache.SizeOf;

import org.vikulin.etherwallet.FullScreenActivity;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.ChatMessage;
import org.vikulin.etherwallet.adapter.pojo.SellItem;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by vadym on 18.03.17.
 */

public class DiskLruCacheActivityBuilder {

    private final Activity activity;

    public DiskLruCacheActivityBuilder(Activity activity){
        this.activity = activity;
        Type listType = new TypeToken<ArrayList<ChatMessage>>(){}.getType();
        this.cacheBarCodeArray = new Builder<>(TYPE_ARRAY_CACHE_NAME, TEST_APP_VERSION, SellItem.class)
                .useReferenceInRam(RAM_MAX_SIZE, new SizeOfTypeArray())
                .useSerializerInDisk(DISK_MAX_SIZE, true, new JsonSerializer(SellItem.class), activity.getBaseContext());
    }

    public final static int TEST_APP_VERSION = 30;
    final static String TYPE_ARRAY_CACHE_NAME = "type_array_cache";
    final static int RAM_MAX_SIZE = 20000;
    final static int DISK_MAX_SIZE = 2000000;//2GB max size

    public Builder cacheBarCodeArray;

    public static class SizeOfTypeArray implements SizeOf<SellItem> {
        @Override
        public int sizeOf(SellItem object) {
            int size = 0;
            if (object.getName() != null) {
                size += object.getName().length() * 2;
            }
            size += 8;
            size += 8;
            return size;
        }
    }

    public static class SizeOfChatArray implements SizeOf<ArrayList<ChatMessage>> {
        @Override
        public int sizeOf(ArrayList<ChatMessage> object) {
            int size = 0;
            for(ChatMessage o: object) {
                if (o.getMessage() != null) {
                    size += o.getMessage().length() * 2;
                }
                size += 8;
                size += 8;
            }
            return size;
        }
    }

    public void putSellItem(Long barCode, SellItem sellItems) {
        cacheBarCodeArray.build().put(String.valueOf(barCode), sellItems);
        //DiskLruCache cache = cacheBarCodeArray.build().getDiskLruCache();
        ((FullScreenActivity)activity).showInfoDialog("", activity.getString(R.string.added_item));
    }

    public void putSellItemSilently(Long barCode, SellItem sellItems) {
        cacheBarCodeArray.build().put(String.valueOf(barCode), sellItems);
    }

    public DiskLruCache getCache(){
        return cacheBarCodeArray.build().getDiskLruCache();
    }

    public SellItem getSellItem(Long barCode) {
        Object sellItem = cacheBarCodeArray.build().get(String.valueOf(barCode));
        if(sellItem!=null){
            return (SellItem) sellItem;
        } else {
            return null;
        }
    }
}
