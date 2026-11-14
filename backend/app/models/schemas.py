from typing import List, Optional, Literal
from pydantic import BaseModel, Field

class Artist(BaseModel):
    id: Optional[str] = None
    name: str

class Album(BaseModel):
    id: Optional[str] = None
    name: str
    cover_url: Optional[str] = None

class Track(BaseModel):
    id: str  # YouTube videoId or Spotify track ID
    title: str
    artists: List[Artist]
    album: Optional[Album] = None
    duration_seconds: int = 0
    cover_url: Optional[str] = None
    source: Literal["youtube", "spotify"] = "youtube"
    youtube_id: Optional[str] = None
    spotify_id: Optional[str] = None
    is_liked: bool = False

class Playlist(BaseModel):
    id: str
    title: str
    description: Optional[str] = ""
    cover_url: Optional[str] = None
    track_count: int = 0
    source: Literal["youtube", "spotify"] = "youtube"
    tracks: Optional[List[Track]] = []

class SearchResult(BaseModel):
    query: str
    tracks: List[Track] = []
    playlists: List[Playlist] = []
    artists: List[Artist] = []

class ActiveAccountState(BaseModel):
    active_provider: Literal["spotify", "youtube", "guest"] = "guest"
    spotify_connected: bool = False
    youtube_connected: bool = False
    spotify_user: Optional[dict] = None
    youtube_user: Optional[dict] = None

class SwitchAccountRequest(BaseModel):
    provider: Literal["spotify", "youtube", "guest"]

class SaveTrackRequest(BaseModel):
    track_id: str
    title: str
    artist: str
    album: Optional[str] = None
    youtube_id: Optional[str] = None
    spotify_id: Optional[str] = None
    target_provider: Optional[Literal["spotify", "youtube"]] = None

class LyricLine(BaseModel):
    time: float  # seconds
    text: str

class LyricsResponse(BaseModel):
    track_title: str
    artist_name: str
    is_synced: bool
    lyrics: List[LyricLine] = []
    plain_lyrics: Optional[str] = None

class StreamInfo(BaseModel):
    stream_url: str
    youtube_id: str
    format: str
    bitrate: Optional[int] = None
    duration_seconds: int
