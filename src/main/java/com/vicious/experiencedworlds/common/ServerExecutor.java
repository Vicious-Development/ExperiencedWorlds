package com.vicious.experiencedworlds.common;

public class ServerExecutor {
    public static void execute(Runnable run){
        new Thread(run).start();
    }
}
