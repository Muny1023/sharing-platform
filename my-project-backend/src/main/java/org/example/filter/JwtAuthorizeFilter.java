package org.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.utils.JwtUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizeFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtils utils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取 Authorization
        String authorization = request.getHeader("Authorization");
        DecodedJWT jwt = utils.resolveJwt(authorization);

        if (jwt != null) {
            // 从 JWT 中解析出 UserDetails
            UserDetails user = utils.toUser(jwt);

            // 创建 Spring Security 的认证令牌（包含用户信息和权限）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            // 记录请求的详细信息（如 IP、Session 等）
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 将认证信息放入 SecurityContext，表示当前用户已登录
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 把用户 ID 存入 request 属性，方便后续 Controller 通过 @RequestAttribute 获取
            request.setAttribute("id", utils.toId(jwt));
        }

        // 继续执行后续过滤器链
        filterChain.doFilter(request, response);
    }
}
