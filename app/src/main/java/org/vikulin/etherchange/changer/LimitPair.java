package org.vikulin.etherchange.changer;

import java.io.Serializable;

/**
 * Created by DDD on 30.03.2017.
 */

public class LimitPair implements Serializable {

    private Double min_amount;
    private Double max_amount;

    public LimitPair(){

    }

    public LimitPair(Double min_amount, Double max_amount){
        this.min_amount = min_amount;
        this.max_amount = max_amount;
    }

    public Double getMinAmount() {
        return min_amount;
    }

    public Double getMaxAmount() {
        return max_amount;
    }

}
