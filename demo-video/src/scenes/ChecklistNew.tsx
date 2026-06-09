import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress, sceneOpacity } from "../components/motion";
import { sceneDuration } from "../timing";

const items = [
  "主动反问",
  "条件筛选",
  "多轮追问与细化",
  "对比决策",
  "反选排除约束",
  "场景化组合推荐",
  "购物车与下单",
  "拍照找货",
  "单轮模糊推荐",
];

export const ChecklistNew: React.FC = () => {
  const frame = useCurrentFrame();
  const dur = sceneDuration("checklist");

  return (
    <div className="checklistScene" style={{ opacity: sceneOpacity(frame, dur) }}>
      <div style={{ color: "rgba(255,255,255,0.40)", fontSize: 14, fontWeight: 650, letterSpacing: "0.08em", textTransform: "uppercase", ...entryTransform(frame, 0, 10) }}>
        9 / 9 课题场景全覆盖
      </div>
      <div className="checklist" style={{ marginTop: 28 }}>
        {items.map((item, i) => {
          const p = progress(frame, 10 + i * 6, 28 + i * 6);
          return (
            <div
              key={item}
              className="checkItem"
              style={{ opacity: p, transform: `translateX(${interpolate(p, [0, 1], [-14, 0], clamp)}px)` }}
            >
              <span />
              {item}
            </div>
          );
        })}
      </div>
    </div>
  );
};
