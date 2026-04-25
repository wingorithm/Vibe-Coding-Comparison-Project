package wingorithm.ticketing.vibecoding.application.booking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import wingorithm.ticketing.vibecoding.application.exception.InvalidBookingStateException;
import wingorithm.ticketing.vibecoding.application.exception.PaymentFailedException;
import wingorithm.ticketing.vibecoding.application.exception.ResourceNotFoundException;
import wingorithm.ticketing.vibecoding.domain.booking.Booking;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.booking.BookingRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentGatewayRequest;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentGatewayResponse;

import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final RestClient restClient;
    
    @Value("${payment.gateway.url:http://localhost:9001/api/v1/payment}")
    private String paymentGatewayUrl;

    public PaymentService(BookingRepository bookingRepository, EventRepository eventRepository, RestClient.Builder restClientBuilder) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public PaymentGatewayResponse processPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new InvalidBookingStateException("Payment can only be processed for RESERVED bookings.");
        }

        PaymentGatewayRequest paymentRequest = PaymentGatewayRequest.builder()
                .eventName(booking.getEvent().getName())
                .customerName(booking.getCustomer().getName())
                .trxId(booking.getId().toString())
                .amount(booking.getTotalAmount())
                .currency("IDR")
                .build();

        try {
            // Hit external payment gateway
            log.info("Initiating payment for Booking ID: {}", bookingId);
            ResponseEntity<PaymentGatewayResponse> responseEntity = restClient.post()
                    .uri(paymentGatewayUrl)
                    .body(paymentRequest)
                    .retrieve()
                    .toEntity(PaymentGatewayResponse.class);

            PaymentGatewayResponse response = responseEntity.getBody();

            if (responseEntity.getStatusCode() == HttpStatus.OK && "COMPLETED".equals(response.getStatus())) {
                // UAC 1: Success - Update state to SOLD, save receipt
                booking.setStatus(BookingStatus.SOLD);
                booking.setReceiptUrl(response.getReceiptUrl());
                bookingRepository.save(booking);
                log.info("Payment SUCCESS. Booking ID: {} moved to SOLD.", bookingId);
                return response;
            } else {
                return handleFailedPayment(booking, response);
            }

        } catch (HttpClientErrorException e) {
            // UAC 2: Handle 400 Bad Request / Failed Payment
            PaymentGatewayResponse errorResponse = null;
            try {
                errorResponse = e.getResponseBodyAs(PaymentGatewayResponse.class);
            } catch (Exception ex) {
                log.error("Failed to parse error response from payment gateway", ex);
            }
            return handleFailedPayment(booking, errorResponse);
        } catch (Exception e) {
            log.error("Critical error communicating with payment gateway", e);
            return handleFailedPayment(booking, null);
        }
    }

    private PaymentGatewayResponse handleFailedPayment(Booking booking, PaymentGatewayResponse gatewayResponse) {
        // UAC 2: Invalid/Failed - Update state to CANCELLED, replenish tickets
        log.warn("Payment FAILED for Booking ID: {}. Reverting reservation.", booking.getId());
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Event event = eventRepository.findByIdWithLock(booking.getEvent().getId())
                .orElseThrow(() -> new IllegalStateException("Event missing during restock"));
        
        event.setAvailableTickets(event.getAvailableTickets() + 1);
        eventRepository.save(event);

        if (gatewayResponse != null && gatewayResponse.getError() != null) {
            throw new PaymentFailedException(gatewayResponse.getError().getMessage());
        } else {
            throw new PaymentFailedException("Payment processing failed due to a gateway error.");
        }
    }
}
