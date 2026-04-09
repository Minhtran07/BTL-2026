package com.auction.model.item;

public class Electronics extends Item {
    private static final long  serialVersionUID = 1L;

    private String brand;
    private String model;
    private String condition;

    public Electronics(){super();}
    public Electronics(String name, String description,double startingPrice, String sellerId,
                    String brand,String model,String condition){
        super(name, description, startingPrice, sellerId);
        this.brand = brand;
        this.model = model;
        this.condition = condition;
    }

    @Override
    public ItemCategory getCategory(){
        return ItemCategory.ELECTRONICS;
    }

    //đóng gói
    public String getBrand(){return brand;}
    public void setBrand(String brand){this.brand =brand;}

    public String getModel(){return model;}
    public void setModel(String model){this.model=model;}

    public String getCondition(){return condition;}
    public void setCondition(String condition){this.condition = condition;}

    @Override
    public String printInfo(){
        return String.format("ELECTRONICS[id=%s,name=%s,brand=%s,model=%s,condition=%s,price=%.2f",
                getId(),getName(),brand,model,condition,getStartingPrice());
    }

}
