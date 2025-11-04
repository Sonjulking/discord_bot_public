package com.kkc.discord_bot;

import com.kkc.discord_bot.entity.MusicList;
import com.kkc.discord_bot.repository.MusicListRepository;
import com.kkc.discord_bot.service.MusicListService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor
class DiscordBotApplicationTests {
    @Autowired
    private MusicListRepository musicListRepository;
    @Autowired
    private MusicListService musicListService;

    @Test
    void contextLoads() {
    }

    @Test
    void save() {
        MusicList m = new MusicList();
        m.setUrl("ggggg");
        musicListService.save(m);
    }

}
