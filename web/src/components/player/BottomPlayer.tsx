"use client";

import { usePlayerStore } from "@/store/usePlayerStore";
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Volume2,
  Volume1,
  VolumeX,
  ListMusic,
  Heart,
} from "lucide-react";

export default function BottomPlayer() {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    volume,
    isMuted,
    isShuffle,
    repeatMode,
    isRightSidebarOpen,
    likedSongs,
    togglePlay,
    skipNext,
    skipPrev,
    seekTo,
    setVolume,
    toggleMute,
    toggleShuffle,
    toggleRepeat,
    toggleRightSidebar,
    toggleLike,
  } = usePlayerStore();

  const isLiked = currentTrack ? likedSongs.some((t) => t.id === currentTrack.id) : false;

  function formatTime(secs: number) {
    if (isNaN(secs) || secs < 0) return "0:00";
    const mins = Math.floor(secs / 60);
    const remainingSecs = Math.floor(secs % 60);
    return `${mins}:${remainingSecs < 10 ? "0" : ""}${remainingSecs}`;
  }

  const currentVol = isMuted ? 0 : volume;

  return (
    <footer className="fixed bottom-0 left-0 right-0 h-[90px] bg-black border-t border-white/10 px-4 flex items-center justify-between z-50 select-none">
      {/* 1. Left: Track Info & Heart Button */}
      <div className="flex items-center gap-3 w-1/4 min-w-[180px]">
        {currentTrack ? (
          <>
            <img
              src={currentTrack.coverUrl || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=100"}
              alt={currentTrack.title}
              className="w-14 h-14 rounded object-cover flex-shrink-0 shadow"
            />
            <div className="min-w-0 pr-2">
              <p className="text-white text-sm font-semibold truncate hover:underline cursor-pointer">
                {currentTrack.title}
              </p>
              <p className="text-neutral-400 text-xs truncate hover:underline hover:text-white cursor-pointer mt-0.5">
                {currentTrack.artist}
              </p>
            </div>
            <button
              onClick={() => toggleLike(currentTrack)}
              className={`p-1.5 transition-colors ${
                isLiked ? "text-spotify-green" : "text-neutral-400 hover:text-white"
              }`}
            >
              <Heart className={`w-4 h-4 ${isLiked ? "fill-spotify-green" : ""}`} />
            </button>
          </>
        ) : (
          <div className="flex items-center gap-3 text-neutral-500 text-xs">
            <div className="w-14 h-14 rounded bg-neutral-900 border border-white/5 flex items-center justify-center">
              <span className="text-neutral-600">♫</span>
            </div>
            <span>No track selected</span>
          </div>
        )}
      </div>

      {/* 2. Center: Controls & Scrubber */}
      <div className="flex flex-col items-center justify-center max-w-[722px] w-2/4 gap-2">
        {/* Buttons */}
        <div className="flex items-center gap-5">
          {/* Shuffle Button */}
          <button
            onClick={toggleShuffle}
            className={`transition-colors relative p-1 ${
              isShuffle ? "text-spotify-green" : "text-neutral-400 hover:text-white"
            }`}
            title="Shuffle"
          >
            <Shuffle className="w-4 h-4" />
            {isShuffle && (
              <span className="w-1 h-1 bg-spotify-green rounded-full absolute -bottom-1 left-1/2 -translate-x-1/2" />
            )}
          </button>

          {/* Previous Button */}
          <button
            onClick={skipPrev}
            className="text-neutral-400 hover:text-white transition-colors"
            title="Previous"
          >
            <SkipBack className="w-5 h-5 fill-current" />
          </button>

          {/* Play / Pause Primary Button */}
          <button
            onClick={togglePlay}
            className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 active:scale-95 transition-transform"
            title={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? (
              <Pause className="w-4 h-4 fill-black" />
            ) : (
              <Play className="w-4 h-4 fill-black ml-0.5" />
            )}
          </button>

          {/* Next Button */}
          <button
            onClick={skipNext}
            className="text-neutral-400 hover:text-white transition-colors"
            title="Next"
          >
            <SkipForward className="w-5 h-5 fill-current" />
          </button>

          {/* Repeat Button */}
          <button
            onClick={toggleRepeat}
            className={`transition-colors relative p-1 ${
              repeatMode !== "off" ? "text-spotify-green" : "text-neutral-400 hover:text-white"
            }`}
            title={`Repeat: ${repeatMode}`}
          >
            {repeatMode === "one" ? (
              <Repeat1 className="w-4 h-4" />
            ) : (
              <Repeat className="w-4 h-4" />
            )}
            {repeatMode !== "off" && (
              <span className="w-1 h-1 bg-spotify-green rounded-full absolute -bottom-1 left-1/2 -translate-x-1/2" />
            )}
          </button>
        </div>

        {/* Timeline Scrubber */}
        <div className="w-full flex items-center gap-2 text-xs text-neutral-400 font-medium">
          <span className="w-9 text-right tabular-nums">{formatTime(currentTime)}</span>
          <div className="group flex-1 flex items-center relative py-1">
            <input
              type="range"
              min={0}
              max={duration > 0 ? duration : 100}
              value={currentTime}
              onChange={(e) => seekTo(Number(e.target.value))}
              className="w-full h-1"
            />
          </div>
          <span className="w-9 text-left tabular-nums">{formatTime(duration)}</span>
        </div>
      </div>

      {/* 3. Right: Queue & Volume */}
      <div className="flex items-center justify-end gap-3 w-1/4 min-w-[180px]">
        {/* Queue Toggle Button */}
        <button
          onClick={toggleRightSidebar}
          className={`p-1.5 transition-colors ${
            isRightSidebarOpen ? "text-spotify-green" : "text-neutral-400 hover:text-white"
          }`}
          title="Queue / Now Playing"
        >
          <ListMusic className="w-5 h-5" />
        </button>

        {/* Volume Mute & Slider */}
        <div className="group flex items-center gap-2 w-32">
          <button
            onClick={toggleMute}
            className="text-neutral-400 hover:text-white transition-colors"
          >
            {currentVol === 0 ? (
              <VolumeX className="w-5 h-5" />
            ) : currentVol < 50 ? (
              <Volume1 className="w-5 h-5" />
            ) : (
              <Volume2 className="w-5 h-5" />
            )}
          </button>
          <input
            type="range"
            min={0}
            max={100}
            value={currentVol}
            onChange={(e) => setVolume(Number(e.target.value))}
            className="w-full h-1"
          />
        </div>
      </div>
    </footer>
  );
}
