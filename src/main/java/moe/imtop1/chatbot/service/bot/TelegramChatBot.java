package moe.imtop1.chatbot.service.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import moe.imtop1.chatbot.domain.enums.AppConstants;
import moe.imtop1.chatbot.service.apis.ApiService;
import moe.imtop1.chatbot.utils.ToolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TelegramChatBot extends TelegramLongPollingBot {
    @Value(value = "${telegram.bot.config.token}")
    private String token;
    @Value(value = "${telegram.bot.config.userName}")
    private String userName;

    private final ApiService apiService;

    @Autowired
    public TelegramChatBot(DefaultBotOptions botOptions, ApiService apiService) {
        super(botOptions);

        this.apiService = apiService;
    }
    @Override
    public String getBotUsername() {
        return this.userName;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }


    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                this.handleCommand(text, chatId);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            //log.error("Error processing update.", e);
        }
    }

    /**
     * 根据输入命令和参数生成响应消息。
     * @param command 用户输入的命令。
     * @param chatId 消息所在的聊天ID。
     * @param param 命令后可能跟随的参数。
     * @return 一个包含所有响应消息的列表。
     */
    private List<SendMessage> getMessage(String command, long chatId, String param) {
        List<SendMessage> msgList = new ArrayList<>();

        if ("/chat".equals(command)) {
            String text = String.valueOf(param);
            if (StringUtils.hasText(text)) {
                String body = this.createJsonRequest(text);

                Flux<String> stringFlux = apiService.proxyRequest(body, "llama3-405b");
                //FIXME markdown语法不解析问题
                String s = ToolUtils.escape(this.getCompleteResponse(stringFlux).block().trim()).trim();

//                String block = completeResponse.block();
//                String s = ToolUtils.escapeMarkdown(block);

                msgList.add(this.createMessage(String.valueOf(chatId), s, null));
            } else {
                msgList.add(this.createMessage(String.valueOf(chatId), "null", null));
            }
        } else {
            if (ToolUtils.containsSlash(command, "/")) {
                SendMessage errorMessage = SendMessage.builder()
                        .text(AppConstants.ILLEGAL_ORDER)
                        .chatId(chatId)
                        .build();

                msgList.add(errorMessage);
            }
        }

        return msgList;
    }

    /**
     * 处理用户通过文本消息发送的命令。
     * @param text 用户发送的文本，包含命令和可能的参数。
     * @param chatId 聊天的唯一标识符。
     * @throws TelegramApiException 如果在执行Telegram API操作时发生错误。
     */
    private void handleCommand(String text, long chatId) throws TelegramApiException {
        String[] parts = text.split(" ", 2);
        String command = parts[0];
        String param = parts.length > 1 ? parts[1] : null;

        List<SendMessage> messages = getMessage(command, chatId, param);
        for (SendMessage message : messages) {
            execute(message);
        }
    }

    /**
     * 创建带有内联键盘按钮的Telegram消息。
     * @param chatId 消息发送的目标聊天ID。
     * @param text 消息文本。
     * @param markupInline 包含内联键盘的布局。
     * @return 构造完成的SendMessage对象。
     */
    private SendMessage createMessage(String chatId, String text, InlineKeyboardMarkup markupInline) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWNV2);

        if (markupInline != null) {
            message.setReplyMarkup(markupInline);
        }
        return message;
    }

    /**
     * 处理结果为字符串
     * @param responseFlux api请求结果
     * @return 结果字符串
     */
    public Mono<String> getCompleteResponse(Flux<String> responseFlux) {
        ObjectMapper objectMapper = new ObjectMapper();

        return responseFlux
                .filter(line -> line != null && !line.isEmpty())
                .map(line -> {
                    if (line.startsWith("data:")) {
                        return line.substring(5).trim();
                    }
                    return line.trim();
                })
                .takeUntil("[DONE]"::equals)
                .concatMap(line -> {
                    if ("[DONE]".equals(line)) {
                        return Mono.empty();
                    }
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        if (jsonNode.has("choices")) {
                            JsonNode choicesNode = jsonNode.get("choices").get(0);
                            if (choicesNode.has("delta") && choicesNode.get("delta").has("content")) {
                                return Mono.just(choicesNode.get("delta").get("content").asText().trim());
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing JSON line: " + e.getMessage());
                    }
                    return Mono.empty();
                })
                .collect(Collectors.joining())
                .doOnError(e -> System.out.println("Error: " + e.getMessage()));
    }

    /**
     * 构造json请求
     *
     * @param content
     * @return
     */
    private static String createJsonRequest(String content) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put("model", "llama3-405b");
        rootNode.put("stream", true);

        ArrayNode messagesArray = rootNode.putArray("messages");
        ObjectNode messageObject = mapper.createObjectNode();
        messageObject.put("role", "user");
        messageObject.put("content", content);

        messagesArray.add(messageObject);

        try {
            // Convert ObjectNode to pretty JSON string
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
