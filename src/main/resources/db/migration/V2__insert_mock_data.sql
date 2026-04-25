-- V2__insert_mock_data.sql
INSERT INTO customers (id, name, email, tier) VALUES 
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'John Beginner', 'john@example.com', 'BEGINNER'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Jane Fan', 'jane@example.com', 'FANS'),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Alice Lover', 'alice@example.com', 'LOVERS')
ON CONFLICT (email) DO NOTHING;

INSERT INTO events (id, name, artist, location, date_time, available_tickets, base_price) VALUES 
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a21', 'Summer Vibe Festival', 'The Vibe Band', 'Jakarta Stadium', '2026-08-15 19:00:00', 500, 150000.00),
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Neon Nights', 'DJ Shadow', 'Bali Beach Club', '2026-09-20 21:00:00', 200, 250000.00)
ON CONFLICT (id) DO NOTHING;
