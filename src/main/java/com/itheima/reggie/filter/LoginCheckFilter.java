package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经登录，如果没有登录则跳转到登录界面
 * 这里使用的是过滤器，当然可以使用SpringMVC的拦截器
 * (springboot实现，加配置类AdminWebConfig implements WebMvcConfigurer，拦截器实现HandlerInceptor）
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    // 路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        /*
            过滤器处理逻辑：
            1. 获取本次请求的URI
            2. 判断本次请求是否需要处理
            3. 如果不需要处理，直接放行
            4. 判断登录状态，如果已经登录，则直接放行
            5. 如果未登录则返回未登录结果
         */

        // 1. 获取本次请求的URI
        String requestURI = request.getRequestURI();
        log.info("拦截请求:{}",requestURI);

        // 不需要拦截的路径集合
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                // 静态资源里面没有数据，需要拦截的是那些请求数据的Controller
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg", // 移动端发送短信
                "/user/login", // 移动端登录
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };

        // 2. 判断本次请求是否需要处理
        boolean check = check(urls, requestURI);
        // 3. 如果不需要处理，直接放行
        if(check){
            log.info("本次请求不需要处理:{}",requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        // 4-1. 判断登录状态，如果已经登录，则直接放行
        if(request.getSession().getAttribute("employee") != null){
            Long empId = (Long) request.getSession().getAttribute("employee");
            log.info("用户已登录，用户id为:{}", empId);
            // 往线程中放empId
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);
            return;
        }

        // 4-2. 判断登录状态（移动端），如果已经登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            Long userId = (Long) request.getSession().getAttribute("user");
            log.info("用户已登录，用户id为:{}", userId);
            // 往线程中放empId
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        // 5. 如果未登录则返回未登录结果,通过输出流的方式向客户端响应数据
        // /backend/js/request.js，每个前端页面都有引入该js文件
        // 该文件的作用是拦截服务器端发过来的请求，如果res.data.code == 0 or res.data.msg == 'NOTLOGIN' 则返回登录界面
        // JSON.toJSONString:将对象转换为JSON字符串
        log.info("用户未登录");
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return ;
    }


    /**
     * 路径匹配，确认本次路径是否需要放行
     * @param urls
     * @param requestURI
     * @return true:放行  false:不放行
     */
    public boolean check(String[] urls, String requestURI){
        boolean isMatch = false;
        for (String url : urls) {
            // 判断路径是否在放行名单中
            isMatch = PATH_MATCHER.match(url, requestURI);
            if(isMatch){
                return true;
            }
        }
        return false;
    }
}
