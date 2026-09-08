package org.example.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.example.utils.Const;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowLimitFilterTest {

    @Mock
    StringRedisTemplate template;

    @InjectMocks
    FlowLimitFilter filter;

    @Test
    void skipsCommunityDataRequestsAndLimitsOnlyAuthRequests() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        AtomicLong count = new AtomicLong();
        when(template.opsForValue()).thenReturn(operations);
        when(template.hasKey(contains(Const.FLOW_LIMIT_COUNTER)))
                .thenAnswer(invocation -> count.get() > 0);
        when(template.hasKey(contains(Const.FLOW_LIMIT_BLOCK))).thenReturn(false);
        when(operations.increment(contains(Const.FLOW_LIMIT_COUNTER)))
                .thenAnswer(invocation -> count.incrementAndGet());
        doAnswer(invocation -> {
            count.set(1);
            return null;
        }).when(operations).set(contains(Const.FLOW_LIMIT_COUNTER), anyString(), anyLong(), any(TimeUnit.class));

        var communityRequest = new MockHttpServletRequest("GET", "/api/post/list");
        communityRequest.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(communityRequest, response, chain);

        assertEquals(0, count.get(), "首页/详情等普通接口不应参与限流计数");

        var authRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        authRequest.setRemoteAddr("127.0.0.1");
        for (int i = 0; i < FlowLimitFilter.MAX_REQUESTS_PER_WINDOW; i++) {
            filter.doFilter(authRequest, response, chain);
        }

        assertEquals(200, response.getStatus(), "窗口内认证请求应正常放行");
        filter.doFilter(authRequest, response, chain);
        assertEquals(403, response.getStatus(), "超过窗口上限后应返回 403");
        verify(chain, times(1 + FlowLimitFilter.MAX_REQUESTS_PER_WINDOW))
                .doFilter(any(), any());
    }
}
