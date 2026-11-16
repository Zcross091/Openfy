"use client";

import { useState, useEffect } from "react";
import { Search, X, Loader2, Play } from "lucide-react";
import { Track } from "@/types";
import { usePlayerStore } from "@/store/usePlayerStore";
import TrackRow from "@/components/track/TrackRow";

const GENRES = [
  { name: "Pop", color: "bg-pink-600" },
  { name: "Hip-Hop", color: "bg-orange-600" },
  { name: "Rock", color: "bg-red-700" },
  { name: "Electronic / Dance", color: "bg-emerald-600" },
  { name: "Indie & Alternative", color: "bg-purple-700" },
  { name: "R&B / Soul", color: "bg-blue-600" },
  { name: "Chill & Lo-Fi", color: "bg-teal-700" },
  { name: "Workout & Energy", color: "bg-amber-600" },
];

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Track[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const { playTrack } = usePlayerStore();

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    const timeout = setTimeout(() => {
      fetch(`/api/search?q=${encodeURIComponent(query.trim())}`)
        .then((res) => res.json())
        .then((data) => {
          if (data.tracks) {
            setResults(data.tracks);
          }
        })
        .catch((err) => console.error("Search error:", err))
        .finally(() => setIsLoading(false));
    }, 350);

    return () => clearTimeout(timeout);
  }, [query]);

  const topResult = results[0];

  return (
    <div className="flex flex-col w-full min-h-full pb-16">
      {/* Sticky Search Header */}
      <div className="sticky top-0 bg-[#121212]/95 backdrop-blur-md px-8 py-5 z-20 flex items-center gap-4 border-b border-white/5">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="What do you want to play?"
            className="w-full bg-[#242424] hover:bg-[#2a2a2a] focus:bg-[#242424] text-white placeholder-neutral-400 pl-10 pr-10 py-3 rounded-full text-sm font-medium border border-transparent focus:border-white/20 outline-none transition-all shadow-inner"
            autoFocus
          />
          {query && (
            <button
              onClick={() => setQuery("")}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-white"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
        {isLoading && <Loader2 className="w-5 h-5 text-spotify-green animate-spin" />}
      </div>

      <div className="px-8 py-6 flex-1 flex flex-col">
        {results.length > 0 ? (
          <div className="flex flex-col gap-8">
            {/* Top Result Card & Top 4 Songs */}
            <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
              {/* Top Result */}
              {topResult && (
                <div className="lg:col-span-2 flex flex-col gap-3">
                  <h3 className="text-xl font-bold text-white">Top Result</h3>
                  <div
                    onClick={() => playTrack(topResult, results)}
                    className="group bg-[#181818] hover:bg-[#282828] p-5 rounded-xl transition-all cursor-pointer relative flex flex-col justify-between h-56 shadow-lg border border-white/5"
                  >
                    <img
                      src={topResult.coverUrl}
                      alt={topResult.title}
                      className="w-24 h-24 rounded-lg object-cover shadow-md"
                    />
                    <div>
                      <h4 className="text-2xl font-black text-white truncate group-hover:text-spotify-green transition-colors">
                        {topResult.title}
                      </h4>
                      <p className="text-sm text-neutral-400 truncate mt-1">
                        <span className="font-semibold text-white">{topResult.artist}</span> • Song
                      </p>
                    </div>

                    <button
                      className="absolute bottom-5 right-5 w-12 h-12 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 hover:scale-105 transition-all"
                      title="Play"
                    >
                      <Play className="w-6 h-6 fill-black ml-0.5" />
                    </button>
                  </div>
                </div>
              )}

              {/* Quick Songs Side Preview */}
              <div className="lg:col-span-3 flex flex-col gap-3">
                <h3 className="text-xl font-bold text-white">Songs</h3>
                <div className="flex flex-col divide-y divide-white/5">
                  {results.slice(0, 4).map((track, i) => (
                    <TrackRow key={track.id} track={track} index={i} queueContext={results} />
                  ))}
                </div>
              </div>
            </div>

            {/* Complete Results Table */}
            <div className="flex flex-col gap-4 mt-4">
              <h3 className="text-xl font-bold text-white">All Results</h3>
              <div className="flex flex-col divide-y divide-white/5 bg-white/[0.02] rounded-xl p-2 border border-white/5">
                {results.map((track, i) => (
                  <TrackRow key={track.id} track={track} index={i} queueContext={results} />
                ))}
              </div>
            </div>
          </div>
        ) : query ? (
          <div className="flex flex-col items-center justify-center py-20 text-neutral-500">
            <Search className="w-12 h-12 stroke-[1.5] mb-3 text-neutral-600" />
            <p className="text-base font-semibold text-white">No results found for "{query}"</p>
            <p className="text-xs text-neutral-500 mt-1">
              Please make sure your words are spelled correctly or use fewer keywords.
            </p>
          </div>
        ) : (
          /* Browse All Genres */
          <div className="flex flex-col gap-6">
            <h2 className="text-2xl font-bold text-white tracking-tight">Browse all</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              {GENRES.map((g) => (
                <div
                  key={g.name}
                  onClick={() => setQuery(g.name)}
                  className={`${g.color} aspect-video rounded-xl p-4 font-bold text-lg text-white hover:scale-[1.02] active:scale-[0.98] transition-transform cursor-pointer relative overflow-hidden shadow-lg select-none`}
                >
                  <span className="relative z-10">{g.name}</span>
                  <div className="absolute -bottom-2 -right-3 text-white/20 text-5xl font-black rotate-12 select-none">
                    ♫
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
