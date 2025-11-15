package com.example.todolist.backend.mapper;

import com.example.todolist.backend.dto.TaskDto;
import com.example.todolist.core.domain.Task;

public final class TaskMapper {
    private TaskMapper() {}

    public static TaskDto toDto(Task task) {
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCompleted(task.isCompleted());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setDueDate(task.getDueDate());
        return dto;
    }

    public static Task toDomain(TaskDto dto) {
        return Task.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .completed(dto.isCompleted())
                .createdAt(dto.getCreatedAt())
                .dueDate(dto.getDueDate())
                .build();
    }
}