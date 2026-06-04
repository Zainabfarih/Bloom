package com.bloom.cvservice.service;

import com.bloom.cvservice.dto.ManualCvRequest;
import com.bloom.cvservice.exception.CvProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Génère un PDF "ATS-friendly" depuis une saisie manuelle :
 * texte sélectionnable, police standard, structure linéaire en sections,
 * pas d'images ni de colonnes — pour maximiser la lisibilité par les ATS.
 */
@Service
@Slf4j
public class CvPdfGenerator {

    private static final float MARGIN       = 50f;
    private static final float PAGE_WIDTH   = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT  = PDRectangle.A4.getHeight();
    private static final float CONTENT_W    = PAGE_WIDTH - 2 * MARGIN;
    private static final float LEADING      = 16f;

    private final PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font fontBold     = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generate(ManualCvRequest req) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Cursor cursor = new Cursor(doc);

            String title = (req.getTitle() != null && !req.getTitle().isBlank())
                    ? req.getTitle().trim() : "Curriculum Vitae";
            cursor.writeLine(title, fontBold, 20f);
            cursor.gap(6f);

            section(cursor, "SUMMARY", List.of(req.getSummary()));
            section(cursor, "EXPERIENCE", req.getExperiences());
            section(cursor, "EDUCATION", req.getEducations());
            section(cursor, "SKILLS", List.of(String.join(", ", req.getSkills())));

            cursor.close();
            doc.save(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Échec de génération du PDF ATS", e);
            throw new CvProcessingException("Impossible de générer le PDF du CV.", e);
        }
    }

    private void section(Cursor cursor, String heading, List<String> items) {
        if (items == null || items.isEmpty()) return;

        cursor.gap(8f);
        cursor.writeLine(heading, fontBold, 13f);
        cursor.gap(2f);

        for (String item : items) {
            if (item == null || item.isBlank()) continue;
            for (String line : wrap(item.trim(), fontRegular, 11f)) {
                cursor.writeLine(line, fontRegular, 11f);
            }
        }
    }

    /** Découpe un texte en lignes tenant dans la largeur de contenu. */
    private List<String> wrap(String text, PDType1Font font, float fontSize) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (textWidth(candidate, font, fontSize) > CONTENT_W && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private float textWidth(String text, PDType1Font font, float fontSize) {
        try {
            return font.getStringWidth(sanitize(text)) / 1000f * fontSize;
        } catch (Exception e) {
            return 0f;
        }
    }

    /** Standard-14 fonts (WinAnsi) ne gèrent pas tous les caractères Unicode. */
    private static String sanitize(String text) {
        return text.replaceAll("[^\\x20-\\x7E\\u00A0-\\u00FF]", " ");
    }

    /** Suit la position d'écriture et gère le saut de page automatique. */
    private final class Cursor {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        Cursor(PDDocument doc) throws Exception {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws Exception {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        void writeLine(String text, PDType1Font font, float fontSize) {
            try {
                if (y <= MARGIN) newPage();
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(sanitize(text));
                stream.endText();
                y -= LEADING;
            } catch (Exception e) {
                throw new CvProcessingException("Erreur d'écriture du PDF.", e);
            }
        }

        void gap(float h) {
            y -= h;
        }

        void close() throws Exception {
            if (stream != null) stream.close();
        }
    }
}
