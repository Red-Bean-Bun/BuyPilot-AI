import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const rows = [
  { label: "检索相关性", weight: "× 0.35", delay: 24 },
  { label: "标准匹配度", weight: "× 0.25", delay: 34 },
  { label: "用户行为信号", weight: "× 0.25", delay: 44 },
  { label: "证据质量", weight: "× 0.10", delay: 54 },
  { label: "风险扣分", weight: "× 0.05", minus: true, delay: 64 },
];

export const TechDecision: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("techDecision");
  const totalP = progress(frame, 78, 96);

  return (
    <div className="techSlide" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 16, ...entryTransform(frame, 0, 10) }}>
        Tech 3 · 决策评分
      </div>
      <div className="sceneTitle center minimal">
        <h1 style={{ color: "#fff" }}>5 维加权评分，LLM 不参与挑选。</h1>
      </div>
      <div className="scoreFormula">
        {rows.map(({ label, weight, minus, delay }) => {
          const p = progress(frame, delay, delay + 18);
          return (
            <div
              key={label}
              className={`scoreRow${minus ? " minus" : ""}`}
              style={{ opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-14, 0], clamp)}px)` }}
            >
              <span>{label}</span>
              <b>{weight}</b>
            </div>
          );
        })}
        <div
          className="scoreRow total"
          style={{ opacity: totalP, transform: `translateY(${interpolate(totalP, [0, 1], [8, 0], clamp)}px)` }}
        >
          <span>final_score</span>
          <b style={{ color: "#FF6A3D" }}>→ high / medium / low</b>
        </div>
      </div>
    </div>
  );
};
