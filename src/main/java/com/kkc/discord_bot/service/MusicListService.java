// 생성됨 - 2025-10-30 02:13:50
package com.kkc.discord_bot.service;

import com.kkc.discord_bot.entity.MusicList;
import com.kkc.discord_bot.repository.MusicListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MusicListService {
    private final MusicListRepository musicListRepository;

    public boolean save(MusicList musicList) {
        if (musicListRepository.existsByNameAndUrl(musicList.getName(), musicList.getUrl())) {
            log.info("Skipping duplicate music entry: {} - {}", musicList.getName(), musicList.getUrl());
            return false;
        }
        musicListRepository.save(musicList);
        return true;
    }

    public List<MusicList> findAll() {
        return musicListRepository.findAll();
    }

    // 이름으로 필터링하여 조회
    public List<MusicList> findByName(String name) {
        return musicListRepository.findAll().stream()
                                  .filter(music -> name.equals(music.getName()))
                                  .collect(Collectors.toList());
    }

    // 재생목록 이름 목록 조회 (중복 제거)
    public List<String> findAllPlaylistNames() {
        return musicListRepository.findAll().stream()
                                  .map(MusicList::getName)
                                  .filter(name -> name != null && !name.isEmpty())
                                  .distinct()
                                  .sorted()
                                  .collect(Collectors.toList());
    }

    // ID로 삭제
    public void deleteById(Long id) {
        musicListRepository.deleteById(id);
        log.info("Music deleted by id: {}", id);
    }

    // 이름으로 모두 삭제
    public int deleteByName(String name) {
        List<MusicList> musicListsToDelete = findByName(name);
        int count = musicListsToDelete.size();
        musicListsToDelete.forEach(musicList -> musicListRepository.deleteById(musicList.getId()));
        log.info("Deleted {} music entries with name: {}", count, name);
        return count;
    }
}
