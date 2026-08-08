package com.diu.ridesharing.service;

import com.diu.ridesharing.entity.ChatMessage;
import com.diu.ridesharing.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatRepo;

    public ChatMessage saveMessage(ChatMessage msg) {
        if (msg.getSentAt() == null) {
            msg.setSentAt(LocalDateTime.now());
        }
        return chatRepo.save(msg);
    }

    public List<ChatMessage> getMessages(Long rideId) {
        return chatRepo.findByRideIdOrderBySentAtAsc(rideId);
    }
}