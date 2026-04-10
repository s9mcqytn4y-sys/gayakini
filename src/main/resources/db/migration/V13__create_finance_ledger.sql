CREATE TABLE commerce.finance_ledger_accounts (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- ASSET, REVENUE, LIABILITY, EXPENSE
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE commerce.finance_ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES commerce.finance_ledger_accounts(id),
    transaction_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- PAYMENT_SETTLEMENT, WITHDRAWAL_COMMIT, WITHDRAWAL_FINALIZE
    reference_id VARCHAR(100), -- Order Number or Withdrawal ID
    debit_amount BIGINT NOT NULL DEFAULT 0,
    credit_amount BIGINT NOT NULL DEFAULT 0,
    description TEXT,
    posted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata JSONB,
    CONSTRAINT uk_ledger_tx_type UNIQUE (transaction_id, transaction_type)
);

CREATE INDEX idx_ledger_entries_account ON commerce.finance_ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_tx ON commerce.finance_ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_ref ON commerce.finance_ledger_entries(reference_id);

CREATE TABLE commerce.finance_payout_destinations (
    id UUID PRIMARY KEY,
    bank_name VARCHAR(100) NOT NULL,
    account_name VARCHAR(120) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    branch VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_sandbox_mock BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE commerce.finance_withdrawal_requests (
    id UUID PRIMARY KEY,
    payout_destination_id UUID NOT NULL REFERENCES commerce.finance_payout_destinations(id),
    amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, APPROVED, PROCESSED, REJECTED
    requested_by UUID NOT NULL,
    approved_by UUID,
    processed_by UUID,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    admin_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Seed Initial Accounts
INSERT INTO commerce.finance_ledger_accounts (id, code, name, type, description) VALUES
('01918384-2591-789a-9e7b-7b3b7b3b7b4a', 'ASSET_GATEWAY', 'Midtrans Gateway Funds', 'ASSET', 'Funds held at payment gateway'),
('01918384-2591-789a-9e7b-7b3b7b3b7b4b', 'REVENUE_SALES', 'Sales Revenue', 'REVENUE', 'Revenue from completed orders'),
('01918384-2591-789a-9e7b-7b3b7b3b7b4c', 'LIABILITY_PAYOUT', 'Pending Payouts', 'LIABILITY', 'Funds committed for withdrawal');

-- Seed Sandbox Mock Payout Destination
INSERT INTO commerce.finance_payout_destinations (id, bank_name, account_name, account_number, branch, is_sandbox_mock) VALUES
('01918384-2591-789a-9e7b-7b3b7b3b7b4d', 'PT. BANK CENTRAL ASIA', 'Abdul Aziz', '7000855809', 'Jakarta', TRUE);
