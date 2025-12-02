package com.master.excel.parser.controller;

import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.repository.VehicleMapping4WRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class Test {

    @Autowired
    VehicleMapping4WRepository vehicleMapping4WRepository;

    @GetMapping("")
    public ResponseEntity<?> test(){
        return ResponseEntity.ok("Working fine!");
    }

    @GetMapping("/get-make-model-code")
    public ResponseEntity<?> testMakeModelCode(){
        List<MakeModelCode> out = vehicleMapping4WRepository.findAllWithJoin();
        return ResponseEntity.ok(out);
    }

}
