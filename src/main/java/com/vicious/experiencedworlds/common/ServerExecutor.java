package com.vicious.experiencedworlds.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerExecutor {
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    public static void execute(Runnable run){
        executor.execute(run);
    }
}
