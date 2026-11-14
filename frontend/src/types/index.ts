export interface Artist {
  id?: string;
  name: string;
}

export interface Album {
  id?: string;
  name: string;
  cover_url?: string;
}

export interface Track {
  id: string;
  title: string;
  artists: Artist[];
  album?: Album;
  duration_seconds: number;
  cover_url?: string;
  source: 'youtube' | 'spotify';
  youtube_id?: string;
  spotify_id?: string;
  is_liked?: boolean;
}

export interface Playlist {
  id: string;
  title: string;
  description?: string;
  cover_url?: string;
  track_count: number;
  source: 'youtube' | 'spotify';
  tracks?: Track[];
}

export type ActiveProvider = 'spotify' | 'youtube' | 'guest';

export interface UserProfile {
  id?: string;
  display_name?: string;
  channel?: string;
  email?: string;
  images?: Array<{ url: string }>;
}

export interface AuthStatus {
  active_provider: ActiveProvider;
  spotify_connected: boolean;
  youtube_connected: boolean;
  spotify_user?: UserProfile;
  youtube_user?: UserProfile;
}

export interface LyricLine {
  time: number; // in seconds
  text: string;
}

export interface LyricsData {
  track_title: string;
  artist_name: string;
  is_synced: boolean;
  lyrics: LyricLine[];
  plain_lyrics?: string;
}
