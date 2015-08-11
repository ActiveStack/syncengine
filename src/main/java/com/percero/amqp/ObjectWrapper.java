package com.percero.amqp;

public class ObjectWrapper {
	public Object data;
	public String className;
	
	public ObjectWrapper(Object d){
		this.data = d;
		this.className = d.getClass().getName();
	}
	
	public ObjectWrapper(){}
}
