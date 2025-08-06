
# Master Excel Parser API Documentation

## Overview

The **Master Excel Parser API** processes and maps data from multiple `.xlsx` files to generate a consolidated and structured output. This API requires three primary Excel files and specific configurations for mapping, details, and vehicle type classification.

---

## API Endpoint

**Method:** `POST`  
**Endpoint:** `/api/upload`  
**Content-Type:** `multipart/form-data`

---

## Required Files

You must upload the following three Excel files in `.xlsx` format:

| Parameter Name     | Description                     |
|--------------------|---------------------------------|
| `masterFile`       | The base reference Excel sheet. |
| `resultFile`       | The result or output template sheet. |
| `liveMasterFile`   | Live or updated master dataset. |

---

## Required Fields in Request Body

### 1. `mapping` (JSON Object)

Defines direct column mappings between the result file and the master file.

**Format:**
```json
{
  "ResultColumn1": "MasterColumnA",
  "ResultColumn2": "MasterColumnB"
}
```

### 2. `details` (Array)

Specifies detailed configuration for custom processing. The array must follow this strict order:

| Index | Description                                 | Source File          |
|-------|---------------------------------------------|----------------------|
| 0     | Reward vehicle fuel type                    | Result File          |
| 1     | TXT fuel                                    | Master File          |
| 2     | Reward vehicle power                        | Result File          |
| 3     | Cubic capacity                              | Result File          |

**Example:**
```json
[
  "FuelTypeColumn_Result",
  "TXT_Fuel_Master",
  "Power_Result",
  "CubicCapacity_Result"
]
```

### 3. `vehicleType` (Array)

Used for distinguishing vehicle types (e.g., 2-wheeler vs 4-wheeler). This array contains objects specifying the column name and the value used to differentiate the types.
Leave this attribute blank if not segregation is required.

**Format:**
```json
[
  
  "column": "vehicleType",
  "value": "Vehicle type column name", "2W"
  
]
```
### 4. `vlookupMapping` (JSON Object)

Used for fetching data from live master. This will contain key value pair where `key = Column data to be fetched` and `value = unique reference column`.

**Format**
```json
{
  "reward_vehicle_type": "vehiclemodelcode"
}
```
---

## Example Request in Postman

**Method:** `POST`  
**URL:** `http://localhost:8080/api/upload`

**Body (form-data):**
```
masterFile: <attach your master.xlsx>
resultFile: <attach your result_template.xlsx>
liveMasterFile: <attach your live_master.xlsx>
mapping: {
    "Reward Power": "Power",
    "Fuel Type": "Fuel"
}
details: [
    "Fuel Type",
    "TXT Fuel",
    "Power",
    "Cubic Capacity"
]
vehicleType: [
    "column": "Vehicle Category",
    "value": "2W"
]

vlookupMap: {
    "reward_vehicle_type": "vehiclemodelcode"
}
```

---

## Response

**Success (200 OK):**

The API responds with a binary `.xlsx` file. Postman will detect it as a file download.

**Example:**
- File Name: `result Template file name_date and time.csv`
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.csv`

To save the output:

1. In Postman, go to the **"Send and Download"** option next to the "Send" button.
2. Choose a location to save the resulting `.csv` file.

---

**Error (400/500):**
```json
{
  "status": "error",
  "message": "Missing required field: masterFile"
}

```

---

## Notes

- All files must be in `.xlsx` or `.csv` format.
- The `details` array **must follow the specified order strictly** for correct processing.
- Mismatched or missing column names in `mapping` or `details` may cause parsing errors.
- Details fetched from live data must be passed in `vlookupMap` field.

---

## Author

**API Owner:** Krishna Dwivedi

**Contact:** kdwivedi343@gmail.com
