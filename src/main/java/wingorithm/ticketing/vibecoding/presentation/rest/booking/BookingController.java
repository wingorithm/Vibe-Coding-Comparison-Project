package wingorithm.ticketing.vibecoding.presentation.rest.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wingorithm.ticketing.vibecoding.application.booking.BookingService;
import wingorithm.ticketing.vibecoding.application.booking.PaymentService;
import wingorithm.ticketing.vibecoding.presentation.dto.BookingRequest;
import wingorithm.ticketing.vibecoding.presentation.dto.BookingResponse;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentGatewayResponse;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentRequest;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping("/reserve")
    public ResponseEntity<BookingResponse> reserveTicket(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody BookingRequest request) {

        BookingResponse response = bookingService.reserveTicket(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentGatewayResponse> processPayment(@RequestBody PaymentRequest request) {
        PaymentGatewayResponse response = paymentService.processPayment(request.getBookingId());
        return ResponseEntity.ok(response);
    }
}
