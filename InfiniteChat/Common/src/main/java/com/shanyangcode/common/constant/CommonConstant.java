package com.shanyangcode.common.constant;

import cn.hutool.jwt.JWT;

import java.util.concurrent.TimeUnit;

// 定位：Common 模块里的常量类（不属于任何分层，就是"全局常数仓库"），把全项目共用的固定值集中放这里，避免到处写魔法数字/字符串，改一处即可全局生效。
// static final 含义：static 表示属于类、不用 new 对象就能直接用 类名.常量；final 表示赋值后不可再改（真正的常量）。
// * 公共常量：
//JWT 密钥、双 Token 过期时间与 Redis Key 前缀，供所有服务共用。
public class CommonConstant {

    // JWT HS256 签名密钥（需满足 HMAC 最小长度要求，正式环境应改为配置/密钥管理服务）
    public static final String TOKEN_SECRET_KEY = "shanyangcodebackendInfiniteChatshanyangcodebackendInfiniteChat";

    // Access Token 过期时长（配合 ACCESS_TOKEN_UNIT=分钟，30 分钟，短期有效）
    public static final Integer ACCESS_TOKEN_EXPIRE_TIME =  30;

    // Refresh Token 过期时长（配合 REFRESH_TOKEN_UNIT=天，7 天，用于刷新续期）
    public static final Integer REFRESH_TOKEN_EXPIRE_TIME =  7;

    // Redis 中 Access Token 的 Key 前缀：access:token:{userId}
    public static final String ACCESS_TOKEN_PREFIX = "access:token:";

    // Redis 中 Refresh Token 的 Key 前缀：refresh:token:{userId}
    public static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    // TimeUnit 是时间单位枚举（MINUTES=分钟、DAYS=天），和上面的数字搭配，告诉 Redis/JWT "30 是 30 分钟"而不是 30 秒
    public static final TimeUnit ACCESS_TOKEN_UNIT = TimeUnit.MINUTES; // 分钟

    public static final TimeUnit REFRESH_TOKEN_UNIT = TimeUnit.DAYS; // 天
}