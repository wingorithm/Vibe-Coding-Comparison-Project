package wingorithm.ticketing.vibecoding.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID bookingId;
    private UUID eventId;
    private String eventName;
    private String customerName;
    private String ticketCode;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime expiresAt;
}
