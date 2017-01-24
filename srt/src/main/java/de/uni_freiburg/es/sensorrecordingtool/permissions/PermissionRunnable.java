package de.uni_freiburg.es.sensorrecordingtool.permissions;

/**
 * Abstract runnable class that contains information about granted and not granted results. For
 * Example to include detailed log in error intents.
 */
public abstract class PermissionRunnable implements Runnable {

    protected String[] grantedResults;
    protected String[] notGrantedResults;

    public void setResults(String[] grantedResults, String[] notGrantedResults) {
        this.grantedResults = grantedResults;
        this.notGrantedResults = notGrantedResults;
    }
}