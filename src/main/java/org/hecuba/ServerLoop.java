package org.hecuba;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * ServerSocketChannel can only accept connections. We need to accept them to create SocketChannel that can be read
 * from and written to.
 */
public class ServerLoop {
    private final Selector selector;
    private final ServerSocketChannel channel;
    private final Thread thread;

    ServerLoop() {
        this.thread = new Thread(this::run, "server-loop");
        try {
            this.selector = Selector.open();
            SocketAddress address = new InetSocketAddress(8000);
            this.channel = ServerSocketChannel.open();
            this.channel.bind(address);
            this.channel.configureBlocking(false);
            this.channel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        this.thread.start();
    }

    private void run() {
        while (true) {
            try {
                this.selector.select();
                var selectedKeys = selector.selectedKeys();
                var iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        SocketChannel client = this.channel.accept();
                        ConnectionLoop conn = new ConnectionLoop(client, this.selector);
                        conn.start();
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
