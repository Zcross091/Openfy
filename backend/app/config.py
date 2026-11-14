import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
STORAGE_DIR = BASE_DIR / "storage"
CACHE_DIR = BASE_DIR / "cache"

STORAGE_DIR.mkdir(parents=True, exist_ok=True)
CACHE_DIR.mkdir(parents=True, exist_ok=True)

# Default Spotify Public Client ID (Users can override in UI or via env var)
# This uses standard Spotify Authorization Code Flow with PKCE (public client)
SPOTIFY_DEFAULT_CLIENT_ID = os.getenv("SPOTIFY_CLIENT_ID", "4d14fa9e2a74421b8b80b74100c61cf4")
SPOTIFY_REDIRECT_URI = os.getenv("SPOTIFY_REDIRECT_URI", "http://localhost:5173/callback")

# Server Config
HOST = os.getenv("HOST", "127.0.0.1")
PORT = int(os.getenv("PORT", 8000))

# CORS settings
CORS_ORIGINS = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://localhost:3000",
    "http://127.0.0.1:3000",
    "*"
]
