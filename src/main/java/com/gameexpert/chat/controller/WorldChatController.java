package com.gameexpert.chat.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Builder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gameexpert.chat.dto.ChatMessageResponse;
import com.gameexpert.chat.service.RecentChatQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WorldChatController {

    private final RecentChatQueryService chatService;

    @GetMapping("/worlds/{worldId}/chats")
    public ResponseEntity<List<ChatMessageResponse>> chats(@PathVariable Long worldId, @RequestParam(defaultValue = "50") int limit) {

        return ResponseEntity.ok(chatService.getRecentMessages(worldId,limit));
    }
}
