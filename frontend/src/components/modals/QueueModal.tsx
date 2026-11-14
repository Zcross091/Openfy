import React from 'react';
import { X, ListMusic, Play } from 'lucide-react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';

export const QueueModal: React.FC = () => {
  const { currentTrack, queue, queueIndex, isQueueOpen, toggleQueue, playTrack } = useAudioPlayer();

  if (!isQueueOpen) return null;

  const nextTracks = queue.slice(queueIndex + 1);

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 select-none">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-md w-full max-h-[80vh] flex flex-col overflow-hidden shadow-2xl animate-in zoom-in-95">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-zinc-800">
          <div className="flex items-center gap-2">
            <ListMusic className="w-5 h-5 text-spotify-green" />
            <h2 className="text-base font-bold text-white">Playback Queue</h2>
          </div>
          <button
            onClick={toggleQueue}
            className="p-1 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 flex flex-col gap-5">
          {/* Now Playing */}
          {currentTrack && (
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2 block">
                Now Playing
              </span>
              <div className="flex items-center gap-3 p-2.5 rounded-lg bg-zinc-800/80 border border-zinc-700/60">
                {currentTrack.cover_url && (
                  <img
                    src={currentTrack.cover_url}
                    alt={currentTrack.title}
                    className="w-11 h-11 rounded object-cover shadow"
                  />
                )}
                <div className="flex flex-col min-w-0 flex-1">
                  <span className="text-sm font-bold text-spotify-green truncate">
                    {currentTrack.title}
                  </span>
                  <span className="text-xs text-zinc-400 truncate">
                    {currentTrack.artists.map((a) => a.name).join(', ')}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Up Next */}
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2 block">
              Next in Queue ({nextTracks.length})
            </span>
            {nextTracks.length > 0 ? (
              <div className="flex flex-col gap-1">
                {nextTracks.map((track, idx) => (
                  <div
                    key={`${track.id}-${idx}`}
                    onClick={() => playTrack(track, queue)}
                    className="group flex items-center justify-between p-2 rounded-md hover:bg-zinc-800 cursor-pointer transition"
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <span className="text-xs font-medium text-zinc-500 w-4">
                        {idx + 1}
                      </span>
                      {track.cover_url && (
                        <img
                          src={track.cover_url}
                          alt={track.title}
                          className="w-9 h-9 rounded object-cover shadow"
                        />
                      )}
                      <div className="flex flex-col min-w-0">
                        <span className="text-xs font-semibold text-white group-hover:text-spotify-green truncate">
                          {track.title}
                        </span>
                        <span className="text-[11px] text-zinc-400 truncate">
                          {track.artists.map((a) => a.name).join(', ')}
                        </span>
                      </div>
                    </div>
                    <Play className="w-3.5 h-3.5 text-zinc-400 opacity-0 group-hover:opacity-100 fill-current" />
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 text-xs text-zinc-500">
                Queue is empty. Play any album or playlist to queue tracks automatically!
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
