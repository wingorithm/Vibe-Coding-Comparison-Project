package wingorithm.ticketing.vibecoding.application.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wingorithm.ticketing.vibecoding.domain.booking.Booking;
import wingorithm.ticketing.vibecoding.domain.booking.BookingStatus;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.booking.BookingRepository;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationSweeperService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;

    /**
     * Runs every 1 minute.
     * Finds bookings stuck in RESERVED state for more than 15 minutes.
     * Cancels them and replenishes the event inventory.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void sweepExpiredReservations() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(15);
        
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.RESERVED, expiryThreshold);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired reservations. Reverting states to CANCELLED and replenishing stock.", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            // 1. Cancel the booking
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // 2. Replenish inventory (Needs a fresh lock to prevent race conditions during restock)
            Event event = eventRepository.findByIdWithLock(booking.getEvent().getId())
                    .orElseThrow(() -> new IllegalStateException("Event missing during restock"));
            
            event.setAvailableTickets(event.getAvailableTickets() + 1);
            eventRepository.save(event);
            
            log.info("Successfully cancelled booking ID: {} and replenished 1 ticket for Event ID: {}", booking.getId(), event.getId());
        }
    }
}
