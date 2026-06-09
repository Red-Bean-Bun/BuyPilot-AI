import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { clamp, entryTransform, progress } from "./motion";

export const EvidenceConnector: React.FC = () => {
  const frame = useCurrentFrame();
  const line = progress(frame, 28, 58);
  const lock = progress(frame, 58, 82);

  return (
    <div className="evidenceConnector">
      <div className="evidenceReason" style={entryTransform(frame, 6, 24)}>
        <span>Recommendation Reason</span>
        <strong>适合油皮，预算内，证据充分</strong>
        <em>LLM explains</em>
      </div>
      <div
        className="evidenceLine"
        style={{
          opacity: line,
          transform: `rotate(16deg) scaleX(${interpolate(line, [0, 1], [0, 1], clamp)})`,
        }}
      />
      <div
        className="evidenceLock"
        style={{
          opacity: lock,
          transform: `scale(${interpolate(lock, [0, 1], [0.82, 1], clamp)})`,
        }}
      >
        locked
      </div>
      <div className="evidenceChunk" style={entryTransform(frame, 42, 24)}>
        <span>Knowledge Chunk</span>
        <strong>official_faq · user_review · product facts</strong>
        <em>Database owns facts</em>
      </div>
      <div className="evidenceTrace" style={entryTransform(frame, 86, 14)}>
        <b />
        evidence_id bound to product card
      </div>
    </div>
  );
};
