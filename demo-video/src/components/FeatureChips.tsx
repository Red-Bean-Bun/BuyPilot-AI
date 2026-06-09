import React from "react";
import { useCurrentFrame } from "remotion";
import { entryTransform } from "./motion";

export const FeatureChips: React.FC<{
  items: string[];
  delay?: number;
  compact?: boolean;
}> = ({ items, delay = 0, compact }) => {
  const frame = useCurrentFrame();

  return (
    <div className={`featureChips ${compact ? "compact" : ""}`}>
      {items.map((item, index) => (
        <div key={item} className="featureChip" style={entryTransform(frame, delay + index * 6, 18)}>
          {item}
        </div>
      ))}
    </div>
  );
};
