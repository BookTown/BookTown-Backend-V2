#!/usr/bin/env bash
set -euo pipefail

DOMAIN="${GRAFANA_DOMAIN:-grafana.booktown.shop}"
CONFIG_SOURCE="${GRAFANA_NGINX_CONFIG:-/opt/booktown/ops/nginx/grafana.booktown.shop.conf}"
CONFIG_TARGET="/etc/nginx/sites-available/${DOMAIN}"
CONFIG_LINK="/etc/nginx/sites-enabled/${DOMAIN}"

if [ ! -f "$CONFIG_SOURCE" ]; then
  echo "Nginx config not found: $CONFIG_SOURCE"
  exit 1
fi

if ! command -v nginx >/dev/null 2>&1; then
  echo "nginx is required"
  exit 1
fi

sudo cp "$CONFIG_SOURCE" "$CONFIG_TARGET"
sudo ln -sf "$CONFIG_TARGET" "$CONFIG_LINK"

if [ ! -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
  if [ -z "${CERTBOT_EMAIL:-}" ]; then
    echo "Certificate is missing. Set CERTBOT_EMAIL and rerun, or issue the certificate manually."
    echo "Example: CERTBOT_EMAIL=you@example.com $0"
    exit 1
  fi

  sudo nginx -t
  sudo systemctl reload nginx
  sudo certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --email "$CERTBOT_EMAIL" --redirect
else
  sudo nginx -t
  sudo systemctl reload nginx
fi

echo "Grafana Nginx route is installed for https://${DOMAIN}"
