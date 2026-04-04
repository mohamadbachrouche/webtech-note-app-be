package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class NoteService {

    private final NoteRepository repository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private AppUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    public Note save(Note note, Long userId) {
        AppUser user = getUser(userId);
        if (note.getColor() == null) {
            note.setColor("");
        }
        if (note.getTags() == null) {
            note.setTags("");
        }
        note.setUser(user);
        return repository.save(note);
    }

    public Note get(Long id, Long userId) {
        return repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public Iterable<Note> getAll(Long userId) {
        return repository.findAllByUserAndInTrashFalse(getUser(userId));
    }

    public Iterable<Note> getAllTrashed(Long userId) {
        return repository.findAllByUserAndInTrashTrue(getUser(userId));
    }

    public Note update(Long id, Note updatedNote, Long userId) {
        Note existingNote = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));

        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        existingNote.setTags(updatedNote.getTags() == null ? "" : updatedNote.getTags());
        existingNote.setColor(updatedNote.getColor() == null ? "" : updatedNote.getColor());
        existingNote.setPinned(updatedNote.isPinned());
        existingNote.setInTrash(updatedNote.isInTrash());
        return repository.save(existingNote);
    }

    public Note moveToTrash(Long id, Long userId) {
        Note note = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));

        note.setInTrash(true);
        note.setPinned(false); // Can't be pinned and in trash
        return repository.save(note);
    }

    public Note restoreFromTrash(Long id, Long userId) {
        Note note = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));

        note.setInTrash(false);
        return repository.save(note);
    }

    public List<String> getAllUniqueTags(Long userId) {
        Iterable<Note> notes = repository.findAllByUserAndInTrashFalse(getUser(userId));
        return StreamSupport.stream(notes.spliterator(), false)
                .map(Note::getTags)
                .filter(tags -> tags != null && !tags.isBlank())
                .flatMap(tags -> Arrays.stream(tags.split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public void deletePermanently(Long id, Long userId) {
        Note note = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));
        repository.delete(note);
    }
}
