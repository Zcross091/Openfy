import requests
import re
from typing import Optional, List
from app.models.schemas import LyricsResponse, LyricLine
from app.services.cache_manager import cache_manager

class LyricsService:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "Openfy-Music-Client/1.0 (https://github.com/Zcross091/Openfy)"
        })

    def parse_lrc(self, lrc_text: str) -> List[LyricLine]:
        """Parses [mm:ss.xx] timestamped lines into LyricLine objects."""
        lines = []
        pattern = re.compile(r"\[(\d{2}):(\d{2}(?:\.\d+)?)\](.*)")
        for line in lrc_text.splitlines():
            line = line.strip()
            if not line:
                continue
            match = pattern.match(line)
            if match:
                minutes = float(match.group(1))
                seconds = float(match.group(2))
                total_seconds = minutes * 60 + seconds
                text = match.group(3).strip()
                lines.append(LyricLine(time=total_seconds, text=text))
        return lines

    def get_lyrics(self, track_name: str, artist_name: str, duration_seconds: int = 0) -> LyricsResponse:
        cache_key = f"{track_name.lower()}::{artist_name.lower()}"
        cached = cache_manager.get_lyrics(cache_key)
        if cached:
            return cached

        # Try LRCLIB API
        url = "https://lrclib.net/api/get"
        params = {
            "track_name": track_name,
            "artist_name": artist_name,
        }
        if duration_seconds > 0:
            params["duration"] = duration_seconds

        try:
            resp = self.session.get(url, params=params, timeout=5)
            if resp.status_code == 200:
                data = resp.json()
                synced_lrc = data.get("syncedLyrics")
                plain_lyrics = data.get("plainLyrics")
                
                if synced_lrc:
                    parsed_lines = self.parse_lrc(synced_lrc)
                    result = LyricsResponse(
                        track_title=track_name,
                        artist_name=artist_name,
                        is_synced=True,
                        lyrics=parsed_lines,
                        plain_lyrics=plain_lyrics
                    )
                    cache_manager.set_lyrics(cache_key, result)
                    return result
                elif plain_lyrics:
                    result = LyricsResponse(
                        track_title=track_name,
                        artist_name=artist_name,
                        is_synced=False,
                        lyrics=[],
                        plain_lyrics=plain_lyrics
                    )
                    cache_manager.set_lyrics(cache_key, result)
                    return result
        except Exception:
            pass

        # Return empty response if not found
        fallback = LyricsResponse(
            track_title=track_name,
            artist_name=artist_name,
            is_synced=False,
            lyrics=[],
            plain_lyrics="Lyrics not available for this track."
        )
        return fallback

lyrics_service = LyricsService()
