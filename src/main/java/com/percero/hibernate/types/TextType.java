package com.percero.hibernate.types;

import java.sql.Types;

public class TextType extends org.hibernate.type.TextType {

	public int sqlType() {
		return Types.BLOB;
	}
}
