package com.example.todolist.backend.controller;

import com.example.todolist.backend.dto.TaskDto;
import com.example.todolist.backend.mapper.TaskMapper;
import com.example.todolist.backend.service.TaskService;
import com.example.todolist.core.domain.Task;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskDto> list() {
        return service.list().stream().map(TaskMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> get(@PathVariable("id") UUID id) {
        return service.get(id)
                .map(TaskMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskDto dto) {
        Task created = service.create(TaskMapper.toDomain(dto));
        return ResponseEntity.created(URI.create("/api/tasks/" + created.getId()))
                .body(TaskMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> update(@PathVariable("id") UUID id, @Valid @RequestBody TaskDto dto) {
        Task updated = service.update(id, TaskMapper.toDomain(dto));
        return ResponseEntity.ok(TaskMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}