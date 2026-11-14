import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AudioPlayerProvider } from './context/AudioPlayerContext';
import { Sidebar } from './components/layout/Sidebar';
import { TopBar } from './components/layout/TopBar';
import { PlayerBar } from './components/layout/PlayerBar';
import { MobileNav } from './components/layout/MobileNav';
import { HomeView } from './components/views/HomeView';
import { SearchView } from './components/views/SearchView';
import { LikedSongsView } from './components/views/LikedSongsView';
import { PlaylistView } from './components/views/PlaylistView';
import { AccountSwitcherModal } from './components/modals/AccountSwitcherModal';
import { LyricsView } from './components/modals/LyricsView';
import { NowPlayingModal } from './components/modals/NowPlayingModal';
import { QueueModal } from './components/modals/QueueModal';
import { Playlist } from './types';
import { api } from './services/api';

const MainLayout: React.FC = () => {
  const [currentView, setCurrentView] = useState<'home' | 'search' | 'liked' | 'playlist' | 'library'>('home');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPlaylist, setSelectedPlaylist] = useState<Playlist | null>(null);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [isAccountSwitcherOpen, setIsAccountSwitcherOpen] = useState(false);

  const { activeProvider } = useAuth();

  // Load user playlists whenever active provider changes
  useEffect(() => {
    const loadPlaylists = async () => {
      try {
        const res = await api.getUserPlaylists();
        setPlaylists(res.playlists || []);
      } catch (e) {
        console.error('Failed to load user playlists:', e);
      }
    };
    loadPlaylists();
  }, [activeProvider]);

  const handleNavigate = (view: string, data?: any) => {
    if (view === 'playlist' && data) {
      setSelectedPlaylist(data);
      setCurrentView('playlist');
    } else if (view === 'account') {
      setIsAccountSwitcherOpen(true);
    } else {
      setCurrentView(view as any);
    }
  };

  return (
    <div className="flex h-screen w-screen overflow-hidden bg-black text-white font-sans">
      {/* Desktop Sidebar */}
      <Sidebar
        currentView={currentView}
        onNavigate={handleNavigate}
        playlists={playlists}
        onOpenAccountSwitcher={() => setIsAccountSwitcherOpen(true)}
      />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col h-full bg-spotify-black rounded-lg md:my-2 md:mr-2 overflow-hidden relative">
        <TopBar
          currentView={currentView}
          onNavigate={handleNavigate}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          onOpenAccountSwitcher={() => setIsAccountSwitcherOpen(true)}
        />

        {/* Dynamic Views */}
        <main className="flex-1 flex flex-col overflow-hidden">
          {currentView === 'home' && <HomeView onNavigate={handleNavigate} />}
          {currentView === 'search' && (
            <SearchView searchQuery={searchQuery} onSearchChange={setSearchQuery} />
          )}
          {currentView === 'liked' && <LikedSongsView />}
          {currentView === 'playlist' && selectedPlaylist && (
            <PlaylistView playlist={selectedPlaylist} />
          )}
          {currentView === 'library' && <LikedSongsView />}
        </main>
      </div>

      {/* Bottom Audio Player Bar */}
      <PlayerBar />

      {/* Mobile Bottom Navigation Bar */}
      <MobileNav
        currentView={currentView}
        onNavigate={handleNavigate}
        onOpenAccountSwitcher={() => setIsAccountSwitcherOpen(true)}
      />

      {/* Modals & Overlays */}
      <AccountSwitcherModal
        isOpen={isAccountSwitcherOpen}
        onClose={() => setIsAccountSwitcherOpen(false)}
      />
      <LyricsView />
      <NowPlayingModal />
      <QueueModal />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AudioPlayerProvider>
        <MainLayout />
      </AudioPlayerProvider>
    </AuthProvider>
  );
};

export default App;
