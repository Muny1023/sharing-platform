package org.example.entity.vo.response;

public record AccountVO(
        Integer id,
        String username,
        String nickname,
        String avatar,
        String role) {
}
