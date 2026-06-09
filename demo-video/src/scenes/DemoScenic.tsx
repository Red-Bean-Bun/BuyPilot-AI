import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const items = [
  { text: "理解出行场景，而非单品需求", delay: 0 },
  { text: "跨品类联合推荐：防晒 + 穿搭", delay: 10 },
  { text: "候选商品覆盖多个品类", delay: 20 },
];

export const DemoScenic: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("dsIntro");
  return (
    <DemoLayout duration={dur} chapterNum="05" eyebrow="场景化组合推荐"
      title={"说出你的出行计划，\nAgent 帮你搭配方案。"}
      phonePrompt="下周去海边，帮我搭配防晒穿搭方案">
      <div style={{ display: "grid", gap: 12 }}>
        {items.map(({ text, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div key={text} style={{
              opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-14, 0], clamp)}px)`,
              display: "flex", alignItems: "center", gap: 12,
              color: "rgba(255,255,255,0.60)", fontSize: 19, fontWeight: 580,
            }}>
              <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0 }} />
              {text}
            </div>
          );
        })}
      </div>
    </DemoLayout>
  );
};
