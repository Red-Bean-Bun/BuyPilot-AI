import React from "react";
import { Easing, interpolate } from "remotion";
import { FPS } from "../timing";
import { clamp, progress } from "./motion";

const captions = [
  { start: 0, end: 18, text: "BuyPilot-AI 把模糊购物需求转化为可解释、可追溯的购物决策。" },
  { start: 18, end: 45, text: "它不是静态原型，而是连接真实后端、商品库和证据链的 Android 应用。" },
  { start: 45, end: 85, text: "图片会同时进入视觉理解和视觉召回，帮助系统找到真正相似的商品。" },
  { start: 85, end: 120, text: "系统先把模糊表达结构化，再进行过滤、召回和推荐解释。" },
  { start: 120, end: 158, text: "商品事实来自数据库，推荐理由绑定证据片段，模型只负责解释。" },
  { start: 158, end: 190, text: "否定条件和预算会被保存为结构化约束，并进入下一轮硬过滤。" },
  { start: 190, end: 215, text: "Agent 不只给建议，也能驱动加购、删除和恢复。" },
  { start: 215, end: 245, text: "Android App 通过 SSE 连接 FastAPI，后端编排完整 Agent 链路。" },
  { start: 245, end: 270, text: "硬条件先由 SQL 过滤，再进入向量召回、融合排序和重排。" },
  { start: 270, end: 285, text: "Database owns facts. LLM only explains." },
  { start: 285, end: 300, text: "BuyPilot-AI 让每一次推荐都有来源，每一次选择都有依据。" },
];

export const Captions: React.FC<{ frame: number }> = ({ frame }) => {
  const current = captions.find((caption) => frame >= caption.start * FPS && frame < caption.end * FPS);

  if (!current) {
    return null;
  }

  const local = frame - current.start * FPS;
  const opacity = Math.min(
    progress(local, 0, 16),
    1 - progress(frame, current.end * FPS - 16, current.end * FPS, Easing.in(Easing.cubic)),
  );
  const y = interpolate(opacity, [0, 1], [12, 0], clamp);

  return (
    <div
      className="captions"
      style={{
        opacity,
        transform: `translate(-50%, ${y}px)`,
      }}
    >
      {current.text}
    </div>
  );
};
