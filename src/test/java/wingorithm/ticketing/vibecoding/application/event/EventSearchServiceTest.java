package wingorithm.ticketing.vibecoding.application.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wingorithm.ticketing.vibecoding.domain.event.Event;
import wingorithm.ticketing.vibecoding.infrastructure.persistence.event.EventRepository;
import wingorithm.ticketing.vibecoding.presentation.dto.EventResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSearchServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventSearchService eventSearchService;

    private Event mockEvent;

    @BeforeEach
    void setUp() {
        mockEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("Summer Vibe Festival")
                .artist("The Vibe Band")
                .location("Jakarta Stadium")
                .dateTime(LocalDateTime.now().plusDays(30))
                .availableTickets(500)
                .basePrice(new BigDecimal("150000.00"))
                .build();
    }

    @Test
    void searchEvents_Positive_ShouldReturnMatchingEvents() {
        // Arrange
        String query = "Jakarta";
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.findByArtistContainingIgnoreCaseOrLocationContainingIgnoreCase(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockEvent)));

        // Act
        Page<EventResponse> result = eventSearchService.searchEvents(query, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Jakarta Stadium", result.getContent().get(0).getLocation());
    }

    @Test
    void searchEvents_Negative_ShouldReturnEmptyListWhenNoMatch() {
        // Arrange
        String query = "UnknownArtist";
        Pageable pageable = PageRequest.of(0, 10);
        when(eventRepository.findByArtistContainingIgnoreCaseOrLocationContainingIgnoreCase(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        Page<EventResponse> result = eventSearchService.searchEvents(query, pageable);

        // Assert
        assertTrue(result.getContent().isEmpty());
    }
}
