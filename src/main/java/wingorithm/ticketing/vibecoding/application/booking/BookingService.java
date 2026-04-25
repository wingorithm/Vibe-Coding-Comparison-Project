package wingorithm.ticketing.vibecoding.application.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wingorithm.ticketing.vibecoding.application.exception.ResourceNotFoundException;
import wingorithm.ticketing.vibecoding.application.exception.SoldOutException;
import wingorithm.ticketing.vibecoding.domain.booking.Booking;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;
import wingorithm.ticketing.vibecoding.domain.booking.IdempotencyRecord;
import wingorithm.ticketing.vibecoding.domain.customer.Customer;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.domain.ticket.Ticket;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BookingResponse reserveTicket(String idempotencyKey, BookingRequest request) {
        // 1. Idempotency Check: If we've processed this request before, return the cached response immediately.
        Optional<IdempotencyRecord> cachedRecord = idempotencyRecordRepository.findById(idempotencyKey);
        if (cachedRecord.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            try {
                return objectMapper.readValue(cachedRecord.get().getResponseBody(), BookingResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize cached response", e);
            }
        }

        // 2. Strict Concurrency Control: Acquire Pessimistic Write Lock on the Event.
        Event event = eventRepository.findByIdWithLock(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getAvailableTickets() <= 0) {
            throw new SoldOutException("Tickets for this event are sold out.");
        }

        // 3. Deduct Inventory
        event.setAvailableTickets(event.getAvailableTickets() - 1);
        eventRepository.save(event);

        // 4. Role-Based Access Pricing: Fetch customer and calculate their specific tier discount.
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        BigDecimal finalPrice = customer.getTier().calculateDiscountedPrice(event.getBasePrice());

        // 5. Create Booking & Ticket
        Booking booking = Booking.builder()
                .customer(customer)
                .event(event)
                .totalAmount(finalPrice)
                .status(BookingStatus.RESERVED)
                .createdAt(LocalDateTime.now())
                .build();
        booking = bookingRepository.save(booking);

        Ticket ticket = Ticket.builder()
                .booking(booking)
                .event(event)
                .ticketCode(UUID.randomUUID().toString())
                .build();
        ticketRepository.save(ticket);

        // 6. Build Response
        BookingResponse response = BookingResponse.builder()
                .bookingId(booking.getId())
                .eventId(event.getId())
                .eventName(event.getName())
                .customerName(customer.getName())
                .ticketCode(ticket.getTicketCode())
                .totalAmount(finalPrice)
                .status(booking.getStatus())
                .expiresAt(booking.getCreatedAt().plusMinutes(15)) // 15-minute hold
                .build();

        // 7. Save Idempotency Record
        try {
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey)
                    .responseStatus(202)
                    .responseBody(objectMapper.writeValueAsString(response))
                    .createdAt(LocalDateTime.now())
                    .build();
            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize booking response for idempotency", e);
        }

        return response;
    }
}
