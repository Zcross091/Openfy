import React, { createContext, useContext, useState, useRef, useEffect } from 'react';
import { Track } from '../types';
import { api } from '../services/api';

interface AudioPlayerContextType {
  currentTrack: Track | null;
  isPlaying: boolean;
  isLoadingTrack: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  queue: Track[];
  queueIndex: number;
  isShuffle: boolean;
  repeatMode: 'off' | 'track' | 'queue';
  isLyricsOpen: boolean;
  isQueueOpen: boolean;
  isNowPlayingExpanded: boolean;
  playTrack: (track: Track, newQueue?: Track[]) => Promise<void>;
  togglePlay: () => void;
  seekTo: (seconds: number) => void;
  setVolumeLevel: (val: number) => void;
  toggleMute: () => void;
  skipNext: () => void;
  skipPrevious: () => void;
  toggleShuffle: () => void;
  toggleRepeat: () => void;
  toggleLyrics: () => void;
  toggleQueue: () => void;
  toggleNowPlayingExpanded: () => void;
}

const AudioPlayerContext = createContext<AudioPlayerContextType | undefined>(undefined);

export const AudioPlayerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoadingTrack, setIsLoadingTrack] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.8);
  const [isMuted, setIsMuted] = useState(false);

  const [queue, setQueue] = useState<Track[]>([]);
  const [queueIndex, setQueueIndex] = useState(-1);
  const [isShuffle, setIsShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState<'off' | 'track' | 'queue'>('off');

  const [isLyricsOpen, setIsLyricsOpen] = useState(false);
  const [isQueueOpen, setIsQueueOpen] = useState(false);
  const [isNowPlayingExpanded, setIsNowPlayingExpanded] = useState(false);

  // Initialize Audio element
  useEffect(() => {
    const audio = new Audio();
    audioRef.current = audio;
    audio.volume = volume;

    const handleTimeUpdate = () => setCurrentTime(audio.currentTime);
    const handleLoadedMetadata = () => setDuration(audio.duration || 0);
    const handleEnded = () => handleTrackEnded();
    const handlePlay = () => setIsPlaying(true);
    const handlePause = () => setIsPlaying(false);

    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('ended', handleEnded);
    audio.addEventListener('play', handlePlay);
    audio.addEventListener('pause', handlePause);

    return () => {
      audio.pause();
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('ended', handleEnded);
      audio.removeEventListener('play', handlePlay);
      audio.removeEventListener('pause', handlePause);
    };
  }, []);

  // Update MediaSession API for OS controls & Windows Lock screen
  useEffect(() => {
    if ('mediaSession' in navigator && currentTrack) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: currentTrack.title,
        artist: currentTrack.artists.map(a => a.name).join(', '),
        album: currentTrack.album?.name || 'Openfy',
        artwork: currentTrack.cover_url ? [
          { src: currentTrack.cover_url, sizes: '512x512', type: 'image/jpeg' }
        ] : []
      });

      navigator.mediaSession.setActionHandler('play', togglePlay);
      navigator.mediaSession.setActionHandler('pause', togglePlay);
      navigator.mediaSession.setActionHandler('nexttrack', skipNext);
      navigator.mediaSession.setActionHandler('previoustrack', skipPrevious);
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (details.seekTime !== undefined) seekTo(details.seekTime);
      });
    }
  }, [currentTrack, queue, queueIndex]);

  const playTrack = async (track: Track, newQueue?: Track[]) => {
    if (!audioRef.current) return;
    setIsLoadingTrack(true);
    setCurrentTrack(track);

    if (newQueue) {
      setQueue(newQueue);
      const idx = newQueue.findIndex(t => t.id === track.id);
      setQueueIndex(idx >= 0 ? idx : 0);
    } else if (!queue.some(t => t.id === track.id)) {
      setQueue(prev => [...prev, track]);
      setQueueIndex(queue.length);
    }

    try {
      let videoId = track.youtube_id;
      // If no direct YouTube video ID, resolve from Spotify metadata
      if (!videoId || videoId.length !== 11) {
        const resolved = await api.resolveTrack(
          track.title,
          track.artists[0]?.name || '',
          track.duration_seconds
        );
        videoId = resolved.youtube_id;
        track.youtube_id = videoId;
      }

      const streamUrl = api.getStreamUrl(videoId);
      audioRef.current.src = streamUrl;
      await audioRef.current.play();
      setIsPlaying(true);
    } catch (e) {
      console.error('Error starting audio playback:', e);
      setIsPlaying(false);
    } finally {
      setIsLoadingTrack(false);
    }
  };

  const togglePlay = () => {
    if (!audioRef.current) return;
    if (isPlaying) {
      audioRef.current.pause();
    } else {
      audioRef.current.play().catch(e => console.error('Play error:', e));
    }
  };

  const seekTo = (seconds: number) => {
    if (!audioRef.current) return;
    audioRef.current.currentTime = seconds;
    setCurrentTime(seconds);
  };

  const setVolumeLevel = (val: number) => {
    if (!audioRef.current) return;
    audioRef.current.volume = val;
    setVolume(val);
    if (val > 0) setIsMuted(false);
  };

  const toggleMute = () => {
    if (!audioRef.current) return;
    if (isMuted) {
      audioRef.current.volume = volume || 0.8;
      setIsMuted(false);
    } else {
      audioRef.current.volume = 0;
      setIsMuted(true);
    }
  };

  const skipNext = () => {
    if (queue.length === 0) return;
    let nextIdx = queueIndex + 1;
    if (isShuffle) {
      nextIdx = Math.floor(Math.random() * queue.length);
    }
    if (nextIdx >= queue.length) {
      if (repeatMode === 'queue') nextIdx = 0;
      else return;
    }
    setQueueIndex(nextIdx);
    playTrack(queue[nextIdx]);
  };

  const skipPrevious = () => {
    if (currentTime > 3) {
      seekTo(0);
      return;
    }
    if (queue.length === 0) return;
    let prevIdx = queueIndex - 1;
    if (prevIdx < 0) {
      prevIdx = queue.length - 1;
    }
    setQueueIndex(prevIdx);
    playTrack(queue[prevIdx]);
  };

  const handleTrackEnded = () => {
    if (repeatMode === 'track') {
      seekTo(0);
      audioRef.current?.play();
    } else {
      skipNext();
    }
  };

  const toggleShuffle = () => setIsShuffle(prev => !prev);
  const toggleRepeat = () => {
    setRepeatMode(prev => (prev === 'off' ? 'queue' : prev === 'queue' ? 'track' : 'off'));
  };

  const toggleLyrics = () => setIsLyricsOpen(prev => !prev);
  const toggleQueue = () => setIsQueueOpen(prev => !prev);
  const toggleNowPlayingExpanded = () => setIsNowPlayingExpanded(prev => !prev);

  return (
    <AudioPlayerContext.Provider
      value={{
        currentTrack,
        isPlaying,
        isLoadingTrack,
        currentTime,
        duration,
        volume,
        isMuted,
        queue,
        queueIndex,
        isShuffle,
        repeatMode,
        isLyricsOpen,
        isQueueOpen,
        isNowPlayingExpanded,
        playTrack,
        togglePlay,
        seekTo,
        setVolumeLevel,
        toggleMute,
        skipNext,
        skipPrevious,
        toggleShuffle,
        toggleRepeat,
        toggleLyrics,
        toggleQueue,
        toggleNowPlayingExpanded,
      }}
    >
      {children}
    </AudioPlayerContext.Provider>
  );
};

export const useAudioPlayer = () => {
  const context = useContext(AudioPlayerContext);
  if (!context) throw new Error('useAudioPlayer must be used within an AudioPlayerProvider');
  return context;
};
