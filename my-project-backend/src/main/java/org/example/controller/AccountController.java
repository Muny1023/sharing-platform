package org.example.controller;

import jakarta.annotation.Resource;
import org.example.entity.RestBean;
import org.example.entity.vo.response.AccountVO;
import org.example.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    AccountService service;

    @GetMapping("/me")
    public RestBean<AccountVO> me(@RequestAttribute("id") Integer id) {
        return service.findAccountById(id)
                .map(account -> RestBean.success(new AccountVO(
                        account.getId(), account.getUsername(),
                        account.getNickname(), account.getAvatar(), account.getRole())))
                .orElseGet(() -> RestBean.failure(404, "账号不存在"));
    }
}
