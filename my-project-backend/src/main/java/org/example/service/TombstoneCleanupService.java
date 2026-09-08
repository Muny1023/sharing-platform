package org.example.service;

import jakarta.annotation.Resource;
import org.example.entity.dto.PostTombstone;
import org.example.mapper.FavoriteMapper;
import org.example.mapper.PostTombstoneMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TombstoneCleanupService {
    @Resource PostTombstoneMapper tombstoneMapper;
    @Resource FavoriteMapper favoriteMapper;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpired() {
        for (PostTombstone tombstone : tombstoneMapper.selectExpired()) {
            favoriteMapper.deleteExpiredInvalidFavorites();
            tombstoneMapper.deleteByPostId(tombstone.getPostId());
        }
    }
}
