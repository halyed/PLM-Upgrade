#!/bin/bash
# PLM Platform — VPS Setup Script
# Run once on a fresh Ubuntu 22.04 VPS (OVH or any provider)
# Usage: bash setup-vps.sh

set -e

# ── Colors ────────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Prerequisites ─────────────────────────────────────────────────────────────
[[ $EUID -ne 0 ]] && error "Run as root: sudo bash setup-vps.sh"

# ── 1. Install Docker ─────────────────────────────────────────────────────────
info "Installing Docker..."
apt-get update -qq
apt-get install -y -qq ca-certificates curl gnupg git

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update -qq
apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable docker
systemctl start docker
info "Docker installed: $(docker --version)"

# ── 2. Clone repo ─────────────────────────────────────────────────────────────
DEPLOY_DIR=/opt/plm-upgrade

if [ ! -d "$DEPLOY_DIR" ]; then
  info "Cloning repository..."
  git clone https://github.com/halyed/PLM-Upgrade.git $DEPLOY_DIR
else
  info "Repo already exists at $DEPLOY_DIR, pulling latest..."
  git -C $DEPLOY_DIR pull origin v2-dev
fi
git -C $DEPLOY_DIR checkout v2-dev

# ── 3. Create .env ────────────────────────────────────────────────────────────
ENV_FILE=$DEPLOY_DIR/infrastructure/docker/.env

if [ ! -f "$ENV_FILE" ]; then
  info "Creating .env from template..."
  cp $DEPLOY_DIR/infrastructure/docker/.env.example $ENV_FILE

  read -rp "Enter your DuckDNS subdomain (e.g. myplm.duckdns.org): " DOMAIN
  read -rp "Enter your DuckDNS token: " DUCKDNS_TOKEN
  read -rsp "Enter a strong password for databases/Keycloak: " STRONG_PASS; echo

  sed -i "s/yourapp.duckdns.org/$DOMAIN/g" $ENV_FILE
  sed -i "s/your-duckdns-token/$DUCKDNS_TOKEN/" $ENV_FILE
  sed -i "s/change_me_strong_password/$STRONG_PASS/g" $ENV_FILE

  info ".env created at $ENV_FILE"
else
  warn ".env already exists — skipping. Edit manually if needed: $ENV_FILE"
  read -r DOMAIN <<< "$(grep ^DOMAIN= $ENV_FILE | cut -d= -f2)"
  read -r DUCKDNS_TOKEN <<< "$(grep ^DUCKDNS_TOKEN= $ENV_FILE | cut -d= -f2)"
fi

# ── 4. Set up DuckDNS auto-update cron ───────────────────────────────────────
info "Setting up DuckDNS IP auto-update (every 5 min)..."
DUCK_SCRIPT=/opt/duckdns-update.sh
SUBDOMAIN=$(echo "$DOMAIN" | cut -d. -f1)

cat > $DUCK_SCRIPT <<DUCK
#!/bin/bash
curl -s "https://www.duckdns.org/update?domains=${SUBDOMAIN}&token=${DUCKDNS_TOKEN}&ip=" > /var/log/duckdns.log 2>&1
DUCK
chmod +x $DUCK_SCRIPT

# Add cron entry if not already present
if ! crontab -l 2>/dev/null | grep -q duckdns-update; then
  (crontab -l 2>/dev/null; echo "*/5 * * * * $DUCK_SCRIPT") | crontab -
fi

# Run once now to register the IP
bash $DUCK_SCRIPT
info "DuckDNS updated. Check: https://www.duckdns.org"

# ── 5. Open firewall ports ────────────────────────────────────────────────────
info "Opening firewall ports 80 and 443..."
if command -v ufw &> /dev/null; then
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw allow 22/tcp
  ufw --force enable
fi

# ── 6. Issue Let's Encrypt certificate ───────────────────────────────────────
info "Issuing SSL certificate for $DOMAIN..."
COMPOSE_CMD="docker compose -f $DEPLOY_DIR/infrastructure/docker/docker-compose.yml"

# Start nginx in init (HTTP-only) mode to serve ACME challenge
docker run -d --name plm-nginx-init \
  -p 80:80 \
  -v plm_certbot_webroot:/var/www/certbot \
  nginx:1.25-alpine \
  sh -c "mkdir -p /var/www/certbot && nginx -g 'daemon off;' 2>/dev/null" || true

sleep 3

# Run certbot to issue certificate
docker run --rm \
  -v plm_letsencrypt_data:/etc/letsencrypt \
  -v plm_certbot_webroot:/var/www/certbot \
  certbot/certbot certonly \
    --webroot \
    --webroot-path /var/www/certbot \
    --email "admin@$DOMAIN" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN" \
  && info "SSL certificate issued successfully!" \
  || error "Certificate issuance failed. Check that $DOMAIN resolves to this server's IP."

# Stop the init nginx
docker stop plm-nginx-init && docker rm plm-nginx-init || true

# ── 7. Start full stack ───────────────────────────────────────────────────────
info "Starting full PLM stack..."
cd $DEPLOY_DIR/infrastructure/docker
docker compose pull --quiet
docker compose up -d --build

info "Waiting for services to start..."
sleep 30

# ── 8. Done ───────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          PLM Platform is ready!                      ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  App:      ${GREEN}https://$DOMAIN${NC}"
echo -e "  Grafana:  http://$DOMAIN:3000  (admin / see .env GRAFANA_PASSWORD)"
echo -e "  MinIO:    http://$DOMAIN:9001  (minioadmin / see .env)"
echo ""
echo -e "  Login:  engineer1 / engineer123   (ENGINEER role)"
echo -e "          admin    / admin123       (ADMIN role)"
echo ""
echo -e "  To view logs:  docker compose -f $DEPLOY_DIR/infrastructure/docker/docker-compose.yml logs -f"
echo ""
