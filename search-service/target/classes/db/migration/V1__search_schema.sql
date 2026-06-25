CREATE TABLE IF NOT EXISTS airports (
    id UUID PRIMARY KEY,
    iata_code VARCHAR(3) NOT NULL UNIQUE,
    city VARCHAR(120) NOT NULL,
    country VARCHAR(120) NOT NULL,
    name VARCHAR(200) NOT NULL,
    timezone VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS providers (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    base_url VARCHAR(500),
    enabled BOOLEAN NOT NULL,
    priority INT NOT NULL
);

CREATE TABLE IF NOT EXISTS search_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    origin_iata VARCHAR(3) NOT NULL,
    destination_iata VARCHAR(3) NOT NULL,
    departure_date DATE NOT NULL,
    return_date DATE,
    passengers INT NOT NULL CHECK (passengers > 0 AND passengers <= 9),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS flight_offers (
    id UUID PRIMARY KEY,
    search_request_id UUID NOT NULL REFERENCES search_requests(id) ON DELETE CASCADE,
    provider_code VARCHAR(80) NOT NULL,
    airline VARCHAR(120) NOT NULL,
    flight_number VARCHAR(20) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    currency VARCHAR(3) NOT NULL,
    departure_at TIMESTAMPTZ NOT NULL,
    arrival_at TIMESTAMPTZ NOT NULL,
    transfers_count INT NOT NULL CHECK (transfers_count >= 0),
    booking_url VARCHAR(500) NOT NULL
);

CREATE TABLE IF NOT EXISTS favorite_offers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    flight_offer_id UUID NOT NULL REFERENCES flight_offers(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, flight_offer_id)
);

CREATE INDEX IF NOT EXISTS idx_airports_iata ON airports(iata_code);
CREATE INDEX IF NOT EXISTS idx_airports_city_country ON airports(city, country);
CREATE INDEX IF NOT EXISTS idx_search_requests_user_created ON search_requests(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_search_requests_route_date ON search_requests(origin_iata, destination_iata, departure_date);
CREATE INDEX IF NOT EXISTS idx_flight_offers_request_price ON flight_offers(search_request_id, price);

INSERT INTO airports (id, iata_code, city, country, name, timezone)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'MOW', 'Москва', 'Россия', 'Все аэропорты Москвы', 'Europe/Moscow'),
    ('00000000-0000-0000-0000-000000000002', 'AER', 'Сочи', 'Россия', 'Сочи', 'Europe/Moscow'),
    ('00000000-0000-0000-0000-000000000003', 'LED', 'Санкт-Петербург', 'Россия', 'Пулково', 'Europe/Moscow')
ON CONFLICT (iata_code) DO NOTHING;

INSERT INTO providers (id, code, name, base_url, enabled, priority)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'mock-air', 'Mock Air Provider', 'local://mock-air', true, 10)
ON CONFLICT (code) DO NOTHING;
