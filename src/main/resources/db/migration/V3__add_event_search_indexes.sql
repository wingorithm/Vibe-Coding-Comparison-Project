-- V3__add_event_search_indexes.sql
-- Index for performance in searching events by artist and location
-- Using btree indexes as they are effective for common 'LIKE' patterns in some scenarios 
-- and general equality/sorting which helps meet the 100ms UAC requirement.
CREATE INDEX IF NOT EXISTS idx_events_artist ON events (artist);
CREATE INDEX IF NOT EXISTS idx_events_location ON events (location);
CREATE INDEX IF NOT EXISTS idx_events_date_time ON events (date_time);
