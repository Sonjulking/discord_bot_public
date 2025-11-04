// 생성됨 - 2025-10-30 02:13:50
package com.kkc.discord_bot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "music_list")
@Data
public class MusicList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = true, length = 255)
    private String title;

    @Lob
    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = true, length = 255)
    private String name;

    @Column(name = "author", nullable = true, length = 255)
    private String author;
}
