package org.saidone.quizmaker.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Slf4j
public class DocumentTextExtractorService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        val filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            if (filename.endsWith(".pdf")) {
                return extractFromPdf(file.getBytes());
            }
            if (filename.endsWith(".docx")) {
                return extractFromDocx(file.getBytes());
            }
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Errore durante l'estrazione del testo da: {}", filename, e);
            throw new IllegalArgumentException("Impossibile leggere il file. Usa un PDF, DOCX o testo semplice.");
        }
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (val document = Loader.loadPDF(bytes)) {
            val stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractFromDocx(byte[] bytes) throws IOException {
        try (val document = new XWPFDocument(new ByteArrayInputStream(bytes));
             val extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
