import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress } from "./motion";

export const ConstraintPanel: React.FC = () => {
  const frame = useCurrentFrame();
  const budget = Math.round(interpolate(progress(frame, 70, 104), [0, 1], [300, 200], clamp));

  return (
    <div className="constraintPanel">
      <ConstraintLine label="Category" value="防晒" delay={10} />
      <ConstraintLine label="Exclude" value="酒精" delay={44} />
      <ConstraintLine label="Budget" value={`≤ ¥${budget}`} delay={78} />
    </div>
  );
};

const ConstraintLine: React.FC<{ label: string; value: string; delay: number }> = ({
  label,
  value,
  delay,
}) => {
  const frame = useCurrentFrame();

  return (
    <div className="constraintLine" style={entryTransform(frame, delay, 22)}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
};
