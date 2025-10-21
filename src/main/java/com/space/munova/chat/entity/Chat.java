package com.space.munova.chat.entity;


import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChatStatus status;

    @Enumerated(EnumType.STRING)
    private ChatType type;

    private Integer max_participant;

    private Integer cur_participant;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    public Chat(String name, ChatStatus status, ChatType type) {
        this.name = name;
        this.status = status;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

}
