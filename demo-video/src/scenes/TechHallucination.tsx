import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const bullets = [
  { text: "SQL 硬过滤先排除不符合约束的商品", sub: "品类 · 预算 · 成分 · 品牌", delay: 24 },
  { text: "pgvector 1024维语义召回", sub: "BM25 + 向量 RRF 融合", delay: 38 },
  { text: "qwen3-rerank 精排", sub: "候选集最终排序", delay: 52 },
  { text: "每条推荐理由绑定到原始 chunk", sub: "LLM 只解释，不编造", delay: 66 },
];

export const TechHallucination: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("techHallucination");

  return (
    <div className="techSlide" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 16, ...entryTransform(frame, 0, 10) }}>
        Tech 1 · 幻觉防御
      </div>
      <div className="sceneTitle center minimal">
        <h1 style={{ color: "#fff" }}>数据库拥有事实，LLM 只负责解释。</h1>
      </div>
      <div className="techBullets">
        {bullets.map(({ text, sub, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div
              key={text}
              className="techBullet"
              style={{
                opacity: p,
                transform: `translateX(${interpolate(p, [0, 1], [-20, 0], clamp)}px)`,
              }}
            >
              <b />
              <span style={{ flex: 1 }}>{text}</span>
              <em>{sub}</em>
            </div>
          );
        })}
      </div>
    </div>
  );
};
