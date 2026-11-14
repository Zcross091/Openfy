import React from 'react';
import { Play, Pause, Heart, Radio } from 'lucide-react';
import { Track } from '../../types';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { useAuth } from '../../context/AuthContext';

interface TrackRowProps {
  track: Track;
  index: number;
  queueContext?: Track[];
}

export const TrackRow: React.FC<TrackRowProps> = ({ track, index, queueContext }) => {
  const { currentTrack, isPlaying, playTrack, togglePlay } = useAudioPlayer();
  const { toggleLikeTrack, activeProvider } = useAuth();

  const isCurrent = currentTrack?.id === track.id || currentTrack?.title === track.title;

  const formatDuration = (seconds: number) => {
    if (!seconds) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const handleClickPlay = () => {
    if (isCurrent) {
      togglePlay();
    } else {
      playTrack(track, queueContext);
    }
  };

  return (
    <div
      className={`group flex items-center justify-between px-3 py-2 rounded-md hover:bg-white/10 transition cursor-pointer select-none ${
        isCurrent ? 'bg-white/10' : ''
      }`}
    >
      {/* Left: Index / Play Icon & Track Details */}
      <div className="flex items-center gap-3.5 min-w-0 flex-1">
        {/* Index or Play button */}
        <div className="w-5 text-center flex items-center justify-center flex-shrink-0">
          <span
            className={`text-sm font-medium ${
              isCurrent ? 'text-spotify-green' : 'text-zinc-400 group-hover:hidden'
            }`}
          >
            {index + 1}
          </span>
          <button
            onClick={handleClickPlay}
            className={`hidden group-hover:flex items-center justify-center text-white ${
              isCurrent ? '!flex text-spotify-green' : ''
            }`}
          >
            {isCurrent && isPlaying ? (
              <Pause className="w-4 h-4 fill-current" />
            ) : (
              <Play className="w-4 h-4 fill-current ml-0.5" />
            )}
          </button>
        </div>

        {/* Thumbnail */}
        {track.cover_url ? (
          <img
            src={track.cover_url}
            alt={track.title}
            className="w-10 h-10 rounded object-cover flex-shrink-0 shadow"
          />
        ) : (
          <div className="w-10 h-10 rounded bg-zinc-800 flex items-center justify-center flex-shrink-0">
            <Radio className="w-5 h-5 text-zinc-500" />
          </div>
        )}

        {/* Title and Artists */}
        <div className="flex flex-col min-w-0 pr-2">
          <span
            className={`text-sm font-semibold truncate ${
              isCurrent ? 'text-spotify-green' : 'text-white'
            }`}
          >
            {track.title}
          </span>
          <span className="text-xs text-zinc-400 truncate group-hover:text-zinc-300">
            {track.artists.map((a) => a.name).join(', ')}
          </span>
        </div>
      </div>

      {/* Center: Album Name (Desktop) */}
      {track.album && (
        <div className="hidden lg:block w-1/3 text-xs text-zinc-400 truncate pr-4">
          {track.album.name}
        </div>
      )}

      {/* Right: Like Heart & Duration */}
      <div className="flex items-center gap-3 flex-shrink-0">
        <button
          onClick={(e) => {
            e.stopPropagation();
            toggleLikeTrack(track);
          }}
          title={`Save to ${activeProvider} library`}
          className="p-1 hover:scale-110 active:scale-95 transition"
        >
          <Heart
            className={`w-4 h-4 ${
              track.is_liked
                ? 'text-spotify-green fill-spotify-green'
                : 'text-zinc-400 group-hover:text-zinc-200 opacity-0 group-hover:opacity-100 sm:opacity-100'
            }`}
          />
        </button>

        <span className="text-xs font-mono text-zinc-400 w-10 text-right">
          {formatDuration(track.duration_seconds)}
        </span>
      </div>
    </div>
  );
};
