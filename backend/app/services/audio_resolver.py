import yt_dlp
from ytmusicapi import YTMusic
from typing import Optional, Dict, Any, List
from app.models.schemas import Track, Artist, Album, StreamInfo
from app.services.cache_manager import cache_manager

class AudioResolver:
    def __init__(self):
        self.ytmusic = YTMusic()
        self.ydl_opts = {
            "format": "bestaudio/best",
            "quiet": True,
            "no_warnings": True,
            "noplaylist": True,
            "socket_timeout": 10,
        }

    def search_tracks(self, query: str, limit: int = 20) -> List[Track]:
        """Searches YouTube Music for songs matching query."""
        cache_key = f"search::tracks::{query.lower()}::{limit}"
        cached = cache_manager.get_search(cache_key)
        if cached:
            return cached

        try:
            results = self.ytmusic.search(query, filter="songs")
        except Exception:
            results = []

        tracks: List[Track] = []
        for item in results[:limit]:
            video_id = item.get("videoId")
            if not video_id:
                continue

            artists = [Artist(name=a.get("name", "Unknown Artist")) for a in item.get("artists", [])]
            album_info = item.get("album")
            album = None
            if album_info and isinstance(album_info, dict):
                album = Album(name=album_info.get("name", ""), id=album_info.get("id"))

            thumbnails = item.get("thumbnails", [])
            cover_url = thumbnails[-1].get("url") if thumbnails else None

            duration_str = item.get("duration", "0:00")
            duration_sec = self._parse_duration(duration_str)

            tracks.append(Track(
                id=video_id,
                title=item.get("title", "Unknown Title"),
                artists=artists,
                album=album,
                duration_seconds=duration_sec,
                cover_url=cover_url,
                source="youtube",
                youtube_id=video_id
            ))

        cache_manager.set_search(cache_key, tracks)
        return tracks

    def resolve_spotify_track(self, title: str, artist: str, duration_sec: int = 0) -> Optional[str]:
        """Finds the best matching YouTube Music videoId for a Spotify track."""
        cache_key = f"resolve::{title.lower()}::{artist.lower()}"
        cached = cache_manager.get_search(cache_key)
        if cached:
            return cached

        search_query = f"{title} {artist}"
        try:
            results = self.ytmusic.search(search_query, filter="songs")
            if not results:
                # Fallback to general search
                results = self.ytmusic.search(search_query)
        except Exception:
            results = []

        best_video_id = None
        min_diff = 999999

        for item in results:
            vid = item.get("videoId")
            if not vid:
                continue
            
            item_dur_str = item.get("duration", "0:00")
            item_dur = self._parse_duration(item_dur_str)

            if duration_sec > 0 and item_dur > 0:
                diff = abs(item_dur - duration_sec)
                if diff < min_diff:
                    min_diff = diff
                    best_video_id = vid
                    if diff <= 3:  # Excellent match within 3 seconds
                        break
            else:
                best_video_id = vid
                break

        if not best_video_id and results and results[0].get("videoId"):
            best_video_id = results[0]["videoId"]

        if best_video_id:
            cache_manager.set_search(cache_key, best_video_id)
        return best_video_id

    def get_stream_info(self, video_id: str) -> Optional[StreamInfo]:
        """Extracts direct audio stream URL from YouTube using yt-dlp."""
        cached_url = cache_manager.get_stream(video_id)
        if cached_url:
            return StreamInfo(
                stream_url=cached_url,
                youtube_id=video_id,
                format="opus/m4a",
                duration_seconds=0
            )

        video_url = f"https://music.youtube.com/watch?v=J{video_id}" if not video_id.startswith("http") else video_id
        if not video_url.startswith("http"):
            video_url = f"https://www.youtube.com/watch?v={video_id}"

        try:
            with yt_dlp.YoutubeDL(self.ydl_opts) as ydl:
                info = ydl.extract_info(video_url, download=False)
                stream_url = info.get("url")
                if stream_url:
                    duration = info.get("duration", 0)
                    bitrate = info.get("abr") or info.get("tbr")
                    fmt = info.get("format", "bestaudio")
                    
                    cache_manager.set_stream(video_id, stream_url)
                    return StreamInfo(
                        stream_url=stream_url,
                        youtube_id=video_id,
                        format=str(fmt),
                        bitrate=int(bitrate) if bitrate else None,
                        duration_seconds=int(duration)
                    )
        except Exception as e:
            print(f"Error extracting stream for {video_id}: {e}")
            return None

        return None

    def _parse_duration(self, dur_str: str) -> int:
        """Parses 'm:ss' or 'h:mm:ss' string into total seconds."""
        if not dur_str:
            return 0
        try:
            parts = [int(p) for p in dur_str.split(":")]
            if len(parts) == 3:
                return parts[0] * 3600 + parts[1] * 60 + parts[2]
            elif len(parts) == 2:
                return parts[0] * 60 + parts[1]
            elif len(parts) == 1:
                return parts[0]
        except Exception:
            pass
        return 0

audio_resolver = AudioResolver()
