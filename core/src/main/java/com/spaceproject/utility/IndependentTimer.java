package com.spaceproject.utility;


/** Variation of SimpleTimer where time is not relative to game time.
 * NOT synced to pause/play. */
public class IndependentTimer extends SimpleTimer {
    
    private long lastEvent;
    
    public IndependentTimer(long time) {
        this(time, false);
    }
    
    public IndependentTimer(long time, boolean setLastEventTime) {
        super(time, setLastEventTime);
        
        if (setLastEventTime)
            lastEvent = System.currentTimeMillis();
    }
    
    @Override
    public void reset() {
        lastEvent = System.currentTimeMillis();
    }
    
    @Override
    public long timeSinceLastEvent() {
        return System.currentTimeMillis() - lastEvent;
    }

}