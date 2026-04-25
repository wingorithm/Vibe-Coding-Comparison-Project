package wingorithm.ticketing.vibecoding.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayRequest {
    @JsonProperty("event_name")
    private String eventName;
    
    @JsonProperty("customer_name")
    private String customerName;
    
    @JsonProperty("trx_id")
    private String trxId;
    
    private BigDecimal amount;
    private String currency;
}
