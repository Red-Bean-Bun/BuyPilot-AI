import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress } from "./motion";

export const ArchitectureFlow: React.FC<{ nodes: string[]; delay?: number }> = ({
  nodes,
  delay = 0,
}) => {
  const frame = useCurrentFrame();

  return (
    <div className="architectureFlow">
      {nodes.map((node, index) => {
        const line = progress(frame, delay + index * 10 + 14, delay + index * 10 + 30);

        return (
          <React.Fragment key={node}>
            <div className="architectureNode" style={entryTransform(frame, delay + index * 10, 22)}>
              {node}
            </div>
            {index < nodes.length - 1 && (
              <div
                className="architectureLine"
                style={{
                  opacity: line,
                  transform: `scaleX(${interpolate(line, [0, 1], [0, 1], clamp)})`,
                }}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};
