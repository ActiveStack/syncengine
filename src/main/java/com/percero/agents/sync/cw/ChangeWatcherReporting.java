package com.percero.agents.sync.cw;

public class ChangeWatcherReporting {

	public static Integer externalRequestsCounter = 0;
	public static Integer internalRequestsCounter = 0;
	
	public static Integer recalcsCounter = 0;
	public static Integer abortedRecalcsCounter = 0;
	public static Integer unchangedResultsCounter = 0;
	public static Integer changedResultsCounter = 0;
	
	public static String stringResults() {
		StringBuilder result = new StringBuilder("Total Requests: ");
		result.append((externalRequestsCounter + internalRequestsCounter)).append("\n");
		result.append("External Requests: ").append(externalRequestsCounter).append("\n");
		result.append("Internal Requests: ").append(internalRequestsCounter).append("\n");
		result.append("Unchanged Results: ").append(unchangedResultsCounter).append("\n");
		result.append("Changed Results: ").append(changedResultsCounter).append("\n");
		result.append("Recalcs: ").append(recalcsCounter).append("\n");
		result.append("Aborted Recalcs: ").append(abortedRecalcsCounter).append("\n");
		result.append("Aborted %: ").append((100.0 * abortedRecalcsCounter / recalcsCounter)).append("\n");
		
		return result.toString();
	}
}
