/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        spotify: {
          green: "#1DB954",
          "green-hover": "#1ed760",
          "green-dark": "#169c46",
          black: "#121212",
          dark: "#181818",
          darker: "#000000",
          card: "#181818",
          "card-hover": "#282828",
          subdued: "#a7a7a7",
          muted: "#6a6a6a",
          border: "#282828",
          player: "#181818"
        },
        rvx: {
          red: "#FF0000",
          purple: "#7c3aed"
        }
      },
      fontFamily: {
        sans: ['Circular Std', 'Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'Helvetica Neue', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
