export interface Track {
  id: string; // YouTube Video ID
  title: string;
  artist: string;
  album?: string;
  coverUrl: string;
  durationSeconds: number;
  isLiked?: boolean;
}

export interface Playlist {
  id: string;
  title: string;
  description?: string;
  coverUrl: string;
  tracks: Track[];
}

export type RepeatMode = "off" | "all" | "one";

export interface PlayerState {
  currentTrack: Track | null;
  isPlaying: boolean;
  isBuffering: boolean;
  currentTime: number;
  duration: number;
  volume: number; // 0 - 100
  isMuted: boolean;

  queue: Track[];
  queueIndex: number;
  history: Track[];

  isShuffle: boolean;
  repeatMode: RepeatMode;
  isRightSidebarOpen: boolean;

  likedSongs: Track[];

  // Actions
  playTrack: (track: Track, newQueue?: Track[]) => void;
  togglePlay: () => void;
  skipNext: () => void;
  skipPrev: () => void;
  seekTo: (seconds: number) => void;
  setVolume: (vol: number) => void;
  toggleMute: () => void;
  toggleShuffle: () => void;
  toggleRepeat: () => void;
  toggleRightSidebar: () => void;
  toggleLike: (track: Track) => void;
  removeFromQueue: (index: number) => void;
  jumpToQueueIndex: (index: number) => void;
  appendToQueue: (tracks: Track[]) => void;

  // Internal audio sync helpers
  setCurrentTime: (time: number) => void;
  setDuration: (dur: number) => void;
  setIsPlaying: (playing: boolean) => void;
  setIsBuffering: (buffering: boolean) => void;
}
