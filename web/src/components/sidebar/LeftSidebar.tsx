"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Search, Heart, Music2, Library } from "lucide-react";
import { usePlayerStore } from "@/store/usePlayerStore";

export default function LeftSidebar() {
  const pathname = usePathname();
  const likedSongs = usePlayerStore((s) => s.likedSongs);

  const navItems = [
    { label: "Home", href: "/", icon: Home },
    { label: "Search", href: "/search", icon: Search },
    { label: "Liked Songs", href: "/liked", icon: Heart, badge: likedSongs.length },
  ];

  const curatedPlaylists = [
    { name: "Today's Top Hits", id: "tth" },
    { name: "Global Top 50", id: "gt50" },
    { name: "RapCaviar", id: "rap" },
    { name: "All Out 2020s", id: "2020s" },
    { name: "Chill Vibes", id: "chill" },
    { name: "Rock Classics", id: "rock" },
  ];

  return (
    <aside className="w-60 bg-black flex flex-col h-full flex-shrink-0 p-2 gap-2 text-neutral-400 select-none">
      {/* Brand & Main Nav Box */}
      <div className="bg-[#121212] rounded-lg p-4 flex flex-col gap-4">
        <Link href="/" className="flex items-center gap-2 text-white font-black text-xl px-2">
          <div className="w-8 h-8 rounded-full bg-spotify-green flex items-center justify-center text-black">
            <Music2 className="w-5 h-5 fill-black" />
          </div>
          <span className="tracking-tight">Openfy</span>
          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-white/10 text-spotify-green ml-auto">
            WEB
          </span>
        </Link>

        <nav className="flex flex-col gap-1 mt-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-4 px-3 py-2.5 rounded-md font-bold text-sm transition-colors ${
                  isActive
                    ? "text-white bg-white/10"
                    : "hover:text-white hover:bg-white/5"
                }`}
              >
                <Icon
                  className={`w-5 h-5 ${
                    item.href === "/liked" && isActive ? "fill-spotify-green text-spotify-green" : ""
                  }`}
                />
                <span>{item.label}</span>
                {item.badge !== undefined && item.badge > 0 && (
                  <span className="ml-auto text-xs px-1.5 py-0.2 rounded-full bg-spotify-green/20 text-spotify-green font-bold">
                    {item.badge}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Library & Playlists Box */}
      <div className="bg-[#121212] rounded-lg p-4 flex-1 flex flex-col overflow-hidden">
        <div className="flex items-center gap-2 px-2 text-neutral-400 hover:text-white transition-colors cursor-pointer mb-3">
          <Library className="w-5 h-5" />
          <span className="font-bold text-sm">Your Library</span>
        </div>

        {/* Scrollable Playlists List */}
        <div className="overflow-y-auto flex-1 flex flex-col gap-2 pr-1 mt-2">
          {curatedPlaylists.map((pl) => (
            <Link
              key={pl.id}
              href={`/search?q=${encodeURIComponent(pl.name)}`}
              className="px-2 py-1.5 rounded text-sm text-neutral-400 hover:text-white hover:bg-white/5 transition-colors truncate"
            >
              {pl.name}
            </Link>
          ))}
        </div>
      </div>
    </aside>
  );
}
