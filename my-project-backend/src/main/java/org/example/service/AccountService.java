package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.ConfirmResetVO;
import org.example.entity.vo.request.EmailRegisterVO;
import org.example.entity.vo.request.EmailResetVO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface AccountService extends IService<Account> , UserDetailsService {
    Account findAccountByNameOrEmail(String text);
    Optional<Account> findAccountById(Integer id);
    String registerEmailVerifyCode(String type, String email, String ip);
    String registerEmailAccount(EmailRegisterVO vo);
    String resetConfirm(ConfirmResetVO vo);
    String resetEmailAccountPassword(EmailResetVO vo);
}
