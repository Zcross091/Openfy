"use client";

import { usePlayerStore } from "@/store/usePlayerStore";
import { Heart, Play, Music2 } from "lucide-react";
import Link from "next/link";
import TrackRow from "@/components/track/TrackRow";

export default function LikedSongsPage() {
  const { likedSongs, playTrack } = usePlayerStore();

  const totalSeconds = likedSongs.reduce((acc, t) => acc + (t.durationSeconds || 210), 0);
  const totalMinutes = Math.floor(totalSeconds / 60);

  return (
    <div className="flex flex-col w-full min-h-full pb-16">
      {/* Liked Songs Gradient Header */}
      <div className="bg-gradient-to-b from-purple-800 via-indigo-900/60 to-[#121212] px-8 pt-12 pb-6 flex items-end gap-6">
        <div className="w-52 h-52 rounded-lg bg-gradient-to-br from-indigo-600 to-purple-800 flex items-center justify-center shadow-2xl flex-shrink-0">
          <Heart className="w-24 h-24 fill-white text-white drop-shadow-lg" />
        </div>

        <div className="flex flex-col gap-2">
          <span className="text-xs font-bold uppercase tracking-wider text-white">Playlist</span>
          <h1 className="text-5xl font-black text-white tracking-tight">Liked Songs</h1>
          <p className="text-sm text-neutral-300 font-medium mt-1">
            <span className="text-white font-bold">Openfy User</span> • {likedSongs.length}{" "}
            {likedSongs.length === 1 ? "song" : "songs"}
            {likedSongs.length > 0 && `, about ${totalMinutes} min`}
          </p>
        </div>
      </div>

      {/* Action Bar */}
      <div className="px-8 py-5 flex items-center gap-4">
        {likedSongs.length > 0 && (
          <button
            onClick={() => playTrack(likedSongs[0], likedSongs)}
            className="w-14 h-14 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl hover:scale-105 active:scale-95 transition-transform"
            title="Play all liked songs"
          >
            <Play className="w-7 h-7 fill-black ml-1" />
          </button>
        )}
      </div>

      {/* Songs Table */}
      <div className="px-8 flex-1 flex flex-col">
        {likedSongs.length > 0 ? (
          <div className="flex flex-col divide-y divide-white/5 bg-white/[0.02] rounded-xl p-2 border border-white/5">
            {likedSongs.map((track, i) => (
              <TrackRow key={track.id} track={track} index={i} queueContext={likedSongs} />
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-neutral-500">
            <div className="w-16 h-16 rounded-full bg-neutral-900 border border-white/10 flex items-center justify-center mb-4">
              <Heart className="w-8 h-8 text-neutral-600" />
            </div>
            <h2 className="text-xl font-bold text-white">Songs you like will appear here</h2>
            <p className="text-sm text-neutral-400 mt-1 max-w-sm text-center">
              Save songs by tapping the heart icon in search, the home feed, or the bottom player bar.
            </p>
            <Link
              href="/search"
              className="mt-6 px-6 py-2.5 bg-white text-black font-bold text-sm rounded-full hover:scale-105 transition-transform"
            >
              Find songs
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
