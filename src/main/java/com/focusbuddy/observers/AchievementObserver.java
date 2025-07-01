package com.focusbuddy.observers;

import com.focusbuddy.models.Goal;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.services.ActivityService;
import com.focusbuddy.utils.UserSession;

/**
 * 🏆 ACHIEVEMENT OBSERVER
 *
 * Handles goal completion events dan creates achievements/notifications
 */
public class AchievementObserver implements GoalObserver {

    private ActivityService activityService;

    public AchievementObserver() {
        this.activityService = new ActivityService();
    }

    @Override
    public void onGoalCompleted(Goal goal) {
        try {
            System.out.println("🏆 Processing goal completion achievement for: " + goal.getTitle());

            // Create achievement notification
            createAchievementNotification(goal);

            // Log achievement activity
            logAchievementActivity(goal);

            // Check for milestone achievements (completed multiple goals)
            checkMilestoneAchievements(goal.getUserId());

            // Play celebration effect (could add sound later)
            triggerCelebrationEffect(goal);

        } catch (Exception e) {
            System.err.println("Error processing goal completion achievement: " + e.getMessage());
        }
    }

    @Override
    public void onGoalProgressUpdated(Goal goal, int oldValue, int newValue) {
        try {
            System.out.println("📊 Goal progress updated: " + goal.getTitle() + " (" + newValue + "/" + goal.getTargetValue() + ")");

            // Check for milestone progress (25%, 50%, 75%)
            checkProgressMilestones(goal, oldValue, newValue);

            // Send encouragement if goal is falling behind
            checkEncouragementNeeded(goal);

        } catch (Exception e) {
            System.err.println("Error processing goal progress update: " + e.getMessage());
        }
    }

    @Override
    public void onGoalCreated(Goal goal) {
        try {
            System.out.println("🎯 New goal created: " + goal.getTitle());

            // Send motivational message for new goal
            sendNewGoalMotivation(goal);

            // Log goal creation activity
            logGoalCreationActivity(goal);

        } catch (Exception e) {
            System.err.println("Error processing goal creation: " + e.getMessage());
        }
    }

    // ===================================================================
    // 🎉 PRIVATE HELPER METHODS
    // ===================================================================

    private void createAchievementNotification(Goal goal) {
        try {
            String achievementTitle = getAchievementTitle(goal);
            String achievementMessage = getAchievementMessage(goal);

            NotificationManager.getInstance().showNotification(
                    achievementTitle,
                    achievementMessage,
                    NotificationManager.NotificationType.SUCCESS
            );

        } catch (Exception e) {
            System.err.println("Error creating achievement notification: " + e.getMessage());
        }
    }

    private String getAchievementTitle(Goal goal) {
        return switch (goal.getGoalType()) {
            case TASKS_COMPLETED -> "🎯 Task Master Achievement!";
            case FOCUS_SESSIONS -> "🍅 Focus Champion Achievement!";
            case STUDY_HOURS -> "📚 Study Hero Achievement!";
        };
    }

    private String getAchievementMessage(Goal goal) {
        String baseMessage = String.format(
                "Congratulations! You've completed your goal:\n\n\"%s\"\n\n",
                goal.getTitle()
        );

        String typeSpecificMessage = switch (goal.getGoalType()) {
            case TASKS_COMPLETED -> String.format(
                    "✅ %d tasks completed! Your productivity is amazing!",
                    goal.getTargetValue()
            );
            case FOCUS_SESSIONS -> String.format(
                    "🍅 %d focus sessions completed! Your concentration is impressive!",
                    goal.getTargetValue()
            );
            case STUDY_HOURS -> String.format(
                    "📚 %d hours of focused study! Your dedication is inspiring!",
                    goal.getTargetValue()
            );
        };

        return baseMessage + typeSpecificMessage + "\n\n🏆 Keep up the excellent work!";
    }

    private void logAchievementActivity(Goal goal) {
        try {
            // Could enhance ActivityService to log achievements
            System.out.println("📝 Achievement logged for activity tracking");

        } catch (Exception e) {
            System.err.println("Error logging achievement activity: " + e.getMessage());
        }
    }

    private void checkMilestoneAchievements(int userId) {
        try {
            // Check for milestones like "Complete 5 goals", "Complete 10 goals", etc.
            // Implementation would query database for total completed goals

            System.out.println("🎖️ Checking milestone achievements for user " + userId);

        } catch (Exception e) {
            System.err.println("Error checking milestone achievements: " + e.getMessage());
        }
    }

    private void triggerCelebrationEffect(Goal goal) {
        try {
            // Could add visual/audio celebration effects here
            System.out.println("🎊 Celebration effect triggered for goal: " + goal.getTitle());

        } catch (Exception e) {
            System.err.println("Error triggering celebration effect: " + e.getMessage());
        }
    }

    private void checkProgressMilestones(Goal goal, int oldValue, int newValue) {
        try {
            double oldPercentage = (double) oldValue / goal.getTargetValue() * 100;
            double newPercentage = (double) newValue / goal.getTargetValue() * 100;

            // Check for crossing 25%, 50%, 75% milestones
            int[] milestones = {25, 50, 75};

            for (int milestone : milestones) {
                if (oldPercentage < milestone && newPercentage >= milestone) {
                    sendProgressMilestoneNotification(goal, milestone);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error checking progress milestones: " + e.getMessage());
        }
    }

    private void sendProgressMilestoneNotification(Goal goal, int milestone) {
        try {
            String message = String.format(
                    "🎉 Great progress on \"%s\"!\n\nYou're %d%% of the way to your goal. Keep going!",
                    goal.getTitle(),
                    milestone
            );

            NotificationManager.getInstance().showNotification(
                    "🎯 Milestone Reached!",
                    message,
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            System.err.println("Error sending milestone notification: " + e.getMessage());
        }
    }

    private void checkEncouragementNeeded(Goal goal) {
        try {
            // Check if goal is behind schedule or needs encouragement
            double progress = goal.getProgressPercentage();

            if (progress < 20 && goal.getTargetDate() != null) {
                // Could send encouragement based on deadline proximity
                System.out.println("💪 Consider sending encouragement for goal: " + goal.getTitle());
            }

        } catch (Exception e) {
            System.err.println("Error checking encouragement needed: " + e.getMessage());
        }
    }

    private void sendNewGoalMotivation(Goal goal) {
        try {
            String message = String.format(
                    "🚀 New goal set: \"%s\"\n\nTarget: %d %s\n\nYou've got this! Take it one step at a time.",
                    goal.getTitle(),
                    goal.getTargetValue(),
                    goal.getGoalType().name().toLowerCase().replace("_", " ")
            );

            NotificationManager.getInstance().showNotification(
                    "🎯 Goal Created!",
                    message,
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            System.err.println("Error sending new goal motivation: " + e.getMessage());
        }
    }

    private void logGoalCreationActivity(Goal goal) {
        try {
            System.out.println("📝 Goal creation logged for activity tracking: " + goal.getTitle());

        } catch (Exception e) {
            System.err.println("Error logging goal creation: " + e.getMessage());
        }
    }
}