import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const axes = ["核心参数", "性能", "续航", "价格"];

export const DemoCompareNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("dxIntro");
  const bigP = progress(frame, 0, 22);
  return (
    <DemoLayout duration={dur} chapterNum="06" eyebrow="多商品对比"
      title={"结构化论证，\n而非简单罗列。"}
      phonePrompt="货比三家">
      <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 22 }}>
        <span style={{ opacity: bigP, transform: `scale(${interpolate(bigP, [0, 1], [0.7, 1], clamp)})`, display: "inline-block", color: "#FF6A3D", fontSize: 88, fontWeight: 760, lineHeight: 1, letterSpacing: "-0.02em" }}>4</span>
        <span style={{ color: "rgba(255,255,255,0.45)", fontSize: 20, fontWeight: 520, opacity: progress(frame, 10, 28) }}>个维度，品类感知自动选取</span>
      </div>
      <div style={{ display: "flex", flexWrap: "wrap" as const, gap: 10, marginBottom: 16 }}>
        {axes.map((axis, i) => {
          const p = progress(frame, 14 + i * 7, 32 + i * 7);
          return <div key={axis} style={{ opacity: p, transform: `translateY(${interpolate(p, [0, 1], [8, 0], clamp)}px)`, height: 38, borderRadius: 999, padding: "0 16px", display: "flex", alignItems: "center", border: "1px solid rgba(255,255,255,0.10)", background: "rgba(255,255,255,0.05)", color: "rgba(255,255,255,0.55)", fontSize: 16, fontWeight: 630 }}>{axis}</div>;
        })}
      </div>
      <div style={{ color: "rgba(255,255,255,0.28)", fontSize: 15, opacity: progress(frame, 36, 50) }}>最终给出带置信度的结论建议，含雷达图可视化</div>
    </DemoLayout>
  );
};
