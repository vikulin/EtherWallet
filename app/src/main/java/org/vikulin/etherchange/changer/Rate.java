package org.vikulin.etherchange.changer;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by DDD on 25.03.2017.
 */

public class Rate implements Serializable {

    public Rate(){

    }

    public Rate(Double rate){
        this.rate = rate;
    }

    public Double getRate() {
        return rate;
    }

    private Double rate;

    public Map<String, String> getPair() {
        return pair;
    }

    private Map<String,String> pair;

}
