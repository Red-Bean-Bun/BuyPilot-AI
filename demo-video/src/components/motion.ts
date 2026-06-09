import { Easing, interpolate } from "remotion";

export const clamp = {
  extrapolateLeft: "clamp" as const,
  extrapolateRight: "clamp" as const,
};

export const premiumEase = Easing.bezier(0.4, 0, 0.2, 1);
export const revealEase = Easing.bezier(0.16, 1, 0.3, 1);

export const progress = (
  frame: number,
  from: number,
  to: number,
  easing = revealEase,
) =>
  interpolate(frame, [from, to], [0, 1], {
    ...clamp,
    easing,
  });

export const sceneOpacity = (frame: number, duration: number, fade = 18) => {
  const fadeIn = progress(frame, 0, fade);
  const fadeOut = 1 - progress(frame, duration - fade, duration, Easing.in(Easing.cubic));
  return Math.min(fadeIn, fadeOut);
};

export const cameraStyle = (frame: number, duration: number, strength = 1) => {
  const enter = progress(frame, 0, 42, premiumEase);
  const exit = progress(frame, duration - 34, duration, Easing.in(Easing.cubic));
  const drift = progress(frame, 0, duration, premiumEase);
  const scale = interpolate(enter, [0, 1], [0.972, 1]) + drift * 0.012 * strength + exit * 0.018;
  const y = interpolate(enter, [0, 1], [26 * strength, 0]) - drift * 10 * strength;
  const blur = interpolate(exit, [0, 1], [0, 10 * strength]);

  return {
    transform: `translateY(${y}px) scale(${scale})`,
    filter: `blur(${blur}px)`,
  };
};

export const entryTransform = (frame: number, delay = 0, distance = 18) => {
  const p = progress(frame, delay, delay + 24);
  const y = interpolate(p, [0, 1], [distance, 0]);
  const scale = interpolate(p, [0, 1], [0.985, 1]);

  return {
    opacity: p,
    transform: `translateY(${y}px) scale(${scale})`,
  };
};
