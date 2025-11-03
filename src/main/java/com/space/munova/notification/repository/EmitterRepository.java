package com.space.munova.notification.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class EmitterRepository {

    private final Map<Object, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(Object emitterId, SseEmitter emitter) {
        emitters.computeIfAbsent(emitterId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);
    }

    public List<SseEmitter> findEmitterById(Object emitterKey) {
        return Optional.ofNullable(emitters.get(emitterKey))
                .orElse(Collections.emptyList());
    }

    public void delete(Object emitterId, SseEmitter emitter) {
        emitters.computeIfPresent(emitterId, (key, list) -> {
            list.remove(emitter);
            if (list.isEmpty()) {
                return null;
            }
            return list;
        });
    }

}
