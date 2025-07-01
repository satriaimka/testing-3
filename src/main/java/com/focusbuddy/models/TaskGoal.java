package com.focusbuddy.models;

public class TaskGoal extends Goal {

    public TaskGoal() {
        super();
        this.goalType = GoalType.TASKS_COMPLETED;
    }

    public TaskGoal(String title, String description, int targetTasks) {
        this();
        this.title = title;
        this.description = description;
        this.targetValue = targetTasks;
    }

    @Override
    public void updateProgress(int tasksIncrement) {
        this.currentValue += tasksIncrement;
        if (isCompleted() && this.status == Status.ACTIVE) {
            this.status = Status.COMPLETED;
        }
    }

    @Override
    public double getProgressPercentage() {
        if (targetValue == 0) return 0.0;
        return Math.min(100.0, (double) currentValue / targetValue * 100.0);
    }

    @Override
    public boolean isCompleted() {
        return currentValue >= targetValue;
    }

    /**
     * Get formatted progress text untuk UI
     */
    public String getProgressText() {
        return String.format("%d/%d tasks completed", currentValue, targetValue);
    }

    /**
     * Get tasks remaining
     */
    public int getTasksRemaining() {
        return Math.max(0, targetValue - currentValue);
    }

    /**
     * Check if goal is almost complete (>80% progress)
     */
    public boolean isAlmostComplete() {
        return getProgressPercentage() >= 80.0;
    }
}