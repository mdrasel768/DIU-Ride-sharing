package com.diu.ridesharing.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private Long senderId;
    private String senderRole;
    private String message;
}