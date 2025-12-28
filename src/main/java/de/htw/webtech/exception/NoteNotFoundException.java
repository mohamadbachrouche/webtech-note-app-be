package de.htw.webtech.exception;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(Long id) {
        super("Note with ID " + id + " not found");
    }

    public NoteNotFoundException(String message) {
        super(message);
    }
}
