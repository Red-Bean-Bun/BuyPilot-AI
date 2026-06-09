import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const promptItems = [
  { label: "12 个 Prompt 模板", sub: "Markdown 版本化，运行时热加载", delay: 22 },
  { label: "JSON Schema 强制约束输出", sub: "不靠模型听话，靠结构保障", delay: 36 },
  { label: "SSE 协议三端自动化守卫", sub: "Python import 时 · Kotlin 编译时 · CI 层", delay: 50 },
  { label: "上下文诊断主动告警", sub: "BUDGET_PATCH_LOST · EXCLUSION_LOST", delay: 64 },
];

export const TechPromptObs: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("techPrompt");

  return (
    <div className="techSlide" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 16, ...entryTransform(frame, 0, 10) }}>
        Tech 4 + 5 · Prompt 契约 &amp; 可观测性
      </div>
      <div className="sceneTitle center minimal">
        <h1 style={{ color: "#fff" }}>用制度保证质量，不靠自觉。</h1>
      </div>
      <div className="techBullets">
        {promptItems.map(({ label, sub, delay }) => {
          const p = progress(frame, delay, delay + 20);
          return (
            <div
              key={label}
              className="techBullet"
              style={{ opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-20, 0], clamp)}px)` }}
            >
              <b />
              <span style={{ flex: 1 }}>{label}</span>
              <em>{sub}</em>
            </div>
          );
        })}
      </div>
    </div>
  );
};
