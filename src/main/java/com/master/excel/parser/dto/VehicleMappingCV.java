package com.master.excel.parser.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_mapping_cv")
public class VehicleMappingCV {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Integer id;
    private String vehicleModelString;
    private String rewardVehicleType;
    private String productType;
    private String ic;
    private String vehicleFuel;
    private String vehiclePowerBi;
    private String rewardModel;

    public VehicleMappingCV() {
    }

    public VehicleMappingCV(Integer id, String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.id = id;
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public VehicleMappingCV(String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public VehicleMappingCV(String vehicleModelString, String rewardVehicleType, String productType, String ic, String vehicleFuel, String vehiclePowerBi, String rewardModel) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
        this.vehicleFuel = vehicleFuel;
        this.vehiclePowerBi = vehiclePowerBi;
        this.rewardModel = rewardModel;
    }

    public String getVehicleFuel() {
        return vehicleFuel;
    }

    public void setVehicleFuel(String vehicleFuel) {
        this.vehicleFuel = vehicleFuel;
    }

    public String getVehiclePowerBi() {
        return vehiclePowerBi;
    }

    public void setVehiclePowerBi(String vehiclePowerBi) {
        this.vehiclePowerBi = vehiclePowerBi;
    }

    public String getRewardModel() {
        return rewardModel;
    }

    public void setRewardModel(String rewardModel) {
        this.rewardModel = rewardModel;
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
