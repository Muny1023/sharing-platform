package org.example.controller;

import jakarta.annotation.Resource;
import org.example.entity.RestBean;
import org.example.entity.vo.response.NotificationPageVO;
import org.example.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Resource NotificationService service;

    @GetMapping("/unread-count")
    public RestBean<Long> unreadCount(@RequestAttribute("id") Integer id) { return RestBean.success(service.unreadCount(id)); }
    @GetMapping
    public RestBean<NotificationPageVO> page(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int size, @RequestParam(defaultValue="all") String filter, @RequestAttribute("id") Integer id) { return RestBean.success(service.page(id, page, size, filter)); }
    @PutMapping("/{notificationId}/read")
    public RestBean<Void> read(@PathVariable Integer notificationId, @RequestAttribute("id") Integer id) { return handle(() -> service.markRead(id, notificationId)); }
    @PutMapping("/read-all")
    public RestBean<Long> readAll(@RequestAttribute("id") Integer id) { return RestBean.success(service.markAllRead(id)); }
    @DeleteMapping("/{notificationId}")
    public RestBean<Void> delete(@PathVariable Integer notificationId, @RequestAttribute("id") Integer id) { return handle(() -> service.deleteOne(id, notificationId)); }
    @DeleteMapping("/read")
    public RestBean<Long> deleteRead(@RequestAttribute("id") Integer id) { return RestBean.success(service.deleteRead(id)); }
    private RestBean<Void> handle(java.util.function.Supplier<String> action) { String message = action.get(); return message == null ? RestBean.success() : RestBean.failure(404, message); }
}
