package wingorithm.ticketing.vibecoding.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.EventResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.EventResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventSearchService {

    private final EventRepository eventRepository;

    public Page<EventResponse> searchEvents(String query, Pageable pageable) {
        Page<Event> events;
        
        if (query == null || query.trim().isEmpty()) {
            events = eventRepository.findAll(pageable);
        } else {
            events = eventRepository.findByArtistContainingIgnoreCaseOrLocationContainingIgnoreCase(query, query, pageable);
        }

        return events.map(this::mapToResponse);
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .artist(event.getArtist())
                .location(event.getLocation())
                .dateTime(event.getDateTime())
                .availableTickets(event.getAvailableTickets())
                .basePrice(event.getBasePrice())
                .build();
    }
}
