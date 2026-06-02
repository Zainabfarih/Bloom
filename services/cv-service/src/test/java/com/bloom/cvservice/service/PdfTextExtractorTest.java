package com.bloom.cvservice.service;

import com.bloom.cvservice.exception.CvProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extracts_text_from_valid_pdf() throws Exception {
        byte[] pdf = buildPdf("Java Spring Boot Developer");

        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, pdf);

        String text = extractor.extract(file);

        assertThat(text).contains("Java Spring Boot Developer");
    }

    @Test
    void rejects_empty_file() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(CvProcessingException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void rejects_non_pdf_file() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(CvProcessingException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void rejects_corrupted_pdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", MediaType.APPLICATION_PDF_VALUE, "not a real pdf".getBytes());

        assertThatThrownBy(() -> extractor.extract(file))
                .isInstanceOf(CvProcessingException.class);
    }

    private byte[] buildPdf(String content) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(100, 700);
                stream.showText(content);
                stream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
