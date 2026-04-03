package de.htw.webtech.repository;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoteRepository extends CrudRepository<Note, Long> {
    // User-scoped queries (used by NoteService)
    Iterable<Note> findAllByUserAndInTrashFalse(AppUser user);
    Iterable<Note> findAllByUserAndInTrashTrue(AppUser user);
    Optional<Note> findByIdAndUser(Long id, AppUser user);
}