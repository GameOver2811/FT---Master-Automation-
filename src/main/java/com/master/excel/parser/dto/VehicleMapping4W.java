package com.master.excel.parser.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_mapping_4w")
public class VehicleMapping4W {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Integer id;
    private String vehicleModelString;
    private String rewardVehicleType;
    private String productType;
    private String ic;

    public VehicleMapping4W() {
    }

    public VehicleMapping4W(Integer id, String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.id = id;
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public VehicleMapping4W(String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getIc() {
        return ic;
    }

    public void setIc(String ic) {
        this.ic = ic;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVehicleModelString() {
        return vehicleModelString;
    }

    public void setVehicleModelString(String vehicleModelString) {
        this.vehicleModelString = vehicleModelString;
    }

    public String getRewardVehicleType() {
        return rewardVehicleType;
    }

    public void setRewardVehicleType(String rewardVehicleType) {
        this.rewardVehicleType = rewardVehicleType;
    }
}
