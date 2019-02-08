package org.vikulin.etherwallet.adapter.pojo;

import android.support.annotation.NonNull;

/**
 * Created by vadym on 30.07.17.
 */

public class Token implements Comparable<Token>{

    private double balance;

    public double getBalance(){
        return balance;
    }

    public void setBalance(double balance){
        this.balance = balance;
    }

    public void setTokenInfo(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    private TokenInfo tokenInfo;

    public TokenInfo getTokenInfo(){
        return tokenInfo;
    }

    @Override
    public int compareTo(@NonNull Token token) {

        if(this.getTokenInfo().getSymbol().equalsIgnoreCase("ETH")){
            return -1;
        }
        if(token.getTokenInfo().getSymbol().equalsIgnoreCase("ETH")){
            return 1;
        }
        return this.getTokenInfo().getSymbol().compareTo(token.getTokenInfo().getSymbol());
    }
}
