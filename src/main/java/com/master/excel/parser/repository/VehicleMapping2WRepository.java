package com.master.excel.parser.repository;

import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.dto.VehicleMapping2W;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleMapping2WRepository extends JpaRepository<VehicleMapping2W, Integer> {
    Optional<VehicleMapping2W> findByVehicleModelString(String vehicleModelString);

    @NativeQuery("SELECT m.id, m.make_name, mk.id as make_code, m.name AS model_name, m.id as model_code FROM master_tw_model AS m JOIN master_tw_make AS mk ON m.make_id = mk.id where m.ic_type is null and mk.ic_type is null")
    List<MakeModelCode> findAllWithJoin();
}
