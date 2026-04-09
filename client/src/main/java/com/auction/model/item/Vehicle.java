package com.auction.model.item;

public class Vehicle extends Item{
    private static final long serialVersionUID =1L;

    private String make;
    private String vehicleModel;
    private int years;
    private int mileage;

    public Vehicle(){super();}

    public Vehicle(String name,String description,double startingPrice,String sellerId,
                   String make,String vehicleModel, int years, int mileage){
        super(name, description, startingPrice, sellerId);
        this.make = make;
        this.vehicleModel =vehicleModel;
        this.mileage = mileage;
    }

    @Override
    public ItemCategory getCategory(){
        return ItemCategory.VEHICLE;
    }

    public String getMake(){return make;}
    public void setMake(String make){this.make = make;}

    public String getVehicleModel(){return vehicleModel;}
    public void setVehicleModel(){this.vehicleModel = vehicleModel;}

    public int getYears(){return  years;}
    public void setYears(int years){this.years=years;}

    public int getMileage(){return  mileage;}
    public void setVehicleModel(int mileage){this.mileage = mileage;}
}
