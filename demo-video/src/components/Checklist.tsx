import React from "react";
import { useCurrentFrame } from "remotion";
import { entryTransform } from "./motion";

export const Checklist: React.FC<{ items: string[]; delay?: number }> = ({ items, delay = 0 }) => {
  const frame = useCurrentFrame();

  return (
    <div className="checklist">
      {items.map((item, index) => (
        <div key={item} className="checkItem" style={entryTransform(frame, delay + index * 5, 14)}>
          <span />
          {item}
        </div>
      ))}
    </div>
  );
};
