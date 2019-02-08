package org.vikulin.etherchange.changer;

import java.io.Serializable;

/**
 * Created by vadym on 04.04.17.
 */

public class Limits implements Serializable{

    public Limits(){

    }

    public Limits(LimitPair limits){
        this.limits = limits;
    }

    private Pair pair;

    private LimitPair limits;

    public Pair getPair() {
        return pair;
    }

    public LimitPair getLimits() {
        return limits;
    }

}
