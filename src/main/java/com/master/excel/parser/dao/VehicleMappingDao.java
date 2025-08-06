package com.master.excel.parser.dao;

import com.master.excel.parser.dto.Mapping;
import com.master.excel.parser.dto.Mapping;
import com.master.excel.parser.repository.MappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleMappingDao {

    @Autowired
    MappingRepository mappingRepository;

    public String saveRewardVehicleType(Mapping mappingRequest) {
        try {
            if(containsVehicleModelString(mappingRequest.getVehicleModelString())){
                return "Mapping already present for : " + mappingRequest.getVehicleModelString() + " -> " + mappingRequest.getRewardVehicleType();
            }
            mappingRepository.save(mappingRequest);
            return "Mapping table updated successfully for : " + mappingRequest.getVehicleModelString() + " -> " + mappingRequest.getRewardVehicleType();
        } catch (Exception e) {
            return "Error saving record for " + mappingRequest.getVehicleModelString() + " -> " + mappingRequest.getRewardVehicleType() + "\n error_txt: " + e.getMessage();
        }
    }

    public Boolean containsVehicleModelString(String vehicleModelString) {
        return mappingRepository.findByVehicleModelString(vehicleModelString).isPresent();
    }

    public String getRewardVehicleType(String vehicleModelString) {
        return mappingRepository
                .findByVehicleModelString(vehicleModelString)
                .map(Mapping::getRewardVehicleType)
                .orElse(null);
    }


}
