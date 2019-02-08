package org.vikulin.etherwallet.adapter.pojo;

import java.io.Serializable;

/**
 * Created by vadym on 03.12.16.
 */
public class SellItem implements Serializable {

    private Double price;

    private String name;

    private Long barCode;

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Long getBarCode(){
        return barCode;
    }

    public void setBarCode(Long barCode){
        this.barCode = barCode;
    }
}
