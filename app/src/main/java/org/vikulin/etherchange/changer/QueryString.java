package org.vikulin.etherchange.changer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by DDD on 30.03.2017.
 */

public class QueryString {

    private String query = "";

    public QueryString(String name, String value) {
        encode(name, value);
    }

    public QueryString add(String name, String value) {
        query += "&";
        encode(name, value);
        return this;
    }

    private void encode(String name, String value) {
        try {
            query += URLEncoder.encode(name, "UTF-8");
            query += "=";
            query += URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Broken VM does not support UTF-8");
        }
    }

    public String getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return getQuery();
    }

}