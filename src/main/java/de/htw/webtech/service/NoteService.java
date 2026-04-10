package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.dto.NoteCreateRequest;
import de.htw.webtech.dto.NoteUpdateRequest;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final NoteRepository repository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public Note create(NoteCreateRequest request, Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Note note = new Note();
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setTags(request.getTags());
        note.setColor(request.getColor());
        note.setPinned(request.isPinned());
        note.setInTrash(false);
        note.setUser(user);
        Note saved = repository.save(note);
        log.info("note.create id={} userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Note get(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Iterable<Note> getAll(Long userId) {
        return repository.findAllByUserIdAndInTrashFalse(userId);
    }

    @Transactional(readOnly = true)
    public Iterable<Note> getAllTrashed(Long userId) {
        return repository.findAllByUserIdAndInTrashTrue(userId);
    }

    public Note update(Long id, NoteUpdateRequest request, Long userId) {
        Note existingNote = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoteNotFoundException(id));

        existingNote.setTitle(request.getTitle());
        existingNote.setContent(request.getContent());
        existingNote.setTags(request.getTags());
        existingNote.setColor(request.getColor());
        existingNote.setPinned(request.isPinned());
        existingNote.setInTrash(request.isInTrash());
        Note saved = repository.save(existingNote);
        log.info("note.update id={} userId={}", id, userId);
        return saved;
    }

    public Note moveToTrash(Long id, Long userId) {
        Note note = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoteNotFoundException(id));

        note.setInTrash(true);
        note.setPinned(false); // Can't be pinned and in trash
        Note saved = repository.save(note);
        log.info("note.moveToTrash id={} userId={}", id, userId);
        return saved;
    }

    public Note restoreFromTrash(Long id, Long userId) {
        Note note = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoteNotFoundException(id));

        note.setInTrash(false);
        Note saved = repository.save(note);
        log.info("note.restoreFromTrash id={} userId={}", id, userId);
        return saved;
    }

    public void deletePermanently(Long id, Long userId) {
        Note note = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoteNotFoundException(id));
        repository.delete(note);
        log.info("note.deletePermanently id={} userId={}", id, userId);
    }
}
