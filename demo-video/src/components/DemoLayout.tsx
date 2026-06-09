import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { Easing } from "remotion";
import { PhonePlaceholder } from "./PhonePlaceholder";

const clamp = { extrapolateLeft: "clamp" as const, extrapolateRight: "clamp" as const };
const spring = Easing.bezier(0.16, 1, 0.3, 1);
const prog = (f: number, a: number, b: number) =>
  interpolate(f, [a, b], [0, 1], { ...clamp, easing: spring });

// Phone travel: left phone flies right to center (+538), right phone flies left to center (-538)
const PHONE_TRAVEL_X = 538;
const EXIT_SCALE_TARGET = 0.91;

export const DemoLayout: React.FC<{
  duration: number;
  chapterNum: string;
  eyebrow: string;
  title: string;
  phonePrompt?: string;
  phoneRight?: boolean;
  children?: React.ReactNode;
}> = ({ duration, chapterNum, eyebrow, title, phonePrompt, phoneRight = false, children }) => {
  const frame = useCurrentFrame();

  const FADE_IN = 18;
  const EXIT_START = duration - 25;

  const fadeIn = prog(frame, 0, FADE_IN);
  // copy fades out earlier, phone continues flying
  const copyFadeOut = interpolate(frame, [EXIT_START, EXIT_START + 16], [1, 0], {
    ...clamp, easing: Easing.in(Easing.cubic),
  });
  // phone flies to center
  const flyP = interpolate(frame, [EXIT_START, duration], [0, 1], {
    ...clamp, easing: Easing.bezier(0.4, 0, 0.2, 1),
  });

  const phoneP = prog(frame, 0, 24);
  const eyebrowP = prog(frame, 8, 28);
  const titleP = prog(frame, 16, 40);
  const bodyP = prog(frame, 28, 52);

  const phoneX = interpolate(flyP, [0, 1], [0, phoneRight ? -PHONE_TRAVEL_X : PHONE_TRAVEL_X], clamp);
  const phoneScale = interpolate(phoneP, [0, 1], [0.92, 1], clamp)
                   * interpolate(flyP, [0, 1], [1, EXIT_SCALE_TARGET], clamp);

  const phoneCol = (
    <div style={{
      position: "relative", display: "flex",
      alignItems: "center", justifyContent: "center",
      padding: phoneRight ? "60px 100px 60px 40px" : "60px 40px 60px 100px",
      background: `radial-gradient(ellipse 80% 65% at ${phoneRight ? "72%" : "28%"} 50%, rgba(255,106,61,0.07) 0%, transparent 65%)`,
      opacity: fadeIn,
    }}>
      <div style={{
        position: "absolute", top: 60, [phoneRight ? "right" : "left"]: 100,
        color: "rgba(255,255,255,0.06)", fontSize: 180, fontWeight: 800,
        lineHeight: 1, letterSpacing: "-0.04em", userSelect: "none", pointerEvents: "none",
      }}>{chapterNum}</div>
      <div style={{
        opacity: phoneP,
        transform: `translate(${phoneX}px, 0px) scale(${phoneScale})`,
        transformOrigin: "center center", zIndex: 10, position: "relative",
      }}>
        <PhonePlaceholder label={eyebrow} sublabel="BuyPilot-AI" prompt={phonePrompt} />
      </div>
    </div>
  );

  const copyCol = (
    <div style={{
      display: "flex", flexDirection: "column" as const, justifyContent: "center",
      padding: phoneRight ? "80px 64px 80px 120px" : "80px 120px 80px 64px",
      position: "relative", opacity: Math.min(fadeIn, copyFadeOut),
    }}>
      <div style={{
        position: "absolute", [phoneRight ? "right" : "left"]: 0,
        top: "14%", bottom: "14%", width: 1,
        background: "linear-gradient(180deg, transparent, rgba(255,255,255,0.09) 30%, rgba(255,255,255,0.09) 70%, transparent)",
      }} />
      <div style={{
        opacity: eyebrowP,
        transform: `translateX(${interpolate(eyebrowP, [0, 1], [phoneRight ? 12 : -12, 0], clamp)}px)`,
        display: "inline-flex", alignItems: "center", gap: 8,
        height: 34, padding: "0 16px", borderRadius: 999, marginBottom: 28,
        border: "1px solid rgba(255,106,61,0.32)", background: "rgba(255,106,61,0.10)",
        color: "#FF9070", fontSize: 12, fontWeight: 700,
        letterSpacing: "0.08em", textTransform: "uppercase" as const, alignSelf: "flex-start",
      }}>
        <span style={{ width: 5, height: 5, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0 }} />
        {eyebrow}
      </div>
      <div style={{
        opacity: titleP,
        transform: `translateY(${interpolate(titleP, [0, 1], [18, 0], clamp)}px) scale(${interpolate(titleP, [0, 1], [0.97, 1], clamp)})`,
        color: "#fff", fontSize: 80, fontWeight: 740,
        lineHeight: 1.05, letterSpacing: "-0.025em", marginBottom: 44,
      }}>{title}</div>
      <div style={{ opacity: bodyP, transform: `translateY(${interpolate(bodyP, [0, 1], [14, 0], clamp)}px)` }}>
        {children}
      </div>
    </div>
  );

  return (
    <div style={{
      position: "absolute", inset: 0,
      display: "grid",
      gridTemplateColumns: phoneRight ? "56% 44%" : "44% 56%",
      fontFamily: "Inter, sans-serif",
    }}>
      {phoneRight ? <>{copyCol}{phoneCol}</> : <>{phoneCol}{copyCol}</>}
    </div>
  );
};
