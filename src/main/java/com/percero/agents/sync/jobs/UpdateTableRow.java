package com.percero.agents.sync.jobs;

import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

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


    public static UpdateTableRow fromResultSet(ResultSet resultSet) throws SQLException{
        UpdateTableRow row = new UpdateTableRow();
        row.ID          = resultSet.getInt("ID");
        row.tableName   = resultSet.getString("tableName");
        row.rowId       = resultSet.getString("rowId");
        row.lockId      = resultSet.getInt("lockId");
        row.lockDate    = resultSet.getDate("lockDate");
        row.type        = UpdateTableRowType.valueOf(resultSet.getString("type"));
        row.timestamp   = resultSet.getDate("timestamp");

        return row;
    }
}
