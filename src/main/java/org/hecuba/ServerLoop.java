package org.hecuba;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * ServerSocketChannel can only accept connections. We need to accept them to create SocketChannel that can be read
 * from and written to.
 */
public class ServerLoop extends Thread {
    private final Selector selector;
    private final ServerSocketChannel channel;
    private final List<ConnectionLoop> connectionLoops;
    private int connectionPosition;
    private final int connectionCount;

    ServerLoop() {
        try {
            this.selector = Selector.open();
            SocketAddress address = new InetSocketAddress(8000);
            this.channel = ServerSocketChannel.open();
            this.channel.bind(address);
            this.channel.configureBlocking(false);
            this.channel.register(selector, SelectionKey.OP_ACCEPT);
            this.setName("server-loop");

            this.connectionPosition = 0;
            this.connectionCount = 10;
            this.connectionLoops = new ArrayList<>();
            for (int i=0; i <= this.connectionCount; i++) {
                this.connectionLoops.add(new ConnectionLoop());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ConnectionLoop getConnection() {
        // Get mutates state
        if (this.connectionPosition >= this.connectionCount) {
            this.connectionPosition = 0;
        }
        var curr = this.connectionPosition;
        this.connectionPosition+=1;
        return this.connectionLoops.get(curr);
    }

    @Override
    public void start() {
        super.start();
        this.connectionLoops.forEach(Thread::startVirtualThread);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                this.selector.select();
                var selectedKeys = selector.selectedKeys();
                var iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        var connection = this.getConnection();
                        connection.register(channel.accept());
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
