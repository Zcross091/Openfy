import type { Metadata } from "next";
import "./globals.css";
import AppLayout from "@/components/layout/AppLayout";

export const metadata: Metadata = {
  title: "Openfy - Open Source Spotify Web Client",
  description: "Spotify web experience streaming full music tracks from YouTube.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className="antialiased bg-black text-white">
        <AppLayout>{children}</AppLayout>
      </body>
    </html>
  );
}
