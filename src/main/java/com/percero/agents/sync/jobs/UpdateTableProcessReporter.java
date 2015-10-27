package com.percero.agents.sync.jobs;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jonnysamps on 10/10/15.
 */
public class UpdateTableProcessReporter {

    private static Logger logger = Logger.getLogger(UpdateTableProcessReporter.class);
    private static final int WINDOW_LENGTH = 10000;
    private static final double WINDOW_LENGTH_SECONDS = roundToOnePlace(WINDOW_LENGTH/1000.0);

    private static UpdateTableProcessReporter instance;

    private UTPReportStat totalStat = new UTPReportStat();
    private Map<String, UTPReportStat> stats = new HashMap<>();

    private UpdateTableProcessReporter(){
        new Thread(this.windowTimer).start(); // Start the reporting thread
    }

    public static UpdateTableProcessReporter getInstance(){
        if(instance == null){
            instance = new UpdateTableProcessReporter();
        }
        return instance;
    }

    public void submitCountAndTime(String key, int count, long time){
        synchronized (this) {
            if(!stats.containsKey(key))
                stats.put(key, new UTPReportStat());

            totalStat.totalCount += count;
            totalStat.totalTime += time;
            totalStat.windowCount += count;
            totalStat.windowTime += time;
            stats.get(key).totalCount += count;
            stats.get(key).totalTime += time;
            stats.get(key).windowCount += count;
            stats.get(key).windowTime += time;
        }
    }

    private static double roundToOnePlace(double number){
        number *= 10;
        int rounded = (int) number;
        number = rounded / 10.0;
        return number;
    }

    private void resetwindow(){
        synchronized (this) {
            totalStat.windowCount = 0;
            totalStat.windowTime = 0;
            for(UTPReportStat stat : stats.values()){
                stat.windowCount = 0;
                stat.windowTime = 0;
            }
        }
    }

    private void printStats(){
        if(logger.isDebugEnabled()) {
            synchronized (this) {
                printStat("Totals", totalStat);
                for (String key : stats.keySet())
                    printStat(key, stats.get(key));
            }
        }
    }

    private static void printStat(String label, UTPReportStat stat){
        double totalTimeSeconds = roundToOnePlace(stat.totalTime / 1000.0);
        double totalOpsPerSecond = roundToOnePlace(stat.totalCount / totalTimeSeconds);
        double windowTimeSeconds = roundToOnePlace(stat.windowTime/1000.0);
        double windowOpsPerSecond = roundToOnePlace(stat.windowCount/windowTimeSeconds);
        logger.debug(
                "UT: " + pad(20, label) +
                        " : Last " + pad(3, WINDOW_LENGTH_SECONDS + "") + "s" +
                        "(" + pad(5, stat.windowCount + "") +
                        ", " + pad(6, windowOpsPerSecond + "") + "/s) " +
                        "Total(" + pad(7, stat.totalCount + "") + ", " +
                        pad(6, totalOpsPerSecond + "") + "/s)"
        );
    }

    private static String pad(int space, String content){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < space-content.length(); i++){
            sb.append(" ");
        }
        sb.append(content);
        return sb.toString();
    }

    /**
     * Window timer will sleep for the window length, wake up print some stats,
     * reset the window counters and then sleep again.
     */
    private Runnable windowTimer = new Runnable() {
        @Override
        public void run() {
            while(true){
                try {
                    Thread.sleep(WINDOW_LENGTH);
                    instance.printStats();
                    instance.resetwindow();
                }catch(InterruptedException e){
                    logger.error(e.getMessage(), e);
                    break;
                }
            }
        }
    };

    private class UTPReportStat{
        public int totalCount = 0;
        public long totalTime = 0;
        public int windowCount = 0;
        public long windowTime = 0;
    }
}
