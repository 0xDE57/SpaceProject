package com.spaceproject.utility;

import com.spaceproject.screens.GameScreen;

public class SimpleTimer {
    
    private long interval;
    private long lastEvent;
    
    public SimpleTimer(long interval) {
        this(interval, false);
    }
    
    public SimpleTimer(long interval, boolean setLastEventTime) {
        this.interval = interval;
        if (setLastEventTime)
            reset();
    }
    
    public boolean tryEvent() {
        if (canDoEvent()) {
            reset();
            return true;
        }
        return false;
    }
    
    public boolean canDoEvent() {
        return timeSinceLastEvent() >= interval;
    }
    
    public void reset() {
        lastEvent = GameScreen.getGameTimeCurrent();
    }
    
    public long getInterval() {
        return interval;
    }
    
    public void setInterval(long interval, boolean reset) {
        this.interval = interval;
        if (reset)
            reset();
    }
    
    public long getLastEvent() {
        return lastEvent;
    }
    
    public void setLastEvent(long time) {
        lastEvent = time;
    }
    
    public void setCanDoEvent() {
        //ensures event can be done right now
        setLastEvent(timeSinceLastEvent() - getInterval());
    }
    
    public long timeSinceLastEvent() {
        return GameScreen.getGameTimeCurrent() - lastEvent;
    }
    
    public float ratio() {
        return Math.min((float) timeSinceLastEvent() / (float) interval, 1.0f);
    }
    
    public void setRatio(float ratio) {
        long newTime = GameScreen.getGameTimeCurrent() - (long)(ratio * interval);
        setLastEvent(newTime);
    }
    
    /** Invert ratio / reverse timer, eg: 0.7 -> 0.3 */
    public void flipRatio() {
        setRatio(1-ratio());
    }
    
    @Override
    public String toString() {
        return timeSinceLastEvent() + " (" + ratio() + ")";
    }
    
}
