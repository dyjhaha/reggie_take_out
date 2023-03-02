package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Resource
    private JavaMailSender javaMailSender;//我们需要用这个进行邮件发送

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(HttpSession session, @RequestBody User user){
        String email = user.getPhone();
        String subject = "瑞吉外卖";
        //StringUtils.isNotEmpty字符串非空判断
        if (StringUtils.isNotEmpty(email)) {
            //发送一个四位数的验证码,把验证码变成String类型
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            String text = "【瑞吉外卖】您好，您的登录验证码为：" + code + "，请尽快登录";
            log.info("验证码为：" + code);
            //发送短信
            userService.sendMsg(email,subject,text);
            //将验证码保存到session当中
            //session.setAttribute(email,code);

            //将生成的验证码缓存到redis中，并且来设置有效期为5分钟

            redisTemplate.opsForValue().set(email,code,5, TimeUnit.MINUTES);
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送异常，请重新发送");
    }
    //登录
    @PostMapping("/login")
    //Map存JSON数据
    public R<User> login(HttpSession session,@RequestBody Map map){
        //获取邮箱，用户输入的
        String phone = map.get("phone").toString();
        //获取验证码，用户输入的
        String code = map.get("code").toString();
        //获取session中保存的验证码
       // Object sessionCode = session.getAttribute(phone);
        //从redis中获取缓存验证码
        Object sessionCode =  redisTemplate.opsForValue().get(phone);
        //如果session的验证码和用户输入的验证码进行比对,&&同时
        if (sessionCode != null && sessionCode.equals(code)) {
            //要是User数据库没有这个邮箱则自动注册,先看看输入的邮箱是否存在数据库
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);
            //获得唯一的用户，因为手机号是唯一的
            User user = userService.getOne(queryWrapper);
            //要是User数据库没有这个邮箱则自动注册
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                //取邮箱的前五位为用户名
                user.setName(phone.substring(0,6));
                userService.save(user);
            }
            //不保存这个用户名就登不上去，因为过滤器需要得到这个user才能放行，程序才知道你登录了
            session.setAttribute("user", user.getId());
            //如果用户登录成功，删除Redis中缓存的验证码
            redisTemplate.delete(phone);
            return R.success(user);
        }
        return R.error("登录失败");
    }
}
