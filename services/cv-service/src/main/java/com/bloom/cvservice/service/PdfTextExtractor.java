package com.bloom.cvservice.service;

import com.bloom.cvservice.exception.CvProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extrait le texte brut d'un fichier CV au format PDF (PDFBox 3.x).
 */
@Service
@Slf4j
public class PdfTextExtractor {

    private static final String PDF_MIME = "application/pdf";

    public String extract(MultipartFile file) {
        validate(file);
        try {
            return extract(file.getBytes());
        } catch (IOException e) {
            log.warn("Échec de lecture du PDF '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new CvProcessingException("Le fichier PDF est corrompu ou illisible.", e);
        }
    }

    /** Extrait le texte d'un PDF déjà chargé en mémoire (ex : fichier stocké en base). */
    public String extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new CvProcessingException("Le fichier CV est vide.");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            if (document.isEncrypted()) {
                throw new CvProcessingException("Le PDF est protégé/chiffré et ne peut être lu.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document).trim();

            if (text.isBlank()) {
                throw new CvProcessingException(
                        "Aucun texte exploitable n'a pu être extrait du PDF " +
                        "(CV scanné en image ?). Réessayez avec un PDF textuel.");
            }

            log.debug("PDF parsé — {} pages, {} caractères extraits",
                    document.getNumberOfPages(), text.length());
            return text;

        } catch (IOException e) {
            log.warn("Échec de lecture du PDF: {}", e.getMessage());
            throw new CvProcessingException("Le fichier PDF est corrompu ou illisible.", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CvProcessingException("Le fichier CV est vide.");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean isPdfMime = PDF_MIME.equalsIgnoreCase(contentType);
        boolean isPdfExt = filename != null && filename.toLowerCase().endsWith(".pdf");

        if (!isPdfMime && !isPdfExt) {
            throw new CvProcessingException(
                    "Seuls les fichiers PDF sont acceptés. Reçu: " + contentType);
        }
    }
}
