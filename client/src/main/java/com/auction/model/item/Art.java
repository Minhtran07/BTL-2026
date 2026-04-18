package com.auction.model.item;

public class Art extends Item{

    private static final long serialVersionUID = 1L;

    private String artist;
    private int years;
    private String medium;

    public Art(){super();}

    public Art(String name, String description, double startingPrice, String sellerId,
               String artist, int years, String medium){
        super(name, description, startingPrice, sellerId);
        this.artist = artist;
        this.years = years;
        this.medium = medium;
    }

    @Override
    public ItemCategory getCategory(){
        return ItemCategory.ART;
    }

    //tính đóng gói vs thuộc tính private
    public String getArtist(){return artist;}
    public void setArtist(String artist){this.artist = artist;}

    public int getYears(){return years;}
    public void setYears(int years){this.years = years;}

    public String getMedium(){return medium;}
    public void setMedium(String medium){this.medium = medium;}

    @Override
    public String printInfo(){
        return String.format("ART[id=%s,name=%s,artist=%s,years=%d,medium=%s,price=%.2f",
                getId(),getName(),artist,years,medium,getStartingPrice());
    }
}