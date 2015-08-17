package com.percero.util;

/**
 * Credit to: http://syntx.io/how-to-generate-a-random-string-in-java/
 */
public class RandomStringGenerator {

    public static enum Mode {
        ALPHA, ALPHANUMERIC, NUMERIC
    }

    public static String string(int length) {
        return string(length, Mode.ALPHANUMERIC);
    }

    public static String string(int length, Mode mode) {

        StringBuffer buffer = new StringBuffer();
        String characters = "";

        switch(mode){

            case ALPHA:
                characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                break;

            case ALPHANUMERIC:
                characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
                break;

            case NUMERIC:
                characters = "1234567890";
                break;
        }

        int charactersLength = characters.length();

        for (int i = 0; i < length; i++) {
            double index = Math.random() * charactersLength;
            buffer.append(characters.charAt((int) index));
        }
        return buffer.toString();
    }
}