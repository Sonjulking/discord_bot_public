// 생성됨 - 2025-10-30 02:13:50
package com.kkc.discord_bot.repository;

import com.kkc.discord_bot.entity.MusicList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MusicListRepository extends JpaRepository<MusicList, Long> {
    boolean existsByNameAndUrl(String name, String url);
}
