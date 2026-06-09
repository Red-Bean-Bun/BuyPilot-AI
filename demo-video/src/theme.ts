export const theme = {
  colors: {
    bg: "#0A0A0B",
    bgWarm: "#111114",
    bgMuted: "#16161A",
    card: "rgba(255, 255, 255, 0.06)",
    cardSolid: "#1C1C1E",
    coral: "#FF6A3D",
    coralSoft: "rgba(255, 106, 61, 0.14)",
    coralLine: "rgba(255, 106, 61, 0.50)",
    text: "#FFFFFF",
    muted: "rgba(255, 255, 255, 0.50)",
    faint: "rgba(255, 255, 255, 0.30)",
    border: "rgba(255, 255, 255, 0.10)",
    tealSoft: "rgba(126, 211, 194, 0.18)",
    green: "#34D399",
    blue: "#60A5FA",
  },
  font: {
    sans: "Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
  shadow: {
    card: "0 28px 90px rgba(31, 35, 41, 0.075)",
    phone: "0 48px 130px rgba(31, 35, 41, 0.18)",
  },
};

export type Theme = typeof theme;
