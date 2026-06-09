import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { DURATION_FRAMES } from "../timing";
import { theme } from "../theme";
import { clamp } from "./motion";

export const Backdrop: React.FC = () => {
  const frame = useCurrentFrame();
  const drift = interpolate(frame, [0, DURATION_FRAMES], [-80, 80], clamp);

  return (
    <AbsoluteFill className="backdrop">
      <div
        className="backdropGlow backdropGlowCoral"
        style={{ transform: `translateX(${drift}px)` }}
      />
      <div
        className="backdropGlow backdropGlowTeal"
        style={{ transform: `translateX(${-drift * 0.8}px)` }}
      />
      <div className="backdropWhite" />
      <div className="backdropGrid" />
      <div
        className="backdropTint"
        style={{
          background: `linear-gradient(135deg, ${theme.colors.bg} 0%, ${theme.colors.bgWarm} 52%, ${theme.colors.bgMuted} 100%)`,
        }}
      />
    </AbsoluteFill>
  );
};
