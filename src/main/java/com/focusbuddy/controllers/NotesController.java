package com.focusbuddy.controllers;

import com.focusbuddy.models.Note;
import com.focusbuddy.models.notes.*;
import com.focusbuddy.services.NotesService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.ErrorHandler;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.web.HTMLEditor;
import javafx.util.Duration;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class NotesController {

    @FXML private VBox notesContainer;
    @FXML private ListView<Note> notesList;
    @FXML private TextField noteTitleField;
    @FXML private HTMLEditor noteEditor;
    @FXML private Button saveNoteButton;
    @FXML private Button newNoteButton;
    @FXML private Button deleteNoteButton;
    @FXML private TextField searchField;
    @FXML private TextField tagsField;
    @FXML private Label wordCountLabel;
    @FXML private Label lastSavedLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryCombo;

    // Export and import buttons
    @FXML private Button exportButton;
    @FXML private Button importButton;

    private NotesService notesService;
    private Note currentNote;
    private Timeline autoSaveTimeline;
    private Timeline wordCountTimeline;
    private boolean hasUnsavedChanges = false;

    @FXML
    private void initialize() {
        try {
            notesService = new NotesService();

            setupNotesList();
            setupButtons();
            setupSearch();
            setupComboBoxes();
            setupAutoSave();
            setupKeyboardShortcuts();
            setupWordCount();

            // ✅ UBAH: Load data real
            loadNotes();

            addEntranceAnimation();

            System.out.println("✅ Notes controller initialized successfully");
        } catch (Exception e) {
            ErrorHandler.handleError("Notes Initialization",
                    "Failed to initialize notes view", e);
            showEmptyState(); // ✅ TAMBAH
        }
    }

    private void setupComboBoxes() {
        try {
            // Setup sort combo
            if (sortCombo != null) {
                sortCombo.getItems().addAll(
                        "Recent",
                        "Alphabetical",
                        "Created Date",
                        "Modified Date"
                );
                sortCombo.setValue("Recent");
                sortCombo.setOnAction(e -> sortNotes());
            }

            // Setup category combo
            if (categoryCombo != null) {
                categoryCombo.getItems().addAll(
                        "General",
                        "Work",
                        "Personal",
                        "Ideas",
                        "Research",
                        "Meeting Notes",
                        "Todo",
                        "Archive"
                );
                categoryCombo.setValue("General");
                categoryCombo.setOnAction(e -> markAsChanged());
            }

            System.out.println("✅ ComboBoxes setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("ComboBox Setup", "Failed to setup combo boxes", e);
        }
    }

    private void sortNotes() {
        try {
            if (sortCombo != null && notesList != null) {
                String sortBy = sortCombo.getValue();
                List<Note> notes = new ArrayList<>(notesList.getItems());

                switch (sortBy) {
                    case "Recent":
                        notes.sort((a, b) -> {
                            LocalDateTime aTime = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
                            LocalDateTime bTime = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                            return bTime != null && aTime != null ? bTime.compareTo(aTime) : 0;
                        });
                        break;
                    case "Alphabetical":
                        notes.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                        break;
                    case "Created Date":
                        notes.sort((a, b) -> {
                            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                                return b.getCreatedAt().compareTo(a.getCreatedAt());
                            }
                            return 0;
                        });
                        break;
                    case "Modified Date":
                        notes.sort((a, b) -> {
                            LocalDateTime aTime = a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt();
                            LocalDateTime bTime = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                            return bTime != null && aTime != null ? bTime.compareTo(aTime) : 0;
                        });
                        break;
                }

                notesList.getItems().setAll(notes);

                // Show feedback
                NotificationManager.getInstance().showNotification(
                        "Notes Sorted",
                        "Notes sorted by " + sortBy,
                        NotificationManager.NotificationType.INFO
                );
            }
        } catch (Exception e) {
            System.err.println("Error sorting notes: " + e.getMessage());
        }
    }

    private void setupNotesList() {
        try {
            notesList.setCellFactory(listView -> new ListCell<Note>() {
                @Override
                protected void updateItem(Note note, boolean empty) {
                    super.updateItem(note, empty);
                    if (empty || note == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setGraphic(createNoteListItem(note));
                        setStyle(getNoteItemStyle(note));

                        // Add hover effect
                        setOnMouseEntered(e -> {
                            if (!isEmpty()) {
                                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                                scale.setToX(1.02);
                                scale.setToY(1.02);
                                scale.play();
                            }
                        });

                        setOnMouseExited(e -> {
                            if (!isEmpty()) {
                                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                                scale.setToX(1.0);
                                scale.setToY(1.0);
                                scale.play();
                            }
                        });
                    }
                }
            });

            notesList.getSelectionModel().selectedItemProperty().addListener((obs, oldNote, newNote) -> {
                if (newNote != null) {
                    // Check for unsaved changes before switching
                    if (hasUnsavedChanges) {
                        showUnsavedChangesDialog(() -> loadNoteContent(newNote));
                    } else {
                        loadNoteContent(newNote);
                    }
                }
            });

            System.out.println("✅ Notes list setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Notes List Setup", "Failed to setup notes list", e);
        }
    }

    private VBox createNoteListItem(Note note) {
        try {
            VBox content = new VBox(6);
            content.setMaxWidth(Double.MAX_VALUE);

            // Title and date row
            HBox titleRow = new HBox(10);
            titleRow.setMaxWidth(Double.MAX_VALUE);

            Label titleLabel = new Label(note.getTitle());
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            titleLabel.setMaxWidth(Double.MAX_VALUE);

            // Format date nicely
            String dateStr = "";
            if (note.getUpdatedAt() != null) {
                dateStr = formatNoteDate(note.getUpdatedAt());
            } else if (note.getCreatedAt() != null) {
                dateStr = formatNoteDate(note.getCreatedAt());
            }

            Label dateLabel = new Label(dateStr);
            dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");

            titleRow.getChildren().addAll(titleLabel, dateLabel);

            // Content preview
            String preview = extractTextFromHtml(note.getContent());
            if (preview.length() > 80) {
                preview = preview.substring(0, 80) + "...";
            }

            Label previewLabel = new Label(preview);
            previewLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
            previewLabel.setWrapText(true);
            previewLabel.setMaxWidth(Double.MAX_VALUE);

            // Tags row
            HBox tagsRow = new HBox(5);
            if (note.getTags() != null && !note.getTags().trim().isEmpty()) {
                String[] tags = note.getTags().split(",");
                for (String tag : tags) {
                    if (tag.trim().length() > 0) {
                        Label tagLabel = new Label("#" + tag.trim());
                        tagLabel.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #3730a3; " +
                                "-fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-radius: 8;");
                        tagsRow.getChildren().add(tagLabel);

                        if (tagsRow.getChildren().size() >= 3) {
                            Label moreLabel = new Label("+" + (tags.length - 3) + " more");
                            moreLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10px;");
                            tagsRow.getChildren().add(moreLabel);
                            break;
                        }
                    }
                }
            }

            content.getChildren().addAll(titleRow, previewLabel);
            if (!tagsRow.getChildren().isEmpty()) {
                content.getChildren().add(tagsRow);
            }

            return content;
        } catch (Exception e) {
            System.err.println("Error creating note item: " + e.getMessage());
            // Fallback
            VBox fallback = new VBox();
            fallback.getChildren().add(new Label(note.getTitle()));
            return fallback;
        }
    }

    private String formatNoteDate(LocalDateTime dateTime) {
        try {
            LocalDateTime now = LocalDateTime.now();

            if (dateTime.toLocalDate().equals(now.toLocalDate())) {
                return "Today " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else if (dateTime.toLocalDate().equals(now.minusDays(1).toLocalDate())) {
                return "Yesterday " + dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            } else if (dateTime.isAfter(now.minusWeeks(1))) {
                return dateTime.format(DateTimeFormatter.ofPattern("EEE HH:mm"));
            } else {
                return dateTime.format(DateTimeFormatter.ofPattern("MMM dd"));
            }
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    private String extractTextFromHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }

    private String getNoteItemStyle(Note note) {
        return "-fx-background-color: #f8fafc; -fx-background-radius: 12px; " +
                "-fx-border-color: #e5e7eb; -fx-border-radius: 12px; -fx-border-width: 1; " +
                "-fx-padding: 12;";
    }

    private void setupButtons() {
        try {
            if (newNoteButton != null) {
                newNoteButton.setOnAction(e -> createNewNote());
                addButtonHoverEffect(newNoteButton);
            }

            if (saveNoteButton != null) {
                saveNoteButton.setOnAction(e -> saveCurrentNote());
                addButtonHoverEffect(saveNoteButton);
            }

            if (deleteNoteButton != null) {
                deleteNoteButton.setOnAction(e -> deleteCurrentNoteWithConfirmation());
                addButtonHoverEffect(deleteNoteButton);
            }

            if (exportButton != null) {
                exportButton.setOnAction(e -> exportCurrentNote());
                addButtonHoverEffect(exportButton);
            }

            if (importButton != null) {
                importButton.setOnAction(e -> importNote());
                addButtonHoverEffect(importButton);
            }

            System.out.println("✅ Buttons setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Buttons Setup", "Failed to setup buttons", e);
        }
    }

    private void addButtonHoverEffect(Button button) {
        try {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            button.setOnMouseEntered(e -> scaleUp.play());
            button.setOnMouseExited(e -> scaleDown.play());
        } catch (Exception e) {
            System.err.println("Warning: Could not add hover effect to button: " + e.getMessage());
        }
    }

    private void setupSearch() {
        try {
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldText, newText) -> {
                    searchNotes(newText);
                });

                // Add search functionality
                searchField.setPromptText("🔍 Search notes by title, content, or tags...");
            }

            System.out.println("✅ Search setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Search Setup", "Failed to setup search", e);
        }
    }

    private void setupAutoSave() {
        try {
            // Auto-save when content changes (with debouncing)
            if (noteTitleField != null) {
                noteTitleField.textProperty().addListener((obs, oldText, newText) -> {
                    markAsChanged();
                    scheduleAutoSave();
                });
            }

            if (noteEditor != null) {
                // Note: HTMLEditor doesn't have a simple text property listener
                // We'll use a timer-based approach to detect changes
                setupContentChangeDetection();
            }

            if (tagsField != null) {
                tagsField.textProperty().addListener((obs, oldText, newText) -> {
                    markAsChanged();
                    scheduleAutoSave();
                });
            }

            System.out.println("✅ Auto-save setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup auto-save: " + e.getMessage());
        }
    }

    private void setupContentChangeDetection() {
        // Check for content changes every 2 seconds
        Timeline contentChecker = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (noteEditor != null && currentNote != null) {
                String currentContent = noteEditor.getHtmlText();
                String savedContent = currentNote.getContent() != null ? currentNote.getContent() : "";

                if (!currentContent.equals(savedContent)) {
                    markAsChanged();
                    updateWordCount();
                }
            }
        }));
        contentChecker.setCycleCount(Timeline.INDEFINITE);
        contentChecker.play();
    }

    private void markAsChanged() {
        hasUnsavedChanges = true;
        updateSaveButtonState();
    }

    private void markAsSaved() {
        hasUnsavedChanges = false;
        updateSaveButtonState();
        updateLastSavedLabel();
    }

    private void updateSaveButtonState() {
        if (saveNoteButton != null) {
            saveNoteButton.setText(hasUnsavedChanges ? "💾 Save" : "✅ Saved");
            saveNoteButton.setDisable(!hasUnsavedChanges);
        }
    }

    private void updateLastSavedLabel() {
        if (lastSavedLabel != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            lastSavedLabel.setText("Last saved: " + timestamp);

            // Add fade animation
            FadeTransition fade = new FadeTransition(Duration.millis(300), lastSavedLabel);
            fade.setFromValue(0.5);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    private void scheduleAutoSave() {
        try {
            if (autoSaveTimeline != null) {
                autoSaveTimeline.stop();
            }

            autoSaveTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                if (currentNote != null && hasUnsavedChanges &&
                        noteTitleField != null && !noteTitleField.getText().trim().isEmpty()) {
                    saveCurrentNoteSilently();
                }
            }));
            autoSaveTimeline.play();
        } catch (Exception e) {
            System.err.println("Error scheduling auto-save: " + e.getMessage());
        }
    }

    private void setupWordCount() {
        try {
            if (wordCountTimeline != null) {
                wordCountTimeline.stop();
            }

            wordCountTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateWordCount()));
            wordCountTimeline.setCycleCount(Timeline.INDEFINITE);
            wordCountTimeline.play();

            System.out.println("✅ Word count setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup word count: " + e.getMessage());
        }
    }

    private void updateWordCount() {
        try {
            if (wordCountLabel != null && noteEditor != null) {
                String text = extractTextFromHtml(noteEditor.getHtmlText());
                String[] words = text.trim().split("\\s+");
                int wordCount = text.trim().isEmpty() ? 0 : words.length;
                int charCount = text.length();

                wordCountLabel.setText(String.format("Words: %d | Characters: %d", wordCount, charCount));
            }
        } catch (Exception e) {
            System.err.println("Error updating word count: " + e.getMessage());
        }
    }

    private void setupKeyboardShortcuts() {
        try {
            if (notesContainer != null) {
                notesContainer.setOnKeyPressed(e -> {
                    if (e.isControlDown()) {
                        switch (e.getCode()) {
                            case N:
                                createNewNote();
                                e.consume();
                                break;
                            case S:
                                saveCurrentNote();
                                e.consume();
                                break;
                            case F:
                                if (searchField != null) {
                                    searchField.requestFocus();
                                }
                                e.consume();
                                break;
                        }
                    } else if (e.getCode() == KeyCode.DELETE && currentNote != null) {
                        deleteCurrentNoteWithConfirmation();
                        e.consume();
                    }
                });
            }

            System.out.println("✅ Keyboard shortcuts setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup keyboard shortcuts: " + e.getMessage());
        }
    }

    private void addEntranceAnimation() {
        try {
            if (notesContainer != null) {
                notesContainer.setOpacity(0);
                notesContainer.setTranslateY(20);

                FadeTransition fade = new FadeTransition(Duration.millis(600), notesContainer);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(600), notesContainer);
                slide.setFromY(20);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition entrance = new ParallelTransition(fade, slide);
                entrance.play();
            }
        } catch (Exception e) {
            // Fallback: just show the container
            if (notesContainer != null) {
                notesContainer.setOpacity(1);
                notesContainer.setTranslateY(0);
            }
        }
    }

    // ✅ UBAH NAMA dari clearNotesState() ke showEmptyState()
    private void showEmptyState() {
        if (notesList != null) {
            notesList.getItems().clear();
            Label emptyLabel = new Label("📝 Create your first note to get started!");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            notesList.setPlaceholder(emptyLabel);
        }

        if (noteEditor != null) {
            noteEditor.setHtmlText("");
        }

        if (noteTitleField != null) {
            noteTitleField.clear();
        }
        if (tagsField != null) {
            tagsField.clear();
        }

        if (wordCountLabel != null) {
            wordCountLabel.setText("Words: 0 | Characters: 0");
        }

        if (lastSavedLabel != null) {
            lastSavedLabel.setText("Ready to create your first note");
        }
    }

    private void loadNotes() {
        if (notesService == null) {
            System.err.println("Notes service not available");
            showEmptyState(); // ✅ UBAH
            return;
        }

        try {
            CompletableFuture.supplyAsync(() -> {
                try {
                    int userId = UserSession.getInstance().getCurrentUser().getId();
                    return notesService.getNotesForUser(userId);
                } catch (Exception e) {
                    System.err.println("Error loading notes: " + e.getMessage());
                    return List.<Note>of();
                }
            }).thenAccept(notes -> {
                Platform.runLater(() -> {
                    try {
                        if (notesList != null) {
                            if (!notes.isEmpty()) {
                                notesList.getItems().setAll(notes);
                                System.out.println("✅ Notes loaded successfully: " + notes.size() + " notes");
                            } else {
                                // ✅ TAMBAH: Show empty state untuk user baru
                                showEmptyState();
                            }
                        }
                    } catch (Exception e) {
                        ErrorHandler.handleError("Notes Loading", "Failed to display loaded notes", e);
                        showEmptyState(); // ✅ TAMBAH
                    }
                });
            });
        } catch (Exception e) {
            ErrorHandler.handleError("Notes Loading", "Failed to load notes", e);
            showEmptyState(); // ✅ TAMBAH
        }
    }

    private void loadNoteContent(Note note) {
        try {
            currentNote = note;
            hasUnsavedChanges = false;

            if (noteTitleField != null) noteTitleField.setText(note.getTitle());
            if (noteEditor != null) noteEditor.setHtmlText(note.getContent() != null ? note.getContent() : "");
            if (tagsField != null) tagsField.setText(note.getTags() != null ? note.getTags() : "");

            // Set category if available
            if (categoryCombo != null && note.getCategory() != null) {
                categoryCombo.setValue(note.getCategory());
            }

            updateWordCount();
            updateSaveButtonState();
            updateLastSavedLabel();

            // Add form animation
            addFormChangeAnimation();

        } catch (Exception e) {
            ErrorHandler.handleError("Note Loading", "Failed to load note content", e);
        }
    }

    private void addFormChangeAnimation() {
        try {
            if (noteTitleField != null) {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), noteTitleField.getParent());
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.01);
                pulse.setToY(1.01);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        } catch (Exception e) {
            // Ignore animation errors
        }
    }

    private void createNewNote() {
        try {
            // Check for unsaved changes
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog(this::doCreateNewNote);
                return;
            }

            doCreateNewNote();
        } catch (Exception e) {
            ErrorHandler.handleError("New Note", "Failed to create new note", e);
        }
    }

    private void doCreateNewNote() {
        try {
            currentNote = new Note();
            currentNote.setUserId(UserSession.getInstance().getCurrentUser().getId());
            currentNote.setTitle("New Note");
            currentNote.setContent("");

            if (noteTitleField != null) noteTitleField.setText("New Note");
            if (noteEditor != null) noteEditor.setHtmlText("");
            if (tagsField != null) tagsField.setText("");
            if (categoryCombo != null) categoryCombo.setValue("General");

            if (notesList != null) notesList.getSelectionModel().clearSelection();

            hasUnsavedChanges = false;
            updateSaveButtonState();
            updateWordCount();

            // Focus on title field and select all text
            if (noteTitleField != null) {
                Platform.runLater(() -> {
                    noteTitleField.requestFocus();
                    noteTitleField.selectAll();
                });
            }

            // Add creation animation
            addFormChangeAnimation();

        } catch (Exception e) {
            ErrorHandler.handleError("New Note Creation", "Failed to setup new note", e);
        }
    }

    private void saveCurrentNote() {
        try {
            if (currentNote == null) {
                doCreateNewNote();
            }

            String title = noteTitleField != null ? noteTitleField.getText().trim() : "";
            String content = noteEditor != null ? noteEditor.getHtmlText() : "";
            String tags = tagsField != null ? tagsField.getText().trim() : "";
            String category = categoryCombo != null ? categoryCombo.getValue() : "General";

            if (title.isEmpty()) {
                showValidationError("Note title cannot be empty");
                if (noteTitleField != null) {
                    noteTitleField.requestFocus();
                    addFieldErrorAnimation(noteTitleField);
                }
                return;
            }

            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setTags(tags);
            currentNote.setCategory(category);

            // Save in background
            CompletableFuture.supplyAsync(() -> {
                if (currentNote.getId() == 0) {
                    return notesService.createNote(currentNote);
                } else {
                    return notesService.updateNote(currentNote);
                }
            }).thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    String action = currentNote.getId() == 0 ? "created" : "updated";
                    NotificationManager.getInstance().showNotification(
                            "Note Saved",
                            "Your note has been " + action + " successfully! 📝",
                            NotificationManager.NotificationType.SUCCESS
                    );

                    markAsSaved();
                    loadNotes(); // Refresh the list

                    // Add success animation
                    addSuccessAnimation();
                } else {
                    NotificationManager.getInstance().showNotification(
                            "Error",
                            "Failed to save note. Please try again.",
                            NotificationManager.NotificationType.ERROR
                    );
                }
            }));

        } catch (Exception e) {
            ErrorHandler.handleError("Note Save", "Failed to save note", e);
        }
    }

    private void saveCurrentNoteSilently() {
        try {
            if (currentNote != null && noteTitleField != null && !noteTitleField.getText().trim().isEmpty()) {
                String title = noteTitleField.getText().trim();
                String content = noteEditor != null ? noteEditor.getHtmlText() : "";
                String tags = tagsField != null ? tagsField.getText().trim() : "";
                String category = categoryCombo != null ? categoryCombo.getValue() : "General";

                currentNote.setTitle(title);
                currentNote.setContent(content);
                currentNote.setTags(tags);
                currentNote.setCategory(category);

                // Save silently in background
                CompletableFuture.runAsync(() -> {
                    if (currentNote.getId() == 0) {
                        notesService.createNote(currentNote);
                    } else {
                        notesService.updateNote(currentNote);
                    }
                }).thenRun(() -> Platform.runLater(this::markAsSaved));
            }
        } catch (Exception e) {
            System.err.println("Silent save failed: " + e.getMessage());
        }
    }

    private void deleteCurrentNoteWithConfirmation() {
        if (currentNote == null || currentNote.getId() == 0) {
            showValidationError("No note selected for deletion");
            return;
        }

        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Note");
            alert.setHeaderText("Are you sure you want to delete this note?");
            alert.setContentText("Note: \"" + currentNote.getTitle() + "\"\n\nThis action cannot be undone.");

            // Style the alert
            alert.getDialogPane().getStylesheets().addAll(
                    notesContainer.getScene().getStylesheets());

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    deleteCurrentNote();
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Delete Confirmation", "Failed to show delete confirmation", e);
        }
    }

    private void deleteCurrentNote() {
        try {
            Note noteToDelete = currentNote;

            // Delete in background
            CompletableFuture.supplyAsync(() -> notesService.deleteNote(noteToDelete.getId()))
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            NotificationManager.getInstance().showNotification(
                                    "Note Deleted",
                                    "Note has been deleted successfully",
                                    NotificationManager.NotificationType.SUCCESS
                            );

                            loadNotes(); // Refresh the list
                            doCreateNewNote(); // Create a new note

                            // Add deletion animation
                            addDeletionAnimation();
                        } else {
                            NotificationManager.getInstance().showNotification(
                                    "Error",
                                    "Failed to delete note. Please try again.",
                                    NotificationManager.NotificationType.ERROR
                            );
                        }
                    }));

        } catch (Exception e) {
            ErrorHandler.handleError("Note Deletion", "Failed to delete note", e);
        }
    }

    private void searchNotes(String searchText) {
        try {
            if (searchText == null || searchText.trim().isEmpty()) {
                loadNotes();
                return;
            }

            // Search in background
            CompletableFuture.supplyAsync(() -> {
                try {
                    int userId = UserSession.getInstance().getCurrentUser().getId();
                    return notesService.searchNotes(userId, searchText);
                } catch (Exception e) {
                    System.err.println("Error searching notes: " + e.getMessage());
                    return List.<Note>of();
                }
            }).thenAccept(searchResults -> {
                Platform.runLater(() -> {
                    if (notesList != null) {
                        notesList.getItems().setAll(searchResults);
                    }
                });
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Note Search", "Failed to search notes", e);
        }
    }

    private void exportCurrentNote() {
        try {
            if (currentNote == null || currentNote.getId() == 0) {
                showValidationError("No note to export. Please save the note first.");
                return;
            }

            // This would implement note export functionality
            NotificationManager.getInstance().showNotification(
                    "Export",
                    "Export functionality coming soon! 📄",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Note Export", "Failed to export note", e);
        }
    }

    private void importNote() {
        try {
            // This would implement note import functionality
            NotificationManager.getInstance().showNotification(
                    "Import",
                    "Import functionality coming soon! 📥",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Note Import", "Failed to import note", e);
        }
    }

    private void showUnsavedChangesDialog(Runnable onConfirm) {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes!");
            alert.setContentText("Do you want to save your changes before continuing?");

            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            alert.showAndWait().ifPresent(result -> {
                if (result == saveButton) {
                    saveCurrentNote();
                    onConfirm.run();
                } else if (result == discardButton) {
                    hasUnsavedChanges = false;
                    onConfirm.run();
                }
                // If cancel, do nothing
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Unsaved Changes Dialog", "Failed to show unsaved changes dialog", e);
        }
    }

    private void showValidationError(String message) {
        try {
            NotificationManager.getInstance().showNotification(
                    "Validation Error",
                    message,
                    NotificationManager.NotificationType.WARNING
            );
        } catch (Exception e) {
            System.err.println("Error showing validation error: " + e.getMessage());
        }
    }

    private void addFieldErrorAnimation(Control field) {
        try {
            // Red border flash
            String originalStyle = field.getStyle();
            field.setStyle(originalStyle + " -fx-border-color: #ef4444; -fx-border-width: 2;");

            Timeline resetStyle = new Timeline(new KeyFrame(Duration.seconds(2),
                    e -> field.setStyle(originalStyle)));
            resetStyle.play();

            // Shake animation
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), field);
            shake.setFromX(0);
            shake.setByX(5);
            shake.setCycleCount(6);
            shake.setAutoReverse(true);
            shake.setOnFinished(e -> field.setTranslateX(0));
            shake.play();
        } catch (Exception e) {
            System.err.println("Error adding field error animation: " + e.getMessage());
        }
    }

    private void addSuccessAnimation() {
        try {
            if (saveNoteButton != null) {
                // Success pulse
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), saveNoteButton);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.1);
                pulse.setToY(1.1);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        } catch (Exception e) {
            System.err.println("Error adding success animation: " + e.getMessage());
        }
    }

    private void addDeletionAnimation() {
        try {
            if (deleteNoteButton != null) {
                // Deletion flash
                FadeTransition flash = new FadeTransition(Duration.millis(100), deleteNoteButton);
                flash.setFromValue(1.0);
                flash.setToValue(0.3);
                flash.setCycleCount(4);
                flash.setAutoReverse(true);
                flash.play();
            }
        } catch (Exception e) {
            System.err.println("Error adding deletion animation: " + e.getMessage());
        }
    }
}