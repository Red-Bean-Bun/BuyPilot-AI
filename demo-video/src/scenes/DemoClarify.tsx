import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const chips = ["4000元以上", "2000-4000元", "1000-2000元"];

export const DemoClarify: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("d0Intro");
  return (
    <DemoLayout duration={dur} chapterNum="01" eyebrow="主动反问"
      title={"不盲推，\n先识别槽位再澄清。"}
      phonePrompt="推荐一款手机">
      <div style={{ display: "grid", gap: 10 }}>
        <div style={{ color: "rgba(255,255,255,0.35)", fontSize: 14, marginBottom: 4 }}>
          系统识别品类后，追问关键槽位：
        </div>
        {chips.map((chip, i) => {
          const p = progress(frame, i * 8, i * 8 + 22);
          const isTop = i === 0;
          return (
            <div key={chip} style={{
              opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-16, 0], clamp)}px)`,
              height: 52, borderRadius: 999, display: "flex", alignItems: "center", padding: "0 22px", gap: 12,
              border: `1px solid ${isTop ? "rgba(255,106,61,0.45)" : "rgba(255,255,255,0.10)"}`,
              background: isTop ? "rgba(255,106,61,0.12)" : "rgba(255,255,255,0.04)",
              color: isTop ? "#FF9070" : "rgba(255,255,255,0.50)", fontSize: 20, fontWeight: 650,
            }}>
              {isTop && <span style={{ width: 7, height: 7, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0 }} />}
              {chip}
            </div>
          );
        })}
      </div>
    </DemoLayout>
  );
};
