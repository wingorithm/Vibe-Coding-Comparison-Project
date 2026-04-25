package wingorithm.ticketing.vibecoding.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PaymentGatewayResponse {
    
    @JsonProperty("transaction_id")
    private String transactionId;
    
    private String status; // COMPLETED or FAILED
    
    private BigDecimal amount;
    
    private String currency;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("receipt_url")
    private String receiptUrl;
    
    // For failed responses
    private String remark;
    private PaymentError error;

    @Data
    @NoArgsConstructor
    public static class PaymentError {
        private String code;
        private String message;
        @JsonProperty("decline_code")
        private String declineCode;
    }
}
