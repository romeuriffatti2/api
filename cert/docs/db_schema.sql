-- Script de Criação do Banco de Dados (PostgreSQL)
-- Gerado a partir das entidades JPA mapeadas no sistema

-- 1. Tabela de Pessoas (Destinatários)
CREATE TABLE IF NOT EXISTS person (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    cpf VARCHAR(14) UNIQUE
);

-- 2. Tabela de Usuários (Administradores e Gerentes de Revistas)
CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    cpf VARCHAR(14) UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    birth_date DATE,
    role VARCHAR(50) NOT NULL DEFAULT 'CLIENT',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de Revistas
CREATE TABLE IF NOT EXISTS magazine (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    isbn VARCHAR(50),
    issn VARCHAR(50),
    email VARCHAR(255),
    cnpj VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela de Relacionamento Usuário x Revista (Muitos para Muitos)
CREATE TABLE IF NOT EXISTS user_magazine (
    user_id BIGINT NOT NULL,
    magazine_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, magazine_id),
    CONSTRAINT fk_user_magazine_user FOREIGN KEY (user_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_magazine_magazine FOREIGN KEY (magazine_id) REFERENCES magazine(id) ON DELETE CASCADE
);


-- 6. Tabela de Templates de Certificado (PDFME)
CREATE TABLE IF NOT EXISTS certificate_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    system_default BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    issuer_name VARCHAR(255),
    json_schema TEXT NOT NULL,
    owner_id BIGINT,
    source_template_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_owner FOREIGN KEY (owner_id) REFERENCES usuario(id) ON DELETE SET NULL
);

-- 7. Tabela de Certificados (Gerados)
CREATE TABLE IF NOT EXISTS certificate (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    validation_code UUID DEFAULT gen_random_uuid(),
    magazine_id BIGINT NOT NULL,
    person_id BIGINT,
    template_id BIGINT,
    volume VARCHAR(50),
    number VARCHAR(50),
    type VARCHAR(50),
    recipient_email VARCHAR(255),
    status VARCHAR(50) DEFAULT 'GENERATED',
    metadata JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_certificate_magazine FOREIGN KEY (magazine_id) REFERENCES magazine(id),
    CONSTRAINT fk_certificate_person FOREIGN KEY (person_id) REFERENCES person(id),
    CONSTRAINT fk_certificate_template FOREIGN KEY (template_id) REFERENCES certificate_template(id)
);

-- Comentários sobre os relacionamentos:
-- - Usuario <-> Magazine: ManyToMany via user_magazine
-- - Usuario <-> CertificateTemplate: OneToMany (um usuário é dono de seus templates)
-- - Magazine <-> Certificate: OneToMany (uma revista emite múltiplos certificados)
-- - Person <-> Certificate: OneToMany (uma pessoa pode ter vários certificados)
-- - CertificateTemplate <-> Certificate: ManyToOne (um certificado usa um template histórico)
