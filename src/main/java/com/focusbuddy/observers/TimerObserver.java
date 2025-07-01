package com.focusbuddy.observers;

/**
 * Observer interface for timer events
 * Allows components to listen to timer state changes
 */
public interface TimerObserver {

    /**
     * Called when timer starts
     * @param timerType Type of timer session (FOCUS or BREAK)
     */
    void onTimerStart(String timerType);

    /**
     * Called when timer is paused
     * @param timerType Type of timer session (FOCUS or BREAK)
     */
    void onTimerPause(String timerType);

    /**
     * Called when timer is reset
     * @param timerType Type of timer session (FOCUS or BREAK)
     */
    void onTimerReset(String timerType);

    /**
     * Called when timer completes
     * @param timerType Type of timer session that completed (FOCUS or BREAK)
     * @param durationSeconds Duration of completed session in seconds
     */
    void onTimerComplete(String timerType, int durationSeconds);
}