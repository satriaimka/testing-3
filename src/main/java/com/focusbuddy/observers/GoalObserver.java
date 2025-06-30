package com.focusbuddy.observers;

import com.focusbuddy.models.Goal;

public interface GoalObserver {
    void onGoalCompleted(Goal goal);
    void onGoalProgressUpdated(Goal goal, int oldValue, int newValue);
    void onGoalCreated(Goal goal);
}
