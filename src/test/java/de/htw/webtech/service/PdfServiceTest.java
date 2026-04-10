package de.htw.webtech.service;

import de.htw.webtech.domain.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private PdfService service;

    @BeforeEach
    void setUp() {
        service = new PdfService();
    }

    private Note noteWithContent(String title, String html) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(html);
        note.setTags("alpha, beta");
        note.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        note.setLastModified(LocalDateTime.of(2024, 1, 2, 9, 30));
        return note;
    }

    // ----- generatePdf: smoke tests across HTML tag families -----

    @Test
    void shouldGeneratePdfForPlainNote() {
        Note note = noteWithContent("Plain", "<p>Just some text.</p>");

        byte[] pdf = service.generatePdf(note);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "PDF should be non-trivial in size");
        assertEquals("%PDF", new String(pdf, 0, 4),
                "PDF output must start with the %PDF magic header");
    }

    @Test
    void shouldGeneratePdfForAllHeadingLevels() {
        String html = "<h1>H1</h1><h2>H2</h2><h3>H3</h3>"
                + "<h4>H4</h4><h5>H5</h5><h6>H6</h6>";
        Note note = noteWithContent("Headings", html);

        byte[] pdf = service.generatePdf(note);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void shouldGeneratePdfForOrderedAndUnorderedLists() {
        String html = "<ul><li>First</li><li>Second</li></ul>"
                + "<ol><li>Step 1</li><li>Step 2</li></ol>";
        Note note = noteWithContent("Lists", html);

        byte[] pdf = service.generatePdf(note);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void shouldGeneratePdfForBlockquoteAndInlineFormatting() {
        String html = "<blockquote>Quoted text</blockquote>"
                + "<p><b>bold</b> and <i>italic</i> and <strong>strong</strong>"
                + " and <em>emphasis</em> and <u>underline</u></p>";
        Note note = noteWithContent("Formatting", html);

        byte[] pdf = service.generatePdf(note);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void shouldHandleEmptyAndNullContent() {
        Note nullContent = noteWithContent("Null", null);
        Note blankContent = noteWithContent("Blank", "   ");

        byte[] pdfNull = service.generatePdf(nullContent);
        byte[] pdfBlank = service.generatePdf(blankContent);

        assertNotNull(pdfNull);
        assertNotNull(pdfBlank);
        assertTrue(pdfNull.length > 0);
        assertTrue(pdfBlank.length > 0);
    }

    @Test
    void shouldHandleMissingTagsAndTimestamps() {
        Note note = new Note();
        note.setTitle("No metadata");
        note.setContent("<p>body</p>");
        // tags, createdAt, lastModified all null

        byte[] pdf = service.generatePdf(note);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    // ----- sanitizeFilename: edge cases -----

    @Test
    void sanitizeFilenameShouldReplaceSpacesWithUnderscores() {
        assertEquals("My_Great_Note", service.sanitizeFilename("My Great Note"));
    }

    @Test
    void sanitizeFilenameShouldStripIllegalCharacters() {
        assertEquals("Hello_World", service.sanitizeFilename("Hello / World?!*"));
    }

    @Test
    void sanitizeFilenameShouldPreserveAllowedCharacters() {
        assertEquals("note-1_v2", service.sanitizeFilename("note-1_v2"));
    }

    @Test
    void sanitizeFilenameShouldReturnFallbackForNullOrBlank() {
        assertEquals("note", service.sanitizeFilename(null));
        assertEquals("note", service.sanitizeFilename(""));
        assertEquals("note", service.sanitizeFilename("   "));
    }

    @Test
    void sanitizeFilenameShouldReturnFallbackWhenOnlyIllegalCharacters() {
        assertEquals("note", service.sanitizeFilename("/??!!***"));
    }

    @Test
    void sanitizeFilenameShouldTruncateLongTitles() {
        String longTitle = "a".repeat(250);
        String result = service.sanitizeFilename(longTitle);
        assertEquals(100, result.length(),
                "Sanitized filename should be truncated at 100 characters");
    }

    @Test
    void sanitizeFilenameShouldCollapseMultipleSpaces() {
        assertEquals("hello_world", service.sanitizeFilename("hello     world"));
    }
}
