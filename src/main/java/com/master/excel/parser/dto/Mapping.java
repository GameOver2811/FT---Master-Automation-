package com.master.excel.parser.dto;

import jakarta.persistence.*;

@Entity
public class Mapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Integer id;

    private String vehicleModelString;
    private String rewardVehicleType;

    public Mapping() {
    }

    public Mapping(Integer id, String vehicleModelString, String rewardVehicleType) {
        this.id = id;
        this.vehicleModelString = vehicleModelString;
        this.rewardVehicleType = rewardVehicleType;
    }

    public Mapping(String concatenatedString, String rewardType) {
        this.rewardVehicleType = rewardType;
        this.vehicleModelString = concatenatedString;
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

