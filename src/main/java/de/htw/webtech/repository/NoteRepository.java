package de.htw.webtech.repository;

import de.htw.webtech.domain.Note;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteRepository extends CrudRepository<Note, Long> {
    // --- ADD THESE TWO LINES ---
    Iterable<Note> findAllByInTrashFalse();
    Iterable<Note> findAllByInTrashTrue();
}