package com.percero.amqp;

public interface IDecoder {
	public Object decode(byte[] data) throws Exception;
}
