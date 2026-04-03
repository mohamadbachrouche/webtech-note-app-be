package de.htw.webtech.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import de.htw.webtech.domain.Note;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(33, 33, 33));
    private static final Font TAG_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(100, 100, 100));
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
    private static final Font ITALIC_FONT = new Font(Font.HELVETICA, 12, Font.ITALIC, Color.BLACK);
    private static final Font BOLD_ITALIC_FONT = new Font(Font.HELVETICA, 12, Font.BOLDITALIC, Color.BLACK);
    private static final Font H1_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(33, 33, 33));
    private static final Font H2_FONT = new Font(Font.HELVETICA, 17, Font.BOLD, new Color(50, 50, 50));
    private static final Font H3_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(70, 70, 70));
    private static final Font LINK_FONT = new Font(Font.HELVETICA, 12, Font.UNDERLINE, new Color(0, 102, 204));
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 9, Font.ITALIC, new Color(130, 130, 130));

    public byte[] generatePdf(Note note) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        addTitle(document, note);
        addTags(document, note);
        addSeparator(document);
        addContent(document, note);
        addFooter(document, note);

        document.close();
        return out.toByteArray();
    }

    private void addTitle(Document document, Note note) {
        Paragraph title = new Paragraph(note.getTitle(), TITLE_FONT);
        title.setSpacingAfter(5f);
        document.add(title);
    }

    private void addTags(Document document, Note note) {
        if (note.getTags() == null || note.getTags().isBlank()) {
            return;
        }
        String[] tags = note.getTags().split(",");
        StringBuilder tagLine = new StringBuilder();
        for (String tag : tags) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                if (!tagLine.isEmpty()) {
                    tagLine.append("  ");
                }
                tagLine.append("#").append(trimmed);
            }
        }
        if (!tagLine.isEmpty()) {
            Paragraph tagParagraph = new Paragraph(tagLine.toString(), TAG_FONT);
            tagParagraph.setSpacingAfter(8f);
            document.add(tagParagraph);
        }
    }

    private void addSeparator(Document document) {
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(new Color(200, 200, 200));
        separator.setLineWidth(0.5f);
        document.add(new Chunk(separator));
        document.add(Chunk.NEWLINE);
    }

    private void addContent(Document document, Note note) {
        String html = note.getContent();
        if (html == null || html.isBlank()) {
            return;
        }
        org.jsoup.nodes.Document parsed = Jsoup.parseBodyFragment(html);
        for (Node child : parsed.body().childNodes()) {
            processNode(document, child, BODY_FONT);
        }
    }

    private void processNode(Document document, Node node, Font currentFont) {
        if (node instanceof TextNode textNode) {
            String text = textNode.getWholeText();
            if (!text.isBlank()) {
                document.add(new Paragraph(text, currentFont));
            }
        } else if (node instanceof Element element) {
            processElement(document, element, currentFont);
        }
    }

    private void processElement(Document document, Element element, Font currentFont) {
        String tag = element.tagName().toLowerCase();

        switch (tag) {
            case "h1" -> addHeading(document, element, H1_FONT, 16f, 8f);
            case "h2" -> addHeading(document, element, H2_FONT, 12f, 6f);
            case "h3", "h4", "h5", "h6" -> addHeading(document, element, H3_FONT, 10f, 4f);
            case "p", "div" -> {
                Paragraph p = buildParagraph(element, currentFont);
                p.setSpacingAfter(6f);
                document.add(p);
            }
            case "br" -> document.add(Chunk.NEWLINE);
            case "ul" -> addList(document, element, false);
            case "ol" -> addList(document, element, true);
            case "blockquote" -> {
                Paragraph quote = buildParagraph(element, ITALIC_FONT);
                quote.setIndentationLeft(20f);
                quote.setSpacingBefore(6f);
                quote.setSpacingAfter(6f);
                document.add(quote);
            }
            case "a" -> {
                Paragraph linkP = new Paragraph();
                Anchor anchor = new Anchor(element.text(), LINK_FONT);
                anchor.setReference(element.attr("href"));
                linkP.add(anchor);
                document.add(linkP);
            }
            case "b", "strong" -> {
                Paragraph p = buildParagraph(element, BOLD_FONT);
                document.add(p);
            }
            case "i", "em" -> {
                Paragraph p = buildParagraph(element, ITALIC_FONT);
                document.add(p);
            }
            case "u" -> {
                Font underlineFont = new Font(currentFont);
                underlineFont.setStyle(currentFont.getStyle() | Font.UNDERLINE);
                Paragraph p = buildParagraph(element, underlineFont);
                document.add(p);
            }
            default -> {
                // For unknown elements, process children
                for (Node child : element.childNodes()) {
                    processNode(document, child, currentFont);
                }
            }
        }
    }

    private void addHeading(Document document, Element element, Font font, float spaceBefore, float spaceAfter) {
        Paragraph heading = new Paragraph(element.text(), font);
        heading.setSpacingBefore(spaceBefore);
        heading.setSpacingAfter(spaceAfter);
        document.add(heading);
    }

    private void addList(Document document, Element element, boolean ordered) {
        com.lowagie.text.List list = new com.lowagie.text.List(ordered);
        list.setIndentationLeft(15f);
        if (ordered) {
            list.setLettered(false);
        } else {
            list.setListSymbol("\u2022  ");
        }

        for (Element li : element.getElementsByTag("li")) {
            Paragraph liContent = buildParagraph(li, BODY_FONT);
            ListItem item = new ListItem(liContent);
            list.add(item);
        }

        document.add(list);
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(4f);
        document.add(spacer);
    }

    private Paragraph buildParagraph(Element element, Font defaultFont) {
        Paragraph paragraph = new Paragraph();
        buildInlineContent(paragraph, element, defaultFont, false, false, false);
        return paragraph;
    }

    private void buildInlineContent(Paragraph paragraph, Element element, Font defaultFont,
                                    boolean isBold, boolean isItalic, boolean isUnderline) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode textNode) {
                String text = textNode.getWholeText();
                if (!text.isEmpty()) {
                    Font font = resolveFont(isBold, isItalic, isUnderline);
                    paragraph.add(new Chunk(text, font));
                }
            } else if (child instanceof Element childEl) {
                String childTag = childEl.tagName().toLowerCase();
                boolean newBold = isBold || "b".equals(childTag) || "strong".equals(childTag);
                boolean newItalic = isItalic || "i".equals(childTag) || "em".equals(childTag);
                boolean newUnderline = isUnderline || "u".equals(childTag);

                if ("a".equals(childTag)) {
                    Anchor anchor = new Anchor(childEl.text(), LINK_FONT);
                    anchor.setReference(childEl.attr("href"));
                    paragraph.add(anchor);
                } else if ("br".equals(childTag)) {
                    paragraph.add(Chunk.NEWLINE);
                } else {
                    buildInlineContent(paragraph, childEl, defaultFont, newBold, newItalic, newUnderline);
                }
            }
        }
    }

    private Font resolveFont(boolean bold, boolean italic, boolean underline) {
        Font font;
        if (bold && italic) {
            font = new Font(BOLD_ITALIC_FONT);
        } else if (bold) {
            font = new Font(BOLD_FONT);
        } else if (italic) {
            font = new Font(ITALIC_FONT);
        } else {
            font = new Font(BODY_FONT);
        }
        if (underline) {
            font.setStyle(font.getStyle() | Font.UNDERLINE);
        }
        return font;
    }

    private void addFooter(Document document, Note note) {
        document.add(Chunk.NEWLINE);
        addSeparator(document);

        String created = note.getCreatedAt() != null ? note.getCreatedAt().format(DATE_FORMAT) : "N/A";
        String modified = note.getLastModified() != null ? note.getLastModified().format(DATE_FORMAT) : "N/A";

        Paragraph footer = new Paragraph(
                "Created: " + created + "  |  Last Modified: " + modified,
                FOOTER_FONT
        );
        footer.setSpacingBefore(4f);
        document.add(footer);
    }

    public String sanitizeFilename(String title) {
        if (title == null || title.isBlank()) {
            return "note";
        }
        String sanitized = title.replaceAll("[^a-zA-Z0-9\\-_ ]", "").trim();
        sanitized = sanitized.replaceAll("\\s+", "_");
        if (sanitized.isEmpty()) {
            return "note";
        }
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized;
    }
}
