package com.shanyangcode.realtimeservice.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

// 处理器定位：这是 Pipeline 链上最后一级的"业务 Handler"，负责处理这条连接上进来的消息和连接生命周期事件。

// @Slf4j：Lombok 注解，贴在类上，编译期自动生成一个名为 log 的日志对象，直接写 log.error/info 即可打印带级别、带时间的日志，不用自己 new Logger
@Slf4j
// 泛型 <TextWebSocketFrame>：SimpleChannelInboundHandler 是个泛型父类，尖括号里指定"我只处理这一种消息类型"，这里即只处理文本帧（前端发的聊天文字）
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /** 每收到一个文本帧回调一次：text() 即前端发来的字符串内容，初版先直接打印 */
    // @Override：表示下面这个方法是重写（覆盖）父类/接口里已有的方法；编译器会帮你核对方法签名是否真的和父类一致，写错签名会报错，是给人看+给编译器校验用的标记
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {
        System.out.println(textWebSocketFrame.text());
    }

    /** Pipeline 中抛出异常时记录日志并关闭连接，避免异常沿栈扩散 */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught in channel pipeline", cause);
        ctx.close();
    }

    // 下面 channelActive/channelInactive/handlerAdded/handlerRemoved 上的 @Override 同上，略；它们都是 Netty 规定好的生命周期回调钩子
    /** 连接建立成功（active）回调，用于记录上线事件 */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("channel active");
    }

    /** 连接断开（inactive）回调，用于清理该连接对应的在线状态 */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("channel inActive");
    }

    /** Handler 被加入 Pipeline 时回调 */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        System.out.println("handler added");
    }

    /** Handler 从 Pipeline 移除时回调（连接关闭后触发） */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        System.out.println("handler removed");
    }

}