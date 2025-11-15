package com.example.todolist.backend.repository;

import com.example.todolist.core.domain.Task;
import com.example.todolist.core.ports.TaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class JsonTaskRepository implements TaskRepository {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<UUID, Task> storage = new ConcurrentHashMap<>();

    private final Path storagePath;

    public JsonTaskRepository(@Value("${json.storage.path:./data/tasks.json}") String storagePathStr) {
        this.storagePath = Path.of(storagePathStr);
        objectMapper.findAndRegisterModules();
        initStorageFile();
        load();
    }

    private void initStorageFile() {
        try {
            Path dir = storagePath.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            if (!Files.exists(storagePath)) {
                Files.writeString(storagePath, "[]");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize storage file: " + storagePath, e);
        }
    }

    private void load() {
        try {
            List<Task> tasks = objectMapper.readValue(storagePath.toFile(), new TypeReference<List<Task>>() {});
            tasks.forEach(task -> storage.put(task.getId(), task));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load tasks from JSON storage", e);
        }
    }

    private synchronized void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), new ArrayList<>(storage.values()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist tasks to JSON storage", e);
        }
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(UUID.randomUUID());
            task.setCreatedAt(LocalDateTime.now());
        }
        storage.put(task.getId(), task);
        persist();
        return task;
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
        persist();
    }
}