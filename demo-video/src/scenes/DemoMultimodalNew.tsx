import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { DemoLayout } from "../components/DemoLayout";
import { clamp, progress } from "../components/motion";
import { sceneDuration } from "../timing";

const channels = [
  { left: "图片", mid: "Qwen-VL 理解 →", right: "文本语义召回", delay: 0 },
  { left: "图片", mid: "视觉 Embedding →", right: "图像向量召回", delay: 12 },
];

export const DemoMultimodalNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("d2Intro");
  const mergeP = progress(frame, 28, 46);
  return (
    <DemoLayout duration={dur} chapterNum="04" phoneRight eyebrow="拍照找同款"
      title={"真正的多模态 RAG：\n文本 + 视觉双通道并行。"}
      phonePrompt="拍一张照片，帮我找同款">
      <div style={{ display: "grid", gap: 10 }}>
        {channels.map(({ left, mid, right, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div key={right} style={{ opacity: p, transform: `translateY(${interpolate(p, [0, 1], [12, 0], clamp)}px)`, display: "flex", alignItems: "center" }}>
              <div style={{ height: 46, borderRadius: 12, padding: "0 14px", display: "flex", alignItems: "center", background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.09)", color: "#fff", fontWeight: 650, fontSize: 14, whiteSpace: "nowrap" as const }}>{left}</div>
              <div style={{ flex: 1, height: 2, background: "rgba(255,106,61,0.35)" }} />
              <div style={{ height: 46, borderRadius: 12, padding: "0 14px", display: "flex", alignItems: "center", background: "rgba(255,255,255,0.04)", border: "1px solid rgba(255,255,255,0.07)", color: "rgba(255,255,255,0.45)", fontSize: 13 }}>{mid}</div>
              <div style={{ flex: 1, height: 2, background: "rgba(255,106,61,0.35)" }} />
              <div style={{ height: 46, borderRadius: 12, padding: "0 14px", display: "flex", alignItems: "center", background: "rgba(255,106,61,0.08)", border: "1px solid rgba(255,106,61,0.25)", color: "#FF9070", fontWeight: 670, fontSize: 13, whiteSpace: "nowrap" as const }}>{right}</div>
            </div>
          );
        })}
        <div style={{ opacity: mergeP, transform: `scale(${interpolate(mergeP, [0, 1], [0.92, 1], clamp)})`, height: 46, borderRadius: 12, display: "flex", alignItems: "center", justifyContent: "center", gap: 10, background: "rgba(255,106,61,0.12)", border: "1px solid rgba(255,106,61,0.30)", color: "#FF6A3D", fontSize: 15, fontWeight: 700 }}>
          <span style={{ width: 7, height: 7, borderRadius: "50%", background: "#FF6A3D" }} />RRF 融合 + Rerank → 推荐结果
        </div>
        <div style={{ color: "rgba(255,255,255,0.28)", fontSize: 14, opacity: progress(frame, 40, 55) }}>100 张商品图片预建视觉索引</div>
      </div>
    </DemoLayout>
  );
};
