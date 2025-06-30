package com.focusbuddy.models;

public class StudyGoal extends Goal {
    
    public StudyGoal() {
        super();
        this.goalType = GoalType.STUDY_HOURS;
    }
    
    public StudyGoal(String title, String description, int targetHours) {
        this();
        this.title = title;
        this.description = description;
        this.targetValue = targetHours;
    }
    
    @Override
    public void updateProgress(int hoursIncrement) {
        this.currentValue += hoursIncrement;
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
}
