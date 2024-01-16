package org.hecuba;

import java.util.Arrays;

public class RequestTokenizer {

    public enum RequestMethod {
        GET,
    }

    private byte[] getHTTPMethod = new byte[]{71,69,84};

    private RequestMethod requestMethod;
    private byte[] requestPath;
    private byte[] requestVersion;

    private int spaceCounter = 0;
    private int lineCounter = 0;

    public RequestMethod getRequestMethod() {
        // Perf improvement would be to delay enum comparison until we call get, so lazy parsing
        return this.requestMethod;
    }

    public byte[] getRequestPath() {
        return this.requestPath;
    }

    public byte[] getRequestVersion() {
        return this.requestVersion;
    }

    public RequestTokenizer(byte[] input) {
        byte[] buffer = new byte[100];
        int bufferWritePosition = 0;

        for (int i = 0; i < input.length; i++) {
            byte curr = input[i];
            byte last = i > 0 ? input[i-1] : Byte.MIN_VALUE;
            if (curr == 0 && last == 0) {
                break;
            }

            buffer[bufferWritePosition] = curr;

            // Hit space
            if (curr == 32) {
                // HTTP Method
                if (lineCounter == 0 && spaceCounter == 0) {
                    if (Arrays.compare(buffer, 0, bufferWritePosition -1, this.getHTTPMethod, 0, this.getHTTPMethod.length-1) == 0) {
                        this.requestMethod = RequestMethod.GET;
                    }
                }
                // HTTP Path
                if (lineCounter == 0 && spaceCounter == 1) {
                    this.requestPath = Arrays.copyOfRange(buffer, 0, bufferWritePosition);
                }

                this.spaceCounter++;
                Arrays.fill(buffer, Byte.MIN_VALUE);
                bufferWritePosition = 0;
                continue;
            }

            if (curr == 10 && last == 13) {
                // HTTP Version
                if (lineCounter == 0 && spaceCounter == 2) {
                    this.requestVersion = Arrays.copyOfRange(buffer, 0, bufferWritePosition);
                }

                this.lineCounter++;
                Arrays.fill(buffer, Byte.MIN_VALUE);
                bufferWritePosition = 0;
                continue;
            }
            bufferWritePosition++;
        }
    }
}
