package de.htw.webtech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload accepted by POST /api/notes. Intentionally omits server-managed
 * fields (id, createdAt, lastModified, user) so clients can't smuggle them in.
 */
public class NoteCreateRequest {

    @NotBlank(message = "Title is required and cannot be blank")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @Size(max = 50_000, message = "Content must be at most 50000 characters")
    private String content;

    @Size(max = 500, message = "Tags must be at most 500 characters")
    private String tags;

    @Pattern(
        regexp = "^(#[0-9a-fA-F]{6})?$",
        message = "Color must be a 6-digit hex string starting with # (e.g. #33aaff) or empty"
    )
    private String color;

    private boolean pinned;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}
