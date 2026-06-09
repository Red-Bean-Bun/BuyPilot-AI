import React from "react";
import { AbsoluteFill, Video, interpolate, staticFile, useCurrentFrame } from "remotion";
import { theme } from "../theme";
import { clamp, progress } from "./motion";

export const PhonePlaceholder: React.FC<{
  label: string;
  sublabel?: string;
  prompt?: string;
  recording?: string;
}> = ({ label, sublabel = "Recording Placeholder", prompt, recording }) => {
  const frame = useCurrentFrame();
  const p = progress(frame, 0, 30);
  const scale = interpolate(p, [0, 1], [0.92, 1], clamp);
  const y = interpolate(p, [0, 1], [40, 0], clamp);

  return (
    <div
      className="phoneShell"
      style={{
        opacity: p,
        transform: `translateY(${y}px) scale(${scale})`,
      }}
    >
      <div className="phoneScreen">
        {recording ? (
          <Video src={staticFile(recording)} className="phoneVideo" />
        ) : (
          <AbsoluteFill className="phonePlaceholder">
            <div className="phoneStatus" />
            {prompt && <div className="phonePrompt">{prompt}</div>}
            <div className="phoneHero">
              <span>{sublabel}</span>
              <strong>{label}</strong>
            </div>
            <div className="phoneCards">
              {[0, 1, 2].map((item) => (
                <div key={item} className={item === 0 ? "active" : ""}>
                  <b />
                  <section>
                    <i />
                    <i />
                  </section>
                </div>
              ))}
            </div>
            <div className="phoneAction" style={{ background: theme.colors.coral }} />
          </AbsoluteFill>
        )}
      </div>
    </div>
  );
};
