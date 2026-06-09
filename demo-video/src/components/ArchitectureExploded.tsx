import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress } from "./motion";

const layers = [
  {
    label: "Android App",
    detail: "Native client · SSE stream",
    tone: "device",
  },
  {
    label: "FastAPI Agent Runtime",
    detail: "intent · criteria · retrieval · cart",
    tone: "runtime",
  },
  {
    label: "PostgreSQL + pgvector",
    detail: "products · chunks · evidence",
    tone: "data",
  },
];

export const ArchitectureExploded: React.FC = () => {
  const frame = useCurrentFrame();
  const spine = progress(frame, 48, 104);

  return (
    <div className="architectureExploded">
      <div
        className="architectureSpine"
        style={{
          opacity: spine,
          transform: `scaleY(${interpolate(spine, [0, 1], [0, 1], clamp)})`,
        }}
      />
      {layers.map((layer, index) => (
        <div
          key={layer.label}
          className={`architecturePlate ${layer.tone}`}
          style={entryTransform(frame, 26 + index * 18, 32)}
        >
          <span>0{index + 1}</span>
          <strong>{layer.label}</strong>
          <em>{layer.detail}</em>
        </div>
      ))}
      <div className="architectureOrbit" style={entryTransform(frame, 104, 18)}>
        Evidence-bound answer
      </div>
    </div>
  );
};
