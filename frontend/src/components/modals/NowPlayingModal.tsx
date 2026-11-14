import React from 'react';
import {
  ChevronDown,
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Heart,
  Mic2,
  ListMusic,
  Radio,
  Loader2,
} from 'lucide-react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { useAuth } from '../../context/AuthContext';

export const NowPlayingModal: React.FC = () => {
  const {
    currentTrack,
    isPlaying,
    isLoadingTrack,
    currentTime,
    duration,
    isShuffle,
    repeatMode,
    isNowPlayingExpanded,
    toggleNowPlayingExpanded,
    togglePlay,
    seekTo,
    skipNext,
    skipPrevious,
    toggleShuffle,
    toggleRepeat,
    toggleLyrics,
    toggleQueue,
  } = useAudioPlayer();

  const { toggleLikeTrack, activeProvider } = useAuth();

  if (!isNowPlayingExpanded || !currentTrack) return null;

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  return (
    <div className="fixed inset-0 z-50 bg-gradient-to-b from-zinc-800 via-zinc-950 to-black p-6 flex flex-col justify-between select-none animate-in slide-in-from-bottom duration-300">
      {/* Top Bar with Dismiss Chevron */}
      <div className="flex items-center justify-between pt-2">
        <button
          onClick={toggleNowPlayingExpanded}
          className="p-2 -ml-2 text-zinc-300 hover:text-white"
        >
          <ChevronDown className="w-7 h-7" />
        </button>
        <div className="flex flex-col items-center">
          <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400">
            Playing from
          </span>
          <span className="text-xs font-bold text-white capitalize">
            {activeProvider} Library
          </span>
        </div>
        <div className="w-7" />
      </div>

      {/* Center: Big Square Album Art */}
      <div className="flex-1 flex items-center justify-center my-6">
        <div className="w-full max-w-sm aspect-square rounded-2xl overflow-hidden shadow-2xl bg-zinc-800">
          {currentTrack.cover_url ? (
            <img
              src={currentTrack.cover_url}
              alt={currentTrack.title}
              className="w-full h-full object-cover shadow-2xl"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-zinc-600">
              <Radio className="w-20 h-20" />
            </div>
          )}
        </div>
      </div>

      {/* Bottom Controls Area */}
      <div className="flex flex-col gap-5 max-w-sm w-full mx-auto pb-4">
        {/* Track Title and Heart Button */}
        <div className="flex items-center justify-between">
          <div className="flex flex-col min-w-0 pr-4">
            <h2 className="text-xl md:text-2xl font-black text-white truncate">
              {currentTrack.title}
            </h2>
            <p className="text-sm font-medium text-zinc-400 truncate">
              {currentTrack.artists.map((a) => a.name).join(', ')}
            </p>
          </div>

          <button
            onClick={() => toggleLikeTrack(currentTrack)}
            title={`Save to ${activeProvider} library`}
            className="p-2 hover:scale-110 active:scale-95 transition"
          >
            <Heart
              className={`w-7 h-7 ${
                currentTrack.is_liked
                  ? 'text-spotify-green fill-spotify-green'
                  : 'text-zinc-400 hover:text-white'
              }`}
            />
          </button>
        </div>

        {/* Progress Scrubber */}
        <div className="flex flex-col gap-1.5">
          <input
            type="range"
            min={0}
            max={duration || 100}
            value={currentTime}
            onChange={(e) => seekTo(Number(e.target.value))}
            className="w-full h-1.5 bg-zinc-700 rounded-lg appearance-none cursor-pointer"
          />
          <div className="flex justify-between text-xs font-mono text-zinc-400">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Main Playback Buttons */}
        <div className="flex items-center justify-between px-2">
          <button
            onClick={toggleShuffle}
            className={`p-2 transition ${
              isShuffle ? 'text-spotify-green' : 'text-zinc-400 hover:text-white'
            }`}
          >
            <Shuffle className="w-5 h-5" />
          </button>

          <button onClick={skipPrevious} className="p-2 text-white">
            <SkipBack className="w-7 h-7 fill-current" />
          </button>

          <button
            onClick={togglePlay}
            disabled={isLoadingTrack}
            className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition shadow-2xl"
          >
            {isLoadingTrack ? (
              <Loader2 className="w-7 h-7 animate-spin" />
            ) : isPlaying ? (
              <Pause className="w-7 h-7 fill-black" />
            ) : (
              <Play className="w-7 h-7 fill-black ml-1" />
            )}
          </button>

          <button onClick={skipNext} className="p-2 text-white">
            <SkipForward className="w-7 h-7 fill-current" />
          </button>

          <button
            onClick={toggleRepeat}
            className={`p-2 transition ${
              repeatMode !== 'off' ? 'text-spotify-green' : 'text-zinc-400 hover:text-white'
            }`}
          >
            {repeatMode === 'track' ? <Repeat1 className="w-5 h-5" /> : <Repeat className="w-5 h-5" />}
          </button>
        </div>

        {/* Bottom Auxiliary Buttons (Lyrics & Queue) */}
        <div className="flex items-center justify-between px-3 pt-2">
          <button
            onClick={() => {
              toggleNowPlayingExpanded();
              toggleLyrics();
            }}
            className="flex items-center gap-1.5 text-xs font-semibold text-zinc-300 hover:text-white bg-zinc-800/80 px-3 py-1.5 rounded-full"
          >
            <Mic2 className="w-4 h-4 text-spotify-green" />
            <span>Lyrics</span>
          </button>

          <button
            onClick={() => {
              toggleNowPlayingExpanded();
              toggleQueue();
            }}
            className="flex items-center gap-1.5 text-xs font-semibold text-zinc-300 hover:text-white bg-zinc-800/80 px-3 py-1.5 rounded-full"
          >
            <ListMusic className="w-4 h-4" />
            <span>Queue</span>
          </button>
        </div>
      </div>
    </div>
  );
};
