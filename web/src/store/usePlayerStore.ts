import { create } from "zustand";
import { persist } from "zustand/middleware";
import { PlayerState, Track, RepeatMode } from "@/types";

export const usePlayerStore = create<PlayerState>()(
  persist(
    (set, get) => ({
      currentTrack: null,
      isPlaying: false,
      isBuffering: false,
      currentTime: 0,
      duration: 0,
      volume: 80,
      isMuted: false,

      queue: [],
      queueIndex: -1,
      history: [],

      isShuffle: false,
      repeatMode: "off",
      isRightSidebarOpen: true,

      likedSongs: [],

      playTrack: (track: Track, newQueue?: Track[]) => {
        const state = get();
        let queue = newQueue || state.queue;

        // If track is not in queue, add it
        let queueIndex = queue.findIndex((t) => t.id === track.id);
        if (queueIndex === -1) {
          queue = [...queue, track];
          queueIndex = queue.length - 1;
        }

        const history = state.currentTrack
          ? [state.currentTrack, ...state.history.slice(0, 49)]
          : state.history;

        set({
          currentTrack: track,
          queue,
          queueIndex,
          history,
          isPlaying: true,
          currentTime: 0,
        });
      },

      togglePlay: () => {
        set((state) => ({ isPlaying: !state.isPlaying }));
      },

      skipNext: () => {
        const state = get();
        if (state.queue.length === 0) return;

        let nextIndex = state.queueIndex + 1;

        if (state.isShuffle) {
          nextIndex = Math.floor(Math.random() * state.queue.length);
        }

        if (nextIndex >= state.queue.length) {
          if (state.repeatMode === "all") {
            nextIndex = 0;
          } else {
            // Endless Radio: Fetch related songs dynamically
            const lastTrack = state.currentTrack;
            if (lastTrack) {
              fetch(`/api/search?q=${encodeURIComponent(lastTrack.artist + " top hits")}`)
                .then((res) => res.json())
                .then((data) => {
                  if (data.tracks && data.tracks.length > 0) {
                    const newTracks = data.tracks.filter(
                      (t: Track) => !state.queue.some((q) => q.id === t.id)
                    );
                    if (newTracks.length > 0) {
                      set((s) => ({
                        queue: [...s.queue, ...newTracks],
                        currentTrack: newTracks[0],
                        queueIndex: s.queue.length,
                        isPlaying: true,
                        currentTime: 0,
                      }));
                    }
                  }
                })
                .catch((e) => console.error("Endless Radio fetch error:", e));
            }
            return;
          }
        }

        const nextTrack = state.queue[nextIndex];
        if (nextTrack) {
          set({
            currentTrack: nextTrack,
            queueIndex: nextIndex,
            isPlaying: true,
            currentTime: 0,
          });
        }
      },

      skipPrev: () => {
        const state = get();
        // If track played for more than 3 seconds, restart it
        if (state.currentTime > 3) {
          set({ currentTime: 0 });
          return;
        }

        const prevIndex = state.queueIndex - 1;
        if (prevIndex >= 0 && state.queue[prevIndex]) {
          set({
            currentTrack: state.queue[prevIndex],
            queueIndex: prevIndex,
            isPlaying: true,
            currentTime: 0,
          });
        } else {
          set({ currentTime: 0 });
        }
      },

      seekTo: (seconds: number) => {
        set({ currentTime: seconds });
      },

      setVolume: (vol: number) => {
        set({ volume: Math.max(0, Math.min(100, vol)), isMuted: vol === 0 });
      },

      toggleMute: () => {
        set((state) => ({ isMuted: !state.isMuted }));
      },

      toggleShuffle: () => {
        set((state) => ({ isShuffle: !state.isShuffle }));
      },

      toggleRepeat: () => {
        const modes: RepeatMode[] = ["off", "all", "one"];
        const next = modes[(modes.indexOf(get().repeatMode) + 1) % modes.length];
        set({ repeatMode: next });
      },

      toggleRightSidebar: () => {
        set((state) => ({ isRightSidebarOpen: !state.isRightSidebarOpen }));
      },

      toggleLike: (track: Track) => {
        set((state) => {
          const exists = state.likedSongs.some((t) => t.id === track.id);
          const updated = exists
            ? state.likedSongs.filter((t) => t.id !== track.id)
            : [{ ...track, isLiked: true }, ...state.likedSongs];
          return { likedSongs: updated };
        });
      },

      removeFromQueue: (index: number) => {
        set((state) => {
          const newQueue = state.queue.filter((_, i) => i !== index);
          let newIndex = state.queueIndex;
          if (index < state.queueIndex) {
            newIndex -= 1;
          }
          return { queue: newQueue, queueIndex: newIndex };
        });
      },

      jumpToQueueIndex: (index: number) => {
        const state = get();
        if (state.queue[index]) {
          set({
            currentTrack: state.queue[index],
            queueIndex: index,
            isPlaying: true,
            currentTime: 0,
          });
        }
      },

      appendToQueue: (tracks: Track[]) => {
        set((state) => ({ queue: [...state.queue, ...tracks] }));
      },

      setCurrentTime: (time: number) => set({ currentTime: time }),
      setDuration: (dur: number) => set({ duration: dur }),
      setIsPlaying: (playing: boolean) => set({ isPlaying: playing }),
      setIsBuffering: (buffering: boolean) => set({ isBuffering: buffering }),
    }),
    {
      name: "openfy-player-storage",
      partialize: (state) => ({
        likedSongs: state.likedSongs,
        volume: state.volume,
        isShuffle: state.isShuffle,
        repeatMode: state.repeatMode,
      }),
    }
  )
);
