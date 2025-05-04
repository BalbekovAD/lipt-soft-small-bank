CREATE TABLE client (
                        id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name          VARCHAR(100) NOT NULL
);

CREATE TABLE account (
                         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                         client_id     BIGINT NOT NULL,
                         currency      VARCHAR(3) NOT NULL,
                         balance       DECIMAL(19,2) NOT NULL,
                         account_number VARCHAR(20) NOT NULL UNIQUE,
                         CONSTRAINT fk_client FOREIGN KEY (client_id) REFERENCES client(id)
);
