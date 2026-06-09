import React from "react";
import { AbsoluteFill, Easing, Video, interpolate, staticFile, useCurrentFrame } from "remotion";

const clamp = { extrapolateLeft: "clamp" as const, extrapolateRight: "clamp" as const };
const spring = Easing.bezier(0.16, 1, 0.3, 1);

// Canvas is 1920×1080. Phone is centered at (960, 540), 400×805px.
const CANVAS_W = 1920;
const CANVAS_H = 1080;
const PHONE_W = 400;
const PHONE_H = 805;
const PHONE_LEFT = (CANVAS_W - PHONE_W) / 2;   // 760
const PHONE_TOP  = (CANVAS_H - PHONE_H) / 2;   // 137.5
const CALLOUT_W = 300;
const GAP = 48; // gap between phone edge and callout

export type Callout = {
  side: "left" | "right";
  title: string;
  body?: string;
  // 0–1 vertical position along phone height
  y: number;
  // frame to start appearing
  at: number;
};

export const PhoneFullscreen: React.FC<{
  duration: number;
  recording?: string;
  startFrom?: number;
  caption?: string;
  callouts?: Callout[];
  summary?: { headline: string; metrics: { value: string; label: string }[] };
}> = ({ duration, recording, startFrom = 0, caption, callouts = [], summary }) => {
  const frame = useCurrentFrame();
  const ENTER = 8;
  const EXIT = 18;
  const SUMMARY_START = duration - 150;

  const enterP = interpolate(frame, [0, ENTER], [0, 1], { ...clamp, easing: spring });
  const exitP  = interpolate(frame, [duration - EXIT, duration], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });
  const sceneOpacity = Math.min(enterP, 1 - exitP);
  // Phone arrives via DemoLayout fly animation — no scale-in here, just fade
  const phoneScale = 1;

  const summaryP = summary
    ? interpolate(frame, [SUMMARY_START, SUMMARY_START + 30], [0, 1], { ...clamp, easing: spring })
    : 0;

  const calloutsOpacity = 1 - summaryP;

  return (
    <AbsoluteFill style={{ background: "#0a0a0b", opacity: sceneOpacity, fontFamily: "Inter, sans-serif" }}>

      {/* ambient glow */}
      <div style={{
        position: "absolute",
        left: CANVAS_W / 2 - 300, top: CANVAS_H / 2 - 300,
        width: 600, height: 600, borderRadius: "50%",
        background: "radial-gradient(circle, rgba(255,106,61,0.10) 0%, transparent 70%)",
        filter: "blur(60px)",
        transform: `scale(${phoneScale})`,
        pointerEvents: "none",
      }} />

      {/* ── Callouts ── */}
      {callouts.map((c, i) => {
        const next = callouts[i + 1];
        const exitAt = next ? next.at - 20 : SUMMARY_START - 10;

        const enterP = interpolate(frame, [c.at, c.at + 24], [0, 1], { ...clamp, easing: spring });
        const exitP  = interpolate(frame, [exitAt, exitAt + 18], [0, 1], { ...clamp, easing: Easing.in(Easing.cubic) });

        // line grows in, shrinks out
        const LINE_MAX = GAP + 40;
        const lineLen = interpolate(enterP, [0, 1], [0, LINE_MAX], clamp)
                      * interpolate(exitP, [0, 1], [1, 0], clamp);

        // text slides in from side, floats up on exit
        const textSlideIn  = interpolate(enterP, [0, 1], [c.side === "left" ? -20 : 20, 0], clamp);
        const textFloatOut = interpolate(exitP,  [0, 1], [0, -14], clamp);
        const textX = textSlideIn;
        const textY = textFloatOut;

        const opacity = Math.min(enterP, 1 - exitP) * calloutsOpacity;
        const dotY = PHONE_TOP + c.y * PHONE_H;

        const textStyle = {
          fontSize: 22, fontWeight: 680, lineHeight: 1.25,
          letterSpacing: "-0.01em", color: "#fff",
        } as const;
        const bodyStyle = {
          color: "rgba(255,255,255,0.40)", fontSize: 16,
          fontWeight: 500, marginTop: 5, lineHeight: 1.4,
        } as const;

        if (c.side === "left") {
          return (
            <div key={i} style={{ position: "absolute", opacity, top: dotY - 2, left: PHONE_LEFT - LINE_MAX - CALLOUT_W, display: "flex", alignItems: "center" }}>
              <div style={{ width: CALLOUT_W, textAlign: "right", paddingRight: 16, transform: `translate(${textX}px, ${textY}px)` }}>
                <div style={textStyle}>{c.title}</div>
                {c.body && <div style={bodyStyle}>{c.body}</div>}
              </div>
              <div style={{ width: lineLen, height: 1, background: "linear-gradient(90deg, rgba(255,255,255,0.15), rgba(255,255,255,0.60))", flexShrink: 0 }} />
              <div style={{ width: 6, height: 6, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0, boxShadow: "0 0 0 5px rgba(255,106,61,0.20)" }} />
            </div>
          );
        } else {
          return (
            <div key={i} style={{ position: "absolute", opacity, top: dotY - 2, left: PHONE_LEFT + PHONE_W + LINE_MAX - LINE_MAX, display: "flex", alignItems: "center" }}>
              <div style={{ width: 6, height: 6, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0, boxShadow: "0 0 0 5px rgba(255,106,61,0.20)" }} />
              <div style={{ width: lineLen, height: 1, background: "linear-gradient(90deg, rgba(255,255,255,0.60), rgba(255,255,255,0.15))", flexShrink: 0 }} />
              <div style={{ width: CALLOUT_W, paddingLeft: 16, transform: `translate(${textX}px, ${textY}px)` }}>
                <div style={textStyle}>{c.title}</div>
                {c.body && <div style={bodyStyle}>{c.body}</div>}
              </div>
            </div>
          );
        }
      })}

      {/* ── Phone (always on top of callouts) ── */}
      <div style={{
        position: "absolute",
        left: PHONE_LEFT, top: PHONE_TOP,
        width: PHONE_W, height: PHONE_H,
        padding: 14, borderRadius: 56, background: "#1c1c1e",
        boxShadow: "0 60px 160px rgba(0,0,0,0.70), 0 1px 0 rgba(255,255,255,0.12) inset",
        transform: `scale(${phoneScale})`,
        transformOrigin: "center center",
        // fade phone out when summary appears
        opacity: 1 - summaryP * 0.85,
      }}>
        <div style={{ width: "100%", height: "100%", overflow: "hidden", borderRadius: 44, background: "#111" }}>
          {/* eslint-disable @remotion/no-object-fit-on-media-video */}
          {recording ? (
            <Video src={staticFile(recording)} startFrom={Math.round(startFrom * 30)}
              style={{ width: "100%", height: "100%", objectFit: "cover" as const }} />
          ) : (
            <div style={{ width: "100%", height: "100%", background: "linear-gradient(180deg,#1a1a1d,#111114)", display: "flex", alignItems: "center", justifyContent: "center", color: "rgba(255,255,255,0.20)", fontSize: 20, fontWeight: 600 }}>录屏素材</div>
          )}
        </div>
      </div>

      {/* caption pill */}
      {caption && (
        <div style={{
          position: "absolute", bottom: 44, left: "50%", transform: "translateX(-50%)",
          opacity: interpolate(enterP, [0.4, 1], [0, 1], clamp) * (1 - summaryP),
          background: "rgba(255,255,255,0.08)", border: "1px solid rgba(255,255,255,0.10)",
          borderRadius: 999, padding: "12px 28px", color: "rgba(255,255,255,0.70)",
          fontSize: 20, fontWeight: 580, whiteSpace: "nowrap" as const, backdropFilter: "blur(16px)",
        }}>{caption}</div>
      )}

      {/* ── Summary: frosted glass card centered over the phone ── */}
      {summary && (
        <div style={{
          position: "absolute",
          left: PHONE_LEFT - 100,
          right: CANVAS_W - PHONE_LEFT - PHONE_W - 100,
          top: "50%",
          transform: `translateY(calc(-50% + ${interpolate(summaryP, [0, 1], [60, 0], { ...clamp, easing: spring })}px))`,
          opacity: summaryP,
          borderRadius: 32,
          background: "rgba(10, 10, 12, 0.80)",
          border: "1px solid rgba(255,255,255,0.16)",
          backdropFilter: "blur(48px)",
          WebkitBackdropFilter: "blur(48px)",
          boxShadow: "0 0 0 1px rgba(255,255,255,0.06) inset, 0 40px 120px rgba(0,0,0,0.60)",
          padding: "44px 48px",
          display: "flex",
          flexDirection: "column" as const,
          gap: 28,
          zIndex: 10,
        }}>
          <div style={{ color: "#fff", fontSize: 36, fontWeight: 720, lineHeight: 1.2, letterSpacing: "-0.015em" }}>
            {summary.headline}
          </div>
          <div style={{ display: "flex", gap: 48 }}>
            {summary.metrics.map(({ value, label }, i) => {
              const mp = interpolate(frame, [SUMMARY_START + 20 + i * 12, SUMMARY_START + 44 + i * 12], [0, 1], { ...clamp, easing: spring });
              return (
                <div key={label} style={{ opacity: mp, display: "flex", flexDirection: "column" as const, gap: 4 }}>
                  <span style={{ color: "#FF6A3D", fontSize: 72, fontWeight: 760, lineHeight: 1, letterSpacing: "-0.03em" }}>{value}</span>
                  <span style={{ color: "rgba(255,255,255,0.40)", fontSize: 20, fontWeight: 580 }}>{label}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </AbsoluteFill>
  );
};
