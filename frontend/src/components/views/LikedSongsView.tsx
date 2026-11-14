import React, { useEffect, useState } from 'react';
import { Heart, Play, Clock, Loader2, Music } from 'lucide-react';
import { Track } from '../../types';
import { api } from '../../services/api';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { useAuth } from '../../context/AuthContext';
import { TrackRow } from '../ui/TrackRow';

export const LikedSongsView: React.FC = () => {
  const [tracks, setTracks] = useState<Track[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const { playTrack } = useAudioPlayer();
  const { authStatus, activeProvider } = useAuth();

  const currentUser =
    activeProvider === 'spotify'
      ? authStatus.spotify_user
      : activeProvider === 'youtube'
      ? authStatus.youtube_user
      : null;

  useEffect(() => {
    const loadLikedTracks = async () => {
      setIsLoading(true);
      try {
        const res = await api.getLikedLibrary();
        setTracks(res.tracks || []);
      } catch (e) {
        console.error('Failed to load liked library:', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadLikedTracks();
  }, [activeProvider]);

  const handlePlayAll = () => {
    if (tracks.length > 0) {
      playTrack(tracks[0], tracks);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto pb-32 select-none">
      {/* Hero Header with Spotify-Signature Purple/Indigo Gradient */}
      <div className="bg-gradient-to-b from-indigo-800 via-indigo-950 to-spotify-black px-4 md:px-8 pt-8 pb-6 flex flex-col md:flex-row items-start md:items-end gap-6">
        {/* Big Heart Square Artwork */}
        <div className="w-36 h-36 md:w-52 md:h-52 rounded-lg bg-gradient-to-br from-indigo-600 via-purple-700 to-pink-500 flex items-center justify-center flex-shrink-0 shadow-2xl">
          <Heart className="w-16 h-16 md:w-24 md:h-24 text-white fill-white" />
        </div>

        {/* Header Metadata */}
        <div className="flex flex-col gap-2">
          <span className="text-xs font-bold uppercase tracking-wider text-white">
            Playlist
          </span>
          <h1 className="text-3xl md:text-6xl font-black text-white tracking-tight">
            Liked Songs
          </h1>
          <div className="flex items-center gap-2 text-xs text-zinc-300 font-medium mt-1">
            <span className="font-bold text-white">
              {currentUser?.display_name || 'My Library'}
            </span>
            <span>•</span>
            <span className="capitalize">{activeProvider} Library</span>
            <span>•</span>
            <span>{tracks.length} songs</span>
          </div>
        </div>
      </div>

      {/* Action Bar (Big Green Play Button) */}
      <div className="px-4 md:px-8 py-4 flex items-center gap-4">
        <button
          onClick={handlePlayAll}
          disabled={tracks.length === 0}
          className="w-14 h-14 rounded-full bg-spotify-green text-black flex items-center justify-center hover:scale-105 active:scale-95 transition shadow-lg disabled:opacity-50 disabled:scale-100"
          title="Play all"
        >
          <Play className="w-6 h-6 fill-black ml-1" />
        </button>
      </div>

      {/* Tracks Table Header */}
      <div className="px-4 md:px-8">
        <div className="grid grid-cols-12 text-xs font-medium text-zinc-400 border-b border-zinc-800 pb-2 px-3 mb-2">
          <div className="col-span-1">#</div>
          <div className="col-span-8 lg:col-span-6">Title</div>
          <div className="hidden lg:block lg:col-span-4">Album</div>
          <div className="col-span-3 lg:col-span-1 text-right flex items-center justify-end">
            <Clock className="w-4 h-4" />
          </div>
        </div>

        {/* Tracks List */}
        {isLoading ? (
          <div className="flex items-center justify-center py-16 gap-2 text-zinc-400">
            <Loader2 className="w-6 h-6 animate-spin text-spotify-green" />
            <span className="text-sm font-medium">Loading your liked songs...</span>
          </div>
        ) : tracks.length > 0 ? (
          <div className="flex flex-col gap-1">
            {tracks.map((track, idx) => (
              <TrackRow
                key={`${track.id}-${idx}`}
                track={track}
                index={idx}
                queueContext={tracks}
              />
            ))}
          </div>
        ) : (
          <div className="text-center py-16 text-zinc-400 flex flex-col items-center gap-3">
            <div className="w-16 h-16 rounded-full bg-zinc-800 flex items-center justify-center text-zinc-500">
              <Music className="w-8 h-8" />
            </div>
            <h3 className="font-bold text-lg text-white">Songs you like will appear here</h3>
            <p className="text-xs text-zinc-400 max-w-sm">
              Save songs by tapping the heart icon while listening. When logged in via{' '}
              <span className="capitalize text-spotify-green">{activeProvider}</span>, your likes
              are instantly synced!
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
