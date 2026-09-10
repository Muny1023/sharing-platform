package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.PostCreateVO;
import org.example.entity.vo.request.PostUpdateVO;
import org.example.entity.vo.response.SearchCommentVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostDetailVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.service.PostService;
import org.example.mapper.PostTombstoneMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/post")
public class PostController {

    @Resource
    PostService service;
    @Resource PostTombstoneMapper tombstoneMapper;

    @PostMapping
    public RestBean<Void> createPost(@RequestBody @Valid PostCreateVO vo,
                                     @RequestAttribute("id") Integer id) {
        return this.messageHandle(() -> service.createPost(id, vo));
    }

    @PutMapping("/{id}")
    public RestBean<Void> updatePost(@PathVariable Integer id, @RequestBody @Valid PostUpdateVO vo, @RequestAttribute("id") Integer authorId) { return this.messageHandle(() -> service.updatePost(authorId, id, vo)); }
    @DeleteMapping("/{id}")
    public RestBean<Void> deletePost(@PathVariable Integer id, @RequestAttribute("id") Integer authorId) { return this.messageHandle(() -> service.deletePost(authorId, id)); }
    @GetMapping("/mine")
    public RestBean<PageVO<PostListItemVO>> myPosts(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="latest") String sort,@RequestAttribute("id") Integer authorId){return RestBean.success(service.listMyPosts(authorId,page,size,sort));}
    @GetMapping("/search")
    public RestBean<PageVO<PostListItemVO>> searchPosts(@RequestParam String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){return RestBean.success(service.searchPosts(keyword,page,size));}
    @GetMapping("/search-comments")
    public RestBean<PageVO<SearchCommentVO>> searchComments(@RequestParam String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size){return RestBean.success(service.searchComments(keyword,page,size));}

    @GetMapping("/list")
    public RestBean<PageVO<PostListItemVO>> listPosts(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "latest") String sort) {
        return RestBean.success(service.listPosts(page, size, sort));
    }

    @GetMapping("/{id}")
    public RestBean<PostDetailVO> getPostDetail(@PathVariable Integer id) {
        PostDetailVO vo = service.getPostDetail(id);
        if (vo != null && vo.deleted()) return RestBean.failure(410, "该帖子已被作者删除");
        if (vo == null && tombstoneMapper.selectById(id) != null) return RestBean.failure(410, "该帖子已被作者删除");
        return vo == null ? RestBean.failure(404, "帖子不存在或已被删除") : RestBean.success(vo);
    }

    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
