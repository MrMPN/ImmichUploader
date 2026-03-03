# Unraid Deployment (Docker Image)

This repo can publish a versioned web image to GHCR via:

- `.github/workflows/publish-web.yml`

The image name is:

- `ghcr.io/<your-github-username>/immichuploader-web`

Examples of tags produced by CI:

- `main` (every push to default branch)
- `sha-<commit>`
- `v1.2.3` (when you push git tag `v1.2.3`)

## Unraid `docker-compose.yml` service

```yaml
services:
  immichuploader-web:
    image: ghcr.io/<your-github-username>/immichuploader-web:main
    container_name: immichuploader-web
    restart: unless-stopped
    ports:
      - "8088:8080"
```

URL input behavior:

- You can enter a full Immich URL such as `https://fotos.example.com` or `http://192.168.1.50:2283`.
- The web app relays API traffic through same-origin `/__immich_proxy/...`, so browser CORS setup is not required.
- Relative paths like `/api` are still accepted when you already have your own reverse-proxy route.

If the package is private, run once on Unraid:

```bash
docker login ghcr.io
```

Use a GitHub personal access token with `read:packages`.

## Update command on Unraid

```bash
docker compose pull immichuploader-web && docker compose up -d immichuploader-web
```

## Rollback

Pin to a previous image tag in `docker-compose.yml`, then redeploy:

```yaml
image: ghcr.io/<your-github-username>/immichuploader-web:v1.2.3
```

```bash
docker compose up -d immichuploader-web
```
