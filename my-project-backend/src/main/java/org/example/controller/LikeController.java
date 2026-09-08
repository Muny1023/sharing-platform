package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.LikeToggleVO;
import org.example.service.LikeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/like")
public class LikeController {

    @Resource
    LikeService service;

    @PostMapping("/toggle")
    public RestBean<Map<String, Object>> toggleLike(@RequestBody @Valid LikeToggleVO vo,
                                                    @RequestAttribute("id") Integer id) {
        LikeService.LikeToggleResult result = service.toggleLike(id, vo);
        if (result.message() != null) {
            return RestBean.failure(400, result.message());
        }
        return RestBean.success(Map.of("liked", result.liked()));
    }

    /**
     * 批量查询当前用户已点赞的目标 id：targetType = post / comment，targetIds 逗号分隔。
     */
    @GetMapping("/status")
    public RestBean<Set<Integer>> likedStatus(@RequestParam String targetType,
                                              @RequestParam List<Integer> targetIds,
                                              @RequestAttribute("id") Integer id) {
        if (!"post".equals(targetType) && !"comment".equals(targetType)) {
            return RestBean.failure(400, "目标类型只能是 post 或 comment");
        }
        return RestBean.success(service.findLikedTargetIds(id, targetType, targetIds));
    }
}
