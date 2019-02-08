package org.vikulin.etherchange.changer;

import java.io.Serializable;

/**
 * Created by DDD on 30.03.2017.
 */

public class Pair implements Serializable {
    public String getSend() {
        return send;
    }

    public String getReceive() {
        return receive;
    }

    private String send;
    private String receive;
}
