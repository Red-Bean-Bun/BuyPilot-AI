import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress } from "./motion";

export const MetricCard: React.FC<{
  label: string;
  value: string;
  numeric?: number;
  suffix?: string;
  delay?: number;
}> = ({ label, value, numeric, suffix = "", delay = 0 }) => {
  const frame = useCurrentFrame();
  const count = numeric === undefined ? value : Math.round(interpolate(progress(frame, delay + 8, delay + 42), [0, 1], [0, numeric], clamp));

  return (
    <div className="metricCard" style={entryTransform(frame, delay, 28)}>
      <strong>{numeric === undefined ? value : `${count}${suffix}`}</strong>
      <span>{label}</span>
    </div>
  );
};
