package xyz.cirno.pseudodcbacklight;

import android.os.Handler;

public final class HandlerTimer {
    private Handler handler;
    private Runnable callback;
    private long intervalMillis;

    private Runnable wrapper;

    public HandlerTimer(Handler handler, long intervalMillis, Runnable callback) {
        this.handler = handler;
        this.callback = callback;
        this.intervalMillis = intervalMillis;

        wrapper = new Runnable() {
            @Override
            public void run() {
                callback.run();
                handler.postDelayed(this, HandlerTimer.this.intervalMillis);
            }
        };
    }

    public void start() {
        handler.postDelayed(wrapper, intervalMillis);
    }

    public void stop() {
        handler.removeCallbacks(wrapper);
    }

    public void setIntervalMillis(long interval) {
        this.intervalMillis = interval;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }
}
