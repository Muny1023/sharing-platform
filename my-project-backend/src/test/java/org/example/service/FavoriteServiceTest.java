package org.example.service;

import org.example.entity.dto.Favorite;
import org.example.entity.dto.Post;
import org.example.mapper.FavoriteMapper;
import org.example.service.impl.FavoriteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteMapper mapper;
    @Mock PostService postService;
    @Spy @InjectMocks FavoriteServiceImpl service;

    @Test void toggle_rejectsMissingPost() {
        when(postService.getById(404)).thenReturn(null);
        doReturn(null).when(service).getOne(any());
        var result = service.toggle(1, 404);
        assertFalse(result.favorited());
        assertEquals("帖子不存在", result.message());
    }
}
