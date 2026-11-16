import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const query = searchParams.get("q")?.trim();

  if (!query) {
    return NextResponse.json({ error: "Missing query parameter 'q'" }, { status: 400 });
  }

  const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || "http://140.245.226.77";

  // 1. Query the secure Oracle Cloud backend server holding the YouTube API key
  try {
    const remoteUrl = `${backendUrl}/api/search?q=${encodeURIComponent(query)}`;
    const remoteRes = await fetch(remoteUrl, { next: { revalidate: 300 } });
    if (remoteRes.ok) {
      const data = await remoteRes.json();
      return NextResponse.json(data);
    }
  } catch (err) {
    console.warn("Oracle Cloud backend unreachable, falling back to local scraper:", err);
  }

  // 2. Direct local fallback if backend is down
  try {
    const searchUrl = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false";
    const payload = {
      context: {
        client: {
          clientName: "WEB_REMIX",
          clientVersion: "1.20241110.01.00",
          hl: "en",
          gl: "US",
        },
      },
      query,
      params: "EgWKAQIIAWoQEAMQBBAJEAoQBRAREBAQFQ%3D%3D",
    };

    const res = await fetch(searchUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        Origin: "https://music.youtube.com",
      },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      const data = await res.json();
      const tracks: any[] = [];
      const tabs = data.contents?.tabbedSearchResultsRenderer?.tabs || [];
      const sections = tabs[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

      for (const section of sections) {
        const shelf = section.musicShelfRenderer;
        if (!shelf) continue;

        for (const item of shelf.contents || []) {
          const mfr = item.musicResponsiveListItemRenderer;
          if (!mfr) continue;

          const cols = mfr.flexColumns || [];
          if (cols.length < 2) continue;

          const title = cols[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Unknown";
          const artist = cols[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Unknown Artist";
          const vid = mfr.playlistItemData?.videoId || mfr.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;

          if (vid) {
            const thumbs = mfr.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
            const cover = thumbs[thumbs.length - 1]?.url || "";
            tracks.push({
              id: vid,
              title,
              artist,
              coverUrl: cover,
              durationSeconds: 210,
            });
            if (tracks.length >= 25) break;
          }
        }
      }

      if (tracks.length > 0) {
        return NextResponse.json({
          source: "local_innertube_fallback",
          query,
          tracks,
        });
      }
    }
  } catch (fallbackErr) {
    console.error("Local fallback error:", fallbackErr);
  }

  // 3. Fallback mock tracks so the interface never crashes
  return NextResponse.json({
    source: "mock_fallback",
    query,
    tracks: [
      {
        id: "J7p4bzqLvCw",
        title: `${query} (Official)`,
        artist: "Featured Artist",
        coverUrl: "https://i.ytimg.com/vi/J7p4bzqLvCw/hqdefault.jpg",
        durationSeconds: 202,
      },
      {
        id: "4NRXx6U8ABQ",
        title: `${query} (Radio Edit)`,
        artist: "Various Artists",
        coverUrl: "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
        durationSeconds: 215,
      },
    ],
  });
}
