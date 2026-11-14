import React from 'react';
import { Play, Clock, Library } from 'lucide-react';
import { Playlist } from '../../types';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { TrackRow } from '../ui/TrackRow';

interface PlaylistViewProps {
  playlist: Playlist;
}

export const PlaylistView: React.FC<PlaylistViewProps> = ({ playlist }) => {
  const { playTrack } = useAudioPlayer();
  const tracks = playlist.tracks || [];

  const handlePlayAll = () => {
    if (tracks.length > 0) {
      playTrack(tracks[0], tracks);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto pb-32 select-none">
      {/* Hero Header */}
      <div className="bg-gradient-to-b from-emerald-900 via-zinc-900 to-spotify-black px-4 md:px-8 pt-8 pb-6 flex flex-col md:flex-row items-start md:items-end gap-6">
        {playlist.cover_url ? (
          <img
            src={playlist.cover_url}
            alt={playlist.title}
            className="w-36 h-36 md:w-52 md:h-52 rounded-lg object-cover flex-shrink-0 shadow-2xl"
          />
        ) : (
          <div className="w-36 h-36 md:w-52 md:h-52 rounded-lg bg-zinc-800 flex items-center justify-center flex-shrink-0 shadow-2xl text-zinc-500">
            <Library className="w-16 h-16" />
          </div>
        )}

        <div className="flex flex-col gap-2">
          <span className="text-xs font-bold uppercase tracking-wider text-white">
            Playlist
          </span>
          <h1 className="text-3xl md:text-5xl font-black text-white tracking-tight">
            {playlist.title}
          </h1>
          {playlist.description && (
            <p className="text-xs text-zinc-300 max-w-xl line-clamp-2">
              {playlist.description}
            </p>
          )}
          <div className="flex items-center gap-2 text-xs text-zinc-400 font-medium mt-1">
            <span className="capitalize text-white font-semibold">{playlist.source}</span>
            <span>•</span>
            <span>{playlist.track_count || tracks.length} songs</span>
          </div>
        </div>
      </div>

      {/* Action Bar */}
      <div className="px-4 md:px-8 py-4 flex items-center gap-4">
        <button
          onClick={handlePlayAll}
          disabled={tracks.length === 0}
          className="w-14 h-14 rounded-full bg-spotify-green text-black flex items-center justify-center hover:scale-105 active:scale-95 transition shadow-lg disabled:opacity-50"
          title="Play playlist"
        >
          <Play className="w-6 h-6 fill-black ml-1" />
        </button>
      </div>

      {/* Table */}
      <div className="px-4 md:px-8">
        <div className="grid grid-cols-12 text-xs font-medium text-zinc-400 border-b border-zinc-800 pb-2 px-3 mb-2">
          <div className="col-span-1">#</div>
          <div className="col-span-8 lg:col-span-6">Title</div>
          <div className="hidden lg:block lg:col-span-4">Album</div>
          <div className="col-span-3 lg:col-span-1 text-right flex items-center justify-end">
            <Clock className="w-4 h-4" />
          </div>
        </div>

        {tracks.length > 0 ? (
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
          <div className="text-center py-12 text-xs text-zinc-500">
            No tracks in this playlist yet.
          </div>
        )}
      </div>
    </div>
  );
};
