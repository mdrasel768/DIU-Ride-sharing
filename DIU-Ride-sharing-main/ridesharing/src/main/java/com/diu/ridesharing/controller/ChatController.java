package com.diu.ridesharing.controller;

import com.diu.ridesharing.dto.ChatMessageRequest;
import com.diu.ridesharing.entity.ChatMessage;
import com.diu.ridesharing.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{rideId}/send")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long rideId,
            @RequestBody ChatMessageRequest request
    ) {
        if (request.getSenderId() == null) {
            return ResponseEntity.badRequest().body("senderId is required");
        }

        if (request.getSenderRole() == null || request.getSenderRole().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("senderRole is required");
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("message is required");
        }

        ChatMessage msg = new ChatMessage();
        msg.setRideId(rideId);
        msg.setSenderId(request.getSenderId());
        msg.setSenderRole(request.getSenderRole().trim().toUpperCase());
        msg.setMessage(request.getMessage().trim());

        return ResponseEntity.ok(chatService.saveMessage(msg));
    }

    @GetMapping("/{rideId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long rideId) {
        return ResponseEntity.ok(chatService.getMessages(rideId));
    }
}