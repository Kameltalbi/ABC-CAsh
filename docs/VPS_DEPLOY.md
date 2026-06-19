# Déploiement ABC Cash API sur VPS Ubuntu

API **Kotlin Ktor** + **PostgreSQL**, sans Supabase.  
Conçue pour coexister avec d'autres applications sur le même VPS (port local **8081**, path Nginx `/abc-cash/`).

## Prérequis sur le VPS

- Ubuntu 22/24 LTS
- Docker + Docker Compose
- Nginx (déjà utilisé pour vos autres apps)

## 1. Build du JAR (en local ou sur le VPS)

```bash
cd "/chemin/vers/ABC CASH"
./gradlew :server:jar
```

Le JAR est généré dans `server/build/libs/server-1.0.0.jar`.

## 2. Déploiement Docker

```bash
cd deploy
cp .env.example .env
# Éditer .env : JWT_SECRET, POSTGRES_PASSWORD, DATABASE_PASSWORD

docker compose build
docker compose up -d
```

Vérification locale sur le VPS :

```bash
curl http://127.0.0.1:8081/health
# {"status":"ok","service":"abc-cash-api","version":"1.0.0"}
```

## 3. Nginx (multi-apps)

Ajoutez le bloc de `nginx-abc-cash.conf.example` dans votre config Nginx existante, puis :

```bash
sudo nginx -t && sudo systemctl reload nginx
```

Test public :

```bash
curl http://213.130.144.183/abc-cash/health
```

## 4. Endpoints API

| Méthode | URL | Auth |
|---------|-----|------|
| GET | `/health` | Non |
| POST | `/v1/auth/register` | Non |
| POST | `/v1/auth/login` | Non |
| GET | `/v1/sync` | JWT |
| POST | `/v1/sync` | JWT |

Base URL app Android : `http://213.130.144.183/abc-cash/` (ou votre domaine HTTPS).

### Inscription

```bash
curl -X POST http://213.130.144.183/abc-cash/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "entrepriseNom": "Ma Société",
    "nom": "Admin",
    "email": "admin@example.com",
    "telephone": "+21600000000",
    "password": "secret12"
  }'
```

### Login

```bash
curl -X POST http://213.130.144.183/abc-cash/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"secret12"}'
```

### Sync (avec token)

```bash
curl http://213.130.144.183/abc-cash/v1/sync \
  -H "Authorization: Bearer VOTRE_TOKEN"
```

## 5. Sécurité

- Changez **JWT_SECRET** et mots de passe Postgres en production
- Préférez **HTTPS** (Let's Encrypt) sur votre domaine
- Postgres n'est **pas** exposé sur Internet (réseau Docker interne uniquement)
- Sauvegarde Postgres : `docker exec abc-cash-db pg_dump -U abc_cash abc_cash > backup.sql`

## 6. Prochaine étape (app Android)

Connecter l'app à `http://213.130.144.183/abc-cash/` :
- inscription / login via API
- sync automatique push/pull
- Room en cache offline
