package com.master.excel.parser.dao;

import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.dto.VehicleMapping4W;
import com.master.excel.parser.repository.VehicleMapping4WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
            return;

//            Temporary -- For DB Correction
            /*
            Optional<VehicleMapping4W> optional =
                    vehicleMapping4WRepository.findByVehicleModelString(data.getVehicleModelString());

            VehicleMapping4W entity;

            if (optional.isPresent()) {
                VehicleMapping4W existing = optional.get();

                if (existing.getIc().equalsIgnoreCase("digit")
                        || existing.getIc().equalsIgnoreCase("raheja")
                        || existing.getIc().equalsIgnoreCase("zuno")) {
                    entity = null;
                } else {
                    existing.setRewardVehicleType(data.getRewardVehicleType());
                    existing.setProductType(data.getProductType());
                    existing.setIc(data.getIc());
                    existing.setVehicleFuel(data.getVehicleFuel());
                    existing.setRewardModel(data.getRewardModel());
                    existing.setVehiclePowerBi(data.getVehiclePowerBi());
                    entity = existing;
                }
            } else {
                entity = data;
            }

            // Save only if something to save
            if (entity != null) {
                vehicleMapping4WRepository.save(entity);
            }

             */

        } catch (Exception e) {
            System.out.println("Error saving record for " + data.getVehicleModelString() + " -> " + data.getRewardVehicleType() + "\n error_txt: " + e.getMessage());
            return;
        }
    }

    public Boolean containsVehicleModelString(String vehicleModelString) {
        return vehicleMapping4WRepository.existsByVehicleModelString(vehicleModelString);
    }

    public String getRewardVehicleType(String vehicleModelString) {
        return vehicleMapping4WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping4W::getRewardVehicleType)
                .orElse(null);
    }

    public String getVehiclePowerBi(String vehicleModelString) {
        return vehicleMapping4WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping4W::getVehiclePowerBi)
                .orElse(null);
    }

    public String getRewardModel(String vehicleModelString) {
        return vehicleMapping4WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping4W::getRewardModel)
                .orElse(null);
    }

    public String getRewardVehicleFuelType(String vehicleModelString) {
        return vehicleMapping4WRepository
                .findByVehicleModelString(vehicleModelString)
                .map(VehicleMapping4W::getVehicleFuel)
                .orElse(null);
    }

    public List<MakeModelCode> getMakeModelCode(){
        return vehicleMapping4WRepository.findAllWithJoin();
    }

}
