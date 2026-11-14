import React from 'react';
import { Play } from 'lucide-react';

interface CardProps {
  title: string;
  subtitle: string;
  imageUrl?: string;
  onClick: () => void;
  onPlayClick?: () => void;
}

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  imageUrl,
  onClick,
  onPlayClick,
}) => {
  return (
    <div
      onClick={onClick}
      className="group relative bg-spotify-card hover:bg-spotify-card-hover p-3.5 rounded-lg transition-all duration-300 cursor-pointer flex flex-col select-none"
    >
      {/* Artwork container */}
      <div className="relative aspect-square w-full rounded-md overflow-hidden bg-zinc-800 mb-3 shadow-md">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-zinc-700 to-zinc-900 text-zinc-500 font-bold text-xl">
            {title.slice(0, 2).toUpperCase()}
          </div>
        )}

        {/* Hover Green Play Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            if (onPlayClick) onPlayClick();
            else onClick();
          }}
          className="absolute right-2 bottom-2 w-11 h-11 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 hover:scale-105 active:scale-95 transition-all duration-300"
          title="Play"
        >
          <Play className="w-5 h-5 fill-black ml-0.5" />
        </button>
      </div>

      {/* Title & Subtitle */}
      <div className="flex flex-col min-w-0">
        <span className="font-bold text-sm text-white truncate mb-1">{title}</span>
        <span className="text-xs text-spotify-subdued line-clamp-2 leading-relaxed">
          {subtitle}
        </span>
      </div>
    </div>
  );
};
