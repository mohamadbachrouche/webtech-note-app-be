package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.dto.NoteCreateRequest;
import de.htw.webtech.dto.NoteUpdateRequest;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

    public Note create(NoteCreateRequest request, Long userId) {
        AppUser user = getUser(userId);
        Note note = new Note();
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setTags(request.getTags());
        note.setColor(request.getColor() == null ? "" : request.getColor());
        note.setPinned(request.isPinned());
        note.setInTrash(false);
        note.setUser(user);
        return repository.save(note);
    }

    @Transactional(readOnly = true)
    public Note get(Long id, Long userId) {
        return repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Iterable<Note> getAll(Long userId) {
        return repository.findAllByUserAndInTrashFalse(getUser(userId));
    }

    @Transactional(readOnly = true)
    public Iterable<Note> getAllTrashed(Long userId) {
        return repository.findAllByUserAndInTrashTrue(getUser(userId));
    }

    public Note update(Long id, NoteUpdateRequest request, Long userId) {
        Note existingNote = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));

        existingNote.setTitle(request.getTitle());
        existingNote.setContent(request.getContent());
        existingNote.setTags(request.getTags());
        existingNote.setColor(request.getColor() == null ? "" : request.getColor());
        existingNote.setPinned(request.isPinned());
        existingNote.setInTrash(request.isInTrash());
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

    public void deletePermanently(Long id, Long userId) {
        Note note = repository.findByIdAndUser(id, getUser(userId))
                .orElseThrow(() -> new NoteNotFoundException(id));
        repository.delete(note);
    }
}
