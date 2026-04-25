package wingorithm.ticketing.vibecoding.application.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wingorithm.ticketing.vibecoding.application.exception.SoldOutException;
import wingorithm.ticketing.vibecoding.domain.booking.Booking;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;
import wingorithm.ticketing.vibecoding.domain.customer.Customer;
import wingorithm.ticketing.vibecoding.domain.customer.CustomerTier;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.booking.BookingRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.booking.IdempotencyRecordRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.customer.CustomerRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.ticket.TicketRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.BookingRequest;
import wingorithm.ticketing.vibecoding.presentation.dto.BookingResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BookingService bookingService;

    private UUID eventId;
    private UUID customerId;
    private Event event;
    private Customer customer;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        event = Event.builder()
                .id(eventId)
                .name("Test Event")
                .availableTickets(10)
                .basePrice(new BigDecimal("100.00"))
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .tier(CustomerTier.BEGINNER)
                .build();
    }

    @Test
    void reserveTicket_Positive_ShouldCreateReservation() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setEventId(eventId);
        request.setCustomerId(customerId);

        when(idempotencyRecordRepository.findById(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        Booking mockBooking = Booking.builder().id(UUID.randomUUID()).status(BookingStatus.RESERVED).createdAt(LocalDateTime.now()).build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        // Act
        BookingResponse response = bookingService.reserveTicket("idemp-key-123", request);

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.RESERVED, response.getStatus());
        assertEquals(9, event.getAvailableTickets()); // 1 ticket deducted
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(ticketRepository, times(1)).save(any());
        verify(idempotencyRecordRepository, times(1)).save(any());
    }

    @Test
    void reserveTicket_Negative_ShouldThrowSoldOutException() {
        // Arrange
        event.setAvailableTickets(0); // Sold out
        BookingRequest request = new BookingRequest();
        request.setEventId(eventId);
        request.setCustomerId(customerId);

        when(idempotencyRecordRepository.findById(anyString())).thenReturn(Optional.empty());
        when(eventRepository.findByIdWithLock(eventId)).thenReturn(Optional.of(event));

        // Act & Assert
        assertThrows(SoldOutException.class, () -> bookingService.reserveTicket("idemp-key-456", request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
