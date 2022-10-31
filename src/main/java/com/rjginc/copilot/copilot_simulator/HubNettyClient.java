package com.rjginc.copilot.copilot_simulator;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.rjginc.interop.messages.HubCopilotMessageCodec;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

@Component()
@Scope("prototype")
public class HubNettyClient {
    private static final Logger log = LoggerFactory.getLogger(HubNettyClient.class);

    @Autowired
    HubAddress hubAddress;

    EventLoopGroup workerGroup = new NioEventLoopGroup();
    Bootstrap b = new Bootstrap(); // (1)

    @Autowired
    Provider<HubClientHandler> clientHandlerProvider;
    
    SimulatedCopilot simulatedCopilot;

    @Autowired
    Provider<HubCopilotMessageCodec> hubCopilotMessageCodecProvider;

    ChannelFuture future = null;
    Channel channel = null;

    int retryCount = 0;
    boolean connect = false;

    public HubNettyClient() {
        super();
    }
    
    

    public void setSimulatedCopilot(SimulatedCopilot simulatedCopilot)
	{	
    	this.simulatedCopilot = simulatedCopilot;
//		clientHandler.setSimulatedCopilot(simulatedCopilot);
	}

	@PostConstruct
    public void init() {
//        eventBus.register(this);
        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
//        b.option(ChannelOption.SO_SNDBUF, 16777216 + 2);
//        b.option(ChannelOption.SO_RCVBUF, 4*1024);
        
//        ResourceLeakDetector.setLevel(Level.PARANOID);
        
        
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
            	HubClientHandler hubClientHandler = clientHandlerProvider.get();
                hubClientHandler.setSimulatedCopilot(simulatedCopilot);           	
                ch.pipeline().addLast("fieldEncoder", new LengthFieldPrepender(3, 0, false));
                ch.pipeline().addLast("fieldDecoder", new LengthFieldBasedFrameDecoder(16777216 + 2, 0, 3, 0, 0, true));
                // write the message type and message bytes
                ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(45, 30, 0));
                ch.pipeline().addLast("messageCodec", hubCopilotMessageCodecProvider.get());
                ch.pipeline().addLast(hubClientHandler);
            }
        });

        // NOTE this will wait till "something" loads the configuration and
        // posts a HubAddress to the eventBus
//        connect();

    }

    public Future<Void> connect() {
    	connect = true;
        return reconnect();
    }

    private Future<Void> reconnect() {

        if (connect && hubAddress.isEnabled() && future == null && channel == null) {
            future = b.connect(hubAddress.getHubInetAddress(), hubAddress.getHubPort());
//            future.channel().pipeline().fireUserEventTriggered(new HubConnectionLeapRestMessage(
//                    HubConnectionLeapRestMessage.HubConnectionLeapRestMessageType.CONNECTING,
//                    String.format("Connecting to %s:%d", hubAddress.getHubInetAddress(), hubAddress.getHubPort())));
            if (retryCount < 5)
            {
                log.info("Connecting to {}:{}", hubAddress.getHubInetAddress(), hubAddress.getHubPort());
            }
            else if (retryCount == 5)
            {
                log.info("Suppressing connection messages to {}:{}", hubAddress.getHubInetAddress(), hubAddress.getHubPort());
            }
            retryCount++;

            future.addListener((ChannelFutureListener) f -> {

                if (f.isSuccess()) {
                    retryCount = 0;
                    channel = f.channel();
                    channel.closeFuture().addListener((ChannelFutureListener) f1 -> {

                        channel = null;
                        future = null;
                        if (f1.isSuccess()) {
                            f1.channel().eventLoop().schedule((Runnable) this::reconnect, 5, TimeUnit.SECONDS);
                        }
                    });

                } else {

                    if (retryCount < 5)
                    {
                        log.info("Failed to connect: {}:{}", hubAddress.getHubInetAddress(), hubAddress.getHubPort(),f.cause());
                    }

//                    future.channel().pipeline()
//                            .fireUserEventTriggered(new HubConnectionLeapRestMessage(
//                                    HubConnectionLeapRestMessage.HubConnectionLeapRestMessageType.FAILED,
//                                    String.format("Failed to connect (%s)", f.cause().getMessage())));
                    channel = null;
                    future = null;

                    f.channel().eventLoop().schedule((Runnable) this::reconnect, 30, TimeUnit.SECONDS);
                }

            });
            return future;

        }

        if (future != null) {
            return future;
        }
        return new DefaultChannelPromise(null).setSuccess();
    }

    public synchronized void disconnect() {
        if (future != null) {
            future.cancel(true);
            future.syncUninterruptibly();
        }
        if (channel != null && channel.isActive()) {
            //channel.disconnect();
        	channel.flush();
        	channel.close();
        	
//            channel.closeFuture().syncUninterruptibly();
        }
        connect = false;
        workerGroup.shutdownGracefully();
    }

    public boolean isConnected() {
        return channel != null;
    }
}
