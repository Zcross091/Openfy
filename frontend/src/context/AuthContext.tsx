import React, { createContext, useContext, useState, useEffect } from 'react';
import { AuthStatus, ActiveProvider, Track } from '../types';
import { api } from '../services/api';

interface AuthContextType {
  authStatus: AuthStatus;
  isLoading: boolean;
  activeProvider: ActiveProvider;
  switchAccount: (provider: ActiveProvider) => Promise<void>;
  connectSpotify: (token: string) => Promise<void>;
  disconnectSpotify: () => Promise<void>;
  connectYoutubeDemo: () => Promise<void>;
  connectYoutubeCookie: (cookie: string) => Promise<void>;
  disconnectYoutube: () => Promise<void>;
  toggleLikeTrack: (track: Track) => Promise<boolean>;
  refreshStatus: () => Promise<void>;
  saveFeedback: string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authStatus, setAuthStatus] = useState<AuthStatus>({
    active_provider: 'guest',
    spotify_connected: false,
    youtube_connected: false,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [saveFeedback, setSaveFeedback] = useState<string | null>(null);

  const refreshStatus = async () => {
    try {
      const status = await api.getAuthStatus();
      setAuthStatus(status);
    } catch (e) {
      console.error('Failed to get auth status:', e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refreshStatus();
  }, []);

  const switchAccount = async (provider: ActiveProvider) => {
    try {
      await api.switchActiveAccount(provider);
      setAuthStatus(prev => ({ ...prev, active_provider: provider }));
    } catch (e) {
      console.error('Failed to switch account:', e);
    }
  };

  const connectSpotify = async (token: string) => {
    const res = await api.setSpotifyToken(token);
    if (res.success) {
      await refreshStatus();
    }
  };

  const disconnectSpotify = async () => {
    await api.disconnectSpotify();
    await refreshStatus();
  };

  const connectYoutubeDemo = async () => {
    await api.connectYoutubeDemo();
    await refreshStatus();
  };

  const connectYoutubeCookie = async (cookie: string) => {
    const res = await api.connectYoutubeCookie(cookie);
    if (res.success) {
      await refreshStatus();
    }
  };

  const disconnectYoutube = async () => {
    await api.disconnectYoutube();
    await refreshStatus();
  };

  const toggleLikeTrack = async (track: Track): Promise<boolean> => {
    const isCurrentlyLiked = track.is_liked;
    const provider = authStatus.active_provider;
    
    try {
      if (isCurrentlyLiked) {
        await api.removeTrack(track.id, provider);
        track.is_liked = false;
        triggerFeedback(`Removed from ${provider === 'spotify' ? 'Spotify Liked Songs' : provider === 'youtube' ? 'YouTube Liked Music' : 'Library'}`);
        return false;
      } else {
        await api.saveTrack(track, provider);
        track.is_liked = true;
        triggerFeedback(`Saved to ${provider === 'spotify' ? 'Spotify Liked Songs' : provider === 'youtube' ? 'YouTube Liked Music' : 'Library'}!`);
        return true;
      }
    } catch (e) {
      console.error('Failed to toggle track like:', e);
      return isCurrentlyLiked || false;
    }
  };

  const triggerFeedback = (message: string) => {
    setSaveFeedback(message);
    setTimeout(() => setSaveFeedback(null), 3000);
  };

  return (
    <AuthContext.Provider
      value={{
        authStatus,
        isLoading,
        activeProvider: authStatus.active_provider,
        switchAccount,
        connectSpotify,
        disconnectSpotify,
        connectYoutubeDemo,
        connectYoutubeCookie,
        disconnectYoutube,
        toggleLikeTrack,
        refreshStatus,
        saveFeedback,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
