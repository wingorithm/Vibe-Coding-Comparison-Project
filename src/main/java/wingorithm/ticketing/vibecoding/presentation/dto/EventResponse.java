package wingorithm.ticketing.vibecoding.presentation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID id;
    private String name;
    private String artist;
    private String location;
    private LocalDateTime dateTime;
    private Integer availableTickets;
    private BigDecimal basePrice;
}
