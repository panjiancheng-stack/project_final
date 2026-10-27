package com.shanyangcode.common.utils;


import cn.hutool.jwt.Claims;
import com.shanyangcode.common.constant.CommonConstant;
import io.jsonwebtoken.;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// JWT工具类（优化点：完善异常日志、统一过期时间配置）
// 工具类定位：JwtUtil 是 Common 里的纯静态工具类（final 禁止被继承，方法全 static，不用 new），专门负责"签发 JWT"和"解析/验签 JWT"两件事，供所有服务调用。
@Slf4j
public final class JwtUtil {

    // 生成JWT（支持自定义过期时间）
// 生成 JWT：入参 userId 写进载荷、timeout+unit 决定有效期；返回拼好的三段式 token 字符串
    public static String generate(String userId, long timeout, TimeUnit unit) {
        Date now = new Date(); // 当前时间作为签发时间
        //设置过期时间：当前毫秒数 + 把 timeout 按 unit 换算成的毫秒数，得到到期时刻
        Date expiration = new Date(now.getTime() + unit.toMillis(timeout));
        System.out.println(expiration);
        // 链式调用 Jwts.builder()：依次填充载荷、时间，最后 signWith 签名、compact() 压缩成最终字符串
        return Jwts.builder()
                .setSubject(userId) // 存储用户唯一标识（如手机号/用户ID）
                .setIssuedAt(now) // 签发时间
                .setExpiration(expiration) // JWT自身过期时间长一点
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();
    }

    // 获取签名密钥（从常量类读取，避免硬编码）
// 从常量类取出密钥字符串，先 Base64 解码成字节，再交给 Keys 工具生成 HS256 算法要求长度的签名密钥对象
    public static Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(CommonConstant.TOKEN_SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 解析JWT（区分不同异常类型，便于排查）
// 解析 JWT：入参 token，成功返回载荷 Claims（可从中取 userId），任何失败都返回 null，由调用方按"未登录/失效"处理
    public static Claims parse(String token) {
        // 分支一：token 为空直接记 warn 并返回 null，不进入验签
        if (StringUtils.isEmpty(token)) {
            log.warn("解析JWT失败：token 为空");
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) { // 单独捕获“已过期”：与签名非法区分，供上层决定是否走刷新流程
            log.warn("JWT 已过期：{}", e.getMessage());
            return null;
        } catch (JwtException e) { // 签名无效/格式错误等其他 JWT 异常
            log.error("JWT 签名无效或格式错误：{}", e.getMessage());
            return null;
        } catch (Exception e) { // 兜底未知异常，记录后返回 null 由调用方按未登录处理
            log.error("JWT 解析发生未知错误：", e);
            return null;
        }
    }


}