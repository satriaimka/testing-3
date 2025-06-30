package com.focusbuddy.services;

import com.focusbuddy.models.Task;
import com.focusbuddy.models.MoodEntry;
import com.focusbuddy.models.Note;
import com.focusbuddy.models.Goal;
import com.focusbuddy.utils.UserSession;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ExportService {
    
    private final TaskService taskService;
    private final MoodService moodService;
    private final NotesService notesService;
    private final GoalsService goalsService;
    
    public ExportService() {
        this.taskService = new TaskService();
        this.moodService = new MoodService();
        this.notesService = new NotesService();
        this.goalsService = new GoalsService();
    }
    
    public CompletableFuture<Boolean> exportAllDataToCSV(String directoryPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int userId = UserSession.getInstance().getCurrentUser().getId();
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                
                // Export tasks
                exportTasksToCSV(userId, directoryPath + "/tasks_" + timestamp + ".csv");
                
                // Export mood entries
                exportMoodEntriesToCSV(userId, directoryPath + "/mood_entries_" + timestamp + ".csv");
                
                // Export notes
                exportNotesToCSV(userId, directoryPath + "/notes_" + timestamp + ".csv");
                
                // Export goals
                exportGoalsToCSV(userId, directoryPath + "/goals_" + timestamp + ".csv");
                
                // Create summary report
                createSummaryReport(userId, directoryPath + "/summary_" + timestamp + ".txt");
                
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    private void exportTasksToCSV(int userId, String filePath) throws IOException {
        List<Task> tasks = taskService.getTasksForUser(userId);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // CSV Header
            writer.println("ID,Title,Description,Priority,Status,Due Date,Created At");
            
            // Data rows
            for (Task task : tasks) {
                writer.printf("%d,\"%s\",\"%s\",%s,%s,%s,%s%n",
                    task.getId(),
                    escapeCSV(task.getTitle()),
                    escapeCSV(task.getDescription()),
                    task.getPriority(),
                    task.getStatus(),
                    task.getDueDate() != null ? task.getDueDate().toString() : "",
                    task.getCreatedAt() != null ? task.getCreatedAt().toString() : ""
                );
            }
        }
    }
    
    private void exportMoodEntriesToCSV(int userId, String filePath) throws IOException {
        List<MoodEntry> entries = moodService.getRecentMoodEntries(userId, 365); // Last year
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // CSV Header
            writer.println("ID,Mood Level,Description,Entry Date,Created At");
            
            // Data rows
            for (MoodEntry entry : entries) {
                writer.printf("%d,%d,\"%s\",%s,%s%n",
                    entry.getId(),
                    entry.getMoodLevel(),
                    escapeCSV(entry.getMoodDescription()),
                    entry.getEntryDate().toString(),
                    entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : ""
                );
            }
        }
    }
    
    private void exportNotesToCSV(int userId, String filePath) throws IOException {
        List<Note> notes = notesService.getNotesForUser(userId);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // CSV Header
            writer.println("ID,Title,Content,Tags,Created At,Updated At");
            
            // Data rows
            for (Note note : notes) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",%s,%s%n",
                    note.getId(),
                    escapeCSV(note.getTitle()),
                    escapeCSV(stripHTML(note.getContent())),
                    escapeCSV(note.getTags()),
                    note.getCreatedAt() != null ? note.getCreatedAt().toString() : "",
                    note.getUpdatedAt() != null ? note.getUpdatedAt().toString() : ""
                );
            }
        }
    }
    
    private void exportGoalsToCSV(int userId, String filePath) throws IOException {
        List<Goal> goals = goalsService.getGoalsForUser(userId);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // CSV Header
            writer.println("ID,Title,Description,Goal Type,Target Value,Current Value,Progress %,Status,Target Date,Created At");
            
            // Data rows
            for (Goal goal : goals) {
                writer.printf("%d,\"%s\",\"%s\",%s,%d,%d,%.1f,%s,%s,%s%n",
                    goal.getId(),
                    escapeCSV(goal.getTitle()),
                    escapeCSV(goal.getDescription()),
                    goal.getGoalType(),
                    goal.getTargetValue(),
                    goal.getCurrentValue(),
                    goal.getProgressPercentage(),
                    goal.getStatus(),
                    goal.getTargetDate().toString(),
                    goal.getCreatedAt() != null ? goal.getCreatedAt().toString() : ""
                );
            }
        }
    }
    
    private void createSummaryReport(int userId, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("=== FOCUSBUDDY DATA EXPORT SUMMARY ===");
            writer.println("Export Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("User: " + UserSession.getInstance().getCurrentUser().getFullName());
            writer.println();
            
            // Tasks summary
            List<Task> tasks = taskService.getTasksForUser(userId);
            writer.println("TASKS SUMMARY:");
            writer.println("Total Tasks: " + tasks.size());
            writer.println("Completed: " + tasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count());
            writer.println("Pending: " + tasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count());
            writer.println("In Progress: " + tasks.stream().filter(t -> t.getStatus() == Task.Status.IN_PROGRESS).count());
            writer.println();
            
            // Mood summary
            List<MoodEntry> moods = moodService.getRecentMoodEntries(userId, 30);
            if (!moods.isEmpty()) {
                writer.println("MOOD SUMMARY (Last 30 Days):");
                writer.println("Total Entries: " + moods.size());
                double avgMood = moods.stream().mapToInt(MoodEntry::getMoodLevel).average().orElse(0.0);
                writer.println("Average Mood: " + String.format("%.1f/5", avgMood));
                writer.println();
            }
            
            // Notes summary
            List<Note> notes = notesService.getNotesForUser(userId);
            writer.println("NOTES SUMMARY:");
            writer.println("Total Notes: " + notes.size());
            writer.println();
            
            // Goals summary
            List<Goal> goals = goalsService.getGoalsForUser(userId);
            writer.println("GOALS SUMMARY:");
            writer.println("Total Goals: " + goals.size());
            writer.println("Completed: " + goals.stream().filter(g -> g.getStatus() == Goal.Status.COMPLETED).count());
            writer.println("Active: " + goals.stream().filter(g -> g.getStatus() == Goal.Status.ACTIVE).count());
            writer.println();
            
            writer.println("=== END OF SUMMARY ===");
        }
    }
    
    public CompletableFuture<Boolean> backupDatabase(String backupPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This would implement database backup functionality
                // For MySQL, you could use mysqldump command
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String backupFile = backupPath + "/focusbuddy_backup_" + timestamp + ".sql";
                
                ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump", 
                    "-u", "root", 
                    "-p", 
                    "focusbuddy"
                );
                
                Process process = pb.start();
                
                try (FileOutputStream fos = new FileOutputStream(backupFile);
                     InputStream is = process.getInputStream()) {
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                int exitCode = process.waitFor();
                return exitCode == 0;
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    private String escapeCSV(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
    
    private String stripHTML(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "");
    }
}
