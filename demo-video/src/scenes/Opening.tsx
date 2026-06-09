import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

export const Opening: React.FC = () => {
  const frame = useCurrentFrame();
  const duration = sceneDuration("opening");
  const title = progress(frame, 10, 42);
  const line = progress(frame, 58, 96);
  const scan = progress(frame, 34, 112);

  return (
    <div className="scene openingScene" style={{ opacity: sceneOpacity(frame, duration, 18) }}>
      <div className="openingOptics">
        <i
          style={{
            opacity: scan * 0.72,
            transform: `scaleX(${interpolate(scan, [0, 1], [0.08, 1], clamp)})`,
          }}
        />
        <b
          style={{
            opacity: scan * 0.38,
            transform: `scaleX(${interpolate(scan, [0, 1], [0.04, 1], clamp)})`,
          }}
        />
      </div>
      <div
        className="openingMark"
        style={{
          opacity: title,
          transform: `translateY(${interpolate(title, [0, 1], [18, 0], clamp)}px) scale(${interpolate(
            title,
            [0, 1],
            [0.96, 1],
            clamp,
          )})`,
        }}
      >
        <strong>BuyPilot-AI</strong>
      </div>
      <div
        className="openingStatement"
        style={{
          opacity: line,
          transform: `translateY(${interpolate(line, [0, 1], [18, 0], clamp)}px)`,
        }}
      >
        每一次选择，都有<span>依据</span>。
      </div>
      <div
        className="openingTeam"
        style={{
          opacity: line,
          transform: `translateY(${interpolate(line, [0, 1], [18, 0], clamp)}px)`,
        }}
      >
        豆沙包 · 刘轩 / 刘梓杰 / 张博榕
      </div>
    </div>
  );
};
