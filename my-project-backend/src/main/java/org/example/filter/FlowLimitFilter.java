package org.example.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RestBean;
import org.example.utils.Const;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitFilter extends HttpFilter {

    private static final String AUTH_PREFIX = "/api/auth/";
    static final int MAX_REQUESTS_PER_WINDOW = 10;

    @Resource
    StringRedisTemplate template;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 认证接口才需要防刷；帖子列表/详情等页面数据请求不参与计数
        if(!request.getRequestURI().startsWith(AUTH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String address = request.getRemoteAddr();
        if(this.tryCount(address)) {
            chain.doFilter(request, response);
        } else {
            this.writeBlockMessage(response);
        }
    }

    private void writeBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.forbidden("操作频繁，请稍后再试").asJsonString());
    }

    private boolean tryCount(String ip) {
        synchronized (ip.intern()) {
            if(Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_BLOCK + ip)))
                return false;
            return this.limitPeriodCheck(ip);
        }
    }

    private boolean limitPeriodCheck(String ip) {
        if (Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_COUNTER + ip))) {
            long increment = Optional.ofNullable(template.opsForValue().increment(Const.FLOW_LIMIT_COUNTER + ip)).orElse(0L);
            if(increment > MAX_REQUESTS_PER_WINDOW) {
                template.opsForValue().set(Const.FLOW_LIMIT_BLOCK + ip, "", 30, TimeUnit.SECONDS);
                return false;
            }
        } else {
            template.opsForValue().set(Const.FLOW_LIMIT_COUNTER + ip, "1", 3, TimeUnit.SECONDS);
        }
        return true;
    }
}
