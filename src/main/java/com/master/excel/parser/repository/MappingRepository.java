package com.master.excel.parser.repository;

import com.master.excel.parser.dto.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MappingRepository extends JpaRepository<Mapping, Integer> {
    Optional<Mapping> findByVehicleModelString(String vehicleModelString);

    Optional<Mapping> getByVehicleModelString(String vehicleModelString);
}
