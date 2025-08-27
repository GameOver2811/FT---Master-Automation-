package com.master.excel.parser.dao;

import com.master.excel.parser.dto.VehicleMapping4W;
import com.master.excel.parser.repository.VehicleMapping4WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleMapping4WDao {

    @Autowired
    VehicleMapping4WRepository vehicleMapping4WRepository;

    public void save(VehicleMapping4W data) {
        try {
            if(containsVehicleModelString(data.getVehicleModelString())){
                System.out.println("Mapping already present for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
                return;
            }
            vehicleMapping4WRepository.save(data);
            // System.out.println("Mapping table updated successfully for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
            return;
        } catch (Exception e) {
            System.out.println("Error saving record for " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType() + "\n error_txt: " + e.getMessage());
            return;
        }
    }

    public Boolean containsVehicleModelString(String vehicleModelString) {
        return vehicleMapping4WRepository.findByVehicleModelString(vehicleModelString).isPresent();
    }

    public String getRewardVehicleType(String vehicleModelString) {
        return vehicleMapping4WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping4W::getRewardVehicleType)
                .orElse(null);
    }

}
