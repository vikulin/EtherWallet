package org.vikulin.etherwallet.comparator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;

/**
 * Created by vadym on 01.12.16.
 */

public class JSONKeyObject {

    public static Comparator<JSONObject> JSONObjectNameComparator = new Comparator<JSONObject>() {

        public int compare(JSONObject json1, JSONObject json2) {

            String name1 = null;
            String name2 = null;
            String key_name1 = null;
            String key_name2 = null;

            try {
                name1 = json1.getString("address");
                name2 = json2.getString("address");
                key_name1 = json1.getString("key_name");
                key_name2 = json2.getString("key_name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(key_name1!=null && isDomain(key_name1) && key_name2!=null && isDomain(key_name2)){
                return key_name1.compareTo(key_name2);
            }
            if(key_name1!=null && isDomain(key_name1)){
                return 1;
            }
            if(key_name2!=null && isDomain(key_name2)){
                return -1;
            }
            //ascending order
            return name1.compareTo(name2);
            //descending order
            //return fruitName2.compareTo(fruitName1);
        }

    };

    public static boolean isDomain(String address){
        return address.endsWith(".eth");
    }
}
