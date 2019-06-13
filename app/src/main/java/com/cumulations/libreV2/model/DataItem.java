package com.cumulations.libreV2.model;

public class DataItem {
    private Integer ItemID;
    private String ItemName;
    private String ItemType;
    private Integer favorite;

    public String getItemAlbumURL() {
        return ItemAlbumURL;
    }

    public void setItemAlbumURL(String itemAlbumURL) {
        ItemAlbumURL = itemAlbumURL;
    }

    private String ItemAlbumURL;

    public DataItem() {

        ItemID = null;
        ItemName = null;
        ItemType = null;
        favorite=null;
        ItemAlbumURL = null;
    }


    public Integer getItemID() {
        return ItemID;
    }

    public String getItemName() {
        return ItemName;
    }

    public String getItemType() {
        return ItemType;
    }

    public void setItemID(Integer itemID) {
        ItemID = itemID;
    }

    public void setItemName(String itemName) {
        ItemName = itemName;
    }

    public void setItemType(String itemType) {
        ItemType = itemType;
    }

    public Integer getFavorite() {
        return favorite;
    }

    public void setFavorite(Integer favorite) {
        this.favorite = favorite;
    }


}
