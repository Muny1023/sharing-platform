package org.example.controller;
import jakarta.annotation.Resource;
import org.example.entity.RestBean;
import org.example.entity.vo.response.FavoriteItemVO;
import org.example.entity.vo.response.PageVO;
import org.example.service.FavoriteService;
import org.example.entity.vo.request.FavoriteToggleVO;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/favorite")
public class FavoriteController {
    @Resource FavoriteService service;
    @PostMapping("/toggle") public RestBean<Map<String,Object>> toggle(@RequestBody FavoriteToggleVO vo, @RequestAttribute("id") Integer userId) { var r=service.toggle(userId,vo.getPostId()); return r.message()==null?RestBean.success(Map.of("favorited",r.favorited())):RestBean.failure(400,r.message()); }
    @GetMapping("/status") public RestBean<Boolean> status(@RequestParam Integer postId,@RequestAttribute("id") Integer userId){return RestBean.success(service.isFavorited(userId,postId));}
    @GetMapping("/list") public RestBean<PageVO<FavoriteItemVO>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="latest") String sort,@RequestAttribute("id") Integer userId){return RestBean.success(service.listMine(userId,page,size,sort));}
}
