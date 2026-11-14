import json
from pathlib import Path
from typing import Optional, Dict, Any, List
from ytmusicapi import YTMusic
from app.config import STORAGE_DIR
from app.models.schemas import Track, Artist, Album, Playlist

YTMUSIC_SESSION_FILE = STORAGE_DIR / "ytmusic_session.json"

class YTMusicService:
    def __init__(self):
        self.client: Optional[YTMusic] = None
        self.user_profile: Optional[Dict[str, Any]] = None
        self.is_authenticated: bool = False
        self._init_client()

    def _init_client(self):
        if YTMUSIC_SESSION_FILE.exists():
            try:
                self.client = YTMusic(str(YTMUSIC_SESSION_FILE))
                self.is_authenticated = True
                self.user_profile = {
                    "display_name": "YouTube Music User",
                    "id": "yt_user",
                    "channel": "Connected Account"
                }
            except Exception:
                self.client = YTMusic()
                self.is_authenticated = False
        else:
            self.client = YTMusic()
            self.is_authenticated = False

    def is_connected(self) -> bool:
        return self.is_authenticated

    def set_cookie_session(self, cookie_string: str) -> bool:
        """Sets YouTube session using browser cookie headers."""
        try:
            headers_raw = {
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Accept": "*/*",
                "Accept-Language": "en-US,en;q=0.5",
                "Content-Type": "application/json",
                "X-Goog-AuthUser": "0",
                "x-origin": "https://music.youtube.com",
                "cookie": cookie_string
            }
            with open(YTMUSIC_SESSION_FILE, "w") as f:
                json.dump(headers_raw, f, indent=2)

            self.client = YTMusic(str(YTMUSIC_SESSION_FILE))
            self.is_authenticated = True
            self.user_profile = {
                "display_name": "YouTube Music User",
                "id": "yt_connected",
                "channel": "YouTube Library"
            }
            return True
        except Exception as e:
            print("Failed to initialize ytmusic session with cookies:", e)
            return False

    def set_demo_connected(self):
        """Allows testing YouTube Music active mode with instant mock connection."""
        self.is_authenticated = True
        self.user_profile = {
            "display_name": "YouTube User (Demo)",
            "id": "yt_demo",
            "channel": "My YouTube Channel"
        }

    def get_liked_music(self, limit: int = 50) -> List[Track]:
        """Fetches tracks from the user's YouTube Liked Music playlist (LM)."""
        if not self.is_authenticated or not self.client:
            # Fallback to general charts if unauthenticated
            return self.get_trending_tracks(limit)

        try:
            liked = self.client.get_liked_songs(limit=limit)
            tracks: List[Track] = []
            for item in liked.get("tracks", []):
                vid = item.get("videoId")
                if not vid:
                    continue
                artists = [Artist(name=a["name"], id=a.get("id")) for a in item.get("artists", [])]
                album_info = item.get("album") or {}
                cover = item.get("thumbnails", [{}])[-1].get("url") if item.get("thumbnails") else None
                
                tracks.append(Track(
                    id=vid,
                    title=item.get("title", "Unknown"),
                    artists=artists,
                    album=Album(name=album_info.get("name", ""), id=album_info.get("id"), cover_url=cover),
                    duration_seconds=int(item.get("duration_seconds", 0) or 0),
                    cover_url=cover,
                    source="youtube",
                    youtube_id=vid,
                    is_liked=True
                ))
            return tracks
        except Exception as e:
            print("Error fetching liked songs:", e)
            return self.get_trending_tracks(limit)

    def get_user_playlists(self, limit: int = 30) -> List[Playlist]:
        """Fetches user's created YouTube Music playlists."""
        if not self.is_authenticated or not self.client:
            return []
        try:
            playlists_data = self.client.get_library_playlists(limit=limit)
            playlists: List[Playlist] = []
            for item in playlists_data:
                cover = item.get("thumbnails", [{}])[-1].get("url") if item.get("thumbnails") else None
                playlists.append(Playlist(
                    id=item.get("playlistId"),
                    title=item.get("title", "Untitled Playlist"),
                    description=item.get("description", ""),
                    cover_url=cover,
                    track_count=int(item.get("count", 0) or 0),
                    source="youtube"
                ))
            return playlists
        except Exception as e:
            print("Error fetching library playlists:", e)
            return []

    def get_trending_tracks(self, limit: int = 25) -> List[Track]:
        """Returns global trending and chart tracks from YouTube Music."""
        try:
            charts = self.client.get_charts(country="GLOBAL")
            tracks: List[Track] = []
            items = charts.get("videos", {}).get("items", [])
            for item in items[:limit]:
                vid = item.get("videoId")
                if not vid:
                    continue
                artists = [Artist(name=a.get("name", "Various Artists")) for a in item.get("artists", [])]
                cover = item.get("thumbnails", [{}])[-1].get("url") if item.get("thumbnails") else None
                tracks.append(Track(
                    id=vid,
                    title=item.get("title", "Trending Hit"),
                    artists=artists,
                    cover_url=cover,
                    source="youtube",
                    youtube_id=vid,
                    duration_seconds=0
                ))
            return tracks
        except Exception as e:
            print("Error fetching trending tracks:", e)
            return []

    def save_track(self, video_id: str) -> bool:
        """Likes a track on YouTube Music (saves to Liked Music)."""
        if not self.is_authenticated or not self.client:
            # If in demo mode, acknowledge save
            return True
        try:
            self.client.rate_song(video_id, "LIKE")
            return True
        except Exception as e:
            print(f"Error liking song {video_id}:", e)
            return False

    def remove_track(self, video_id: str) -> bool:
        """Removes like from a song on YouTube Music."""
        if not self.is_authenticated or not self.client:
            return True
        try:
            self.client.rate_song(video_id, "INDIFFERENT")
            return True
        except Exception as e:
            print(f"Error unliking song {video_id}:", e)
            return False

    def disconnect(self):
        self.is_authenticated = False
        self.user_profile = None
        if YTMUSIC_SESSION_FILE.exists():
            YTMUSIC_SESSION_FILE.unlink()

ytmusic_service = YTMusicService()
