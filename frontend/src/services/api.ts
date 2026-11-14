import { Track, Playlist, AuthStatus, ActiveProvider, LyricsData } from '../types';

const API_BASE = '/api';

export const api = {
  async getAuthStatus(): Promise<AuthStatus> {
    const res = await fetch(`${API_BASE}/auth/status`);
    return res.json();
  },

  async switchActiveAccount(provider: ActiveProvider): Promise<void> {
    await fetch(`${API_BASE}/auth/active-account`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ provider })
    });
  },

  async setSpotifyToken(accessToken: string, refreshToken?: string): Promise<any> {
    const res = await fetch(`${API_BASE}/auth/spotify/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ access_token: accessToken, refresh_token: refreshToken })
    });
    return res.json();
  },

  async disconnectSpotify(): Promise<void> {
    await fetch(`${API_BASE}/auth/spotify/disconnect`, { method: 'POST' });
  },

  async connectYoutubeDemo(): Promise<any> {
    const res = await fetch(`${API_BASE}/auth/youtube/demo`, { method: 'POST' });
    return res.json();
  },

  async connectYoutubeCookie(cookie: string): Promise<any> {
    const res = await fetch(`${API_BASE}/auth/youtube/cookie`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cookie })
    });
    return res.json();
  },

  async disconnectYoutube(): Promise<void> {
    await fetch(`${API_BASE}/auth/youtube/disconnect`, { method: 'POST' });
  },

  async getLikedLibrary(): Promise<{ provider: ActiveProvider; tracks: Track[] }> {
    const res = await fetch(`${API_BASE}/library/liked`);
    return res.json();
  },

  async getUserPlaylists(): Promise<{ provider: ActiveProvider; playlists: Playlist[] }> {
    const res = await fetch(`${API_BASE}/library/playlists`);
    return res.json();
  },

  async saveTrack(track: Track, targetProvider?: ActiveProvider): Promise<boolean> {
    const res = await fetch(`${API_BASE}/library/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        track_id: track.id,
        title: track.title,
        artist: track.artists[0]?.name || '',
        album: track.album?.name,
        youtube_id: track.youtube_id || (track.source === 'youtube' ? track.id : undefined),
        spotify_id: track.spotify_id || (track.source === 'spotify' ? track.id : undefined),
        target_provider: targetProvider
      })
    });
    const data = await res.json();
    return data.success;
  },

  async removeTrack(trackId: string, provider?: ActiveProvider): Promise<boolean> {
    const query = provider ? `?track_id=${trackId}&provider=${provider}` : `?track_id=${trackId}`;
    const res = await fetch(`${API_BASE}/library/save${query}`, { method: 'DELETE' });
    const data = await res.json();
    return data.success;
  },

  async search(query: string): Promise<Track[]> {
    const res = await fetch(`${API_BASE}/search?q=${encodeURIComponent(query)}`);
    const data = await res.json();
    return data.tracks || [];
  },

  async getCharts(): Promise<Track[]> {
    const res = await fetch(`${API_BASE}/charts`);
    const data = await res.json();
    return data.trending || [];
  },

  async resolveTrack(title: string, artist: string, duration = 0): Promise<{ youtube_id: string; stream_url: string }> {
    const res = await fetch(`${API_BASE}/track/resolve?title=${encodeURIComponent(title)}&artist=${encodeURIComponent(artist)}&duration=${duration}`);
    if (!res.ok) throw new Error('Failed to resolve track stream');
    return res.json();
  },

  async getLyrics(title: string, artist: string, duration = 0): Promise<LyricsData> {
    const res = await fetch(`${API_BASE}/lyrics?title=${encodeURIComponent(title)}&artist=${encodeURIComponent(artist)}&duration=${duration}`);
    return res.json();
  },

  getStreamUrl(videoId: string): string {
    return `${API_BASE}/stream?id=${encodeURIComponent(videoId)}`;
  }
};
