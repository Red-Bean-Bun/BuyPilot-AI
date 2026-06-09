import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const checks = [
  { label: "预算内", value: "¥170", delay: 0 },
  { label: "不含酒精香精", value: "✓", delay: 10 },
  { label: "适合日常护肤", value: "✓", delay: 20 },
];

export const DemoCriteriaEvidence: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("d1Intro");
  return (
    <DemoLayout duration={dur} chapterNum="02" phoneRight eyebrow="条件推荐 + 证据绑定"
      title={"每条推荐理由，\n都有原文出处。"}
      phonePrompt="推荐防晒霜，不要酒精也不要香精，200以内">
      <div style={{ display: "grid", gap: 12 }}>
        {checks.map(({ label, value, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div key={label} style={{
              opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-14, 0], clamp)}px)`,
              display: "flex", alignItems: "center", gap: 14, height: 60, borderRadius: 16,
              border: "1px solid rgba(255,255,255,0.08)", background: "rgba(255,255,255,0.04)", padding: "0 20px",
            }}>
              <span style={{ width: 7, height: 7, borderRadius: "50%", background: "#34D399", flexShrink: 0 }} />
              <span style={{ color: "rgba(255,255,255,0.55)", fontSize: 19, fontWeight: 600, flex: 1 }}>{label}</span>
              <strong style={{ color: "#FF6A3D", fontSize: 20, fontWeight: 730 }}>{value}</strong>
            </div>
          );
        })}
        <div style={{ marginTop: 8, color: "rgba(255,255,255,0.28)", fontSize: 15 }}>
          推荐理由来自商品知识库原文，可一键追溯
        </div>
      </div>
    </DemoLayout>
  );
};
