package com.auction.model.item;


/**
 * Lớp Art - sản phẩm nghệ thuật.
 * Kế thừa Item - Inheritance & Polymorphism.
 */
public class Art extends Item {

    private static final long serialVersionUID = 1L;

    private String artist;
    private int year;
    private String medium; // Oil, Watercolor, Digital, etc.

    public Art() {
        super();
    }

    public Art(String name, String description, double startingPrice,
               String sellerId, String artist, int year, String medium) {
        super(name, description, startingPrice, sellerId);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.ART;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
        markUpdated();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        markUpdated();
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
        markUpdated();
    }

    @Override
    public String printInfo() {
        return String.format("Art[id=%s, name=%s, artist=%s, year=%d, medium=%s, price=%.2f]",
                getId(), getName(), artist, year, medium, getStartingPrice());
    }
}