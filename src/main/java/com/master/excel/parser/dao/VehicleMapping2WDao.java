package com.master.excel.parser.dao;

import com.master.excel.parser.dto.VehicleMapping2W;
import com.master.excel.parser.repository.VehicleMapping2WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleMapping2WDao {

    @Autowired
    VehicleMapping2WRepository vehicleMapping2WRepository;

    public void save(VehicleMapping2W data) {
        try {
            if(containsVehicleModelString(data.getVehicleModelString())){
                System.out.println("Mapping already present for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
                return;
            }
            vehicleMapping2WRepository.save(data);
            // System.out.println("Mapping table updated successfully for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
            return;
        } catch (Exception e) {
            System.out.println("Error saving record for " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType() + "\n error_txt: " + e.getMessage());
            return;
        }
    }

    public Boolean containsVehicleModelString(String vehicleModelString) {
        return vehicleMapping2WRepository.findByVehicleModelString(vehicleModelString).isPresent();
    }

    public String getRewardVehicleType(String vehicleModelString) {
        return vehicleMapping2WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping2W::getRewardVehicleType)
                .orElse(null);
    }

    public String getVehiclePowerBi(String vehicleModelString) {
        return vehicleMapping2WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping2W::getVehiclePowerBi)
                .orElse(null);
    }

    public String getRewardModel(String vehicleModelString) {
        return vehicleMapping2WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping2W::getRewardModel)
                .orElse(null);
    }

    public String getRewardVehicleFuelType(String vehicleModelString) {
        return vehicleMapping2WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping2W::getVehicleFuel)
                .orElse(null);
    }

}
