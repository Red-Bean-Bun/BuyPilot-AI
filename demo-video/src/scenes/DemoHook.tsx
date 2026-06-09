import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

export const DemoHook: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("hook");
  return (
    <DemoLayout duration={dur} chapterNum="00" eyebrow="BuyPilot-AI"
      title={"把模糊的需求，\n变成可解释的决策。"}
      phonePrompt="推荐一款手机">
      <div style={{ display: "grid", gap: 14 }}>
        {[
          { text: "意图理解 → 标准生成 → 混合检索", bold: true },
          { text: "证据绑定 → 推荐可追溯，杜绝幻觉", bold: false },
          { text: "多模态 · 多轮 · 场景化 · 购物车闭环", bold: false },
        ].map(({ text, bold }, i) => {
          const p = progress(frame, i * 10, i * 10 + 22);
          return (
            <div key={text} style={{ opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-14, 0], clamp)}px)`, display: "flex", alignItems: "center", gap: 12, color: bold ? "#fff" : "rgba(255,255,255,0.50)", fontSize: bold ? 20 : 17, fontWeight: bold ? 650 : 520 }}>
              <span style={{ width: 6, height: 6, borderRadius: "50%", background: bold ? "#FF6A3D" : "rgba(255,255,255,0.20)", flexShrink: 0 }} />{text}
            </div>
          );
        })}
        <div style={{ marginTop: 16, opacity: progress(frame, 28, 44) }}>
          <span style={{ display: "inline-flex", alignItems: "center", height: 28, padding: "0 12px", borderRadius: 999, background: "rgba(52,211,153,0.12)", border: "1px solid rgba(52,211,153,0.25)", color: "#34D399", fontSize: 13, fontWeight: 680 }}>ByteDance AI Full-Stack Challenge 2026</span>
        </div>
      </div>
    </DemoLayout>
  );
};
