import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

export const ClosingNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("closing");
  const titleP = progress(frame, 8, 32);
  const subP = progress(frame, 24, 46);
  const repoP = progress(frame, 38, 58);

  return (
    <div className="closingCenterScene" style={{ opacity: sceneOpacity(frame, dur, 20) }}>
      <div
        className="closingTitle"
        style={{
          opacity: titleP,
          transform: `translateY(${interpolate(titleP, [0, 1], [20, 0], clamp)}px) scale(${interpolate(titleP, [0, 1], [0.96, 1], clamp)})`,
        }}
      >
        Buy<span>Pilot</span>-AI
      </div>
      <div
        className="closingSub"
        style={{
          opacity: subP,
          transform: `translateY(${interpolate(subP, [0, 1], [12, 0], clamp)}px)`,
        }}
      >
        基于 RAG 的多模态电商智能导购 Agent
      </div>
      <div
        style={{
          opacity: subP,
          transform: `translateY(${interpolate(subP, [0, 1], [12, 0], clamp)}px)`,
          marginTop: 12,
          color: "rgba(255,255,255,0.30)",
          fontSize: 20,
          fontWeight: 520,
        }}
      >
        豆沙包 · 刘轩 / 刘梓杰 / 张博榕
      </div>
      <div
        className="closingRepo"
        style={{
          opacity: repoP,
          transform: `translateY(${interpolate(repoP, [0, 1], [10, 0], clamp)}px)`,
        }}
      >
        github.com/Red-Bean-Bun/BuyPilot-AI
      </div>
    </div>
  );
};
