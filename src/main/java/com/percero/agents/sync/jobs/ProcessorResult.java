package com.percero.agents.sync.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jonnysamps on 8/31/15.
 */
public class ProcessorResult {
    private int total;
    private int numFailed;

    private boolean success = true;
    private Map<String, Integer> details = new HashMap<String, Integer>();
    private Map<String, List<String>> failures = new HashMap<String, List<String>>();

    public int getTotal() {
        return total;
    }

    public int getNumFailed(){
        return numFailed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Integer> getDetails() {
        return details;
    }

    public void addResult(String type, boolean wasSuccess, String message){
        total++;
        if(!wasSuccess) {
            success = false;
            numFailed++;
            if(!failures.containsKey(type)) failures.put(type, new ArrayList<String>());
            failures.get(type).add(message);
        }
        else{
            if(!details.containsKey(type)) details.put(type, 0);
            details.put(type, details.get(type)+1);
        }
    }

    /**
     * @see ProcessorResult.addResult(String, boolean, String)
     * @param type
     */
    public void addResult(String type){
        this.addResult(type, true, null);
    }

    /**
     * Some niceties for logging
     * @return
     */
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Overall Success :"); sb.append(isSuccess()); sb.append("\n");
        sb.append("Total results   :"); sb.append(getTotal()); sb.append("\n");
        sb.append("Total failures  :"); sb.append(getNumFailed()); sb.append("\n");
        sb.append("Failures by type:"); sb.append("\n");
        for(String key : failures.keySet()){
            sb.append("  "); sb.append(key); sb.append(":"); sb.append("\n");
            int i = 1;
            for(String message : failures.get(key)){
                sb.append("    "); sb.append(i); sb.append(": "); sb.append(message); sb.append("\n");
                i++;
            }
        }

        return sb.toString();
    }
}
