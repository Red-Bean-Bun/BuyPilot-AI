import React from "react";
import { AbsoluteFill, Easing, interpolate } from "remotion";
import { FPS, SceneKey, scenes } from "../timing";

const clamp = { extrapolateLeft: "clamp" as const, extrapolateRight: "clamp" as const };

// Cuts where the NEXT scene is a Screen (intro→screen): use hard black
const screenScenes = new Set<SceneKey>(
  (Object.keys(scenes) as SceneKey[]).filter((k) => k.endsWith("Screen"))
);

const cutPoints = (Object.entries(scenes) as [SceneKey, readonly [number, number]][])
  .map(([key, [start]]) => ({ key, start }))
  .filter(({ start }) => start > 0);

export const SceneTransitions: React.FC<{ frame: number }> = ({ frame }) => {
  return (
    <AbsoluteFill style={{ zIndex: 12, pointerEvents: "none", overflow: "hidden" }}>
      {cutPoints.map(({ key, start }) => {
        const cut = start * FPS;
        const local = frame - cut; // negative = before cut, positive = after

        const isScreenCut = screenScenes.has(key);

        if (isScreenCut) {
          // No black flash — phone flies from intro to screen uninterrupted
          return null;
        } else {
          // Cross-dissolve with 6f black hold at center
          if (local < -16 || local > 16) return null;
          const peak = interpolate(
            Math.abs(local),
            [0, 6, 16],
            [1, 1, 0],
            { ...clamp, easing: Easing.out(Easing.quad) }
          );
          return (
            <div
              key={key}
              style={{ position: "absolute", inset: 0, background: "#000", opacity: peak * 0.88 }}
            />
          );
        }
      })}
    </AbsoluteFill>
  );
};
