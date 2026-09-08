package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Post;
import org.example.entity.vo.request.PostCreateVO;
import org.example.entity.vo.response.PageVO;
import org.example.entity.vo.response.PostDetailVO;
import org.example.entity.vo.response.PostListItemVO;
import org.example.entity.vo.response.SearchCommentVO;
import org.example.entity.vo.request.PostUpdateVO;
import java.util.List;

public interface PostService extends IService<Post> {
    String createPost(Integer authorId, PostCreateVO vo);
    PageVO<PostListItemVO> listPosts(int page, int size, String sort);
    PostDetailVO getPostDetail(Integer id);
    String updatePost(Integer authorId, Integer postId, PostUpdateVO vo);
    String deletePost(Integer authorId, Integer postId);
    PageVO<PostListItemVO> listMyPosts(Integer authorId, int page, int size, String sort);
    PageVO<PostListItemVO> searchPosts(String keyword, int page, int size);
    PageVO<SearchCommentVO> searchComments(String keyword, int page, int size);
}
