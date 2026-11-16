"use client";

import { usePlayerStore } from "@/store/usePlayerStore";
import { X, Play, Trash2, Heart, Radio } from "lucide-react";

export default function NowPlayingSidebar() {
  const {
    currentTrack,
    queue,
    queueIndex,
    jumpToQueueIndex,
    removeFromQueue,
    isRightSidebarOpen,
    toggleRightSidebar,
    likedSongs,
    toggleLike,
  } = usePlayerStore();

  if (!isRightSidebarOpen) return null;

  const isLiked = currentTrack ? likedSongs.some((t) => t.id === currentTrack.id) : false;
  const upcomingQueue = queue.slice(queueIndex + 1);

  return (
    <aside className="w-80 bg-[#121212] m-2 rounded-lg flex flex-col h-[calc(100vh-106px)] flex-shrink-0 text-white p-4 overflow-hidden border border-white/5 select-none">
      {/* Header */}
      <div className="flex items-center justify-between pb-3 border-b border-white/10">
        <h2 className="font-bold text-sm tracking-wide">Now Playing</h2>
        <button
          onClick={toggleRightSidebar}
          className="text-neutral-400 hover:text-white p-1 rounded-full hover:bg-white/10 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="overflow-y-auto flex-1 pr-1 flex flex-col gap-6 pt-4">
        {/* Large Cover Art & Track Info */}
        {currentTrack ? (
          <div className="flex flex-col gap-4">
            <div className="relative aspect-square w-full rounded-lg overflow-hidden shadow-2xl bg-neutral-900 border border-white/10">
              <img
                src={currentTrack.coverUrl || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500"}
                alt={currentTrack.title}
                className="w-full h-full object-cover"
              />
            </div>

            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <h3 className="font-bold text-lg leading-tight truncate">{currentTrack.title}</h3>
                <p className="text-sm text-neutral-400 truncate mt-0.5">{currentTrack.artist}</p>
              </div>
              <button
                onClick={() => toggleLike(currentTrack)}
                className={`p-1.5 transition-colors ${
                  isLiked ? "text-spotify-green" : "text-neutral-400 hover:text-white"
                }`}
              >
                <Heart className={`w-5 h-5 ${isLiked ? "fill-spotify-green" : ""}`} />
              </button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-center text-neutral-500">
            <Radio className="w-12 h-12 stroke-[1.5] mb-2" />
            <p className="text-sm font-medium">No track playing</p>
            <p className="text-xs text-neutral-600 mt-1">Play any song to see details</p>
          </div>
        )}

        {/* Next In Queue Section */}
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h4 className="text-xs font-bold text-neutral-400 uppercase tracking-wider">
              Next In Queue
            </h4>
            <span className="text-xs text-neutral-500">
              {upcomingQueue.length} {upcomingQueue.length === 1 ? "track" : "tracks"}
            </span>
          </div>

          {upcomingQueue.length > 0 ? (
            <div className="flex flex-col gap-1">
              {upcomingQueue.map((track, relativeIndex) => {
                const actualIndex = queueIndex + 1 + relativeIndex;
                return (
                  <div
                    key={`${track.id}-${actualIndex}`}
                    className="group flex items-center justify-between p-2 rounded-md hover:bg-white/5 transition-colors text-xs"
                  >
                    <div
                      onClick={() => jumpToQueueIndex(actualIndex)}
                      className="flex items-center gap-3 min-w-0 flex-1 cursor-pointer"
                    >
                      <div className="relative w-9 h-9 rounded overflow-hidden bg-neutral-800 flex-shrink-0">
                        <img
                          src={track.coverUrl}
                          alt={track.title}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                          <Play className="w-4 h-4 fill-white text-white" />
                        </div>
                      </div>
                      <div className="min-w-0">
                        <p className="font-semibold text-white truncate group-hover:text-spotify-green">
                          {track.title}
                        </p>
                        <p className="text-neutral-400 truncate mt-0.5">{track.artist}</p>
                      </div>
                    </div>

                    <button
                      onClick={() => removeFromQueue(actualIndex)}
                      className="opacity-0 group-hover:opacity-100 text-neutral-500 hover:text-red-400 p-1.5 transition-opacity"
                      title="Remove from queue"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="p-3 bg-white/5 rounded-lg text-center text-xs text-neutral-400">
              <p className="font-semibold text-white mb-1">Endless Radio Active</p>
              <p>When this track finishes, related songs will automatically queue.</p>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
