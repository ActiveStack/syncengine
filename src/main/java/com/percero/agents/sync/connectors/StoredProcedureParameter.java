package com.percero.agents.sync.connectors;

import java.sql.Types;

import org.springframework.util.StringUtils;

public class StoredProcedureParameter {
	
	public StoredProcedureParameter() {
		
	}
	
	private Integer position;
	private String name;
	private String type;
	private String direction;

	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}
	
	private Integer preparedStatementType = null;
	public Integer retrievePreparedStatementType() {
		if (preparedStatementType == null) {
			preparedStatementType = convertStringToTypes(getType());
		}
		return preparedStatementType;
	}
	
	public static int convertStringToTypes(String theType) {
		if (!StringUtils.hasText(theType)) {
			return Types.NULL;
		}
		else if ("array".equalsIgnoreCase(theType)) {
			return Types.ARRAY;
		}
		else if ("bigint".equalsIgnoreCase(theType) || "biginteger".equalsIgnoreCase(theType)) {
			return Types.BIGINT;
		}
		else if ("binary".equalsIgnoreCase(theType)) {
			return Types.BINARY;
		}
		else if ("bit".equalsIgnoreCase(theType)) {
			return Types.BIT;
		}
		else if ("blob".equalsIgnoreCase(theType)) {
			return Types.BLOB;
		}
		else if ("bool".equalsIgnoreCase(theType) || "boolean".equalsIgnoreCase(theType)) {
			return Types.BOOLEAN;
		}
		else if ("date".equalsIgnoreCase(theType)) {
			return Types.DATE;
		}
		else if ("decimal".equalsIgnoreCase(theType)) {
			return Types.DECIMAL;
		}
		else if ("double".equalsIgnoreCase(theType)) {
			return Types.DOUBLE;
		}
		else if ("float".equalsIgnoreCase(theType)) {
			return Types.FLOAT;
		}
		else if ("int".equalsIgnoreCase(theType) || "integer".equalsIgnoreCase(theType)) {
			return Types.INTEGER;
		}
		else if ("numeric".equalsIgnoreCase(theType)) {
			return Types.NUMERIC;
		}
		else if ("nvarchar".equalsIgnoreCase(theType)) {
			return Types.NVARCHAR;
		}
		else if ("rowid".equalsIgnoreCase(theType)) {
			return Types.ROWID;
		}
		else if ("smallint".equalsIgnoreCase(theType) || "smallinteger".equalsIgnoreCase(theType)) {
			return Types.SMALLINT;
		}
		else if ("time".equalsIgnoreCase(theType)) {
			return Types.TIME;
		}
		else if ("timestamp".equalsIgnoreCase(theType)) {
			return Types.TIMESTAMP;
		}
		else if ("tinyint".equalsIgnoreCase(theType) || "tinyinteger".equalsIgnoreCase(theType)) {
			return Types.TINYINT;
		}
		else if ("varbinary".equalsIgnoreCase(theType)) {
			return Types.VARBINARY;
		}
		else if ("varchar".equalsIgnoreCase(theType) || "varchar2".equalsIgnoreCase(theType)) {
			return Types.VARCHAR;
		}
		else {
			return Types.NULL;
		}
	}
}
