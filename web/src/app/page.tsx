"use client";

import { usePlayerStore } from "@/store/usePlayerStore";
import { Track } from "@/types";
import { Play } from "lucide-react";
import TrackRow from "@/components/track/TrackRow";

const FEATURED_HITS: Track[] = [
  {
    id: "J7p4bzqLvCw",
    title: "Blinding Lights",
    artist: "The Weeknd",
    album: "After Hours",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
    durationSeconds: 202,
  },
  {
    id: "4NRXx6U8ABQ",
    title: "Save Your Tears",
    artist: "The Weeknd",
    album: "After Hours",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
    durationSeconds: 215,
  },
  {
    id: "hT_nvWreIhg",
    title: "Counting Stars",
    artist: "OneRepublic",
    album: "Native",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b273b749622cf3977e2d97adba4a",
    durationSeconds: 257,
  },
  {
    id: "JGwWNGJdvx8",
    title: "Shape of You",
    artist: "Ed Sheeran",
    album: "÷ (Divide)",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96",
    durationSeconds: 233,
  },
  {
    id: "0VwhorQBdMc",
    title: "Sunflower",
    artist: "Post Malone, Swae Lee",
    album: "Spider-Man: Into the Spider-Verse",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b273e2e352d89826aef6dbd5ff8f",
    durationSeconds: 158,
  },
  {
    id: "nfWlot6h_JM",
    title: "Shake It Off",
    artist: "Taylor Swift",
    album: "1989",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b273904445d70d04eb34d6de7970",
    durationSeconds: 219,
  },
  {
    id: "YQHsXMglC9A",
    title: "Hello",
    artist: "Adele",
    album: "25",
    coverUrl: "https://i.scdn.co/image/ab67616d0000b27329df04c45b59f77f3050c266",
    durationSeconds: 295,
  },
];

const SHELVES = [
  {
    title: "Today's Top Hits",
    description: "The hottest global releases on Spotify & YouTube Music.",
    tracks: FEATURED_HITS.slice(0, 5),
  },
  {
    title: "Quick Play / Charts",
    description: "Popular tracks trending right now worldwide.",
    tracks: FEATURED_HITS.slice(2, 7),
  },
];

export default function HomePage() {
  const { playTrack } = usePlayerStore();

  return (
    <div className="flex flex-col w-full">
      {/* Hero Banner Header */}
      <div className="bg-gradient-to-b from-indigo-900/70 via-indigo-950/40 to-[#121212] px-8 pt-10 pb-6">
        <h1 className="text-3xl font-black tracking-tight text-white mb-2">Good evening</h1>
        <p className="text-neutral-400 text-sm">
          Welcome to Openfy Web. Streaming full-length music ad-free via YouTube.
        </p>

        {/* Quick Picks Cards Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 mt-6">
          {FEATURED_HITS.slice(0, 6).map((track) => (
            <div
              key={track.id}
              onClick={() => playTrack(track, FEATURED_HITS)}
              className="group flex items-center bg-white/5 hover:bg-white/20 transition-all rounded-md overflow-hidden cursor-pointer pr-4 shadow-md"
            >
              <img
                src={track.coverUrl}
                alt={track.title}
                className="w-16 h-16 object-cover flex-shrink-0"
              />
              <span className="font-bold text-sm text-white px-4 truncate flex-1">
                {track.title}
              </span>
              <button
                className="w-10 h-10 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl opacity-0 group-hover:opacity-100 hover:scale-105 transition-all flex-shrink-0"
                title="Play"
              >
                <Play className="w-5 h-5 fill-black ml-0.5" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Main Shelves Sections */}
      <div className="px-8 py-6 flex flex-col gap-10">
        {/* Today's Top Hits Table */}
        <div className="flex flex-col gap-4">
          <div className="flex items-end justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white tracking-tight">Today's Top Hits</h2>
              <p className="text-xs text-neutral-400 mt-1">
                Listen to the top tracks trending globally.
              </p>
            </div>
            <button
              onClick={() => playTrack(FEATURED_HITS[0], FEATURED_HITS)}
              className="flex items-center gap-2 px-4 py-2 bg-spotify-green text-black font-bold text-xs rounded-full hover:scale-105 transition-transform"
            >
              <Play className="w-3.5 h-3.5 fill-black" />
              Play All
            </button>
          </div>

          <div className="flex flex-col divide-y divide-white/5 bg-white/[0.02] rounded-xl p-2 border border-white/5">
            {FEATURED_HITS.map((track, i) => (
              <TrackRow key={track.id} track={track} index={i} queueContext={FEATURED_HITS} />
            ))}
          </div>
        </div>

        {/* Shelf Cards */}
        {SHELVES.map((shelf, shelfIdx) => (
          <div key={shelfIdx} className="flex flex-col gap-4">
            <div>
              <h3 className="text-xl font-bold text-white tracking-tight">{shelf.title}</h3>
              <p className="text-xs text-neutral-400 mt-1">{shelf.description}</p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              {shelf.tracks.map((track) => (
                <div
                  key={track.id}
                  onClick={() => playTrack(track, shelf.tracks)}
                  className="group bg-[#181818] hover:bg-[#282828] p-3.5 rounded-lg transition-all cursor-pointer flex flex-col relative"
                >
                  <div className="relative aspect-square w-full rounded-md overflow-hidden bg-neutral-900 mb-3 shadow-lg">
                    <img
                      src={track.coverUrl}
                      alt={track.title}
                      className="w-full h-full object-cover"
                    />
                    <button className="absolute bottom-2 right-2 w-10 h-10 rounded-full bg-spotify-green text-black flex items-center justify-center shadow-xl opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 hover:scale-105 transition-all">
                      <Play className="w-5 h-5 fill-black ml-0.5" />
                    </button>
                  </div>
                  <p className="font-bold text-sm text-white truncate">{track.title}</p>
                  <p className="text-xs text-neutral-400 truncate mt-1">{track.artist}</p>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
