import React from "react";
import { useCurrentFrame } from "remotion";
import { entryTransform } from "./motion";

export const GlassCard: React.FC<{
  title: string;
  desc?: string;
  kicker?: string;
  delay?: number;
  className?: string;
}> = ({ title, desc, kicker, delay = 0, className }) => {
  const frame = useCurrentFrame();

  return (
    <div className={`glassCard ${className ?? ""}`} style={entryTransform(frame, delay, 28)}>
      {kicker && <span>{kicker}</span>}
      <strong>{title}</strong>
      {desc && <em>{desc}</em>}
    </div>
  );
};
