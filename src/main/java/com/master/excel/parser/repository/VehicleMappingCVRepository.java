package com.master.excel.parser.repository;

import com.master.excel.parser.dto.VehicleMappingCV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleMappingCVRepository extends JpaRepository<VehicleMappingCV, Integer> {
    Optional<VehicleMappingCV> findByVehicleModelString(String vehicleModelString);
    Optional<VehicleMappingCV> findByVehicleModelStringAndIc(String vehicleModelString, String ic);
}
