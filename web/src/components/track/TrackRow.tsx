"use client";

import { useState } from "react";
import { Track } from "@/types";
import { usePlayerStore } from "@/store/usePlayerStore";
import { Play, Pause, Heart } from "lucide-react";

interface TrackRowProps {
  track: Track;
  index: number;
  queueContext?: Track[];
}

export default function TrackRow({ track, index, queueContext }: TrackRowProps) {
  const [isHovered, setIsHovered] = useState(false);
  const { currentTrack, isPlaying, playTrack, togglePlay, likedSongs, toggleLike } = usePlayerStore();

  const isCurrent = currentTrack?.id === track.id;
  const isLiked = likedSongs.some((t) => t.id === track.id);

  function formatDuration(seconds: number) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`;
  }

  function handlePlayClick() {
    if (isCurrent) {
      togglePlay();
    } else {
      playTrack(track, queueContext);
    }
  }

  return (
    <div
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onDoubleClick={handlePlayClick}
      className={`group flex items-center px-4 py-2 rounded-md transition-colors cursor-pointer text-sm ${
        isCurrent ? "bg-white/10" : isHovered ? "bg-white/5" : "hover:bg-white/5"
      }`}
    >
      {/* Index or Play Button */}
      <div className="w-8 flex items-center justify-start text-neutral-400">
        {isHovered ? (
          <button
            onClick={handlePlayClick}
            className="hover:scale-110 text-white transition-transform"
          >
            {isCurrent && isPlaying ? (
              <Pause className="w-4 h-4 fill-white" />
            ) : (
              <Play className="w-4 h-4 fill-white" />
            )}
          </button>
        ) : isCurrent ? (
          <div className="w-4 h-4 flex items-center justify-center">
            {isPlaying ? (
              <div className="flex items-end gap-[2px] h-3">
                <span className="w-[3px] bg-spotify-green h-full animate-bounce" />
                <span className="w-[3px] bg-spotify-green h-2/3 animate-bounce delay-75" />
                <span className="w-[3px] bg-spotify-green h-4/5 animate-bounce delay-150" />
              </div>
            ) : (
              <span className="text-spotify-green font-bold">{index + 1}</span>
            )}
          </div>
        ) : (
          <span className="text-xs">{index + 1}</span>
        )}
      </div>

      {/* Track Cover Art, Title & Artist */}
      <div className="flex items-center gap-3 flex-1 min-w-0 pr-4">
        {track.coverUrl ? (
          <img
            src={track.coverUrl}
            alt={track.title}
            className="w-10 h-10 rounded object-cover flex-shrink-0"
          />
        ) : (
          <div className="w-10 h-10 rounded bg-neutral-800 flex-shrink-0" />
        )}
        <div className="min-w-0">
          <p
            className={`font-medium truncate ${
              isCurrent ? "text-spotify-green" : "text-white"
            }`}
          >
            {track.title}
          </p>
          <p className="text-xs text-neutral-400 truncate hover:underline hover:text-white">
            {track.artist}
          </p>
        </div>
      </div>

      {/* Album (hidden on smaller screens) */}
      <div className="hidden md:block w-1/4 text-neutral-400 truncate pr-4 text-xs">
        {track.album || "Single"}
      </div>

      {/* Heart like button */}
      <div className="flex items-center gap-4">
        <button
          onClick={(e) => {
            e.stopPropagation();
            toggleLike(track);
          }}
          className={`transition-opacity ${
            isLiked ? "text-spotify-green opacity-100" : "text-neutral-400 hover:text-white"
          } ${isHovered || isLiked ? "opacity-100" : "opacity-0"}`}
        >
          <Heart className={`w-4 h-4 ${isLiked ? "fill-spotify-green" : ""}`} />
        </button>

        {/* Duration */}
        <span className="text-xs text-neutral-400 w-10 text-right">
          {formatDuration(track.durationSeconds || 210)}
        </span>
      </div>
    </div>
  );
}
