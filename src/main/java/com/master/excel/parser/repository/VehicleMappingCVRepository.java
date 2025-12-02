package com.master.excel.parser.repository;

import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.dto.VehicleMappingCV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleMappingCVRepository extends JpaRepository<VehicleMappingCV, Integer> {
    Optional<VehicleMappingCV> findByVehicleModelString(String vehicleModelString);
    Optional<VehicleMappingCV> findByVehicleModelStringAndIc(String vehicleModelString, String ic);

    @NativeQuery("SELECT m.id, m.make_name, mk.id as make_code, m.name AS model_name, m.id as model_code FROM master_cv_model AS m JOIN master_cv_make AS mk ON m.make_id = mk.id")
    List<MakeModelCode> findAllWithJoin();
}
