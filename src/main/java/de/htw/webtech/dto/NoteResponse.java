package de.htw.webtech.dto;

import de.htw.webtech.domain.Note;

import java.time.LocalDateTime;

/**
 * Response shape for note endpoints. Built from a Note entity so the JPA
 * object never crosses the API boundary (prevents accidental lazy-loading,
 * leaked relations, or future field leaks).
 */
public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private boolean pinned;
    private boolean inTrash;
    private String tags;
    private String color;

    public NoteResponse(Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        this.createdAt = note.getCreatedAt();
        this.lastModified = note.getLastModified();
        this.pinned = note.isPinned();
        this.inTrash = note.isInTrash();
        this.tags = note.getTags();
        this.color = note.getColor();
    }

    public static NoteResponse from(Note note) {
        return new NoteResponse(note);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModified() { return lastModified; }
    public boolean isPinned() { return pinned; }
    public boolean isInTrash() { return inTrash; }
    public String getTags() { return tags; }
    public String getColor() { return color; }
}
