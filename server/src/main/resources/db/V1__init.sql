-- ABC Cash API — PostgreSQL schema v1
-- Run once: psql -U postgres -f V1__init.sql

CREATE DATABASE abc_cash;
\c abc_cash

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE entreprises (
    id              TEXT PRIMARY KEY,
    nom             TEXT NOT NULL,
    email           TEXT NOT NULL DEFAULT '',
    telephone       TEXT NOT NULL DEFAULT '',
    adresse         TEXT NOT NULL DEFAULT '',
    date_creation   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    admin_id        TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              TEXT PRIMARY KEY,
    entreprise_id   TEXT NOT NULL REFERENCES entreprises(id) ON DELETE CASCADE,
    nom             TEXT NOT NULL,
    email           TEXT NOT NULL UNIQUE,
    telephone       TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL,
    permissions     TEXT NOT NULL DEFAULT '',
    date_inscription TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id              TEXT PRIMARY KEY,
    entreprise_id   TEXT NOT NULL REFERENCES entreprises(id) ON DELETE CASCADE,
    invoice_number  TEXT NOT NULL,
    client_name     TEXT NOT NULL,
    total_amount    DOUBLE PRECISION NOT NULL,
    due_date        DATE NOT NULL,
    created_date    DATE NOT NULL,
    category        TEXT NOT NULL DEFAULT 'OTHER',
    category_label  TEXT NOT NULL DEFAULT '',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (entreprise_id, invoice_number)
);

CREATE TABLE payments (
    id              TEXT PRIMARY KEY,
    invoice_id      TEXT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    amount          DOUBLE PRECISION NOT NULL,
    date            DATE NOT NULL,
    method          TEXT NOT NULL,
    note            TEXT NOT NULL DEFAULT '',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE expenses (
    id                  TEXT PRIMARY KEY,
    entreprise_id       TEXT NOT NULL REFERENCES entreprises(id) ON DELETE CASCADE,
    label               TEXT NOT NULL,
    amount              DOUBLE PRECISION NOT NULL,
    date                DATE NOT NULL,
    is_recurring        BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence          TEXT,
    recurrence_end_date DATE,
    is_paid             BOOLEAN NOT NULL DEFAULT TRUE,
    payment_method      TEXT,
    created_date        DATE NOT NULL,
    category            TEXT NOT NULL DEFAULT 'OTHER',
    category_label      TEXT NOT NULL DEFAULT '',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_entreprise ON users(entreprise_id);
CREATE INDEX idx_invoices_entreprise ON invoices(entreprise_id);
CREATE INDEX idx_expenses_entreprise ON expenses(entreprise_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
