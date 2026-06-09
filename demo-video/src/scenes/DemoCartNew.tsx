import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

export const DemoCartNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("d4Intro");
  const count = frame < 30 ? 0 : 1;
  const flipP = progress(frame % 30, 0, 15);
  return (
    <DemoLayout duration={dur} chapterNum="07" phoneRight eyebrow="购物车闭环"
      title={"加购、删除、结算，\n全程自然语言驱动。"}
      phonePrompt="加入购物车">
      <div style={{ display: "flex", gap: 24, alignItems: "center" }}>
        <div style={{ display: "flex", flexDirection: "column" as const, alignItems: "center", gap: 6 }}>
          <strong style={{ color: "#FF6A3D", fontSize: 88, fontWeight: 760, lineHeight: 1, letterSpacing: "-0.02em", transform: `rotateX(${interpolate(flipP, [0, 0.5, 1], [0, 90, 0], clamp)}deg)`, display: "inline-block" }}>{count}</strong>
          <span style={{ color: "rgba(255,255,255,0.35)", fontSize: 14, fontWeight: 600 }}>购物车商品数</span>
        </div>
        <div style={{ flex: 1, display: "grid", gap: 10 }}>
          {[
            { label: "加购响应", value: "1.2s" },
            { label: "结算确认", value: "39ms" },
            { label: "状态持久化", value: "✓" },
          ].map(({ label, value }, i) => {
            const p = progress(frame, i * 10, i * 10 + 20);
            return <div key={label} style={{ opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-12, 0], clamp)}px)`, height: 50, borderRadius: 14, display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 18px", border: "1px solid rgba(255,255,255,0.08)", background: "rgba(255,255,255,0.04)" }}>
              <span style={{ color: "rgba(255,255,255,0.45)", fontSize: 16, fontWeight: 580 }}>{label}</span>
              <strong style={{ color: "#FF6A3D", fontSize: 18, fontWeight: 720 }}>{value}</strong>
            </div>;
          })}
        </div>
      </div>
    </DemoLayout>
  );
};
