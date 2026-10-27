package com.shanyangcode.realtimeservice.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.NettyRuntime;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

import javax.swing.*;

// 概念小卡片：HTTP vs WebSocket。普通 HTTP 是"一问一答、短连接"：浏览器发一次请求，服务器回一次就断开，服务器无法主动给浏览器推消息。
// WebSocket 是"长连接"：握手成功后这条 TCP 通道一直保持，服务器和前端随时都能双向主动发消息（聊天、在线状态实时推送就靠它）。
// 概念小卡片：Netty 是什么？一个高性能 NIO 网络框架，自己写 socket 编程很繁琐，Netty 把"监听端口、接收连接、读写、编解码"封装好。
// bossGroup / workerGroup 分工：bossGroup（老板组）只干一件事——监听端口、Accept 新来的连接，所以 1 个线程就够；workerGroup（工人组）负责把已建立连接上的读写事件分给多个线程处理，线程数通常取 CPU 核数2，两组分开避免"接新连接"堵住"收发消息"。
// Pipeline（责任链）：每个新连接都会挂一条处理链，消息按 addLast 的顺序一个一个 Handler 往后传，前一个做完交给后一个，就像流水线。
// * Netty WebSocket 服务启动类（初版）。
//        * 通过 @Configuration 随 Spring 容器启动，@PostConstruct 时绑定端口，@PreDestroy 时优雅关闭线程组。
// @Configuration：贴在类上，表示这是一个"配置类"，Spring 容器启动时会扫描它、把它纳入管理；这里借容器启动时机去拉起 Netty 服务
@Configuration
public class NettyService {


    // 长连接监听端口（独立于 Spring Boot 的 HTTP 端口）
    private final int port = 9101;


    // bossGroup：只负责 Accept 新连接，1 个线程专门监听端口即可
    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);


    // workerGroup：负责已建连通道的读写事件，线程数取 CPU 核数2，与 bossGroup 分工避免相互阻塞
    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup(NettyRuntime.availableProcessors() * 2);


    // @PostConstruct：生命周期注解，贴在方法上，Spring 把当前 Bean 构造好之后会自动调用这个方法一次；这里借此在容器启动后顺手把 Netty 端口绑上
    @PostConstruct
    public void start() throws InterruptedException {
        run();
    }


    public void run() throws InterruptedException {

        // ServerBootstrap 是 Netty 启动服务的"装配器"，下面链式调用一步步配好再启动
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        // group(老板组, 工人组)：指定两组线程；channel(...) 指定用 NIO 类型的服务端通道；childHandler(...) 给每个新接入的子连接初始化处理链
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // ChannelInitializer<SocketChannel> 是一个匿名内部类：泛型 <SocketChannel> 表示它初始化的是"客户端连接通道"；每来一个新连接就回调一次 initChannel
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) {

                        // 每个新连接都走这条 Pipeline，按 addLast 顺序逐级编解码与处理
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        channelPipeline.addLast(new HttpServerCodec());            // HTTP 编解码：解析请求/响应行、头、体
                        channelPipeline.addLast(new HttpObjectAggregator(65536));  // 把分段的 HTTP 消息聚合为完整 FullHttpRequest，上限 64KB
                        channelPipeline.addLast(new WebSocketServerProtocolHandler("/ws/netty")); // 完成 WebSocket 握手升级，路径 /ws/netty
                        channelPipeline.addLast(new WebSocketHandler());          // 业务处理器：处理文本帧、连接生命周期回调
                    }
                });

        // bind(port) 绑定 9101 端口开始监听；.sync() 同步等待绑定成功（阻塞到端口就绪），启动失败会在这里抛异常
        serverBootstrap.bind(port).sync();
    }


    // @PreDestroy：生命周期注解，贴在方法上，Spring 容器关闭（程序正常退出）前自动调用一次，用于释放资源；这里优雅关闭两组线程
    // 容器关闭前优雅停机：先停止接收新连接，再等待任务完成后释放 boss/worker 线程组
    @PreDestroy
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}