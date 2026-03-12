package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SendMessageRequest;
import com.example.demo.model.ChatMessage;
import com.example.demo.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 發送訊息
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        try {
            ChatMessage message = chatService.sendMessage(
                    request.getSenderId(),
                    request.getReceiverId(),
                    request.getMessage());
            log.info("訊息已送出: senderId={}, receiverId={}", request.getSenderId(), request.getReceiverId());
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "發送訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得兩個用戶之間的聊天記錄
     * GET /api/chat/history?user1={userId1}&user2={userId2}
     */
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(
            @RequestParam("user1") Long userId1,
            @RequestParam("user2") Long userId2) {
        try {
            List<ChatMessage> messages = chatService.getChatHistory(userId1, userId2);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得聊天記錄失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶的未讀訊息
     * GET /api/chat/unread/{userId}
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<?> getUnreadMessages(@PathVariable Long userId) {
        try {
            List<ChatMessage> messages = chatService.getUnreadMessages(userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得未讀訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶的未讀訊息數量
     * GET /api/chat/unread-count/{userId}
     */
    @GetMapping("/unread-count/{userId}")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long userId) {
        try {
            Long count = chatService.getUnreadCount(userId);
            Map<String, Long> response = new HashMap<>();
            response.put("unreadCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得未讀訊息數量失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 標記訊息為已讀
     * PUT /api/chat/read/{messageId}
     */
    @PutMapping("/read/{messageId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long messageId) {
        try {
            boolean success = chatService.markAsRead(messageId);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "訊息已標記為已讀");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "找不到該訊息");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "標記已讀失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 標記與特定用戶的對話為已讀
     * PUT /api/chat/read-chat?receiverId={receiverId}&senderId={senderId}
     */
    @PutMapping("/read-chat")
    public ResponseEntity<?> markChatAsRead(
            @RequestParam Long receiverId,
            @RequestParam Long senderId) {
        try {
            chatService.markChatAsRead(receiverId, senderId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "對話已標記為已讀");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "標記對話已讀失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶發送的所有訊息
     * GET /api/chat/sent/{userId}
     */
    @GetMapping("/sent/{userId}")
    public ResponseEntity<?> getSentMessages(@PathVariable Long userId) {
        try {
            List<ChatMessage> messages = chatService.getSentMessages(userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得發送訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶接收的所有訊息
     * GET /api/chat/received/{userId}
     */
    @GetMapping("/received/{userId}")
    public ResponseEntity<?> getReceivedMessages(@PathVariable Long userId) {
        try {
            List<ChatMessage> messages = chatService.getReceivedMessages(userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得接收訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得用戶所有相關訊息
     * GET /api/chat/all/{userId}
     */
    @GetMapping("/all/{userId}")
    public ResponseEntity<?> getAllUserMessages(@PathVariable Long userId) {
        try {
            List<ChatMessage> messages = chatService.getAllUserMessages(userId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得用戶訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 刪除訊息
     * DELETE /api/chat/{messageId}
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        try {
            boolean success = chatService.deleteMessage(messageId);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "訊息已刪除");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "找不到該訊息");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "刪除訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 取得訊息詳情
     * GET /api/chat/message/{messageId}
     */
    @GetMapping("/message/{messageId}")
    public ResponseEntity<?> getMessageById(@PathVariable Long messageId) {
        try {
            return chatService.getMessageById(messageId)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        Map<String, String> error = new HashMap<>();
                        error.put("error", "找不到該訊息");
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    });
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "取得訊息失敗: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
