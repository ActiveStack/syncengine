package com.percero.client;

/**
 * Interface to define the way that the client does async processing with callbacks
 * Created by jonnysamps on 9/15/15.
 */
public interface Callback<T> {
    void execute(T result);
}
