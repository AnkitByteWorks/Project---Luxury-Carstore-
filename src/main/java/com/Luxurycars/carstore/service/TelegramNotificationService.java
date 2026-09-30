package com.Luxurycars.carstore.service;

import com.Luxurycars.carstore.dto.TestDriveResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Free Instant Notification Service delivering real-time mobile alerts
 * directly to the dealer/admin's phone via Telegram Bot API.
 */
@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    @Value("${app.telegram.enabled:false}")
    private boolean enabled;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.chat-id:}")
    private String chatId;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendTestDriveAlert(TestDriveResponseDTO dto) {
        String message = String.format(
                "🏎️ *NEW VIP TEST DRIVE BOOKED!*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📋 *Ref:* `%s`\n" +
                "👤 *Client:* %s\n" +
                "📞 *Phone:* %s\n" +
                "✉️ *Email:* %s\n" +
                "🚘 *Hypercar:* %s (%s)\n" +
                "📅 *Date:* %s\n" +
                "⏰ *Slot:* %s\n" +
                "🏁 *Experience:* %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "⚡ _Action Required: Review in Carstore Admin Portal_",
                dto.getReferenceCode(),
                dto.getCustomerName(),
                dto.getPhone(),
                dto.getEmail(),
                dto.getCarName(),
                dto.getCarBrand(),
                dto.getPreferredDate(),
                dto.getTimeSlot(),
                dto.getExperienceType()
        );

        dispatchNotification("VIP Test Drive Booking", message);
    }

    public void sendOrderAlert(Long orderId, String customerName, BigDecimal total, String carName) {
        String message = String.format(
                "💎 *NEW BESPOKE ORDER CONFIRMED!*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "📦 *Order ID:* `#%d`\n" +
                "👤 *Client:* %s\n" +
                "🚘 *Model:* %s\n" +
                "💰 *Total:* ₹%,.2f\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🛡️ _White-Glove Enclosed Carrier Prepared for Dispatch_",
                orderId, customerName, carName, total
        );

        dispatchNotification("Bespoke Order Confirmed", message);
    }

    public void sendVaultInquiryAlert(String clientName, String clientPhone, String allocationName) {
        String message = String.format(
                "🔒 *CONFIDENTIAL SECRET VAULT INQUIRY!*\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "👤 *VIP Client:* %s\n" +
                "📞 *Private Line:* %s\n" +
                "🏎️ *Hypercar Slot:* %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🚨 _Managing Director Contact Requested Within 15 Mins_",
                clientName, clientPhone, allocationName
        );

        dispatchNotification("Secret Vault Inquiry", message);
    }

    private void dispatchNotification(String alertType, String formattedMarkdown) {
        if (enabled && botToken != null && !botToken.isBlank() && chatId != null && !chatId.isBlank()) {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new HashMap<>();
                body.put("chat_id", chatId);
                body.put("text", formattedMarkdown);
                body.put("parse_mode", "Markdown");

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, entity, String.class);
                log.info("Telegram alert sent successfully for: {}", alertType);
            } catch (Exception e) {
                log.warn("Failed to deliver Telegram notification: {}", e.getMessage());
            }
        } else {
            // Prominent Console Banner for local development/demonstration
            log.info("\n" +
                    "╔═══════════════════════════════════════════════════════════════════════════════════╗\n" +
                    "║  📱 INSTANT DEALER ALERT: {} \n" +
                    "╠═══════════════════════════════════════════════════════════════════════════════════╣\n" +
                    "║ {}\n" +
                    "║ 💡 [FREE SETUP]: Set TELEGRAM_ENABLED=true, TELEGRAM_BOT_TOKEN & TELEGRAM_CHAT_ID\n" +
                    "║    in application.properties or .env to receive this live on your phone!\n" +
                    "╚═══════════════════════════════════════════════════════════════════════════════════╝",
                    alertType, formattedMarkdown.replace("\n", "\n║  "));
        }
    }
}
