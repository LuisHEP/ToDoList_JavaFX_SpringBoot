package com.example.todolist.backend.service;

import com.example.todolist.core.domain.Task;
import com.example.todolist.core.ports.TaskRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> list() {
        return repository.findAll();
    }

    public Optional<Task> get(@NotNull UUID id) {
        return repository.findById(id);
    }

    public Task create(@Valid Task task) {
        task.setId(null);
        return repository.save(task);
    }

    public Task update(@NotNull UUID id, @Valid Task task) {
        task.setId(id);
        return repository.save(task);
    }

    public void delete(@NotNull UUID id) {
        repository.deleteById(id);
    }
}