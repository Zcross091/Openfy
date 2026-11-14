import pytest
from app.services.lyrics_service import lyrics_service
from app.services.cache_manager import cache_manager
from app.models.schemas import Track, Artist, Album

def test_lyrics_parser():
    lrc_sample = """
    [00:12.50]I've been on my own for long enough
    [00:18.20]Maybe you can show me how to love, maybe
    [00:24.00]I'm going through withdrawals
    """
    lines = lyrics_service.parse_lrc(lrc_sample)
    assert len(lines) == 3
    assert abs(lines[0].time - 12.50) < 0.01
    assert lines[0].text == "I've been on my own for long enough"
    assert abs(lines[1].time - 18.20) < 0.01

def test_cache_manager():
    cache_manager.set_stream("test_vid", "https://stream.googlevideo.com/test", ttl_seconds=100)
    assert cache_manager.get_stream("test_vid") == "https://stream.googlevideo.com/test"
    assert cache_manager.get_stream("non_existent") is None

def test_track_schema():
    track = Track(
        id="test_id",
        title="Blinding Lights",
        artists=[Artist(name="The Weeknd")],
        album=Album(name="After Hours"),
        duration_seconds=200,
        source="spotify"
    )
    assert track.title == "Blinding Lights"
    assert track.artists[0].name == "The Weeknd"
