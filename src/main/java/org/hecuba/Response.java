package org.hecuba;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

public class Response {

    public static class Header {
        private final String key;
        private final String value;

        public Header(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public byte[] serialize() {
            // We add COLON SPACE after key and CLRF after value
            int length = key.length() + value.length() + 4;
            byte[] res = new byte[length];
            int resWritePosition = 0;
            for (byte i: key.getBytes()) {
                res[resWritePosition] = i;
                resWritePosition++;
            }

            //COLON SPACE
            res[resWritePosition] = 58;
            resWritePosition++;
            res[resWritePosition] = 32;
            resWritePosition++;

            for (byte i: value.getBytes()) {
                res[resWritePosition] = i;
                resWritePosition++;
            }

            //CLRF
            res[resWritePosition] = 13;
            resWritePosition++;
            res[resWritePosition] = 10;
            return res;
        }
    }

    public enum StatusCode {
        TwoHundred;

        public byte[] getStatusBytes() {
            // Always followed by space
            switch (this) {
                case TwoHundred -> {
                    return new byte[]{50,48,48,32};
                }
                default -> {
                    return null;
                }
            }
        }

        public byte[] getReasonBytes() {
            // Always followed by CLRF
            switch (this) {
                case TwoHundred -> {
                    return new byte[]{79,75,13,10};
                }
                default -> {
                    return null;
                }
            }
        }
    }

    private final StatusCode statusCode;
    private final List<Header> headers;
    // HTTP/1.0
    private final static byte[] versionBytes = new byte[]{72,84,84,80,47,49,46,48,32};

    public Response(StatusCode statusCode, List<Header> headers){
        this.statusCode = statusCode;
        this.headers = headers;
    }

    private int writeInto (byte[] src, byte[] dest, int currPosition) {
        // We return the last write position
        int writePosition = currPosition;
        for (byte i: src) {
            dest[writePosition] = i;
            writePosition++;
        }
        return writePosition;
    }

    public byte[] serialize() {
        int length = 0;
        length+= versionBytes.length;
        assert this.statusCode.getStatusBytes() != null;
        length+= this.statusCode.getStatusBytes().length;
        assert this.statusCode.getReasonBytes() != null;
        length+= this.statusCode.getReasonBytes().length;

        var serializedHeaders = this.headers.stream().map(Header::serialize).toList();
        length += serializedHeaders.stream().map(v -> v.length).reduce(0, Integer::sum);

        byte[] hello = "Hello World\n".getBytes(StandardCharsets.US_ASCII);
        length += hello.length;

        byte[] res = new byte[length+2];

        int writePosition = 0;
        writePosition = writeInto(versionBytes, res, writePosition);
        writePosition = writeInto(this.statusCode.getStatusBytes(), res, writePosition);
        writePosition = writeInto(this.statusCode.getReasonBytes(), res, writePosition);
        for (byte[] header: serializedHeaders) {
            writePosition = writeInto(header, res, writePosition);
        }
        // Write CLRF empty line
        writePosition = writeInto(new byte[]{13,10}, res, writePosition);

        writeInto(hello, res, writePosition);
        return res;
    }
}
