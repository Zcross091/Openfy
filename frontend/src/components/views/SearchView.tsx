import React, { useEffect, useState } from 'react';
import { Search as SearchIcon, Play, Loader2 } from 'lucide-react';
import { Track } from '../../types';
import { api } from '../../services/api';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { TrackRow } from '../ui/TrackRow';

interface SearchViewProps {
  searchQuery: string;
  onSearchChange: (q: string) => void;
}

const CATEGORIES = [
  { name: 'Pop', color: 'from-pink-600 to-rose-700' },
  { name: 'Hip-Hop', color: 'from-amber-600 to-orange-700' },
  { name: 'Rock', color: 'from-red-600 to-stone-800' },
  { name: 'Chill', color: 'from-blue-600 to-indigo-800' },
  { name: 'Indie', color: 'from-emerald-600 to-teal-800' },
  { name: 'Dance & EDM', color: 'from-purple-600 to-violet-800' },
  { name: 'R&B', color: 'from-yellow-600 to-amber-800' },
  { name: 'Workout', color: 'from-cyan-600 to-blue-800' },
  { name: 'Focus & Study', color: 'from-teal-600 to-emerald-900' },
  { name: 'Gaming', color: 'from-fuchsia-600 to-purple-900' },
  { name: 'Acoustic', color: 'from-orange-600 to-amber-900' },
  { name: 'Bollywood & Desi', color: 'from-rose-600 to-red-800' },
];

export const SearchView: React.FC<SearchViewProps> = ({ searchQuery, onSearchChange }) => {
  const [results, setResults] = useState<Track[]>([]);
  const [isSearching, setIsSearching] = useState(false);

  const { playTrack } = useAudioPlayer();

  useEffect(() => {
    if (!searchQuery.trim()) {
      setResults([]);
      setIsSearching(false);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const tracks = await api.search(searchQuery);
        setResults(tracks);
      } catch (e) {
        console.error('Search failed:', e);
      } finally {
        setIsSearching(false);
      }
    }, 350);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const topResult = results[0];

  return (
    <div className="flex-1 overflow-y-auto px-4 md:px-8 pb-32 pt-2 select-none">
      {/* Mobile search bar if topbar input is hidden on very small screens */}
      <div className="relative mb-6 sm:hidden">
        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search songs, artists, albums..."
          className="w-full bg-zinc-800 text-white placeholder-zinc-400 pl-9 pr-4 py-2.5 rounded-full text-sm font-medium focus:outline-none focus:ring-2 focus:ring-white border border-transparent"
        />
      </div>

      {/* Loading Spinner */}
      {isSearching && (
        <div className="flex items-center justify-center py-12 gap-2 text-zinc-400">
          <Loader2 className="w-5 h-5 animate-spin text-spotify-green" />
          <span className="text-sm font-medium">Searching RVX audio catalog...</span>
        </div>
      )}

      {/* If Search Has Results */}
      {!isSearching && results.length > 0 && (
        <div className="flex flex-col gap-8">
          {/* Top Result + Songs Columns */}
          <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
            {/* Top Result Card */}
            {topResult && (
              <div className="lg:col-span-2 flex flex-col">
                <h2 className="text-xl font-bold text-white mb-3">Top result</h2>
                <div
                  onClick={() => playTrack(topResult, results)}
                  className="group relative bg-spotify-card hover:bg-spotify-card-hover p-5 rounded-xl transition duration-300 cursor-pointer flex flex-col justify-between flex-1 shadow-lg"
                >
                  <div>
                    {topResult.cover_url ? (
                      <img
                        src={topResult.cover_url}
                        alt={topResult.title}
                        className="w-24 h-24 rounded-lg object-cover mb-4 shadow"
                      />
                    ) : (
                      <div className="w-24 h-24 rounded-lg bg-zinc-700 flex items-center justify-center text-3xl font-bold mb-4">
                        ♪
                      </div>
                    )}
                    <h3 className="font-extrabold text-2xl text-white truncate mb-1">
                      {topResult.title}
                    </h3>
                    <p className="text-sm text-spotify-subdued">
                      Song • {topResult.artists.map((a) => a.name).join(', ')}
                    </p>
                  </div>

                  {/* Play Button */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      playTrack(topResult, results);
                    }}
                    className="self-end w-12 h-12 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl hover:scale-105 active:scale-95 transition"
                  >
                    <Play className="w-6 h-6 fill-black ml-0.5" />
                  </button>
                </div>
              </div>
            )}

            {/* Songs List */}
            <div className="lg:col-span-3 flex flex-col">
              <h2 className="text-xl font-bold text-white mb-3">Songs</h2>
              <div className="flex flex-col gap-1">
                {results.slice(0, 5).map((track, idx) => (
                  <TrackRow key={track.id} track={track} index={idx} queueContext={results} />
                ))}
              </div>
            </div>
          </div>

          {/* More Results Table */}
          {results.length > 5 && (
            <div>
              <h2 className="text-xl font-bold text-white mb-3">More results</h2>
              <div className="flex flex-col gap-1">
                {results.slice(5).map((track, idx) => (
                  <TrackRow key={track.id} track={track} index={idx + 5} queueContext={results} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* If No Query: Browse All Categories */}
      {!searchQuery.trim() && (
        <section>
          <h2 className="text-xl md:text-2xl font-bold text-white mb-4 tracking-tight">
            Browse all
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {CATEGORIES.map((cat) => (
              <div
                key={cat.name}
                onClick={() => onSearchChange(cat.name)}
                className={`relative aspect-square rounded-xl bg-gradient-to-br ${cat.color} p-4 cursor-pointer overflow-hidden shadow-md hover:scale-[1.02] active:scale-[0.98] transition`}
              >
                <span className="font-bold text-lg text-white leading-tight block">
                  {cat.name}
                </span>
                <span className="absolute bottom-2 right-2 text-3xl font-extrabold opacity-40 select-none">
                  #
                </span>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
