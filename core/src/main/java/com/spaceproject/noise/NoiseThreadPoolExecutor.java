package com.spaceproject.noise;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NoiseThreadPoolExecutor extends ThreadPoolExecutor {
    
    private final List<Runnable> activeTasks;
    private final Array<INoiseGenListener> listeners;
    
    NoiseThreadPoolExecutor(int numThreads) {
        super(numThreads, numThreads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        allowCoreThreadTimeOut(true);
        
        listeners = new Array<>();
        activeTasks = Collections.synchronizedList(new ArrayList<Runnable>());
    
        Gdx.app.log(this.getClass().getSimpleName(), "Detected " + Runtime.getRuntime().availableProcessors() + " available CPUs");
        Gdx.app.log(this.getClass().getSimpleName(), "Created ThreadPool with " + getMaximumPoolSize() + " threads");
    }

    public void addListener(INoiseGenListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListenersNoiseFinished(NoiseThread noise) {
        for (int i = 0; i < listeners.size; i++) {
            listeners.get(i).threadFinished(noise);
        }
    }
    
    @Override
    public void execute(Runnable runnable) {
        if (activeTasks.contains(runnable)) {
            Gdx.app.log(this.getClass().getSimpleName(), "Seed already exists. Ignoring:" + runnable.toString());
            return;
        }
        
        activeTasks.add(runnable);
        super.execute(runnable);
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        activeTasks.remove(r);
        
        if (t == null) {
            notifyListenersNoiseFinished((NoiseThread) r);
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "Task failed", t);
        }
    }

    @Override
    public String toString() {
        return "completed: [" + getCompletedTaskCount() + "/" + getTaskCount()
                + "] active: [" + getActiveCount() + "/" + getCorePoolSize()
                + "] -> " + activeTasks
         + "\nQ:" + getQueue();
    }
    
}

