package com.percero.agents.sync.jobs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Class to represent a row from the an update table
 * Created by jonnysamps on 9/3/15.
 */
public class UpdateTableRow {

    private static Logger logger = Logger.getLogger(UpdateTableRow.class);

    public int ID;
    public String tableName;
    public String rowId;
    public int lockId;
    public Date lockDate;
    public Date timestamp;
    public UpdateTableRowType type;

    public Date getTimestamp() {
        return timestamp;
    }

    public int getID() {
        return ID;
    }

    public String getTableName() {
        return tableName;
    }

    public String getRowId() {
        return rowId;
    }

    public int getLockId() {
        return lockId;
    }

    public Date getLockDate() {
        return lockDate;
    }

    public UpdateTableRowType getType() {
        return type;
    }


}
