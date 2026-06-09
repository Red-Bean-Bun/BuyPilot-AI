import React from "react";
import { useCurrentFrame } from "remotion";
import { entryTransform } from "./motion";

export const SceneTitle: React.FC<{
  eyebrow?: string;
  title: string;
  body?: string;
  align?: "left" | "center";
  minimal?: boolean;
}> = ({ eyebrow, title, body, align = "left", minimal = false }) => {
  const frame = useCurrentFrame();

  return (
    <div className={`sceneTitle ${align === "center" ? "center" : ""} ${minimal ? "minimal" : ""}`}>
      {eyebrow && !minimal && (
        <div className="eyebrow" style={entryTransform(frame, 0, 12)}>
          {eyebrow}
        </div>
      )}
      <h1 style={entryTransform(frame, 6)}>{title}</h1>
      {body && !minimal && <p style={entryTransform(frame, 12)}>{body}</p>}
    </div>
  );
};
