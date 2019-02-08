package org.vikulin.etherchange.changelly;

import java.io.Serializable;

/**
 * Created by vadym on 26.05.17.
 */

public class Result implements Serializable{

    private String address;
    private String extraId;

    public String getAddress() {
        return address;
    }

    public String getExtraId() {
        return extraId;
    }
}
