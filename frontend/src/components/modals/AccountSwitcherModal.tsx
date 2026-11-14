import React, { useState } from 'react';
import { X, Check, Radio, LogOut, KeyRound, Sparkles, ExternalLink } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { ActiveProvider } from '../../types';

interface AccountSwitcherModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AccountSwitcherModal: React.FC<AccountSwitcherModalProps> = ({ isOpen, onClose }) => {
  const {
    authStatus,
    activeProvider,
    switchAccount,
    connectSpotify,
    disconnectSpotify,
    connectYoutubeDemo,
    connectYoutubeCookie,
    disconnectYoutube,
  } = useAuth();

  const [spotifyTokenInput, setSpotifyTokenInput] = useState('');
  const [youtubeCookieInput, setYoutubeCookieInput] = useState('');
  const [selectedTab, setSelectedTab] = useState<ActiveProvider>(activeProvider);

  if (!isOpen) return null;

  const handleSelectProvider = async (provider: ActiveProvider) => {
    setSelectedTab(provider);
    await switchAccount(provider);
  };

  const handleConnectSpotify = async () => {
    if (!spotifyTokenInput.trim()) return;
    await connectSpotify(spotifyTokenInput.trim());
    setSpotifyTokenInput('');
  };

  const handleConnectYoutubeCookie = async () => {
    if (!youtubeCookieInput.trim()) return;
    await connectYoutubeCookie(youtubeCookieInput.trim());
    setYoutubeCookieInput('');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 select-none">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-lg w-full overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-800">
          <div className="flex items-center gap-2.5">
            <Radio className="w-5 h-5 text-spotify-green" />
            <h2 className="text-lg font-bold text-white">Account & Library Switcher</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-full text-zinc-400 hover:text-white hover:bg-zinc-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 flex flex-col gap-6">
          <p className="text-xs text-zinc-400 leading-relaxed">
            Openfy allows you to log in from your <span className="text-white font-semibold">Spotify</span> or{' '}
            <span className="text-white font-semibold">YouTube Music</span> account, one at a time. When
            listening to any song, clicking the <span className="text-spotify-green font-semibold">Heart</span> icon
            immediately saves that track directly to your active library!
          </p>

          {/* Account Choice Cards */}
          <div className="grid grid-cols-3 gap-3">
            {/* Spotify Option */}
            <div
              onClick={() => handleSelectProvider('spotify')}
              className={`p-3 rounded-xl border flex flex-col items-center text-center cursor-pointer transition ${
                activeProvider === 'spotify'
                  ? 'border-spotify-green bg-spotify-green/10 text-white'
                  : 'border-zinc-800 hover:border-zinc-700 bg-zinc-850 text-zinc-400'
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-spotify-green flex items-center justify-center mb-2 shadow">
                <span className="text-black font-extrabold text-sm">Sp</span>
              </div>
              <span className="text-xs font-bold text-white mb-0.5">Spotify</span>
              <span className="text-[10px] text-zinc-400">
                {authStatus.spotify_connected ? 'Connected' : 'Not Linked'}
              </span>
              {activeProvider === 'spotify' && (
                <span className="mt-2 text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-spotify-green text-black">
                  Active
                </span>
              )}
            </div>

            {/* YouTube Music Option */}
            <div
              onClick={() => handleSelectProvider('youtube')}
              className={`p-3 rounded-xl border flex flex-col items-center text-center cursor-pointer transition ${
                activeProvider === 'youtube'
                  ? 'border-red-500 bg-red-500/10 text-white'
                  : 'border-zinc-800 hover:border-zinc-700 bg-zinc-850 text-zinc-400'
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-red-600 flex items-center justify-center mb-2 shadow">
                <span className="text-white font-extrabold text-sm">YT</span>
              </div>
              <span className="text-xs font-bold text-white mb-0.5">YouTube</span>
              <span className="text-[10px] text-zinc-400">
                {authStatus.youtube_connected ? 'Connected' : 'Not Linked'}
              </span>
              {activeProvider === 'youtube' && (
                <span className="mt-2 text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-red-500 text-white">
                  Active
                </span>
              )}
            </div>

            {/* Guest Option */}
            <div
              onClick={() => handleSelectProvider('guest')}
              className={`p-3 rounded-xl border flex flex-col items-center text-center cursor-pointer transition ${
                activeProvider === 'guest'
                  ? 'border-zinc-400 bg-zinc-800 text-white'
                  : 'border-zinc-800 hover:border-zinc-700 bg-zinc-850 text-zinc-400'
              }`}
            >
              <div className="w-9 h-9 rounded-full bg-zinc-700 flex items-center justify-center mb-2 shadow">
                <span className="text-zinc-200 font-extrabold text-sm">Free</span>
              </div>
              <span className="text-xs font-bold text-white mb-0.5">Guest Mode</span>
              <span className="text-[10px] text-zinc-400">Unrestricted</span>
              {activeProvider === 'guest' && (
                <span className="mt-2 text-[9px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-zinc-600 text-white">
                  Active
                </span>
              )}
            </div>
          </div>

          {/* Active Provider Connection Details */}
          <div className="bg-zinc-950/70 border border-zinc-800/80 rounded-xl p-4 flex flex-col gap-3">
            {activeProvider === 'spotify' && (
              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-spotify-green" />
                    <span className="text-sm font-semibold text-white">
                      Spotify Connection
                    </span>
                  </div>
                  {authStatus.spotify_connected && (
                    <button
                      onClick={disconnectSpotify}
                      className="text-xs text-red-400 hover:text-red-300 flex items-center gap-1"
                    >
                      <LogOut className="w-3.5 h-3.5" /> Disconnect
                    </button>
                  )}
                </div>

                {authStatus.spotify_connected ? (
                  <div className="text-xs text-zinc-300 flex items-center gap-2 bg-zinc-800/60 p-2.5 rounded-lg">
                    <Check className="w-4 h-4 text-spotify-green flex-shrink-0" />
                    <span>
                      Logged in as{' '}
                      <strong className="text-white">
                        {authStatus.spotify_user?.display_name || 'Spotify User'}
                      </strong>
                      . All liked songs and playlists are synced!
                    </span>
                  </div>
                ) : (
                  <div className="flex flex-col gap-2">
                    <p className="text-xs text-zinc-400">
                      Paste your Spotify OAuth access token, or use the PKCE authorization flow:
                    </p>
                    <div className="flex items-center gap-2">
                      <input
                        type="password"
                        value={spotifyTokenInput}
                        onChange={(e) => setSpotifyTokenInput(e.target.value)}
                        placeholder="Paste Bearer access token..."
                        className="flex-1 bg-zinc-800 text-white placeholder-zinc-500 px-3 py-1.5 rounded-lg text-xs font-mono focus:outline-none focus:ring-1 focus:ring-spotify-green border border-zinc-700"
                      />
                      <button
                        onClick={handleConnectSpotify}
                        className="px-3 py-1.5 rounded-lg text-xs font-bold bg-spotify-green text-black hover:bg-spotify-green-hover transition"
                      >
                        Save
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeProvider === 'youtube' && (
              <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500" />
                    <span className="text-sm font-semibold text-white">
                      YouTube Music Connection
                    </span>
                  </div>
                  {authStatus.youtube_connected && (
                    <button
                      onClick={disconnectYoutube}
                      className="text-xs text-red-400 hover:text-red-300 flex items-center gap-1"
                    >
                      <LogOut className="w-3.5 h-3.5" /> Disconnect
                    </button>
                  )}
                </div>

                {authStatus.youtube_connected ? (
                  <div className="text-xs text-zinc-300 flex items-center gap-2 bg-zinc-800/60 p-2.5 rounded-lg">
                    <Check className="w-4 h-4 text-red-500 flex-shrink-0" />
                    <span>
                      Logged in via YouTube Music. Liked tracks save directly to your YouTube Liked
                      Music playlist!
                    </span>
                  </div>
                ) : (
                  <div className="flex flex-col gap-2.5">
                    <p className="text-xs text-zinc-400">
                      Connect via 1-click instant demo mode, or paste session cookies:
                    </p>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={connectYoutubeDemo}
                        className="flex-1 py-2 px-3 rounded-lg text-xs font-bold bg-red-600 hover:bg-red-500 text-white flex items-center justify-center gap-1.5 shadow transition"
                      >
                        <Sparkles className="w-3.5 h-3.5" /> 1-Click Instant Demo Connect
                      </button>
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="password"
                        value={youtubeCookieInput}
                        onChange={(e) => setYoutubeCookieInput(e.target.value)}
                        placeholder="Or paste Cookie header string..."
                        className="flex-1 bg-zinc-800 text-white placeholder-zinc-500 px-3 py-1.5 rounded-lg text-xs font-mono focus:outline-none focus:ring-1 focus:ring-red-500 border border-zinc-700"
                      />
                      <button
                        onClick={handleConnectYoutubeCookie}
                        className="px-3 py-1.5 rounded-lg text-xs font-bold bg-zinc-700 text-white hover:bg-zinc-600 transition"
                      >
                        Connect
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}

            {activeProvider === 'guest' && (
              <div className="text-xs text-zinc-300 flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-spotify-green flex-shrink-0" />
                <span>
                  Guest Mode is active. You can search, browse top charts, and stream unrestricted
                  audio powered by the RVX YouTube Music engine without logging into any service!
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3.5 bg-zinc-950 border-t border-zinc-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-full text-xs font-bold bg-white hover:bg-zinc-200 text-black shadow transition"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
