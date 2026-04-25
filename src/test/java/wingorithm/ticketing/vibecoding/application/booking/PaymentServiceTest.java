package wingorithm.ticketing.vibecoding.application.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import wingorithm.ticketing.vibecoding.application.exception.PaymentFailedException;
import wingorithm.ticketing.vibecoding.domain.booking.Booking;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;
import wingorithm.ticketing.vibecoding.domain.customer.Customer;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.booking.BookingRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentGatewayRequest;
import wingorithm.ticketing.vibecoding.presentation.dto.PaymentGatewayResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PaymentService paymentService;

    private UUID bookingId;
    private Booking booking;
    private Event event;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        paymentService = new PaymentService(bookingRepository, eventRepository, restClientBuilder);
        ReflectionTestUtils.setField(paymentService, "paymentGatewayUrl", "http://localhost:9001/api/v1/payment");

        bookingId = UUID.randomUUID();
        event = Event.builder()
                .id(UUID.randomUUID())
                .name("Test Event")
                .availableTickets(10)
                .build();
        Customer customer = Customer.builder()
                .name("John Doe")
                .build();

        booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.RESERVED)
                .event(event)
                .customer(customer)
                .totalAmount(new BigDecimal("150.00"))
                .build();
    }

    @Test
    void processPayment_Positive_ShouldUpdateBookingToSold() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        
        PaymentGatewayResponse successResponse = new PaymentGatewayResponse();
        successResponse.setStatus("COMPLETED");
        successResponse.setReceiptUrl("https://receipt.url");
        
        ResponseEntity<PaymentGatewayResponse> responseEntity = new ResponseEntity<>(successResponse, HttpStatus.OK);
        
        // Mock RestClient chain
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(PaymentGatewayRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(PaymentGatewayResponse.class)).thenReturn(responseEntity);

        // Act
        PaymentGatewayResponse result = paymentService.processPayment(bookingId);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(BookingStatus.SOLD, booking.getStatus());
        assertEquals("https://receipt.url", booking.getReceiptUrl());
        verify(bookingRepository, times(1)).save(booking);
        verify(eventRepository, never()).findByIdWithLock(any()); // Should not restock
    }

    @Test
    void processPayment_Negative_ShouldCancelBookingAndRestockOnFailure() {
        // Arrange
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(eventRepository.findByIdWithLock(event.getId())).thenReturn(Optional.of(event));

        PaymentGatewayResponse failedResponse = new PaymentGatewayResponse();
        failedResponse.setStatus("FAILED");
        PaymentGatewayResponse.PaymentError errorDetail = new PaymentGatewayResponse.PaymentError();
        errorDetail.setMessage("Insufficient funds");
        failedResponse.setError(errorDetail);

        HttpClientErrorException mockException = new HttpClientErrorException(HttpStatus.BAD_REQUEST) {
            @Override
            public <T> T getResponseBodyAs(Class<T> responseType) {
                return (T) failedResponse;
            }
        };
        
        // Mock RestClient chain
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(PaymentGatewayRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(PaymentGatewayResponse.class)).thenThrow(mockException);

        // Act & Assert
        PaymentFailedException exception = assertThrows(PaymentFailedException.class, () -> paymentService.processPayment(bookingId));
        assertEquals("Insufficient funds", exception.getMessage());
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(11, event.getAvailableTickets()); // Restocked 10 + 1
        
        verify(bookingRepository, times(1)).save(booking);
        verify(eventRepository, times(1)).save(event);
    }
}