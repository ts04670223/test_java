package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.dto.AiChatRequest;
import com.example.demo.dto.AiChatResponse;
import com.example.demo.model.Product;
import com.example.demo.service.LlmService;
import com.example.demo.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 本地 LLM（Ollama）API
 * 基礎路由：/api/ai
 */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 助手", description = "本地 LLM（qwen2.5:0.5b via Ollama）相關 API")
public class AiController {

    private final LlmService llmService;
    private final ProductService productService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    @Value("${ollama.model:qwen2.5:0.5b}")
    private String ollamaModel;

    public AiController(LlmService llmService, ProductService productService) {
        this.llmService = llmService;
        this.productService = productService;
    }

    /**
     * 通用購物助手對話（同步）
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    @Operation(summary = "AI 購物助手", description = "與本地 LLM 進行購物相關對話，支援歷史記錄")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            String reply = llmService.chat(
                    request.getMessage(),
                    request.getHistory(),
                    request.getSystemPrompt());
            return ResponseEntity.ok(AiChatResponse.ok(reply, ollamaModel));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(AiChatResponse.fail(e.getMessage()));
        }
    }

    /**
     * 通用購物助手對話（Streaming / SSE）
     * POST /api/ai/chat/stream
     *
     * <p>前端使用 EventSource 或 fetch + ReadableStream 接收。
     * 第一個 token 約 10-15 秒即可顯示，體感待機時間大幅縮短。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 購物助手（Streaming）",
               description = "SSE 即時串流回傳，處理中 CPU 慣境下體感時間大幅縮短")
    public SseEmitter chatStream(@RequestBody AiChatRequest request) {
        // 300 秒 timeout（大於 num_predict=20 的預期 74 秒）
        SseEmitter emitter = new SseEmitter(300_000L);
        streamExecutor.execute(() -> {
            try {
                llmService.chatStream(
                        request.getMessage(),
                        request.getHistory(),
                        request.getSystemPrompt(),
                        token -> {
                            if (token == null) {
                                // null 表示串流結束
                                try { emitter.send(SseEmitter.event().name("done").data("[END]")); }
                                catch (Exception ignored) { }
                                emitter.complete();
                            } else {
                                try { emitter.send(SseEmitter.event().data(token)); }
                                catch (Exception e) { emitter.completeWithError(e); }
                            }
                        });
            } catch (RuntimeException e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) { }
            }
        });
        return emitter;
    }

    /**
     * 商品推薦
     * POST /api/ai/recommend
     * Body: { "intent": "我想找一台適合上班族的筆電" }
     */
    @PostMapping("/recommend")
    @Operation(summary = "AI 商品推薦", description = "根據使用者意圖，從現有商品中推薦最合適的選項")
    public ResponseEntity<AiChatResponse> recommend(@RequestBody Map<String, String> body) {
        String intent = body.get("intent");
        if (intent == null || intent.isBlank()) {
            return ResponseEntity.badRequest().body(AiChatResponse.fail("請提供 intent 欄位"));
        }
        try {
            List<Product> products = productService.getAllActiveProducts();
            String reply = llmService.recommendProducts(products, intent);
            return ResponseEntity.ok(AiChatResponse.ok(reply, ollamaModel));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(AiChatResponse.fail(e.getMessage()));
        }
    }

    /**
     * 智慧搜尋關鍵字萃取
     * GET /api/ai/search?q=我想找適合打電動的耳機
     */
    @GetMapping("/search")
    @Operation(summary = "AI 搜尋輔助", description = "將自然語言轉換為商品搜尋關鍵字（回傳 JSON）")
    public ResponseEntity<AiChatResponse> searchAssist(
            @RequestParam @NotBlank(message = "搜尋內容不能為空") String q) {
        try {
            String keywords = llmService.extractSearchKeywords(q);
            return ResponseEntity.ok(AiChatResponse.ok(keywords, ollamaModel));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(AiChatResponse.fail(e.getMessage()));
        }
    }

    /**
     * 商品描述 AI 摘要
     * GET /api/ai/products/{id}/summary
     */
    @GetMapping("/products/{id}/summary")
    @Operation(summary = "商品 AI 摘要", description = "用 AI 為指定商品生成吸引人的摘要文字")
    public ResponseEntity<AiChatResponse> productSummary(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(product -> {
                    try {
                        String summary = llmService.summarizeProduct(product);
                        return ResponseEntity.ok(AiChatResponse.ok(summary, ollamaModel));
                    } catch (RuntimeException e) {
                        return ResponseEntity.status(503).<AiChatResponse>body(
                                AiChatResponse.fail(e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().<AiChatResponse>build());
    }

    /**
     * 健康檢查：確認 Ollama 服務是否可用
     * GET /api/ai/health
     */
    @GetMapping("/health")
    @Operation(summary = "Ollama 健康檢查", description = "測試本地 LLM 服務連線狀態")
    public ResponseEntity<AiChatResponse> health() {
        try {
            java.util.Map<String, Object> info = llmService.checkHealth();
            String msg = String.format("Ollama 正常，模型 %s %s",
                    info.get("model"),
                    Boolean.TRUE.equals(info.get("modelLoaded")) ? "(已載入)" : "(模型尚未載入)");
            return ResponseEntity.ok(AiChatResponse.ok(msg, ollamaModel));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(AiChatResponse.fail("Ollama 服務不可用: " + e.getMessage()));
        }
    }
}
