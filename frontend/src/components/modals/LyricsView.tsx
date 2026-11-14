import React, { useEffect, useState, useRef } from 'react';
import { X, Mic2, Loader2 } from 'lucide-react';
import { useAudioPlayer } from '../../context/AudioPlayerContext';
import { LyricsData } from '../../types';
import { api } from '../../services/api';

export const LyricsView: React.FC = () => {
  const { currentTrack, currentTime, isLyricsOpen, toggleLyrics, seekTo } = useAudioPlayer();
  const [lyricsData, setLyricsData] = useState<LyricsData | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const activeLineRef = useRef<HTMLDivElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isLyricsOpen || !currentTrack) return;

    const loadLyrics = async () => {
      setIsLoading(true);
      try {
        const data = await api.getLyrics(
          currentTrack.title,
          currentTrack.artists[0]?.name || '',
          currentTrack.duration_seconds
        );
        setLyricsData(data);
      } catch (e) {
        console.error('Failed to load lyrics:', e);
      } finally {
        setIsLoading(false);
      }
    };

    loadLyrics();
  }, [isLyricsOpen, currentTrack?.id]);

  // Autoscroll to active line
  useEffect(() => {
    if (activeLineRef.current && containerRef.current) {
      activeLineRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [currentTime]);

  if (!isLyricsOpen || !currentTrack) return null;

  const currentLineIndex = lyricsData?.lyrics
    ? lyricsData.lyrics.findIndex((line, i) => {
        const nextTime = lyricsData.lyrics[i + 1]?.time ?? Infinity;
        return currentTime >= line.time && currentTime < nextTime;
      })
    : -1;

  return (
    <div className="fixed inset-0 z-50 bg-gradient-to-b from-teal-950 via-zinc-950 to-black/95 backdrop-blur-xl flex flex-col p-4 md:p-8 animate-in fade-in select-none">
      {/* Top Header */}
      <div className="flex items-center justify-between max-w-4xl w-full mx-auto pb-4 border-b border-white/10">
        <div className="flex items-center gap-3">
          <Mic2 className="w-6 h-6 text-spotify-green" />
          <div className="flex flex-col">
            <span className="font-bold text-lg text-white truncate max-w-xs sm:max-w-md">
              {currentTrack.title}
            </span>
            <span className="text-xs text-zinc-400 truncate">
              {currentTrack.artists.map((a) => a.name).join(', ')}
            </span>
          </div>
        </div>
        <button
          onClick={toggleLyrics}
          className="w-9 h-9 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Lyrics Content */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto max-w-4xl w-full mx-auto py-12 flex flex-col gap-6 text-center md:text-left"
      >
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-zinc-400">
            <Loader2 className="w-8 h-8 animate-spin text-spotify-green" />
            <span className="text-sm font-medium">Fetching synchronized lyrics...</span>
          </div>
        ) : lyricsData?.is_synced && lyricsData.lyrics.length > 0 ? (
          lyricsData.lyrics.map((line, idx) => {
            const isActive = idx === currentLineIndex;
            return (
              <div
                key={idx}
                ref={isActive ? activeLineRef : null}
                onClick={() => seekTo(line.time)}
                className={`text-2xl md:text-4xl font-extrabold cursor-pointer transition-all duration-300 ${
                  isActive
                    ? 'text-white scale-[1.02] drop-shadow-[0_0_20px_rgba(29,185,84,0.4)]'
                    : 'text-white/30 hover:text-white/60'
                }`}
              >
                {line.text}
              </div>
            );
          })
        ) : (
          <div className="text-center py-12 max-w-md mx-auto">
            <p className="text-lg md:text-xl font-medium text-zinc-300 whitespace-pre-line leading-relaxed">
              {lyricsData?.plain_lyrics || "Couldn't find lyrics for this song."}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
