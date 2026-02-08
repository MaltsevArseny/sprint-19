-- Создание таблицы хабов
CREATE TABLE IF NOT EXISTS hubs (
                                    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Создание таблицы датчиков
CREATE TABLE IF NOT EXISTS sensors (
                                       id VARCHAR(255) NOT NULL,
    hub_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    unit VARCHAR(20),
    last_value DOUBLE PRECISION,
    last_updated TIMESTAMP,
    version BIGINT DEFAULT 0,
    PRIMARY KEY (id, hub_id),
    FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE CASCADE
    );

-- Создание таблицы сценариев
CREATE TABLE IF NOT EXISTS scenarios (
                                         id BIGSERIAL PRIMARY KEY,
                                         scenario_id VARCHAR(255) UNIQUE,
    hub_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (hub_id, name),
    FOREIGN KEY (hub_id) REFERENCES hubs(id) ON DELETE CASCADE
    );

-- Создание таблицы условий
CREATE TABLE IF NOT EXISTS conditions (
                                          id BIGSERIAL PRIMARY KEY,
                                          condition_id VARCHAR(255),
    scenario_id VARCHAR(255) NOT NULL,
    sensor_id VARCHAR(255),
    sensor_type VARCHAR(50) NOT NULL,
    operation VARCHAR(20) NOT NULL CHECK (operation IN ('GREATER', 'LESS', 'EQUAL', 'NOT_EQUAL', 'BETWEEN')),
    threshold_value DOUBLE PRECISION,
    threshold_value2 DOUBLE PRECISION,
    logical_operator VARCHAR(10) DEFAULT 'AND',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(scenario_id) ON DELETE CASCADE
    );

-- Создание таблицы действий
CREATE TABLE IF NOT EXISTS actions (
                                       id BIGSERIAL PRIMARY KEY,
                                       action_id VARCHAR(255),
    scenario_id VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    device_id VARCHAR(255),
    value INTEGER,
    delay_ms INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(scenario_id) ON DELETE CASCADE
    );

-- Таблица для отслеживания выполненных сценариев
CREATE TABLE IF NOT EXISTS scenario_executions (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   scenario_id VARCHAR(255) NOT NULL,
    hub_id VARCHAR(255) NOT NULL,
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sensor_values JSONB,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    FOREIGN KEY (scenario_id) REFERENCES scenarios(scenario_id) ON DELETE CASCADE
    );

-- Индексы для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_sensors_hub_id ON sensors(hub_id);
CREATE INDEX IF NOT EXISTS idx_sensors_type ON sensors(type);
CREATE INDEX IF NOT EXISTS idx_scenarios_hub_id_active ON scenarios(hub_id, active);
CREATE INDEX IF NOT EXISTS idx_scenarios_priority ON scenarios(priority);
CREATE INDEX IF NOT EXISTS idx_conditions_scenario_id ON conditions(scenario_id);
CREATE INDEX IF NOT EXISTS idx_actions_scenario_id ON actions(scenario_id);
CREATE INDEX IF NOT EXISTS idx_scenario_executions_hub_time ON scenario_executions(hub_id, triggered_at);