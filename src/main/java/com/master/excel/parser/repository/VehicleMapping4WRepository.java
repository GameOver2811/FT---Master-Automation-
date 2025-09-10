package com.master.excel.parser.repository;

import com.master.excel.parser.dto.VehicleMapping4W;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleMapping4WRepository extends JpaRepository<VehicleMapping4W, Integer> {
    Optional<VehicleMapping4W> findByVehicleModelString(String vehicleModelString);
}
