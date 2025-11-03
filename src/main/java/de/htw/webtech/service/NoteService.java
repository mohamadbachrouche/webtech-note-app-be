package de.htw.webtech.service;

import de.htw.webtech.domain.Note;
import de.htw.webtech.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NoteService {

    @Autowired
    private NoteRepository repository;

    public Iterable<Note> getAll() {
        return repository.findAll();
    }
    // Add other methods for M4 later (save, update, delete)
}