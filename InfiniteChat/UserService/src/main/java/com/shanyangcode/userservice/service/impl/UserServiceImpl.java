import cn.hutool.jwt.Claims;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.userservice.common.ErrorCode;
import com.shanyangcode.userservice.exception.ThrowUtils;
import com.shanyangcode.userservice.model.vo.LoginAndRegisterResponse;
import com.shanyangcode.userservice.model.vo.TokenResponse;

import com.shanyangcode.userservice.model.vo.TokenResponse;
//* 登录/注册成功后签发双 Token：Access Token（短期访问）+ Refresh Token（长期刷新），
//        * 并把两份 Token 各写一份到 Redis（带过期时间），服务端以此做校验与踢下线。
// Service 业务层方法：入参是登录/注册已经组装好的响应对象，返回塞好双 Token 的同一个对象
public LoginAndRegisterResponse createJwt(LoginAndRegisterResponse loginAndRegisterResponse) {
    // getUserId() 取出用户 ID（Long 型），toString() 转成字符串，因为 JWT 载荷里 subject 是字符串
    String userId = loginAndRegisterResponse.getUserId().toString();
    // 用同一 userId 分别签发短期 Access Token 与长期 Refresh Token
    String accessToken = com.shanyangcode.common.utils.JwtUtil.generate(userId, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
    String refreshToken = com.shanyangcode.common.utils.JwtUtil.generate(userId, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
    loginAndRegisterResponse.setAccessToken(accessToken);
    loginAndRegisterResponse.setRefreshToken(refreshToken);
    // Redis 双写：key=access:token:{userId}，value=accessToken，过期时间与 JWT 一致
    stringRedisTemplate.opsForValue().set(CommonConstant.ACCESS_TOKEN_PREFIX + userId, accessToken, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
    // Redis 双写：key=refresh:token:{userId}，value=refreshToken，用于刷新与单设备登录校验
    stringRedisTemplate.opsForValue().set(CommonConstant.REFRESH_TOKEN_PREFIX + userId, refreshToken, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
    return loginAndRegisterResponse;
}

/** 登出：删除 Redis 中该用户的双 Token，使旧凭证立即失效（JWT 无状态，撤销依赖 Redis 删除） */
@Override
public boolean logout(String userId) {
    stringRedisTemplate.delete(CommonConstant.ACCESS_TOKEN_PREFIX + userId);
    stringRedisTemplate.delete(CommonConstant.REFRESH_TOKEN_PREFIX + userId);
    return true;
}
//
//
//     * Access Token 过期后用 Refresh Token 换新对 Token：
//        * 先解析+比对 Redis 中保存的 Refresh Token，一致才签发新对并覆盖旧值（Token 轮换，旧 Refresh 随之作废，实现单设备登录）。
@Override
public TokenResponse refreshToken(String refreshToken) {
    // 1. 解析传入的 Refresh Token
    Claims claims = com.shanyangcode.common.utils.JwtUtil.parse(refreshToken);
    // ThrowUtils.throwIf(条件为真就抛异常, 错误码, 提示语)：相当于一行写完"if(claims==null) throw new BusinessException(...)"，这里解析失败即抛 TOKEN_INVALID 给前端
    ThrowUtils.throwIf(claims == null, ErrorCode.TOKEN_INVALID, "凭证已失效，请重新登录");


    // 2. 从载荷中安全获取 userId
    String userId = claims.getSubject();

    // 3. 校验 Redis，防止 Token 撤销攻击（实现单设备登录的关键）
    String redisRefreshToken = stringRedisTemplate.opsForValue().get(CommonConstant.REFRESH_TOKEN_PREFIX + userId);
    ThrowUtils.throwIf(!refreshToken.equals(redisRefreshToken), ErrorCode.TOKEN_INVALID, "凭证已过期或在其他地方登录");


    // 4. 生成新的一对 Token（轮换：旧 Refresh Token 随后被覆盖作废，防止被窃取后长期复用）
    String newAccessToken = com.shanyangcode.common.utils.JwtUtil.generate(userId, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
    String newRefreshToken = com.shanyangcode.common.utils.JwtUtil.generate(userId, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);

    // 5. 覆盖 Redis 中两份 Token，旧值立即失效
    stringRedisTemplate.opsForValue().set(CommonConstant.ACCESS_TOKEN_PREFIX + userId, newAccessToken, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
    stringRedisTemplate.opsForValue().set(CommonConstant.REFRESH_TOKEN_PREFIX + userId, newRefreshToken, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
    // builder() 链式构造出 TokenResponse 对象：先 builder() 拿建造者，一连串 .xxx(值) 填字段，最后 build() 生成不可变对象返回
    return TokenResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
}