package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Comment;
import org.example.entity.vo.request.CommentCreateVO;
import org.example.entity.vo.response.CommentVO;

import java.util.List;
import org.example.entity.dto.Comment;

public interface CommentService extends IService<Comment> {
    String createComment(Integer authorId, CommentCreateVO vo);
    List<CommentVO> listCommentsByPost(Integer postId);
    List<Comment> listActiveCommentsByPost(Integer postId);
}
