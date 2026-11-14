import React from 'react';
import { ChevronLeft, ChevronRight, Search, User, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface TopBarProps {
  currentView: string;
  onNavigate: (view: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onOpenAccountSwitcher: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  currentView,
  onNavigate,
  searchQuery,
  onSearchChange,
  onOpenAccountSwitcher,
}) => {
  const { authStatus, activeProvider, saveFeedback } = useAuth();

  const currentUser =
    activeProvider === 'spotify'
      ? authStatus.spotify_user
      : activeProvider === 'youtube'
      ? authStatus.youtube_user
      : null;

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between px-4 md:px-8 py-3 bg-spotify-black/80 backdrop-blur-md transition-colors select-none">
      {/* Left: Navigation Buttons & Optional Search */}
      <div className="flex items-center gap-3 flex-1 max-w-md">
        <div className="hidden sm:flex items-center gap-2">
          <button
            onClick={() => window.history.back()}
            className="w-8 h-8 rounded-full bg-black/60 hover:bg-black text-zinc-300 hover:text-white flex items-center justify-center transition"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={() => window.history.forward()}
            className="w-8 h-8 rounded-full bg-black/60 hover:bg-black text-zinc-300 hover:text-white flex items-center justify-center transition"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>

        {/* Search Input */}
        {currentView === 'search' && (
          <div className="relative flex-1 max-w-xs sm:max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="What do you want to play?"
              className="w-full bg-zinc-800 text-white placeholder-zinc-400 pl-9 pr-4 py-2 rounded-full text-sm font-medium focus:outline-none focus:ring-2 focus:ring-white border border-transparent transition"
              autoFocus
            />
          </div>
        )}
      </div>

      {/* Center: Save Feedback Toast */}
      {saveFeedback && (
        <div className="absolute left-1/2 -translate-x-1/2 flex items-center gap-2 bg-spotify-green text-black font-semibold text-xs px-4 py-1.5 rounded-full shadow-lg shadow-spotify-green/20 animate-bounce">
          <CheckCircle2 className="w-4 h-4" />
          {saveFeedback}
        </div>
      )}

      {/* Right: Active Account Pill */}
      <div className="flex items-center gap-3">
        <button
          onClick={onOpenAccountSwitcher}
          className="flex items-center gap-2 pl-2 pr-3 py-1.5 bg-black/70 hover:bg-zinc-850 hover:bg-zinc-800/80 border border-zinc-700/60 rounded-full transition group"
        >
          {/* Avatar / Icon */}
          <div className="relative w-7 h-7 rounded-full bg-zinc-700 flex items-center justify-center text-zinc-200 overflow-hidden">
            {currentUser?.images && currentUser.images[0]?.url ? (
              <img
                src={currentUser.images[0].url}
                alt="Avatar"
                className="w-full h-full object-cover"
              />
            ) : (
              <User className="w-4 h-4" />
            )}
            <span
              className={`absolute bottom-0 right-0 w-2 h-2 rounded-full border border-black ${
                activeProvider === 'spotify'
                  ? 'bg-spotify-green'
                  : activeProvider === 'youtube'
                  ? 'bg-red-500'
                  : 'bg-zinc-400'
              }`}
            />
          </div>

          {/* Account Label */}
          <div className="flex flex-col text-left">
            <span className="text-xs font-semibold text-white leading-tight">
              {currentUser?.display_name || (activeProvider === 'guest' ? 'Guest Mode' : 'Account')}
            </span>
            <span className="text-[10px] text-spotify-subdued uppercase tracking-wider capitalize leading-none">
              {activeProvider}
            </span>
          </div>
        </button>
      </div>
    </header>
  );
};
