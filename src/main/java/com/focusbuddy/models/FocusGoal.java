package com.focusbuddy.models;

public class FocusGoal extends Goal {
    
    public FocusGoal() {
        super();
        this.goalType = GoalType.FOCUS_SESSIONS;
    }
    
    public FocusGoal(String title, String description, int targetSessions) {
        this();
        this.title = title;
        this.description = description;
        this.targetValue = targetSessions;
    }
    
    @Override
    public void updateProgress(int sessionsIncrement) {
        this.currentValue += sessionsIncrement;
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
