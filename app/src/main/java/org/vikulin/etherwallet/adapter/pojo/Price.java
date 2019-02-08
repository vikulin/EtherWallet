package org.vikulin.etherwallet.adapter.pojo;

/**
 * Created by vadym on 30.07.17.
 */

class Price {
    private double rate;
    private double diff;
    private long ts;
    private String currency;
    public double getRate() {
        return rate;
    }

    public double getDiff() {
        return diff;
    }

    public long getTs() {
        return ts;
    }

    public String getCurrency() {
        return currency;
    }
}
