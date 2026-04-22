# Production Deployment Guide - JaspertReport

Guía completa para desplegar JaspertReport en un entorno de producción seguro y escalable.

---

## 🎯 Arquitectura Recomendada

```
┌─────────────────────────────────────────────────────────┐
│                    Cliente (PHP, Web)                    │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │   Proxy Reverso (Nginx)       │
        │   - SSL/TLS                   │
        │   - Rate Limiting             │
        │   - Load Balancing            │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┴───────────────────┐
        │                                   │
    ┌───▼────────┐               ┌────▼────────┐
    │  Instance 1 │               │  Instance 2  │
    │ JasperReport │               │ JasperReport │
    │   :8080     │               │   :8080      │
    └───┬────────┘               └────┬─────────┘
        │                             │
        └───────────────┬─────────────┘
                        │
                    ┌───▼────────┐
                    │   MySQL    │
                    │ Replicado  │
                    └────────────┘
```

---

## ✅ Pre-Requisitos

### Hardware Mínimo
- **CPU:** 2 cores (1 core por instancia)
- **RAM:** 2-4 GB por instancia
- **Disco:** 20 GB (DB) + 10 GB (reportes/logs)
- **Conectividad:** Acceso a BD MySQL, red interna

### Software Requerido
- **Java 17+** (OpenJDK recomendado)
- **MySQL 8.0+** (o MariaDB 10.5+)
- **Nginx 1.18+** (como proxy reverso)
- **Firewall** configurado
- **Certificado SSL/TLS** válido

---

## 🔐 Seguridad

### 1. Gestión de Secretos

**NUNCA hardcodees credenciales.** Usa variables de entorno:

```bash
# Linux: /etc/environment
export SERVICE_TOKEN="$(openssl rand -hex 32)"
export DB_URL="jdbc:mysql://db-prod.example.com:3306/reportes"
export DB_USER="jasper_user"
export DB_PASSWORD="$(openssl rand -base64 32)"
```

O con Docker Secrets:

```bash
echo "mi-token-secreto" | docker secret create service_token -
```

### 2. Firewall

```bash
# Aceptar solo tráfico necesario
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP → Nginx
ufw allow 443/tcp   # HTTPS → Nginx
ufw deny 8080       # JasperReport interno
ufw deny 3306       # MySQL interno
```

### 3. Certificado SSL/TLS

```bash
# Opción A: Let's Encrypt (gratuito)
sudo apt install certbot python3-certbot-nginx
sudo certbot certonly --nginx -d tu-dominio.com

# Opción B: Certificado pagado
# Coloca en: /etc/ssl/certs/
```

### 4. Spring Security (Aplicación)

Habilita en `application.properties`:

```properties
# Logging de requests sensibles
logging.level.org.springframework.security=DEBUG

# HTTPS only (si está detrás de proxy)
server.http2.enabled=true
```

---

## 🚀 Deployment Opción A: Systememd (Linux)

### Paso 1: Crear usuario de servicio

```bash
sudo useradd -r -s /bin/bash jaspertreport
sudo mkdir -p /opt/jaspertreport
sudo chown -R jaspertreport:jaspertreport /opt/jaspertreport
```

### Paso 2: Copiar aplicación

```bash
sudo cp target/JaspertReport-0.0.1-SNAPSHOT.jar /opt/jaspertreport/
sudo chown jaspertreport:jaspertreport /opt/jaspertreport/JaspertReport-*.jar
```

### Paso 3: Crear archivo de servicio

```bash
sudo nano /etc/systemd/system/jaspertreport.service
```

```ini
[Unit]
Description=JasperReport Dynamic Report Engine
After=network.target mysql.service

[Service]
Type=simple
User=jaspertreport
WorkingDirectory=/opt/jaspertreport

# Variables de entorno
EnvironmentFile=/etc/jaspertreport/env

# Comando
ExecStart=/usr/bin/java -jar /opt/jaspertreport/JaspertReport-0.0.1-SNAPSHOT.jar

# Reinicio automático
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=jaspertreport

# Límites
MemoryLimit=2G
CPUQuota=50%

[Install]
WantedBy=multi-user.target
```

### Paso 4: Crear archivo de variables

```bash
sudo nano /etc/jaspertreport/env
```

```bash
SERVICE_TOKEN=xxxxxxxxxxxxx
DB_URL=jdbc:mysql://db-prod:3306/reportes
DB_USER=jasper_user
DB_PASSWORD=xxxxxxxxxxxxx
REPORTES_RUTA=/opt/jaspertreport/reportes/
SERVER_PORT=8080
```

### Paso 5: Habilitar y arrancar

```bash
sudo systemctl daemon-reload
sudo systemctl enable jaspertreport
sudo systemctl start jaspertreport

# Verificar estado
sudo systemctl status jaspertreport
journalctl -u jaspertreport -f  # Logs en vivo
```

---

## 🔧 Nginx como Proxy Reverso

```nginx
# nginx.conf

upstream jaspertreport {
    server jaspertreport:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name tu-dominio.com;
    
    # Redirigir HTTP → HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name tu-dominio.com;

    # Certificados SSL
    ssl_certificate /etc/nginx/certs/cert.pem;
    ssl_certificate_key /etc/nginx/certs/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Headers de seguridad
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/jaspertreport_access.log;
    error_log /var/log/nginx/jaspertreport_error.log;

    # Límite de rate (100 requests por minuto)
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;
    limit_req zone=api_limit burst=20 nodelay;

    # Proxy
    location /reportes/ {
        proxy_pass http://jaspertreport;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Salud
    location /health {
        proxy_pass http://jaspertreport/health;
        access_log off;
    }
}
```

---

## 📊 Monitoreo

### Health Check

Agrega endpoint en Spring:

```java
@RestController
@RequestMapping("/health")
public class HealthController {
    
    @GetMapping
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        ));
    }
}
```

### Logs Centralizados

Usa **ELK Stack** o **Loki**:

```properties
# application.properties
logging.file.name=/var/log/jaspertreport/app.log
logging.file.max-size=100MB
logging.file.max-history=30
```

### Métricas (Actuator)

```properties
# Habilitar endpoints
management.endpoints.web.exposure.include=health,metrics
management.endpoint.health.show-details=when-authorized
```

Accede: `https://tu-dominio.com/actuator/metrics`

---

## 🔄 CI/CD Pipeline

### GitHub Actions Ejemplo

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: 17
      
      - name: Build
        run: mvn clean package -DskipTests
      
      - name: Deploy SSH
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/jaspertreport
            sudo systemctl restart jaspertreport
```

---

## 🔙 Backups

```bash
# Backup de BD
mysqldump -h db-prod -u jasper_user -p reportes > backup-$(date +%Y%m%d).sql

# Backup de reportes
tar -czf reportes-backup-$(date +%Y%m%d).tar.gz /opt/jaspertreport/reportes/

# Script automático (cron)
# 0 2 * * * /usr/local/bin/backup-jaspertreport.sh
```

---

## 🆘 Troubleshooting

### "Out of Memory"
```
Solución: Aumenta memoria en systemd o Docker
ExecStart=java -Xmx1024m -jar ...
```

### "BD Connection Pool exhausted"
```
Solución: Aumenta pool size en application.properties
spring.datasource.hikari.maximum-pool-size=20
```

### "Reportes no se generan"
```
Verificar:
1. /opt/jaspertreport/reportes/ existe y tiene permisos
2. Archivos .jrxml están presentes
3. Logs: journalctl -u jaspertreport -n 50
```

---

## 📞 Monitoreo en Vivo

```bash
# Logs en vivo
sudo journalctl -u jaspertreport -f

# Uso de recursos
docker stats jaspertreport

# Conexiones a BD
mysql -h db-prod -u root -p -e "SHOW PROCESSLIST;"

# Requests por segundo
tail -f /var/log/nginx/jaspertreport_access.log | awk '{print $4}' | uniq -c
```

---

**¡Tu aplicación está lista para producción!** 🎉
