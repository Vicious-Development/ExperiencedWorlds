package com.vicious.experiencedworlds.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ServerExecutor {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    public static void execute(Runnable run){
        executor.execute(run);
    }
}
