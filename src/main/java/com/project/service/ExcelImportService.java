package com.project.service;

import com.project.model.Citizen;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;

public class ExcelImportService {

    private static String getCellAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    public static void importDataFromExcel(SessionFactory sf) {
        String filePath = "C:\\Users\\neera\\eclipse-workspace\\RelativesConnection\\src\\main\\resources\\family_data.xlsx";

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis);
             Session session = sf.openSession()) {

            Sheet sheet = workbook.getSheetAt(0);
            int successCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Transaction tx = session.beginTransaction();
                try {
                    Citizen citizen = new Citizen();

                    String isHead = getCellAsString(row.getCell(0)).toLowerCase();
                    String nationalId = getCellAsString(row.getCell(1));
                    String fullName = getCellAsString(row.getCell(2));
                    String gender = getCellAsString(row.getCell(3));
                    String dobString = getCellAsString(row.getCell(4));
                    
                    if (nationalId.isEmpty() || fullName.isEmpty()) {
                        tx.rollback();
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
                            Citizen head = session.createQuery("FROM Citizen WHERE nationalId = :nid", Citizen.class)
                                                  .setParameter("nid", headNid).uniqueResult();
                            if (head != null) {
                                citizen.setFamilyHead(head);
                                citizen.setRelationToHead(getCellAsString(row.getCell(6)).toUpperCase());
                            } else {
                                tx.rollback();
                                continue;
                            }
                        }
                    }

                    session.persist(citizen);
                    tx.commit();
                    successCount++;

                } catch (Exception rowEx) {
                    tx.rollback();
                }
            }
            System.out.println("\nExcel Import Complete! Successfully imported " + successCount + " records.");
        } catch (Exception e) {
            System.out.println("Failed to read Excel file: " + e.getMessage());
        }
    }
}