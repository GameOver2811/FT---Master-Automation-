package com.master.excel.parser.dto;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_mapping_2w")
public class VehicleMapping2W {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Integer id;
    private String vehicleModelString;
    private String rewardVehicleType;
    private String productType;
    private String vehiclePowerBi;
    private String vehicleFuel;
    private String rewardModel;

    @Column(name = "ic", nullable = false)
    private String ic;

    public VehicleMapping2W() {
    }

    public VehicleMapping2W(Integer id, String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.id = id;
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public VehicleMapping2W(String vehicleModelString, String rewardVehicleType, String productType, String ic) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.ic = ic;
    }

    public VehicleMapping2W(String vehicleModelString, String rewardVehicleType, String productType, String vehiclePowerBi, String vehicleFuel, String rewardModel, String ic) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.vehiclePowerBi = vehiclePowerBi;
        this.vehicleFuel = vehicleFuel;
        this.rewardModel = rewardModel;
        this.ic = ic;
    }

    public VehicleMapping2W(String vehicleModelString, String rewardVehicleType, String productType, String vehiclePowerBi, String ic) {
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
        this.productType = productType;
        this.vehiclePowerBi = vehiclePowerBi;
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

    public String getVehiclePowerBi() {
        return vehiclePowerBi;
    }

    public void setVehiclePowerBi(String vehicle_power_bi) {
        this.vehiclePowerBi = vehicle_power_bi;
    }

    public String getVehicleFuel() {
        return vehicleFuel;
    }

    public void setVehicleFuel(String vehicle_fuel) {
        this.vehicleFuel = vehicle_fuel;
    }

    public String getRewardModel() {
        return rewardModel;
    }

    public void setRewardModel(String reward_model) {
        this.rewardModel = reward_model;
    }
}
