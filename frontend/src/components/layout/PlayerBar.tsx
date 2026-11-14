import React from 'react';
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Volume2,
  VolumeX,
  Volume1,
  Heart,
  Mic2,
  ListMusic,
  Radio,
  Loader2,
  ChevronUp,
} from 'lucide-react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { useAuth } from '../../context/AuthContext';

export const PlayerBar: React.FC = () => {
  const {
    currentTrack,
    isPlaying,
    isLoadingTrack,
    currentTime,
    duration,
    volume,
    isMuted,
    isShuffle,
    repeatMode,
    isLyricsOpen,
    isQueueOpen,
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
  } = useAudioPlayer();

  const { toggleLikeTrack, activeProvider } = useAuth();

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  if (!currentTrack) return null;

  return (
    <>
      {/* ========================================================= */}
      {/* Mobile Floating Mini Player (visible on mobile only)      */}
      {/* ========================================================= */}
      <div className="md:hidden fixed bottom-14 left-2 right-2 z-40 bg-zinc-900/95 backdrop-blur-lg border border-zinc-800 rounded-lg p-2 shadow-2xl flex items-center justify-between select-none">
        {/* Click to open full-screen modal */}
        <div
          onClick={toggleNowPlayingExpanded}
          className="flex items-center gap-3 flex-1 min-w-0 cursor-pointer"
        >
          {currentTrack.cover_url ? (
            <img
              src={currentTrack.cover_url}
              alt={currentTrack.title}
              className="w-11 h-11 rounded-md object-cover flex-shrink-0 shadow"
            />
          ) : (
            <div className="w-11 h-11 rounded-md bg-zinc-800 flex items-center justify-center flex-shrink-0">
              <Radio className="w-5 h-5 text-zinc-400" />
            </div>
          )}
          <div className="flex flex-col min-w-0">
            <span className="text-sm font-semibold text-white truncate">
              {currentTrack.title}
            </span>
            <span className="text-xs text-zinc-400 truncate">
              {currentTrack.artists.map((a) => a.name).join(', ')}
            </span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-1">
          <button
            onClick={() => toggleLikeTrack(currentTrack)}
            title={`Save to ${activeProvider} library`}
            className="p-2 text-zinc-300 hover:text-white transition"
          >
            <Heart
              className={`w-5 h-5 ${
                currentTrack.is_liked
                  ? 'text-spotify-green fill-spotify-green'
                  : 'text-zinc-400'
              }`}
            />
          </button>
          <button
            onClick={togglePlay}
            disabled={isLoadingTrack}
            className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition shadow"
          >
            {isLoadingTrack ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : isPlaying ? (
              <Pause className="w-4 h-4 fill-black" />
            ) : (
              <Play className="w-4 h-4 fill-black ml-0.5" />
            )}
          </button>
        </div>

        {/* Mini progress bar at top of mobile player */}
        <div className="absolute -bottom-1 left-2 right-2 h-0.5 bg-zinc-800 rounded-full overflow-hidden">
          <div
            className="h-full bg-spotify-green transition-all"
            style={{ width: `${(currentTime / (duration || 1)) * 100}%` }}
          />
        </div>
      </div>

      {/* ========================================================= */}
      {/* Desktop Persistent Player Bar (visible on md screens up)  */}
      {/* ========================================================= */}
      <footer className="hidden md:flex h-24 bg-black border-t border-zinc-850 px-4 items-center justify-between z-40 select-none">
        {/* Left: Track Information */}
        <div className="flex items-center gap-3.5 w-1/4 min-w-[200px]">
          <div className="relative group cursor-pointer" onClick={toggleNowPlayingExpanded}>
            {currentTrack.cover_url ? (
              <img
                src={currentTrack.cover_url}
                alt={currentTrack.title}
                className="w-14 h-14 rounded-md object-cover shadow-md"
              />
            ) : (
              <div className="w-14 h-14 rounded-md bg-zinc-800 flex items-center justify-center">
                <Radio className="w-6 h-6 text-zinc-400" />
              </div>
            )}
            <div className="absolute inset-0 bg-black/40 rounded-md opacity-0 group-hover:opacity-100 flex items-center justify-center transition">
              <ChevronUp className="w-5 h-5 text-white" />
            </div>
          </div>

          <div className="flex flex-col min-w-0">
            <span className="font-semibold text-sm text-white hover:underline cursor-pointer truncate">
              {currentTrack.title}
            </span>
            <span className="text-xs text-spotify-subdued hover:underline cursor-pointer truncate">
              {currentTrack.artists.map((a) => a.name).join(', ')}
            </span>
          </div>

          {/* Heart / Save to Active Library */}
          <button
            onClick={() => toggleLikeTrack(currentTrack)}
            title={`Save to ${activeProvider} library`}
            className="p-1.5 text-zinc-400 hover:text-white transition hover:scale-110 active:scale-95"
          >
            <Heart
              className={`w-5 h-5 ${
                currentTrack.is_liked
                  ? 'text-spotify-green fill-spotify-green'
                  : 'text-zinc-400 hover:text-white'
              }`}
            />
          </button>
        </div>

        {/* Center: Controls & Scrubber */}
        <div className="flex flex-col items-center gap-2 max-w-2xl w-2/4">
          <div className="flex items-center gap-5">
            <button
              onClick={toggleShuffle}
              className={`p-1 transition ${
                isShuffle ? 'text-spotify-green' : 'text-zinc-400 hover:text-white'
              }`}
              title="Shuffle"
            >
              <Shuffle className="w-4 h-4" />
            </button>

            <button
              onClick={skipPrevious}
              className="p-1 text-zinc-400 hover:text-white transition"
              title="Previous"
            >
              <SkipBack className="w-5 h-5 fill-current" />
            </button>

            <button
              onClick={togglePlay}
              disabled={isLoadingTrack}
              className="w-9 h-9 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition shadow-lg"
              title={isPlaying ? 'Pause' : 'Play'}
            >
              {isLoadingTrack ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : isPlaying ? (
                <Pause className="w-4 h-4 fill-black" />
              ) : (
                <Play className="w-4 h-4 fill-black ml-0.5" />
              )}
            </button>

            <button
              onClick={skipNext}
              className="p-1 text-zinc-400 hover:text-white transition"
              title="Next"
            >
              <SkipForward className="w-5 h-5 fill-current" />
            </button>

            <button
              onClick={toggleRepeat}
              className={`p-1 transition ${
                repeatMode !== 'off'
                  ? 'text-spotify-green'
                  : 'text-zinc-400 hover:text-white'
              }`}
              title="Repeat"
            >
              {repeatMode === 'track' ? (
                <Repeat1 className="w-4 h-4" />
              ) : (
                <Repeat className="w-4 h-4" />
              )}
            </button>
          </div>

          {/* Time Scrubber */}
          <div className="w-full flex items-center gap-2 text-xs font-mono text-spotify-subdued">
            <span className="w-10 text-right">{formatTime(currentTime)}</span>
            <div className="relative flex-1 flex items-center group">
              <input
                type="range"
                min={0}
                max={duration || 100}
                value={currentTime}
                onChange={(e) => seekTo(Number(e.target.value))}
                className="w-full h-1 bg-zinc-700 rounded-lg appearance-none cursor-pointer group-hover:h-1.5 transition-all"
              />
            </div>
            <span className="w-10">{formatTime(duration)}</span>
          </div>
        </div>

        {/* Right: Synced Lyrics, Queue, Quality, Volume */}
        <div className="flex items-center justify-end gap-3 w-1/4 min-w-[200px]">
          {/* Stream Quality Badge */}
          <span className="hidden lg:inline-block text-[10px] font-bold tracking-wider px-2 py-0.5 rounded bg-zinc-800 text-spotify-green border border-zinc-700/60 uppercase">
            Opus 160k
          </span>

          {/* Lyrics Button */}
          <button
            onClick={toggleLyrics}
            className={`p-2 rounded-full transition ${
              isLyricsOpen
                ? 'text-spotify-green bg-zinc-800'
                : 'text-zinc-400 hover:text-white'
            }`}
            title="Lyrics"
          >
            <Mic2 className="w-4 h-4" />
          </button>

          {/* Queue Button */}
          <button
            onClick={toggleQueue}
            className={`p-2 rounded-full transition ${
              isQueueOpen
                ? 'text-spotify-green bg-zinc-800'
                : 'text-zinc-400 hover:text-white'
            }`}
            title="Queue"
          >
            <ListMusic className="w-4 h-4" />
          </button>

          {/* Volume Control */}
          <div className="flex items-center gap-2">
            <button
              onClick={toggleMute}
              className="text-zinc-400 hover:text-white transition"
            >
              {isMuted || volume === 0 ? (
                <VolumeX className="w-4 h-4 text-red-400" />
              ) : volume < 0.5 ? (
                <Volume1 className="w-4 h-4" />
              ) : (
                <Volume2 className="w-4 h-4" />
              )}
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={isMuted ? 0 : volume}
              onChange={(e) => setVolumeLevel(Number(e.target.value))}
              className="w-20 sm:w-24 h-1 bg-zinc-700 rounded-lg appearance-none cursor-pointer"
            />
          </div>
        </div>
      </footer>
    </>
  );
};
