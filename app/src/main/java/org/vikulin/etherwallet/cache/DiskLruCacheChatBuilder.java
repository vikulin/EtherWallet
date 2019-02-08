package org.vikulin.etherwallet.cache;

import android.content.Context;

import com.vincentbrison.openlibraries.android.dualcache.Builder;
import com.vincentbrison.openlibraries.android.dualcache.JsonSerializer;
import com.vincentbrison.openlibraries.android.dualcache.SizeOf;

import org.vikulin.etherwallet.adapter.pojo.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vadym on 09.05.17.
 */


public class DiskLruCacheChatBuilder {

        public DiskLruCacheChatBuilder(Context context){

            this.cacheChatArray = new Builder<>(TYPE_ARRAY_CACHE_NAME, TEST_APP_VERSION, ArrayList.class)
                    .useReferenceInRam(RAM_MAX_SIZE, new SizeOfChatArray())
                    .useSerializerInDisk(DISK_MAX_SIZE, true, new JsonSerializer(ArrayList.class), context);
        }

        public final static int TEST_APP_VERSION = 30;
        final static String TYPE_ARRAY_CACHE_NAME = "type_array_cache";
        final static int RAM_MAX_SIZE = 2000;
        final static int DISK_MAX_SIZE = 20000;//20kB max size


        public Builder cacheChatArray;

        public static class SizeOfChatArray implements SizeOf {

            @Override
            public int sizeOf(Object obj) {
                int size = 0;
                List<ChatMessage> object = (ArrayList<ChatMessage>)obj;
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

        public void putChatHistory(String address, List<ChatMessage> chatHistory) {
            cacheChatArray.build().put(address, chatHistory);
        }

        public List<ChatMessage> getChatHistory(String address) {
            Object chatHistory = cacheChatArray.build().get(address);
            if(chatHistory!=null){
                return (List<ChatMessage>) chatHistory;
            } else {
                /**
                 * empty history
                 */
                cacheChatArray.build().put(address, new ArrayList<ChatMessage>());
                return (List<ChatMessage>) cacheChatArray.build().get(address);
            }
        }
    }
