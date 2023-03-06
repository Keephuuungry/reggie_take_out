package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.mapper.UserMapper;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequestMapping("/user")
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码 对应前端页面/front/page/login.html中sendMsgApi函数
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession httpSession){
        // 1. 获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotBlank(phone)){
            // 2. 生成随机的四位验证码
            // 自定义的工具类ValidateCodeUtils
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code={}", code);
            // 3. 调用阿里云提供的短信服务API完成发送短信服务
            // 自定义的工具类SMSUtils
//            SMSUtils.sendMessage("瑞吉外卖", "SMS_271520576", phone, "1234");

            // 4. 优化前：将生成的验证码保存到Session中
//            httpSession.setAttribute(phone, code);
            // 优化：将生成的验证码缓存到Redis中，并设置有效期为5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
        }
        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param httpSession
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession httpSession){
        // 前端用户收到验证码 点击登录按钮后，需要验证用户提交的验证码是不是我们发的验证码。
        // 因为User对象中没有code属性，这里使用Map类来接收用户提交的phone和code属性
        log.info(map.toString());

        // 1. 获取手机号phone和验证码code
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();

        // 2. 优化前：从Session中获取保存的验证码（我们生成验证码之后存在Session中用于核验）
//        Object codeInSession = httpSession.getAttribute(phone);
        // 2. 优化：从Redis中获取缓存的验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);

        // 3. 进行验证码比对(页面提交的验证码和Session中保存的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){
            // 4. 判断当前手机号是否在数据库中，如果不在，则为新用户，需要向数据库中完成注册；如果在，则登录
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if(user == null){
                user = new User();
                user.setPhone(phone);
                user.setStatus(1); // 数据库中默认值为启用，这里可以不写，也可以显示的写出来
                userService.save(user);
            }
            // 登录成功，需要向Session中放"user"，否则会被过滤器拦截
            // 优化：如果用户登录成功，删除Redis中缓存的验证码
            httpSession.setAttribute("user", user.getId());
            redisTemplate.delete(phone);
            return R.success(user);
        }

        return R.error("登录失败");
    }
}
