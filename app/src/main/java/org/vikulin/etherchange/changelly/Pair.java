package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class Pair {

    public Pair(String from, String to){
        this.from = from;
        this.to = to;
    }

    private String from;

    private String to;

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
