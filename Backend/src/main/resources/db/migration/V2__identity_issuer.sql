ALTER TABLE usuarios ADD COLUMN cognito_issuer VARCHAR(255);
CREATE UNIQUE INDEX uk_usuarios_issuer_sub ON usuarios(cognito_issuer, cognito_sub);
