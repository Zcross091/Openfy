import requests
from fastapi import FastAPI, Query, HTTPException, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse, JSONResponse
from typing import Optional, Literal

from app.config import CORS_ORIGINS
from app.models.schemas import (
    ActiveAccountState, SwitchAccountRequest, SaveTrackRequest,
    Track, SearchResult, LyricsResponse, StreamInfo
)
from app.services.audio_resolver import audio_resolver
from app.services.spotify_service import spotify_service
from app.services.ytmusic_service import ytmusic_service
from app.services.lyrics_service import lyrics_service

app = FastAPI(
    title="Openfy Audio & Library API",
    description="Backend for Openfy: Spotify UI with RVX YouTube Music Audio Engine",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global active provider state ('spotify', 'youtube', or 'guest')
_current_active_provider: Literal["spotify", "youtube", "guest"] = "guest"

# Auto-detect if any provider is already connected
if spotify_service.is_connected():
    _current_active_provider = "spotify"
elif ytmusic_service.is_connected():
    _current_active_provider = "youtube"


@app.get("/api/health")
def health_check():
    return {
        "status": "healthy",
        "app": "Openfy",
        "active_provider": _current_active_provider,
        "spotify_connected": spotify_service.is_connected(),
        "youtube_connected": ytmusic_service.is_connected()
    }


# ==========================================
# Account & Auth Endpoints
# ==========================================

@app.get("/api/auth/status", response_model=ActiveAccountState)
def get_auth_status():
    global _current_active_provider
    # Verify connections
    sp_conn = spotify_service.is_connected()
    yt_conn = ytmusic_service.is_connected()

    # If active provider disconnected, fallback to another or guest
    if _current_active_provider == "spotify" and not sp_conn:
        _current_active_provider = "youtube" if yt_conn else "guest"
    elif _current_active_provider == "youtube" and not yt_conn:
        _current_active_provider = "spotify" if sp_conn else "guest"

    return ActiveAccountState(
        active_provider=_current_active_provider,
        spotify_connected=sp_conn,
        youtube_connected=yt_conn,
        spotify_user=spotify_service.user_profile,
        youtube_user=ytmusic_service.user_profile
    )


@app.post("/api/auth/active-account")
def switch_active_account(req: SwitchAccountRequest):
    global _current_active_provider
    _current_active_provider = req.provider
    return {"success": True, "active_provider": _current_active_provider}


@app.post("/api/auth/spotify/token")
def set_spotify_token(payload: dict):
    """Handles token set from frontend OAuth PKCE flow."""
    access_token = payload.get("access_token")
    refresh_token = payload.get("refresh_token")
    expires_in = payload.get("expires_in", 3600)
    
    if not access_token:
        raise HTTPException(status_code=400, detail="Missing access_token")
    
    spotify_service.set_token_direct(access_token, refresh_token, expires_in)
    global _current_active_provider
    _current_active_provider = "spotify"
    return {"success": True, "user": spotify_service.user_profile}


@app.post("/api/auth/spotify/disconnect")
def disconnect_spotify():
    global _current_active_provider
    spotify_service.disconnect()
    if _current_active_provider == "spotify":
        _current_active_provider = "youtube" if ytmusic_service.is_connected() else "guest"
    return {"success": True}


@app.post("/api/auth/youtube/cookie")
def connect_youtube_cookie(payload: dict):
    cookie = payload.get("cookie")
    if not cookie:
        raise HTTPException(status_code=400, detail="Missing cookie string")
    success = ytmusic_service.set_cookie_session(cookie)
    if success:
        global _current_active_provider
        _current_active_provider = "youtube"
        return {"success": True, "user": ytmusic_service.user_profile}
    raise HTTPException(status_code=400, detail="Failed to initialize YouTube session with provided cookies")


@app.post("/api/auth/youtube/demo")
def connect_youtube_demo():
    """Allows one-click demo connection for YouTube Music."""
    ytmusic_service.set_demo_connected()
    global _current_active_provider
    _current_active_provider = "youtube"
    return {"success": True, "user": ytmusic_service.user_profile}


@app.post("/api/auth/youtube/disconnect")
def disconnect_youtube():
    global _current_active_provider
    ytmusic_service.disconnect()
    if _current_active_provider == "youtube":
        _current_active_provider = "spotify" if spotify_service.is_connected() else "guest"
    return {"success": True}


# ==========================================
# Audio Streaming & Resolution Endpoints
# ==========================================

@app.get("/api/track/resolve")
def resolve_track(title: str, artist: str, duration: int = 0):
    """Resolves a track to its YouTube Music videoId and direct stream info."""
    video_id = audio_resolver.resolve_spotify_track(title, artist, duration)
    if not video_id:
        raise HTTPException(status_code=404, detail="Track could not be matched on YouTube Music")
    
    stream_info = audio_resolver.get_stream_info(video_id)
    if not stream_info:
        raise HTTPException(status_code=502, detail="Failed to extract audio stream")

    return {
        "youtube_id": video_id,
        "stream_url": stream_info.stream_url,
        "format": stream_info.format,
        "bitrate": stream_info.bitrate,
        "duration_seconds": stream_info.duration_seconds
    }


@app.get("/api/stream")
def stream_audio(request: Request, id: str = Query(..., description="YouTube Video ID")):
    """Streams audio with full HTTP 206 Partial Content range support."""
    stream_info = audio_resolver.get_stream_info(id)
    if not stream_info or not stream_info.stream_url:
        raise HTTPException(status_code=404, detail="Audio stream not found")

    range_header = request.headers.get("Range")
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
    if range_header:
        headers["Range"] = range_header

    try:
        upstream_resp = requests.get(stream_info.stream_url, headers=headers, stream=True, timeout=10)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Upstream stream error: {e}")

    response_headers = {
        "Content-Type": upstream_resp.headers.get("Content-Type", "audio/webm"),
        "Accept-Ranges": "bytes",
    }
    for h in ["Content-Range", "Content-Length"]:
        if h in upstream_resp.headers:
            response_headers[h] = upstream_resp.headers[h]

    return StreamingResponse(
        upstream_resp.iter_content(chunk_size=65536),
        status_code=upstream_resp.status_code,
        headers=response_headers
    )


# ==========================================
# Discovery & Search Endpoints
# ==========================================

@app.get("/api/search", response_model=SearchResult)
def search(q: str = Query(..., min_length=1)):
    """Unified search for songs and playlists."""
    tracks = audio_resolver.search_tracks(q, limit=20)
    return SearchResult(query=q, tracks=tracks)


@app.get("/api/charts")
def get_charts():
    """Returns top trending tracks and mixes."""
    tracks = ytmusic_service.get_trending_tracks(limit=30)
    return {"trending": tracks}


@app.get("/api/lyrics", response_model=LyricsResponse)
def get_lyrics(title: str, artist: str, duration: int = 0):
    """Fetches synchronized karaoke lyrics."""
    return lyrics_service.get_lyrics(title, artist, duration)


# ==========================================
# Library Management (Active Account)
# ==========================================

@app.get("/api/library/liked")
def get_liked_library():
    """Fetches saved/liked tracks for the CURRENT ACTIVE ACCOUNT."""
    if _current_active_provider == "spotify":
        tracks = spotify_service.get_liked_tracks(limit=50)
        return {"provider": "spotify", "tracks": tracks}
    elif _current_active_provider == "youtube":
        tracks = ytmusic_service.get_liked_music(limit=50)
        return {"provider": "youtube", "tracks": tracks}
    else:
        # Guest mode: return trending explore list
        tracks = ytmusic_service.get_trending_tracks(limit=25)
        return {"provider": "guest", "tracks": tracks}


@app.get("/api/library/playlists")
def get_user_playlists():
    """Fetches playlists for the CURRENT ACTIVE ACCOUNT."""
    if _current_active_provider == "spotify":
        playlists = spotify_service.get_user_playlists(limit=30)
        return {"provider": "spotify", "playlists": playlists}
    elif _current_active_provider == "youtube":
        playlists = ytmusic_service.get_user_playlists(limit=30)
        return {"provider": "youtube", "playlists": playlists}
    else:
        return {"provider": "guest", "playlists": []}


@app.post("/api/library/save")
def save_track_to_library(req: SaveTrackRequest):
    """
    Saves/Likes the current track to the user's active library!
    If target_provider is specified, saves to that provider; otherwise saves to active provider.
    """
    target = req.target_provider or _current_active_provider
    saved = False

    if target == "spotify":
        saved = spotify_service.save_track(
            spotify_id=req.spotify_id or req.track_id if req.spotify_id else None,
            title=req.title,
            artist=req.artist
        )
    elif target == "youtube":
        # Need youtube video id
        vid = req.youtube_id or req.track_id
        if not vid or not vid.startswith("http") and len(vid) != 11:
            vid = audio_resolver.resolve_spotify_track(req.title, req.artist)
        if vid:
            saved = ytmusic_service.save_track(vid)
    else:
        # Guest mode
        saved = True

    return {
        "success": saved,
        "provider": target,
        "track_title": req.title
    }


@app.delete("/api/library/save")
def remove_track_from_library(track_id: str, provider: Optional[str] = None):
    """Removes a track from the user's active library."""
    target = provider or _current_active_provider
    removed = False

    if target == "spotify":
        removed = spotify_service.remove_track(track_id)
    elif target == "youtube":
        removed = ytmusic_service.remove_track(track_id)
    else:
        removed = True

    return {"success": removed, "provider": target}
