package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GetExchangeAmountPair extends Pair {

    private String amount;

    public GetExchangeAmountPair(String from, String to, String amount) {
        super(from, to);
        this.amount = amount;
    }

    public String getAmount() {
        return amount;
    }

}
