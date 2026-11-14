import requests
import json
import time
from typing import Optional, Dict, Any, List
from pathlib import Path
from app.config import STORAGE_DIR, SPOTIFY_DEFAULT_CLIENT_ID
from app.models.schemas import Track, Artist, Album, Playlist

SPOTIFY_TOKEN_FILE = STORAGE_DIR / "spotify_session.json"

class SpotifyService:
    def __init__(self):
        self.session = requests.Session()
        self.access_token: Optional[str] = None
        self.refresh_token: Optional[str] = None
        self.expires_at: float = 0
        self.user_profile: Optional[Dict[str, Any]] = None
        self._load_session()

    def _load_session(self):
        if SPOTIFY_TOKEN_FILE.exists():
            try:
                with open(SPOTIFY_TOKEN_FILE, "r") as f:
                    data = json.load(f)
                    self.access_token = data.get("access_token")
                    self.refresh_token = data.get("refresh_token")
                    self.expires_at = data.get("expires_at", 0)
                    self.user_profile = data.get("user_profile")
            except Exception:
                pass

    def _save_session(self):
        try:
            with open(SPOTIFY_TOKEN_FILE, "w") as f:
                json.dump({
                    "access_token": self.access_token,
                    "refresh_token": self.refresh_token,
                    "expires_at": self.expires_at,
                    "user_profile": self.user_profile
                }, f, indent=2)
        except Exception:
            pass

    def is_connected(self) -> bool:
        return bool(self.access_token)

    def exchange_code(self, code: str, redirect_uri: str, code_verifier: str, client_id: Optional[str] = None) -> Dict[str, Any]:
        """Exchanges authorization code for access and refresh tokens using PKCE."""
        cid = client_id or SPOTIFY_DEFAULT_CLIENT_ID
        url = "https://accounts.spotify.com/api/token"
        payload = {
            "grant_type": "authorization_code",
            "code": code,
            "redirect_uri": redirect_uri,
            "client_id": cid,
            "code_verifier": code_verifier
        }
        headers = {"Content-Type": "application/x-www-form-urlencoded"}

        resp = self.session.post(url, data=payload, headers=headers)
        if resp.status_code == 200:
            data = resp.json()
            self.access_token = data.get("access_token")
            self.refresh_token = data.get("refresh_token", self.refresh_token)
            self.expires_at = time.time() + data.get("expires_in", 3600) - 60
            
            # Fetch profile
            self.fetch_profile()
            self._save_session()
            return {"success": True, "user": self.user_profile}
        else:
            return {"success": False, "error": resp.text}

    def set_token_direct(self, access_token: str, refresh_token: Optional[str] = None, expires_in: int = 3600):
        """Allows direct token setting from frontend PKCE completion."""
        self.access_token = access_token
        if refresh_token:
            self.refresh_token = refresh_token
        self.expires_at = time.time() + expires_in - 60
        self.fetch_profile()
        self._save_session()

    def fetch_profile(self) -> Optional[Dict[str, Any]]:
        if not self.access_token:
            return None
        headers = {"Authorization": f"Bearer {self.access_token}"}
        resp = self.session.get("https://api.spotify.com/v1/me", headers=headers)
        if resp.status_code == 200:
            self.user_profile = resp.json()
            self._save_session()
            return self.user_profile
        return None

    def get_liked_tracks(self, limit: int = 50) -> List[Track]:
        """Fetches user's saved Liked Songs from Spotify."""
        if not self.access_token:
            return []
        headers = {"Authorization": f"Bearer {self.access_token}"}
        resp = self.session.get(f"https://api.spotify.com/v1/me/tracks?limit={limit}", headers=headers)
        if resp.status_code != 200:
            return []

        tracks: List[Track] = []
        data = resp.json()
        for item in data.get("items", []):
            t = item.get("track")
            if not t:
                continue
            
            artists = [Artist(name=a["name"], id=a.get("id")) for a in t.get("artists", [])]
            album_info = t.get("album", {})
            album_images = album_info.get("images", [])
            cover_url = album_images[0]["url"] if album_images else None
            album = Album(name=album_info.get("name", ""), id=album_info.get("id"), cover_url=cover_url)

            duration_sec = int(t.get("duration_ms", 0) / 1000)

            tracks.append(Track(
                id=t.get("id"),
                title=t.get("name", "Unknown"),
                artists=artists,
                album=album,
                duration_seconds=duration_sec,
                cover_url=cover_url,
                source="spotify",
                spotify_id=t.get("id"),
                is_liked=True
            ))

        return tracks

    def get_user_playlists(self, limit: int = 30) -> List[Playlist]:
        """Fetches user's playlists from Spotify."""
        if not self.access_token:
            return []
        headers = {"Authorization": f"Bearer {self.access_token}"}
        resp = self.session.get(f"https://api.spotify.com/v1/me/playlists?limit={limit}", headers=headers)
        if resp.status_code != 200:
            return []

        playlists: List[Playlist] = []
        for item in resp.json().get("items", []):
            images = item.get("images", [])
            cover_url = images[0]["url"] if images else None
            playlists.append(Playlist(
                id=item.get("id"),
                title=item.get("name", "Untitled Playlist"),
                description=item.get("description", ""),
                cover_url=cover_url,
                track_count=item.get("tracks", {}).get("total", 0),
                source="spotify"
            ))
        return playlists

    def save_track(self, spotify_id: Optional[str] = None, title: Optional[str] = None, artist: Optional[str] = None) -> bool:
        """Saves (likes) a track to Spotify library."""
        if not self.access_token:
            return False

        # If spotify_id not given, search Spotify for the track
        track_id = spotify_id
        if not track_id and title and artist:
            track_id = self.search_track_id(title, artist)

        if not track_id:
            return False

        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json"
        }
        resp = self.session.put(f"https://api.spotify.com/v1/me/tracks?ids={track_id}", headers=headers)
        return resp.status_code in [200, 201]

    def remove_track(self, spotify_id: str) -> bool:
        """Removes a track from Spotify Liked Songs."""
        if not self.access_token:
            return False
        headers = {"Authorization": f"Bearer {self.access_token}"}
        resp = self.session.delete(f"https://api.spotify.com/v1/me/tracks?ids={spotify_id}", headers=headers)
        return resp.status_code in [200, 204]

    def search_track_id(self, title: str, artist: str) -> Optional[str]:
        """Searches Spotify to find a track ID by title and artist."""
        if not self.access_token:
            return None
        headers = {"Authorization": f"Bearer {self.access_token}"}
        query = f"track:{title} artist:{artist}"
        resp = self.session.get("https://api.spotify.com/v1/search", params={"q": query, "type": "track", "limit": 1}, headers=headers)
        if resp.status_code == 200:
            items = resp.json().get("tracks", {}).get("items", [])
            if items:
                return items[0].get("id")
        return None

    def disconnect(self):
        self.access_token = None
        self.refresh_token = None
        self.user_profile = None
        if SPOTIFY_TOKEN_FILE.exists():
            SPOTIFY_TOKEN_FILE.unlink()

spotify_service = SpotifyService()
