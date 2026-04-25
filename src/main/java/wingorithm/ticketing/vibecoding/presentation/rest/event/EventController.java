package wingorithm.ticketing.vibecoding.presentation.rest.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wingorithm.ticketing.vibecoding.application.event.EventSearchService;
import wingorithm.ticketing.vibecoding.presentation.dto.EventResponse;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventSearchService eventSearchService;

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(name = "query", required = false) String query,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<EventResponse> results = eventSearchService.searchEvents(query, pageable);
        return ResponseEntity.ok(results);
    }
}
