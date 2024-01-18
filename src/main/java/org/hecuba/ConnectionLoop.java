package org.hecuba;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

public class ConnectionLoop extends Thread {
    ByteBuffer buffer;
    Selector selector;

    private class Connection {
        public final SocketChannel socketChannel;
        public final SelectionKey selectionKey;

        public Request request;

        public void addRequest(Request request) {
            this.request = request;
        }

        Connection(SocketChannel socketChannel, SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
            this.socketChannel = socketChannel;
        }
    }


    ConnectionLoop() throws IOException {
        this.buffer = ByteBuffer.allocateDirect(1024);
        this.setName("connection-loop");
        this.selector = Selector.open();
    }

    public void register(SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            SelectionKey selectionKey = socketChannel.register(this.selector, SelectionKey.OP_READ);
            Connection connection = new Connection(socketChannel, selectionKey);
            selectionKey.attach(connection);
            this.selector.wakeup();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.selector.select();
                var selectedKeys = this.selector.selectedKeys();
                var iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isReadable()) {
                        Connection conn = (Connection) key.attachment();
                        this.buffer.clear();
                        int read = conn.socketChannel.read(this.buffer);
                        if (read == -1) {
                            this.buffer.clear();
                            conn.socketChannel.close();
                        } else {
                            //conn.addRequest(new Request(this.buffer.array()));

                            this.buffer.flip();

                            var headers = List.of(
                                    new Response.Header("Content-Type", "text/plain"),
                                    new Response.Header("Content-Length", "12")
                            );

                            var resp = new Response(Response.StatusCode.TwoHundred, headers);
                            int resp_size = resp.serialize(this.buffer);
                            this.buffer.flip();
                            conn.socketChannel.write(this.buffer);
                            conn.socketChannel.close();
                            this.buffer.clear();
                        }
                    }

                    iter.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
