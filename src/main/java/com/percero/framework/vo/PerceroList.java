package com.percero.framework.vo;

import java.util.ArrayList;
import java.util.Collection;

public class PerceroList<E> extends ArrayList<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4590110454912225638L;
	
	public PerceroList(Collection<E> collection) {
		super(collection);
	}
	
	private int totalLength = 0;
	public void setTotalLength(int value) {
		totalLength = value;
	}
	public int getTotalLength() {
		return totalLength;
	}

	private Integer pageSize;
	public Integer getPageSize() {
		return pageSize;
	}
	public void setPageSize(Integer value) {
		pageSize = value;
	}
	
	private Integer pageNumber;
	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer value) {
		pageNumber = value;
	}
	
}
