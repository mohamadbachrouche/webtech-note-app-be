package de.htw.webtech.service;

import com.lowagie.text.DocumentException;
import de.htw.webtech.domain.Note;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generatePdf(Note note) {
        String xhtml = buildXhtml(note);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    public static String sanitizeFilename(String title) {
        if (title == null) return "note";
        String sanitized = title.replaceAll("[^a-zA-Z0-9 _-]", "").trim().replaceAll("\\s+", "_");
        return sanitized.isEmpty() ? "note" : sanitized;
    }

    private String buildXhtml(Note note) {
        String title = escapeXml(note.getTitle() != null ? note.getTitle() : "Untitled");
        String content = normalizeHtml(note.getContent());
        String createdAt = note.getCreatedAt() != null ? note.getCreatedAt().format(FORMATTER) : "-";
        String lastModified = note.getLastModified() != null ? note.getLastModified().format(FORMATTER) : "-";
        String tagsSection = "";
        if (note.getTags() != null && !note.getTags().isBlank()) {
            tagsSection = "<p class=\"tags\">Tags: " + escapeXml(note.getTags()) + "</p>";
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                  <style type="text/css">
                    body { font-family: Arial, sans-serif; margin: 40px; color: #333; }
                    h1.note-title { font-size: 24pt; margin-bottom: 6px; }
                    p.tags { font-size: 10pt; color: #666; margin-top: 0; margin-bottom: 16px; }
                    .content { font-size: 11pt; line-height: 1.5; }
                    .content h1, .content h2, .content h3 { margin-top: 14px; }
                    .content ul, .content ol { margin-left: 20px; }
                    .footer { margin-top: 32px; font-size: 9pt; color: #888; border-top: 1px solid #ccc; padding-top: 8px; }
                  </style>
                </head>
                <body>
                  <h1 class="note-title">%s</h1>
                  %s
                  <div class="content">%s</div>
                  <div class="footer">Created: %s | Last Modified: %s</div>
                </body>
                </html>
                """.formatted(title, tagsSection, content, createdAt, lastModified);
    }

    private String normalizeHtml(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return doc.body().html();
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
