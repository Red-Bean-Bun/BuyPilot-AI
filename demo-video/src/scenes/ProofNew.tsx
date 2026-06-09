import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const nums = [
  { value: 100, label: "商品数据", suffix: "" },
  { value: 1292, label: "知识块", suffix: "" },
  { value: 140, label: "测试用例", suffix: "+" },
];

export const ProofNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("proof");
  const tagP = progress(frame, 52, 72);

  return (
    <div className="proofBigNumbers" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", ...entryTransform(frame, 0, 10) }}>
        工程质量
      </div>
      <div className="proofNumRow">
        {nums.map(({ value, label, suffix }, i) => {
          const p = progress(frame, 14 + i * 10, 34 + i * 10);
          const count = Math.floor(interpolate(p, [0, 1], [0, value], clamp));
          return (
            <React.Fragment key={label}>
              {i > 0 && <div className="proofNumDivider" />}
              <div
                className="proofNum"
                style={{ opacity: p, transform: `translateY(${interpolate(p, [0, 1], [20, 0], clamp)}px)` }}
              >
                <strong>{count}{suffix}</strong>
                <span>{label}</span>
              </div>
            </React.Fragment>
          );
        })}
      </div>
      <div
        className="proofTagline"
        style={{ opacity: tagP, transform: `translateY(${interpolate(tagP, [0, 1], [12, 0], clamp)}px)` }}
      >
        每一条推荐，都能追溯到<span>原始数据</span>。
      </div>
    </div>
  );
};
