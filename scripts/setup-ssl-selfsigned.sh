#!/bin/bash

# Generate self-signed SSL certificate for testing
# Usage: ./scripts/setup-ssl-selfsigned.sh

SSL_DIR="./nginx/ssl"

echo "Creating SSL directory..."
mkdir -p "$SSL_DIR"

echo "Generating self-signed SSL certificate..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$SSL_DIR/privkey.pem" \
    -out "$SSL_DIR/fullchain.pem" \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=AIAgent/OU=Development/CN=localhost"

echo "SSL certificate generated successfully!"
echo "Files created:"
echo "  - $SSL_DIR/privkey.pem (private key)"
echo "  - $SSL_DIR/fullchain.pem (certificate)"
echo ""
echo "Note: This is a self-signed certificate for testing only."
echo "Browsers will show a security warning - this is expected."
