package org.vikulin.etherwallet.adapter.pojo;

/**
 * Created by vadym on 30.07.17.
 */

public class TokenInfo {

    private String address;
    private String name;
    private Long decimals;
    private String symbol;
    private String description;
    //private Price price;
    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    public Long getDecimals() {
        return decimals;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDecimals(Long decimals) {
        this.decimals = decimals;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** TODO
     * Price is receiving boolean instead of object
     */
    //public Price getPrice(){
    //    return price;
    //}

}
