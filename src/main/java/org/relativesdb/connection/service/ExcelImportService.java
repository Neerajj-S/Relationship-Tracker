package org.relativesdb.connection.service;

import org.relativesdb.connection.model.Citizen;
import org.relativesdb.connection.repository.CitizenRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;

@Service
public class ExcelImportService {

    @Autowired
    private CitizenRepository citizenRepository;

    private static String getCellAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public void importDataFromExcel() {
        String filePath = "C:\\Users\\neera\\eclipse-workspace\\RelativesConnection\\src\\main\\resources\\family_data.xlsx";

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Citizen citizen = new Citizen();

                    String isHead = getCellAsString(row.getCell(0)).toLowerCase();
                    String nationalId = getCellAsString(row.getCell(1));
                    String fullName = getCellAsString(row.getCell(2));
                    String gender = getCellAsString(row.getCell(3));
                    String dobString = getCellAsString(row.getCell(4));
                    
                    if (nationalId.isEmpty() || fullName.isEmpty()) {
                        continue;
                    }

                    // Check if citizen already exists to prevent duplicate key crashes
                    if (citizenRepository.findByNationalId(nationalId) != null) {
                        continue;
                    }

                    citizen.setNationalId(nationalId);
                    citizen.setFullName(fullName);
                    citizen.setGender(gender);
                    citizen.setDateOfBirth(LocalDate.parse(dobString));

                    if (isHead.equals("yes")) {
                        citizen.setRelationToHead("SELF");
                        citizen.setFamilyHead(null);
                    } else {
                        String headNid = getCellAsString(row.getCell(5));
                        if (!headNid.isEmpty()) {
                            Citizen head = citizenRepository.findByNationalId(headNid);
                            if (head != null) {
                                citizen.setFamilyHead(head);
                                citizen.setRelationToHead(getCellAsString(row.getCell(6)).toUpperCase());
                            } else {
                                System.out.println("Row " + (i + 1) + ": Family Head " + headNid + " not found in DB. Skipping.");
                                continue;
                            }
                        }
                    }

                    citizenRepository.save(citizen);
                    successCount++;

                } catch (Exception rowEx) {
                    System.out.println("Error on row " + (i + 1) + ": " + rowEx.getMessage());
                }
            }
            System.out.println("\nExcel Import Complete! Successfully imported " + successCount + " records.");
        } catch (Exception e) {
            System.out.println("Failed to read Excel file (Make sure family_data.xlsx is in your project root folder): " + e.getMessage());
        }
    }
}