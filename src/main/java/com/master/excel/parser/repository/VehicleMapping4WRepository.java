package com.master.excel.parser.repository;

import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.dto.VehicleMapping4W;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleMapping4WRepository extends JpaRepository<VehicleMapping4W, Integer> {
    Optional<VehicleMapping4W> findByVehicleModelString(String vehicleModelString);

    @NativeQuery("SELECT m.id, m.make_name, mk.id as make_code, m.name AS model_name, m.id as model_code FROM master_car_model AS m JOIN master_car_make AS mk ON m.make_id = mk.id where m.ic_type is null and mk.ic_type is null")
    List<MakeModelCode> findAllWithJoin();
}
