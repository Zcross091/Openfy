import React from 'react';
import { Home, Search, Library, User } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface MobileNavProps {
  currentView: string;
  onNavigate: (view: string) => void;
  onOpenAccountSwitcher: () => void;
}

export const MobileNav: React.FC<MobileNavProps> = ({
  currentView,
  onNavigate,
  onOpenAccountSwitcher,
}) => {
  const { activeProvider } = useAuth();

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 h-14 bg-black/95 backdrop-blur-md border-t border-zinc-850 flex items-center justify-around z-30 select-none">
      <button
        onClick={() => onNavigate('home')}
        className={`flex flex-col items-center gap-1 py-1 transition ${
          currentView === 'home' ? 'text-white' : 'text-zinc-400 hover:text-zinc-200'
        }`}
      >
        <Home className="w-5 h-5" />
        <span className="text-[10px] font-medium">Home</span>
      </button>

      <button
        onClick={() => onNavigate('search')}
        className={`flex flex-col items-center gap-1 py-1 transition ${
          currentView === 'search' ? 'text-white' : 'text-zinc-400 hover:text-zinc-200'
        }`}
      >
        <Search className="w-5 h-5" />
        <span className="text-[10px] font-medium">Search</span>
      </button>

      <button
        onClick={() => onNavigate('library')}
        className={`flex flex-col items-center gap-1 py-1 transition ${
          currentView === 'library' || currentView === 'liked'
            ? 'text-white'
            : 'text-zinc-400 hover:text-zinc-200'
        }`}
      >
        <Library className="w-5 h-5" />
        <span className="text-[10px] font-medium">Your Library</span>
      </button>

      <button
        onClick={onOpenAccountSwitcher}
        className="flex flex-col items-center gap-1 py-1 relative text-zinc-400 hover:text-zinc-200 transition"
      >
        <div className="relative">
          <User className="w-5 h-5" />
          <span
            className={`absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full ${
              activeProvider === 'spotify'
                ? 'bg-spotify-green'
                : activeProvider === 'youtube'
                ? 'bg-red-500'
                : 'bg-zinc-500'
            }`}
          />
        </div>
        <span className="text-[10px] font-medium capitalize">{activeProvider}</span>
      </button>
    </nav>
  );
};
