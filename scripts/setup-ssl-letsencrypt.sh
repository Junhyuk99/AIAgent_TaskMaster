#!/bin/bash

# Setup Let's Encrypt SSL certificate
# Usage: ./scripts/setup-ssl-letsencrypt.sh your-domain.com your-email@example.com

DOMAIN=$1
EMAIL=$2

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
    echo "Usage: $0 <domain> <email>"
    echo "Example: $0 example.com admin@example.com"
    exit 1
fi

SSL_DIR="./nginx/ssl"
CERTBOT_DIR="./nginx/certbot"

echo "Setting up Let's Encrypt SSL for $DOMAIN..."

# Create directories
mkdir -p "$SSL_DIR"
mkdir -p "$CERTBOT_DIR"

# Stop nginx if running
docker-compose -f docker-compose.prod.yml stop nginx 2>/dev/null

# Run certbot in standalone mode
docker run -it --rm \
    -v "$(pwd)/$SSL_DIR:/etc/letsencrypt/live/$DOMAIN" \
    -v "$(pwd)/$CERTBOT_DIR:/var/www/certbot" \
    -p 80:80 \
    certbot/certbot certonly \
    --standalone \
    --preferred-challenges http \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

if [ $? -eq 0 ]; then
    echo ""
    echo "SSL certificate generated successfully!"
    echo "Certificate location: $SSL_DIR/"
    echo ""
    echo "Now start the production stack:"
    echo "  docker-compose -f docker-compose.prod.yml up -d"
else
    echo "Failed to generate SSL certificate."
    echo "Make sure:"
    echo "  1. Domain $DOMAIN points to this server"
    echo "  2. Port 80 is open in firewall"
    exit 1
fi
