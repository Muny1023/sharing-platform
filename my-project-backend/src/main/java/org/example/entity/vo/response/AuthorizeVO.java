package org.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class AuthorizeVO {
    String username;
    String roles;
    String token;
    Date expire;
}
