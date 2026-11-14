import React, { useEffect, useState } from 'react';
import { Heart, Play, Sparkles, TrendingUp } from 'lucide-react';
import { Track } from '../../types';
import { api } from '../../services/api';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { useAuth } from '../../context/AuthContext';
import { Card } from '../ui/Card';

interface HomeViewProps {
  onNavigate: (view: string, data?: any) => void;
}

export const HomeView: React.FC<HomeViewProps> = ({ onNavigate }) => {
  const [trendingTracks, setTrendingTracks] = useState<Track[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const { playTrack } = useAudioPlayer();
  const { activeProvider } = useAuth();

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  useEffect(() => {
    const loadCharts = async () => {
      try {
        const tracks = await api.getCharts();
        setTrendingTracks(tracks);
      } catch (e) {
        console.error('Failed to load charts:', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadCharts();
  }, []);

  return (
    <div className="flex-1 overflow-y-auto px-4 md:px-8 pb-32 pt-2 select-none">
      {/* Top Greeting */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          {getGreeting()}
        </h1>
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-zinc-800 text-zinc-300 border border-zinc-700/60">
          <Sparkles className="w-3.5 h-3.5 text-spotify-green" />
          <span>RVX High-Res Audio</span>
        </div>
      </div>

      {/* Quick Play 6-Card Grid (Spotify Style) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 mb-8">
        {/* Pinned Liked Songs Card */}
        <div
          onClick={() => onNavigate('liked')}
          className="group flex items-center bg-zinc-800/50 hover:bg-zinc-700/60 rounded-md overflow-hidden cursor-pointer transition shadow"
        >
          <div className="w-16 h-16 bg-gradient-to-br from-indigo-700 via-purple-700 to-pink-500 flex items-center justify-center flex-shrink-0">
            <Heart className="w-7 h-7 text-white fill-white" />
          </div>
          <span className="font-bold text-sm text-white px-4 flex-1 truncate">
            Liked Songs
          </span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              onNavigate('liked');
            }}
            className="w-10 h-10 rounded-full bg-spotify-green text-black flex items-center justify-center mr-4 shadow-lg opacity-0 translate-y-1 group-hover:opacity-100 group-hover:translate-y-0 transition"
          >
            <Play className="w-4 h-4 fill-black ml-0.5" />
          </button>
        </div>

        {/* Quick trending sample cards */}
        {trendingTracks.slice(0, 5).map((track) => (
          <div
            key={track.id}
            onClick={() => playTrack(track, trendingTracks)}
            className="group flex items-center bg-zinc-800/50 hover:bg-zinc-700/60 rounded-md overflow-hidden cursor-pointer transition shadow"
          >
            {track.cover_url ? (
              <img
                src={track.cover_url}
                alt={track.title}
                className="w-16 h-16 object-cover flex-shrink-0"
              />
            ) : (
              <div className="w-16 h-16 bg-zinc-700 flex items-center justify-center flex-shrink-0 text-white font-bold">
                ♪
              </div>
            )}
            <span className="font-bold text-sm text-white px-4 flex-1 truncate">
              {track.title}
            </span>
            <button
              onClick={(e) => {
                e.stopPropagation();
                playTrack(track, trendingTracks);
              }}
              className="w-10 h-10 rounded-full bg-spotify-green text-black flex items-center justify-center mr-4 shadow-lg opacity-0 translate-y-1 group-hover:opacity-100 group-hover:translate-y-0 transition"
            >
              <Play className="w-4 h-4 fill-black ml-0.5" />
            </button>
          </div>
        ))}
      </div>

      {/* Trending Hits Section */}
      <section className="mb-8">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-spotify-green" />
            <h2 className="text-xl md:text-2xl font-bold text-white tracking-tight">
              Today's Biggest Hits
            </h2>
          </div>
          <span className="text-xs font-semibold text-spotify-subdued uppercase tracking-wider">
            Powered by RVX
          </span>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="bg-zinc-800/40 rounded-lg p-3 animate-pulse">
                <div className="aspect-square bg-zinc-700/60 rounded-md mb-3" />
                <div className="h-4 bg-zinc-700/60 rounded mb-2 w-3/4" />
                <div className="h-3 bg-zinc-700/60 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
            {trendingTracks.slice(0, 12).map((track) => (
              <Card
                key={track.id}
                title={track.title}
                subtitle={track.artists.map((a) => a.name).join(', ')}
                imageUrl={track.cover_url}
                onClick={() => playTrack(track, trendingTracks)}
              />
            ))}
          </div>
        )}
      </section>

      {/* Active Account Status Banner */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-zinc-800/80 to-zinc-900 border border-zinc-700/40 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div>
          <h3 className="font-bold text-base text-white">
            Active Provider: <span className="capitalize text-spotify-green">{activeProvider}</span>
          </h3>
          <p className="text-xs text-zinc-400 mt-0.5">
            When you hit the Heart icon while playing any song, it saves directly to your{' '}
            <span className="font-semibold text-zinc-300">
              {activeProvider === 'spotify'
                ? 'Spotify Liked Songs'
                : activeProvider === 'youtube'
                ? 'YouTube Music Library'
                : 'Local Library'}
            </span>
            .
          </p>
        </div>
        <button
          onClick={() => onNavigate('account')}
          className="px-4 py-2 rounded-full text-xs font-bold bg-white hover:bg-zinc-200 text-black shadow transition whitespace-nowrap"
        >
          Manage Accounts
        </button>
      </div>
    </div>
  );
};
