package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Business logic for notes. All read/write operations are scoped to the
 * authenticated user:
 *
 *   - List queries filter by {@code user_id}, so user A never sees user B's notes.
 *   - Mutating operations ({@code update}, {@code moveToTrash}, {@code restoreFromTrash},
 *     {@code deletePermanently}) look up the note by ID without user filter and then
 *     verify ownership, so that:
 *       * a non-existent ID returns 404 {@link NoteNotFoundException}
 *       * a note owned by a different user returns 403 {@link AccessDeniedException}
 *
 * The 404-vs-403 distinction matches the spec's requirement: "verify
 * note.userId == authenticatedUserId, return 403 if not".
 */
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
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user no longer exists: " + userId));
    }

    /**
     * Finds a note by ID and verifies it belongs to the caller.
     * Throws NoteNotFoundException (→404) if no such note exists,
     * AccessDeniedException (→403) if it exists but belongs to another user.
     */
    private Note findOwnedByUser(Long noteId, Long userId) {
        Note note = repository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        AppUser owner = note.getUser();
        if (owner == null || !owner.getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Note " + noteId + " does not belong to user " + userId);
        }
        return note;
    }

    public Note save(Note note, Long userId) {
        AppUser user = getUser(userId);
        if (note.getColor() == null) {
            note.setColor("");
        }
        // Even if the client sent a user in the body, overwrite it with the
        // authenticated principal — clients must never be able to create
        // notes on behalf of other users.
        note.setUser(user);
        return repository.save(note);
    }

    public Note get(Long id, Long userId) {
        return findOwnedByUser(id, userId);
    }

    public Iterable<Note> getAll(Long userId) {
        return repository.findAllByUserAndInTrashFalse(getUser(userId));
    }

    public Iterable<Note> getAllTrashed(Long userId) {
        return repository.findAllByUserAndInTrashTrue(getUser(userId));
    }

    public Note update(Long id, Note updatedNote, Long userId) {
        Note existingNote = findOwnedByUser(id, userId);

        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        existingNote.setTags(updatedNote.getTags());
        existingNote.setColor(updatedNote.getColor() == null ? "" : updatedNote.getColor());
        existingNote.setPinned(updatedNote.isPinned());
        existingNote.setInTrash(updatedNote.isInTrash());
        return repository.save(existingNote);
    }

    public Note moveToTrash(Long id, Long userId) {
        Note note = findOwnedByUser(id, userId);
        note.setInTrash(true);
        note.setPinned(false); // A note can't be pinned and in trash simultaneously.
        return repository.save(note);
    }

    public Note restoreFromTrash(Long id, Long userId) {
        Note note = findOwnedByUser(id, userId);
        note.setInTrash(false);
        return repository.save(note);
    }

    public void deletePermanently(Long id, Long userId) {
        Note note = findOwnedByUser(id, userId);
        repository.delete(note);
    }
}
