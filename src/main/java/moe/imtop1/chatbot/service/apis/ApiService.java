package moe.imtop1.chatbot.service.apis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * llama-3.1 405b api服务
 * @author anoixa
 */
@Service
@Slf4j
public class ApiService {
    private static final String API_URL = "https://fast.snova.ai";
    private static final String API_PATH = "/api/completion";

    private final WebClient webClient;

    @Autowired
    public ApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL)
                .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.5845.97 Safari/537.36")
                .build();
    }

    public Flux<String> proxyRequest(String requestBody, String modelOverride) {
        String modifiedBody = modifyRequestBody(requestBody, modelOverride);
        //log.info("body: {}",modifiedBody);

        return webClient.post()
                .uri(API_PATH)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(modifiedBody))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(Throwable::printStackTrace);
    }

    /**
     * 重新构建json
     * @param requestBody 请求体
     * @param modelOverride 模型类型
     * @author anoixa
     * @return 处理好的json
     */
    private String modifyRequestBody(String requestBody, String modelOverride) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode rootNode = objectMapper.readTree(requestBody);
            ObjectNode bodyNode = objectMapper.createObjectNode();

            if (rootNode.isObject()) {
                rootNode.fieldNames().forEachRemaining(fieldName -> bodyNode.set(fieldName, rootNode.get(fieldName)));
            }

            if (modelOverride != null && !modelOverride.isEmpty()) {
                bodyNode.put("model", modelOverride);
            }

            ArrayNode stopArray = bodyNode.withArray("stop");
            stopArray.removeAll();
            stopArray.add("<|eot_id|>");

            ObjectNode newRootNode = objectMapper.createObjectNode();
            newRootNode.set("body", bodyNode);
            newRootNode.put("env_type", "tp16405b");

            return objectMapper.writeValueAsString(newRootNode);

        } catch (Exception e) {
            e.printStackTrace();

            return requestBody;
        }
    }

}
