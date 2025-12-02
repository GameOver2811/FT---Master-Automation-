package com.master.excel.parser.dto;

import jakarta.persistence.Id;

public class MakeModelCode {

    public MakeModelCode() {
    }

    public MakeModelCode(Integer id, String makeName, Integer makeCode, String modelName, Integer modelCode) {
        this.id = id;
        this.makeName = makeName;
        this.makeCode = makeCode;
        this.modelName = modelName;
        this.modelCode = modelCode;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMakeName() {
        return makeName;
    }

    public void setMakeName(String makeName) {
        this.makeName = makeName;
    }

    public Integer getMakeCode() {
        return makeCode;
    }

    public void setMakeCode(Integer makeCode) {
        this.makeCode = makeCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getModelCode() {
        return modelCode;
    }

    public void setModelCode(Integer modelCode) {
        this.modelCode = modelCode;
    }

    @Id
    private Integer id;
    private String makeName;
    private Integer makeCode;
    private String modelName;
    private Integer modelCode;
}
