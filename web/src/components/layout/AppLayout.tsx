"use client";

import { ReactNode } from "react";
import LeftSidebar from "@/components/sidebar/LeftSidebar";
import NowPlayingSidebar from "@/components/sidebar/NowPlayingSidebar";
import BottomPlayer from "@/components/player/BottomPlayer";
import YouTubePlayer from "@/components/player/YouTubePlayer";

export default function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="h-screen w-screen overflow-hidden flex flex-col bg-black text-white">
      {/* Headless Background Audio Engine */}
      <YouTubePlayer />

      {/* Main 3-Column View Area */}
      <div className="flex-1 flex overflow-hidden">
        {/* 1. Left Navigation Sidebar */}
        <LeftSidebar />

        {/* 2. Main Content View Area */}
        <main className="flex-1 bg-[#121212] m-2 ml-0 rounded-lg overflow-y-auto flex flex-col pb-24 relative border border-white/5">
          {children}
        </main>

        {/* 3. Right 'Now Playing' & Queue Sidebar */}
        <NowPlayingSidebar />
      </div>

      {/* 4. Bottom Playback Controls Bar */}
      <BottomPlayer />
    </div>
  );
}
