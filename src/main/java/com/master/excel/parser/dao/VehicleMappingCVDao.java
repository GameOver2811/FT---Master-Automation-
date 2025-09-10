package com.master.excel.parser.dao;

import com.master.excel.parser.dto.VehicleMappingCV;
import com.master.excel.parser.repository.VehicleMappingCVRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleMappingCVDao {

    @Autowired
    VehicleMappingCVRepository vehicleMappingCVRepository;

    public void save(VehicleMappingCV data, String ic) {
        try {
            if(containsVehicleModelString(data.getVehicleModelString(), ic)){
                System.out.println("Mapping already present for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
                return;
            }
            vehicleMappingCVRepository.save(data);
            // System.out.println("Mapping table updated successfully for : " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType());
            return;
        } catch (Exception e) {
            System.out.println("Error saving record for " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType() + "\n error_txt: " + e.getMessage());
            return;
        }
    }

    public Boolean containsVehicleModelString(String vehicleModelString, String ic) {
        return vehicleMappingCVRepository.findByVehicleModelStringAndIc(vehicleModelString, ic).isPresent();
    }

    public String getRewardVehicleType(String vehicleModelString, String ic) {
        return vehicleMappingCVRepository
                .findByVehicleModelStringAndIc(vehicleModelString, ic)
                .map(VehicleMappingCV::getRewardVehicleType)
                .orElse(null);
    }

    public String getVehiclePowerBi(String vehicleModelString, String ic) {
        return vehicleMappingCVRepository
                .findByVehicleModelStringAndIc(vehicleModelString, ic)
                .map(VehicleMappingCV::getVehiclePowerBi)
                .orElse(null);
    }

    public String getRewardModel(String vehicleModelString, String ic) {
        return vehicleMappingCVRepository
                .findByVehicleModelStringAndIc(vehicleModelString, ic)
                .map(VehicleMappingCV::getRewardModel)
                .orElse(null);
    }

    public String getRewardVehicleFuelType(String vehicleModelString, String ic) {
        return vehicleMappingCVRepository
                .findByVehicleModelStringAndIc(vehicleModelString, ic)
                .map(VehicleMappingCV::getVehicleFuel)
                .orElse(null);
    }

}
