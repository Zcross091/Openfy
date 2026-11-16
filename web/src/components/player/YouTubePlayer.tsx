"use client";

import { useEffect, useRef } from "react";
import { usePlayerStore } from "@/store/usePlayerStore";

declare global {
  interface Window {
    YT: any;
    onYouTubeIframeAPIReady: () => void;
  }
}

export default function YouTubePlayer() {
  const playerRef = useRef<any>(null);
  const isReadyRef = useRef(false);
  const currentVideoIdRef = useRef<string | null>(null);

  const {
    currentTrack,
    isPlaying,
    volume,
    isMuted,
    currentTime,
    repeatMode,
    skipNext,
    setCurrentTime,
    setDuration,
    setIsPlaying,
    setIsBuffering,
  } = usePlayerStore();

  // 1. Load YouTube IFrame API script once
  useEffect(() => {
    if (typeof window === "undefined") return;

    if (!window.YT) {
      const tag = document.createElement("script");
      tag.src = "https://www.youtube.com/iframe_api";
      const firstScriptTag = document.getElementsByTagName("script")[0];
      firstScriptTag?.parentNode?.insertBefore(tag, firstScriptTag);

      window.onYouTubeIframeAPIReady = () => {
        initPlayer();
      };
    } else if (window.YT && window.YT.Player) {
      initPlayer();
    }

    function initPlayer() {
      if (playerRef.current) return;

      playerRef.current = new window.YT.Player("openfy-headless-yt", {
        height: "200",
        width: "200",
        playerVars: {
          autoplay: 1,
          controls: 0,
          disablekb: 1,
          fs: 0,
          rel: 0,
          modestbranding: 1,
          playsinline: 1,
          origin: window.location.origin,
        },
        events: {
          onReady: () => {
            isReadyRef.current = true;
            playerRef.current.setVolume(isMuted ? 0 : volume);
            if (currentTrack?.id) {
              playerRef.current.loadVideoById(currentTrack.id);
              currentVideoIdRef.current = currentTrack.id;
            }
          },
          onStateChange: (event: any) => {
            // YT.PlayerState: -1 (UNSTARTED), 0 (ENDED), 1 (PLAYING), 2 (PAUSED), 3 (BUFFERING), 5 (CUED)
            if (event.data === 1) {
              setIsPlaying(true);
              setIsBuffering(false);
              const dur = playerRef.current?.getDuration?.() || 0;
              if (dur > 0) setDuration(dur);
            } else if (event.data === 2) {
              setIsPlaying(false);
              setIsBuffering(false);
            } else if (event.data === 3) {
              setIsBuffering(true);
            } else if (event.data === 0) {
              // ENDED
              const currentRepeat = usePlayerStore.getState().repeatMode;
              if (currentRepeat === "one") {
                playerRef.current?.seekTo?.(0, true);
                playerRef.current?.playVideo?.();
              } else {
                skipNext();
              }
            }
          },
          onError: (e: any) => {
            console.warn("YouTube Player error:", e.data);
            setIsBuffering(false);
            // On error, try advancing to next track
            skipNext();
          },
        },
      });
    }

    return () => {
      // Keep player alive between route changes
    };
  }, []);

  // 2. Respond to track changes
  useEffect(() => {
    if (!isReadyRef.current || !playerRef.current || !currentTrack) return;

    if (currentVideoIdRef.current !== currentTrack.id) {
      currentVideoIdRef.current = currentTrack.id;
      playerRef.current.loadVideoById({
        videoId: currentTrack.id,
        startSeconds: 0,
      });
    }
  }, [currentTrack?.id]);

  // 3. Respond to Play / Pause toggles
  useEffect(() => {
    if (!isReadyRef.current || !playerRef.current) return;

    try {
      const state = playerRef.current.getPlayerState?.();
      if (isPlaying && state !== 1 && state !== 3) {
        playerRef.current.playVideo();
      } else if (!isPlaying && state === 1) {
        playerRef.current.pauseVideo();
      }
    } catch (e) {
      // Ignore transient state errors
    }
  }, [isPlaying]);

  // 4. Respond to volume changes
  useEffect(() => {
    if (!isReadyRef.current || !playerRef.current) return;

    try {
      playerRef.current.setVolume(isMuted ? 0 : volume);
    } catch (e) {}
  }, [volume, isMuted]);

  // 5. Respond to manual scrubber seeks
  const lastTimeRef = useRef(currentTime);
  useEffect(() => {
    if (!isReadyRef.current || !playerRef.current) return;

    // Only seek in player if time changed significantly from user action (not ticker)
    const currentYtTime = playerRef.current.getCurrentTime?.() || 0;
    if (Math.abs(currentYtTime - currentTime) > 2) {
      playerRef.current.seekTo(currentTime, true);
    }
    lastTimeRef.current = currentTime;
  }, [currentTime]);

  // 6. High frequency progress ticker (updates store every 250ms while playing)
  useEffect(() => {
    if (!isPlaying) return;

    const interval = setInterval(() => {
      if (isReadyRef.current && playerRef.current) {
        try {
          const time = playerRef.current.getCurrentTime?.() || 0;
          const dur = playerRef.current.getDuration?.() || 0;
          setCurrentTime(time);
          if (dur > 0) setDuration(dur);
        } catch (e) {}
      }
    }, 250);

    return () => clearInterval(interval);
  }, [isPlaying]);

  return (
    <div
      aria-hidden="true"
      style={{
        position: "fixed",
        top: -9999,
        left: -9999,
        width: 1,
        height: 1,
        opacity: 0,
        pointerEvents: "none",
        zIndex: -1,
      }}
    >
      <div id="openfy-headless-yt" />
    </div>
  );
}
