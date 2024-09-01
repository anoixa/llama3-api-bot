package moe.imtop1.chatbot.controller;

import moe.imtop1.chatbot.service.apis.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
public class LlamaApi {
    @Autowired
    private ApiService apiService;

    @Autowired
    public LlamaApi(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping(value = "/chat/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleRequest(@RequestBody String body) {
        String modelOverride = "";

        return apiService.proxyRequest(body, modelOverride);
    }
}
