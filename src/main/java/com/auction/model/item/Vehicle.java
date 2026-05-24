package com.auction.model.item;


/**
 * Lớp Vehicle - phương tiện.
 * Kế thừa Item - Inheritance & Polymorphism.
 */
public class Vehicle extends Item {

    private static final long serialVersionUID = 1L;

    private String make;
    private String vehicleModel;
    private int year;
    private int mileage;

    public Vehicle() {
        super();
    }

    public Vehicle(String name, String description, double startingPrice,
                   String sellerId, String make, String vehicleModel, int year, int mileage) {
        super(name, description, startingPrice, sellerId);
        this.make = make;
        this.vehicleModel = vehicleModel;
        this.year = year;
        this.mileage = mileage;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.VEHICLE;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
        markUpdated();
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
        markUpdated();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
        markUpdated();
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
        markUpdated();
    }

    @Override
    public String printInfo() {
        return String.format("Vehicle[id=%s, name=%s, make=%s, model=%s, year=%d, mileage=%dkm, price=%.2f]",
                getId(), getName(), make, vehicleModel, year, mileage, getStartingPrice());
    }
}