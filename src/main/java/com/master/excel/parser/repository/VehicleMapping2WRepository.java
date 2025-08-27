package com.master.excel.parser.repository;

import com.master.excel.parser.dto.VehicleMapping2W;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleMapping2WRepository extends JpaRepository<VehicleMapping2W, Integer> {
    Optional<VehicleMapping2W> findByVehicleModelString(String vehicleModelString);
}
