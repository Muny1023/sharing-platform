package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.CommentCreateVO;
import org.example.entity.vo.response.CommentVO;
import org.example.service.CommentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Resource
    CommentService service;

    @PostMapping
    public RestBean<Void> createComment(@RequestBody @Valid CommentCreateVO vo,
                                        @RequestAttribute("id") Integer id) {
        return this.messageHandle(() -> service.createComment(id, vo));
    }

    @GetMapping("/list/{postId}")
    public RestBean<List<CommentVO>> listComments(@PathVariable Integer postId) {
        return RestBean.success(service.listCommentsByPost(postId));
    }

    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
