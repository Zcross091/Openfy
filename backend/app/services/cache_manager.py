import time
from typing import Dict, Any, Optional

class CacheManager:
    def __init__(self):
        # In-memory caches
        self._stream_cache: Dict[str, Dict[str, Any]] = {}
        self._lyrics_cache: Dict[str, Any] = {}
        self._search_cache: Dict[str, Any] = {}

    def get_stream(self, key: str) -> Optional[str]:
        entry = self._stream_cache.get(key)
        if not entry:
            return None
        # Check expiration (googlevideo links expire in ~6 hours; we set safety margin to 1 hour before expire)
        if entry["expires_at"] < time.time():
            del self._stream_cache[key]
            return None
        return entry["url"]

    def set_stream(self, key: str, url: str, ttl_seconds: int = 14400):
        self._stream_cache[key] = {
            "url": url,
            "expires_at": time.time() + ttl_seconds
        }

    def get_lyrics(self, key: str) -> Optional[Any]:
        return self._lyrics_cache.get(key)

    def set_lyrics(self, key: str, data: Any):
        self._lyrics_cache[key] = data

    def get_search(self, key: str) -> Optional[Any]:
        return self._search_cache.get(key)

    def set_search(self, key: str, data: Any):
        self._search_cache[key] = data

cache_manager = CacheManager()
