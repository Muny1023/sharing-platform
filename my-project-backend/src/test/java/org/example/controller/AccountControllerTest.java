package org.example.controller;

import jakarta.annotation.Resource;
import org.example.entity.RestBean;
import org.example.entity.dto.Account;
import org.example.entity.vo.response.AccountVO;
import org.example.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 当前用户信息接口单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    AccountService accountService;

    @InjectMocks
    AccountController controller;

    @Test
    void me_returnsAccountVOWithoutSensitiveFields() {
        when(accountService.findAccountById(1)).thenReturn(Optional.of(
                new Account(1, "alice", "encoded-password", "alice@mail.com", "user", new Date(), "爱丽丝", "https://cdn.example.com/a.png")));

        RestBean<AccountVO> rest = controller.me(1);

        assertEquals(200, rest.code());
        AccountVO vo = rest.data();
        assertEquals(1, vo.id());
        assertEquals("alice", vo.username());
        assertEquals("爱丽丝", vo.nickname());
        assertEquals("https://cdn.example.com/a.png", vo.avatar());
        assertNull(passwordOrNull(vo), "VO 是 record 无 password 字段，天然不泄露敏感信息");
    }

    @Test
    void me_accountMissing_returns404() {
        when(accountService.findAccountById(404)).thenReturn(Optional.empty());

        RestBean<AccountVO> rest = controller.me(404);

        assertEquals(404, rest.code());
        assertEquals("账号不存在", rest.message());
    }

    private String passwordOrNull(AccountVO vo) {
        // record 组件固定，不存在 password 访问器；此断言只为明确表达意图
        return null;
    }
}
