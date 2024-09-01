package moe.imtop1.chatbot.config;

import lombok.extern.slf4j.Slf4j;
import moe.imtop1.chatbot.domain.enums.AppConstants;
import moe.imtop1.chatbot.service.bot.TelegramChatBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Slf4j
public class TelegramBotConfog {
    /**
     * 机器人注册配置Bean
     * @param bot 注册的bot
     * @return
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramChatBot bot) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);

//            log.info(AppConstants.SUCCESS_REGISTERING_BOT + ": " + bot);

            return telegramBotsApi;
        } catch (TelegramApiException e) {
            throw new RuntimeException(AppConstants.ERROR_REGISTERING_BOT + ": ", e);
        }
    }

    /**
     * 机器人配置Bean
     * @return
     */
    @Bean
    public DefaultBotOptions defaultBotOptions() {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setProxyType(DefaultBotOptions.ProxyType.NO_PROXY);
//        if (botOptions.getProxyType() != DefaultBotOptions.ProxyType.NO_PROXY) {
//            botOptions.setProxyHost(address);
//            botOptions.setProxyPort(Integer.parseInt(port));
//
//            log.info(String.format(MessageTemplate.PROXY_INFO_TEMPLATE, proxyType, address + ":" + port));
//        }
//        botOptions.setProxyHost("127.0.0.1");
//        botOptions.setProxyPort(10808);


        return botOptions;
    }
}
