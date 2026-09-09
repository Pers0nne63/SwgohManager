package swgohManager.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import java.awt.Color;


import lombok.RequiredArgsConstructor;
import swgohManager.model.Joueur;
import swgohManager.repository.JoueurRepository;

@Service
@RequiredArgsConstructor
public class OmicronExportService {

    private final JoueurRepository joueurRepository;
    private final OmicronPlanProgressService omicronPlanProgressService;

    public byte[] exportDetailXlsx() throws IOException {
        List<Joueur> joueurs = joueurRepository.findByPresentInGuildTrueOrderByPlayerNameAsc();
        List<OmicronPlanCalculationService.OmicronColonneDetail> colonnes = omicronPlanProgressService.getColonnesDetail();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Omicrons détail");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle playerStyle = buildPlayerStyle(workbook);
            CellStyle okStyle = buildStatutStyle(workbook, IndexedColors.GREEN.getIndex());
            CellStyle koStyle = buildStatutStyle(workbook, IndexedColors.RED.getIndex());
            CellStyle naStyle = buildStatutStyle(workbook, IndexedColors.GREY_50_PERCENT.getIndex());

            // --- Ligne d'en-tête ---
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(140); // pour les libellés en vertical
            Cell headerJoueur = headerRow.createCell(0);
            headerJoueur.setCellValue("Joueur");
            headerJoueur.setCellStyle(headerStyle);

            for (int col = 0; col < colonnes.size(); col++) {
            	OmicronPlanCalculationService.OmicronColonneDetail c = colonnes.get(col);
                Cell cell = headerRow.createCell(col + 1);
                cell.setCellValue("P" + c.priorite() + " - " + c.label());
                cell.setCellStyle(headerStyle);
            }

            // --- Lignes joueurs ---
            int rowIdx = 1;
            for (Joueur j : joueurs) {
                Row row = sheet.createRow(rowIdx++);
                Cell nomCell = row.createCell(0);
                nomCell.setCellValue(j.getPlayerName());
                nomCell.setCellStyle(playerStyle);

                Map<String, Boolean> statuts = omicronPlanProgressService.getStatutDetailParJoueur(j.getPlayerId(), Portee.GUILDE);

                for (int col = 0; col < colonnes.size(); col++) {
                	OmicronPlanCalculationService.OmicronColonneDetail c = colonnes.get(col);
                    Boolean etat = statuts.get(c.cle());
                    Cell cell = row.createCell(col + 1);

                    if (etat == null) {
                        cell.setCellValue("-");
                        cell.setCellStyle(naStyle);
                    } else if (etat) {
                        cell.setCellValue("\u2713");
                        cell.setCellStyle(okStyle);
                    } else {
                        cell.setCellValue("\u2717");
                        cell.setCellStyle(koStyle);
                    }
                }
            }

            // --- Mise en forme générale ---
            sheet.createFreezePane(1, 1); // fige colonne joueur + ligne d'en-tête
            sheet.setColumnWidth(0, 22 * 256); // colonne "Joueur"
            for (int col = 1; col <= colonnes.size(); col++) {
                sheet.setColumnWidth(col, 7 * 256); // colonnes omicrons étroites
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    	private CellStyle buildHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("Arial");
        font.setColor(IndexedColors.WHITE.getIndex());

        XSSFCellStyle style = (XSSFCellStyle) wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new Color(51, 65, 85), null)); // slate-700, cohérent avec le thème de l'appli
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.BOTTOM);
        style.setRotation((short) 90);
        style.setWrapText(true);
        return style;
    }

    private CellStyle buildPlayerStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("Arial");

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle buildStatutStyle(Workbook wb, short colorIndex) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setColor(colorIndex);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}