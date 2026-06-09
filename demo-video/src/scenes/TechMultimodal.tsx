import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const nodes = [
  { label: "用户图片", accent: false },
  { label: "Qwen-VL 理解", accent: true },
  { label: "文本语义召回", accent: false },
  { label: "RRF 融合", accent: true },
  { label: "推荐结果", accent: false },
];
const nodes2 = [
  { label: "用户图片", accent: false },
  { label: "视觉 Embedding", accent: true },
  { label: "图像向量召回", accent: false },
];

export const TechMultimodal: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("techMultimodal");
  const mergeP = progress(frame, 52, 72);

  return (
    <div className="techSlide" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 16, ...entryTransform(frame, 0, 10) }}>
        Tech 2 · 多模态双通道
      </div>
      <div className="sceneTitle center minimal">
        <h1 style={{ color: "#fff" }}>100 张商品图片，全部预建视觉索引。</h1>
      </div>
      <div style={{ marginTop: 40, display: "grid", gap: 14, width: "min(1000px, 100%)" }}>
        {[nodes, nodes2].map((row, ri) => (
          <div key={ri} style={{ display: "flex", alignItems: "center", gap: 0 }}>
            {row.map((node, i) => {
              const p = progress(frame, ri * 12 + i * 8 + 20, ri * 12 + i * 8 + 36);
              return (
                <React.Fragment key={node.label}>
                  <div
                    style={{
                      opacity: p,
                      transform: `translateY(${interpolate(p, [0, 1], [10, 0], clamp)}px)`,
                      flex: 1, height: 56, borderRadius: 14,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      border: `1px solid ${node.accent ? "rgba(255,106,61,0.35)" : "rgba(255,255,255,0.10)"}`,
                      background: node.accent ? "rgba(255,106,61,0.12)" : "rgba(255,255,255,0.05)",
                      color: node.accent ? "#FF9070" : "rgba(255,255,255,0.70)",
                      fontSize: 15, fontWeight: 660, textAlign: "center", padding: "0 12px",
                    }}
                  >
                    {node.label}
                  </div>
                  {i < row.length - 1 && (
                    <div style={{ width: 28, height: 2, flexShrink: 0, background: "rgba(255,106,61,0.40)" }} />
                  )}
                </React.Fragment>
              );
            })}
          </div>
        ))}
        <div
          style={{
            opacity: mergeP,
            transform: `scale(${interpolate(mergeP, [0, 1], [0.94, 1], clamp)})`,
            height: 52, borderRadius: 14,
            display: "flex", alignItems: "center", justifyContent: "center", gap: 10,
            border: "1px solid rgba(255,106,61,0.40)",
            background: "rgba(255,106,61,0.14)",
            color: "#FF6A3D", fontSize: 17, fontWeight: 720,
          }}
        >
          <span style={{ width: 8, height: 8, borderRadius: "50%", background: "#FF6A3D", flexShrink: 0 }} />
          两路候选 · RRF 融合 · qwen3-rerank 精排
        </div>
      </div>
    </div>
  );
};
