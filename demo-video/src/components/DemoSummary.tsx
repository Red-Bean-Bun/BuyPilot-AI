import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { Easing } from "remotion";

const clamp = { extrapolateLeft: "clamp" as const, extrapolateRight: "clamp" as const };
const spring = Easing.bezier(0.16, 1, 0.3, 1);

const prog = (frame: number, from: number, to: number, ease = spring) =>
  interpolate(frame, [from, to], [0, 1], { ...clamp, easing: ease });

export const DemoSummary: React.FC<{
  duration: number;
  eyebrow: string;
  headline: string;
  metrics: { value: string; label: string }[];
}> = ({ duration, eyebrow, headline, metrics }) => {
  const frame = useCurrentFrame();
  const FADE = 16;
  const fadeIn = prog(frame, 0, FADE);
  const fadeOut = 1 - prog(frame, duration - FADE, duration, Easing.in(Easing.cubic));
  const opacity = Math.min(fadeIn, fadeOut);

  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: "#0a0a0b",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        opacity,
        fontFamily: "Inter, sans-serif",
      }}
    >
      <div
        style={{
          opacity: prog(frame, 6, 22),
          transform: `translateY(${interpolate(prog(frame, 6, 22), [0, 1], [10, 0], clamp)}px)`,
          color: "rgba(255,255,255,0.35)",
          fontSize: 13,
          fontWeight: 650,
          letterSpacing: "0.10em",
          textTransform: "uppercase",
          marginBottom: 18,
        }}
      >
        {eyebrow}
      </div>
      <div
        style={{
          opacity: prog(frame, 12, 30),
          transform: `translateY(${interpolate(prog(frame, 12, 30), [0, 1], [14, 0], clamp)}px) scale(${interpolate(prog(frame, 12, 30), [0, 1], [0.97, 1], clamp)})`,
          color: "#fff",
          fontSize: 52,
          fontWeight: 720,
          lineHeight: 1.08,
          letterSpacing: "-0.01em",
          textAlign: "center",
          maxWidth: 900,
          marginBottom: 48,
        }}
      >
        {headline}
      </div>
      <div style={{ display: "flex", gap: 48, alignItems: "flex-end" }}>
        {metrics.map(({ value, label }, i) => {
          const p = prog(frame, 22 + i * 10, 40 + i * 10);
          return (
            <div
              key={label}
              style={{
                opacity: p,
                transform: `translateY(${interpolate(p, [0, 1], [16, 0], clamp)}px)`,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 8,
              }}
            >
              <span style={{ color: "#FF6A3D", fontSize: 56, fontWeight: 760, lineHeight: 1, letterSpacing: "-0.02em" }}>
                {value}
              </span>
              <span style={{ color: "rgba(255,255,255,0.40)", fontSize: 17, fontWeight: 580 }}>{label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
