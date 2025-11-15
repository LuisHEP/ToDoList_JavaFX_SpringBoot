package com.example.todolist.frontend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import javafx.beans.binding.Bindings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MainController {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ListView<ViewTask> tasksListView;
    @FXML private Button addButton;
    @FXML private TextField searchField;

    private final ObservableList<ViewTask> displayedTasks = FXCollections.observableArrayList();
    private final List<ViewTask> viewTasks = new ArrayList<>();
    private Timeline autoRefresh;

    @FXML
    public void initialize() {
        tasksListView.setItems(displayedTasks);
        addButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> titleField.getText() == null || titleField.getText().trim().isEmpty(),
                titleField.textProperty()
        ));
        tasksListView.setCellFactory(lv -> new ListCell<>() {
            private final Label titleLabel = new Label();
            private final Label descLabel = new Label();
            private final Label dateLabel = new Label();
            private final Button completeBtn = new Button();
            private final Button deleteBtn = new Button("Excluir");
            private final HBox actions = new HBox(8, completeBtn, deleteBtn);
            private final VBox texts = new VBox(2, titleLabel, descLabel, dateLabel);
            private final HBox container = new HBox(12, texts, actions);
            {
                container.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(texts, Priority.ALWAYS);
                completeBtn.setOnAction(e -> {
                    ViewTask item = getItem();
                    if (item != null) toggleCompletion(item.id, !item.completed);
                });
                deleteBtn.setOnAction(e -> {
                    ViewTask item = getItem();
                    if (item != null) deleteTask(item.id);
                });
                MenuItem editItem = new MenuItem("Editar");
                ContextMenu contextMenu = new ContextMenu(editItem);
                editItem.setOnAction(e -> {
                    ViewTask item = getItem();
                    if (item != null) editTask(item);
                });
                setContextMenu(contextMenu);
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        ViewTask item = getItem();
                        if (item != null) editTask(item);
                    }
                });
            }
            @Override
            protected void updateItem(ViewTask item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    titleLabel.setText(item.title);
                    descLabel.setText(item.description == null || item.description.isBlank() ? "" : item.description);
                    dateLabel.setText(item.dueDate != null ? "Entrega: " + item.dueDate.toLocalDate() : "");
                    completeBtn.setText(item.completed ? "Reabrir" : "Concluir");
                    setGraphic(container);
                }
            }
        });
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilter());
        }
        refreshTasks();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshTasks()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    public void onAdd() {
        try {
            var payload = objectMapper.createObjectNode()
                    .put("title", titleField.getText())
                    .put("description", descriptionField.getText())
                    .put("completed", false)
                    .put("dueDate", dueDatePicker.getValue() == null ? null : dueDatePicker.getValue().atStartOfDay().toString());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tasks"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                titleField.clear();
                descriptionField.clear();
                dueDatePicker.setValue(null);
                if (searchField != null) searchField.clear();
                titleField.requestFocus();
                refreshTasks();
            } else {
                showError("Falha ao adicionar tarefa: " + resp.body());
            }
        } catch (IOException | InterruptedException e) {
            showError("Erro: " + e.getMessage());
        }
    }

    @FXML
    public void onRefresh() {
        refreshTasks();
    }

    private void refreshTasks() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tasks"))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                var nodes = objectMapper.readTree(resp.body());
                viewTasks.clear();
                LocalDateTime now = LocalDateTime.now();
                for (var node : nodes) {
                    UUID id = node.path("id").isMissingNode() || node.path("id").isNull() ? null : UUID.fromString(node.get("id").asText());
                    String title = node.path("title").asText("");
                    String description = node.path("description").asText("");
                    boolean completed = node.path("completed").asBoolean(false);
                    LocalDateTime dueDate = null;
                    var dueNode = node.get("dueDate");
                    if (dueNode != null && !dueNode.isNull()) {
                        String dueStr = dueNode.asText();
                        if (dueStr != null && !dueStr.isBlank()) {
                            try { dueDate = LocalDateTime.parse(dueStr); } catch (Exception ignored) {}
                        }
                    }
                    if (dueDate != null && dueDate.toLocalDate().isBefore(LocalDate.now())) {
                        continue;
                    }
                    viewTasks.add(new ViewTask(id, title, description, completed, dueDate));
                }
                viewTasks.sort(Comparator
                        .comparing((ViewTask task) -> task.dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(task -> task.title));
                applyFilter();
            } else {
                showError("Falha ao carregar tarefas: " + resp.body());
            }
        } catch (IOException | InterruptedException e) {
            showError("Erro: " + e.getMessage());
        }
    }

    private void applyFilter() {
        String query = searchField == null ? null : searchField.getText();
        displayedTasks.setAll(viewTasks.stream().filter(viewTask -> {
            if (query == null || query.isBlank()) return true;
            String needle = query.toLowerCase();
            return (viewTask.title != null && viewTask.title.toLowerCase().contains(needle)) ||
                    (viewTask.description != null && viewTask.description.toLowerCase().contains(needle));
        }).toList());
    }

    private void toggleCompletion(UUID id, boolean completed) {
        if (id == null) return;
        try {
            ViewTask vt = viewTasks.stream().filter(t -> id.equals(t.id)).findFirst().orElse(null);
            if (vt == null) return;
            var payload = objectMapper.createObjectNode()
                    .put("title", vt == null ? "" : vt.title)
                    .put("description", vt == null ? "" : vt.description)
                    .put("completed", completed)
                    .put("dueDate", vt == null || vt.dueDate == null ? null : vt.dueDate.toString());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tasks/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                refreshTasks();
                showSuccess("Tarefa feita!");
            } else {
                showError("Falha ao atualizar tarefa: " + resp.body());
            }
        } catch (IOException | InterruptedException e) {
            showError("Erro: " + e.getMessage());
        }
    }

    private void deleteTask(UUID id) {
        if (id == null) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tasks/" + id))
                    .DELETE()
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                refreshTasks();
            } else {
                showError("Falha ao excluir tarefa: " + resp.body());
            }
        } catch (IOException | InterruptedException e) {
            showError("Erro: " + e.getMessage());
        }
    }

    private void editTask(ViewTask task) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar tarefa");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField titleInput = new TextField(task.title);
        TextArea descInput = new TextArea(task.description);
        DatePicker dateInput = new DatePicker(task.dueDate == null ? null : task.dueDate.toLocalDate());
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(8);
        gp.add(new Label("Título"), 0, 0);
        gp.add(titleInput, 1, 0);
        gp.add(new Label("Descrição"), 0, 1);
        gp.add(descInput, 1, 1);
        gp.add(new Label("Data"), 0, 2);
        gp.add(dateInput, 1, 2);
        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        var res = dialog.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                var payload = objectMapper.createObjectNode()
                        .put("title", titleInput.getText())
                        .put("description", descInput.getText())
                        .put("completed", task.completed)
                        .put("dueDate", dateInput.getValue() == null ? null : dateInput.getValue().atStartOfDay().toString());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/tasks/" + task.id))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                        .build();
                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    refreshTasks();
                } else {
                    showError("Falha ao editar tarefa: " + resp.body());
                }
            } catch (IOException | InterruptedException e) {
                showError("Erro: " + e.getMessage());
            }
        }
    }

    private static class ViewTask {
        final UUID id;
        final String title;
        final String description;
        final boolean completed;
        final LocalDateTime dueDate;
        ViewTask(UUID id, String title, String description, boolean completed, LocalDateTime dueDate) {
            this.id = id; this.title = title; this.description = description; this.completed = completed; this.dueDate = dueDate;
        }
    }
    
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        Label text = new Label(msg);
        text.setStyle("-fx-text-fill: #e5e7eb;");
        HBox box = new HBox(10);
        box.getChildren().addAll(text);
        alert.getDialogPane().setContent(box);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        Label text = new Label(msg);
        text.setStyle("-fx-text-fill: #e5e7eb; -fx-font-size: 14px; -fx-font-weight: 600;");
        Label icon = new Label("✔");
        icon.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 20px; -fx-font-weight: 700;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, text, spacer, icon);
        box.setAlignment(Pos.CENTER_LEFT);
        alert.getDialogPane().setContent(box);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        alert.showAndWait();
    }
}