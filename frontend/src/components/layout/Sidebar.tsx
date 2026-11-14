import React from 'react';
import { Home, Search, Library, Heart, Radio, RefreshCw } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { Playlist } from '../../types';

interface SidebarProps {
  currentView: string;
  onNavigate: (view: string, data?: any) => void;
  playlists: Playlist[];
  onOpenAccountSwitcher: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentView,
  onNavigate,
  playlists,
  onOpenAccountSwitcher,
}) => {
  const { authStatus, activeProvider } = useAuth();

  return (
    <aside className="hidden md:flex flex-col w-64 bg-black p-2 gap-2 h-full select-none">
      {/* Top Navigation Box */}
      <div className="bg-spotify-black rounded-lg p-4 flex flex-col gap-4">
        {/* Brand Header */}
        <div className="flex items-center gap-2.5 px-2">
          <div className="w-8 h-8 rounded-full bg-spotify-green flex items-center justify-center shadow-lg shadow-spotify-green/20">
            <Radio className="w-5 h-5 text-black" />
          </div>
          <span className="font-bold text-xl tracking-tight text-white flex items-center gap-1.5">
            Openfy
            <span className="text-[10px] font-semibold uppercase tracking-wider bg-zinc-800 text-zinc-300 px-1.5 py-0.5 rounded border border-zinc-700">
              RVX
            </span>
          </span>
        </div>

        {/* Primary Links */}
        <nav className="flex flex-col gap-1 mt-1">
          <button
            onClick={() => onNavigate('home')}
            className={`flex items-center gap-4 px-3 py-2.5 rounded-md font-semibold text-sm transition-colors ${
              currentView === 'home'
                ? 'text-white bg-zinc-800/60'
                : 'text-spotify-subdued hover:text-white'
            }`}
          >
            <Home className="w-5 h-5" />
            Home
          </button>
          <button
            onClick={() => onNavigate('search')}
            className={`flex items-center gap-4 px-3 py-2.5 rounded-md font-semibold text-sm transition-colors ${
              currentView === 'search'
                ? 'text-white bg-zinc-800/60'
                : 'text-spotify-subdued hover:text-white'
            }`}
          >
            <Search className="w-5 h-5" />
            Search
          </button>
        </nav>
      </div>

      {/* Library & Playlists Box */}
      <div className="bg-spotify-black rounded-lg p-3 flex-1 flex flex-col min-h-0">
        <div className="flex items-center justify-between px-3 py-2 text-spotify-subdued">
          <button
            onClick={() => onNavigate('library')}
            className="flex items-center gap-3 font-semibold text-sm hover:text-white transition-colors"
          >
            <Library className="w-5 h-5" />
            Your Library
          </button>
          
          {/* Active Account Switch Pill */}
          <button
            onClick={onOpenAccountSwitcher}
            title="Switch Active Account"
            className="flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-medium bg-zinc-800 hover:bg-zinc-700 text-zinc-300 hover:text-white border border-zinc-700/60 transition"
          >
            <span
              className={`w-2 h-2 rounded-full ${
                activeProvider === 'spotify'
                  ? 'bg-spotify-green'
                  : activeProvider === 'youtube'
                  ? 'bg-red-500'
                  : 'bg-zinc-500'
              }`}
            />
            <span className="capitalize">{activeProvider}</span>
            <RefreshCw className="w-3 h-3 text-zinc-400" />
          </button>
        </div>

        {/* Liked Songs Pinned Tile */}
        <div className="mt-2 px-1">
          <div
            onClick={() => onNavigate('liked')}
            className={`flex items-center gap-3.5 p-2 rounded-md cursor-pointer transition-colors ${
              currentView === 'liked' ? 'bg-zinc-800/70' : 'hover:bg-zinc-850 hover:bg-zinc-800/40'
            }`}
          >
            <div className="w-12 h-12 rounded bg-gradient-to-br from-indigo-700 via-purple-700 to-pink-500 flex items-center justify-center flex-shrink-0 shadow-md">
              <Heart className="w-5 h-5 text-white fill-white" />
            </div>
            <div className="flex flex-col min-w-0">
              <span className={`font-semibold text-sm truncate ${currentView === 'liked' ? 'text-spotify-green' : 'text-white'}`}>
                Liked Songs
              </span>
              <span className="text-xs text-spotify-subdued capitalize">
                Playlist • {activeProvider} Library
              </span>
            </div>
          </div>
        </div>

        {/* Scrollable Playlists list */}
        <div className="mt-3 flex-1 overflow-y-auto pr-1 flex flex-col gap-1">
          {playlists.map((pl) => (
            <div
              key={pl.id}
              onClick={() => onNavigate('playlist', pl)}
              className="flex items-center gap-3 p-2 rounded-md hover:bg-zinc-800/40 cursor-pointer transition group"
            >
              {pl.cover_url ? (
                <img
                  src={pl.cover_url}
                  alt={pl.title}
                  className="w-11 h-11 rounded object-cover flex-shrink-0"
                />
              ) : (
                <div className="w-11 h-11 rounded bg-zinc-800 flex items-center justify-center flex-shrink-0 text-zinc-400">
                  <Library className="w-5 h-5" />
                </div>
              )}
              <div className="flex flex-col min-w-0">
                <span className="font-medium text-sm text-zinc-200 group-hover:text-white truncate">
                  {pl.title}
                </span>
                <span className="text-xs text-spotify-subdued truncate capitalize">
                  {pl.source} • {pl.track_count} tracks
                </span>
              </div>
            </div>
          ))}
          {playlists.length === 0 && (
            <div className="text-center py-6 px-3 text-xs text-spotify-muted">
              Connect your Spotify or YouTube account to sync playlists.
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
