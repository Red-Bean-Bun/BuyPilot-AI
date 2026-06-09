import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const constraints = [
  { label: "ingredient_avoid", value: "酒精, 香精", delay: 0 },
  { label: "budget_max", value: "¥200", delay: 14 },
  { label: "category", value: "美妆护肤", delay: 22 },
];

export const DemoNegation: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("d3Intro");
  return (
    <DemoLayout duration={dur} chapterNum="03" eyebrow="反选排除"
      title={"否定约束写入数据库，\nLLM 上下文压缩后依然生效。"}
      phonePrompt="不要酒精也不要香精，200以内">
      <div style={{ display: "grid", gap: 12 }}>
        {constraints.map(({ label, value, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div key={label} style={{
              opacity: p, transform: `scaleX(${interpolate(p, [0, 1], [0.94, 1], clamp)})`, transformOrigin: "left",
              display: "flex", alignItems: "center", gap: 14, height: 58, borderRadius: 16,
              border: "1px solid rgba(255,106,61,0.22)", background: "rgba(255,106,61,0.06)", padding: "0 20px",
            }}>
              <span style={{ color: "rgba(255,255,255,0.35)", fontSize: 14, fontWeight: 650, minWidth: 140 }}>{label}</span>
              <strong style={{ color: "#FF9070", fontSize: 18, fontWeight: 720 }}>{value}</strong>
            </div>
          );
        })}
        <div style={{ marginTop: 8, color: "rgba(255,255,255,0.28)", fontSize: 15 }}>
          约束持久化至 feedbacks 表，SQL 硬过滤层直接拦截
        </div>
      </div>
    </DemoLayout>
  );
};
