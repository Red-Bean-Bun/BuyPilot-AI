import React from "react";
import { useCurrentFrame } from "remotion";
import { cameraStyle, sceneOpacity } from "./motion";

export const CameraFrame: React.FC<{
  duration: number;
  strength?: number;
  children: React.ReactNode;
}> = ({ duration, strength = 1, children }) => {
  const frame = useCurrentFrame();

  return (
    <div
      className="cameraFrame"
      style={{
        opacity: sceneOpacity(frame, duration, 22),
        ...cameraStyle(frame, duration, strength),
      }}
    >
      {children}
    </div>
  );
};
