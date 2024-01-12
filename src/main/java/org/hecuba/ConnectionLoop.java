package org.hecuba;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ConnectionLoop {
    private final SocketChannel channel;
    private final Selector selector;
    private final Thread thread;
    ByteBuffer buffer;

    ConnectionLoop(SocketChannel channel, Selector selector) {
        this.thread = new Thread(this::run, "connection-loop");
        this.channel = channel;
        this.selector = selector;
        this.buffer = ByteBuffer.allocate(1024);
        try {
            this.channel.configureBlocking(false);
            this.channel.register(this.selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void run() {
        this.thread.start();
    }

    void start() {
        while (true) {
            try {
                this.selector.select();
                var selectedKeys = this.selector.selectedKeys();
                var iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isReadable()) {
                        this.channel.read(buffer);
                        buffer.flip();
                        this.channel.write(buffer);
                        buffer.clear();
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
