package com.rjginc.copilot.copilot_simulator;

import java.util.HashMap;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rjginc.esm.connection.HubConnection;
import com.rjginc.interop.messages.InteropMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author matt_de This can be sharable with the restrictions that there can
 *         only be one connection to a single hub
 */

@Component()
@Scope("prototype")
public class HubClientHandler extends SimpleChannelInboundHandler<Object> implements HubConnection {
    private static final Logger log = LoggerFactory.getLogger(HubClientHandler.class);
    
    private static final ByteBuf KEEP_ALIVE = Unpooled.unreleasableBuffer( Unpooled.wrappedBuffer(new byte[]{}));

    ChannelHandlerContext ctx;
  
    Map<String, Object> properties = new HashMap<String, Object>();
    
    
    SimulatedCopilot simulatedCopilot;
    
    public void setSimulatedCopilot(SimulatedCopilot simulatedCopilot)
	{
		this.simulatedCopilot = simulatedCopilot;
	}

	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
        
        simulatedCopilot.onConnect(this);
//        properties.put("connected",HubConnectionLeapRestMessageType.CONNECTED );
//        properties.put("activated",HubConnectionLeapRestMessageType.NOT_ACTIVATED );
//        properties.put("address", ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress() );
//        properties.put("port",((InetSocketAddress) ctx.channel().remoteAddress()).getPort() );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        simulatedCopilot.onDisconnect();
//        properties.put("connected",HubConnectionLeapRestMessageType.DISCONNECTED );

    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        
        simulatedCopilot.onWritabilityChanged(ctx.channel().isWritable());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.warn("Netty exception", cause);
        // super.exceptionCaught(ctx, cause);

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        
        if (evt instanceof IdleStateEvent)
        {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                log.trace("Send Keep Alive Packet");
                ctx.writeAndFlush(KEEP_ALIVE);
            }
            return;
        }
        
//        if (evt instanceof HubConnectionLeapRestMessage)
//        {
//            properties.put("connected",((HubConnectionLeapRestMessage) evt).getEventType() );
//        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof InteropMessage)
        {
            ((InteropMessage) msg).setConnection(this);
            simulatedCopilot.onMessage((InteropMessage) msg);
        }
    }

    @Override
    public void disconnect() {
        if (ctx != null && ctx.channel().isOpen()) {
            ctx.disconnect();
        }
    }

    @Override
    public boolean isWritable() {
        if (ctx != null)
            return ctx.channel().isWritable();
        return false;
    }

    @Override
    public boolean isConnected() {
        if (ctx != null)
            return ctx.channel().isActive();
        return false;
    }

    @Override
    public void writeAndFlush(Object message) {
        if (ctx != null) {
            ctx.writeAndFlush(message);
        }
    }

    @Override
    public void close() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Override
    public void flush() {
        if (ctx != null) {
            ctx.flush();
        }
    }

    @Override
    public void write(Object message) {
        if (ctx != null) {
            ctx.write(message);
        }
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

	@Override
	public void writeAndFlush(Object message, int protcolRequired)
	{
		if (ctx != null) {
            ctx.writeAndFlush(message);
        }
	}

	@Override
	public void write(Object message, int protcolRequired)
	{
		if (ctx != null) {
            ctx.write(message);
        }
	}
}
