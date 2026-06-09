import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { Backdrop } from "./components/Backdrop";
import { SceneTransitions } from "./components/SceneTransitions";
import { CameraFrame } from "./components/CameraFrame";
import { Opening } from "./scenes/Opening";
import { DemoHook } from "./scenes/DemoHook";
import { DemoClarify } from "./scenes/DemoClarify";
import { DemoCriteriaEvidence } from "./scenes/DemoCriteriaEvidence";
import { DemoNegation } from "./scenes/DemoNegation";
import { DemoMultimodalNew } from "./scenes/DemoMultimodalNew";
import { DemoScenic } from "./scenes/DemoScenic";
import { DemoCompareNew } from "./scenes/DemoCompareNew";
import { DemoCartNew } from "./scenes/DemoCartNew";
import { D0Screen, D1Screen, D3Screen, D2Screen, DsScreen, DXScreen, D4Screen } from "./scenes/DemoScreens";
import { TechHallucination } from "./scenes/TechHallucination";
import { TechMultimodal } from "./scenes/TechMultimodal";
import { TechDecision } from "./scenes/TechDecision";
import { TechPromptObs } from "./scenes/TechPromptObs";
import { ProofNew } from "./scenes/ProofNew";
import { ChecklistNew } from "./scenes/ChecklistNew";
import { ClosingNew } from "./scenes/ClosingNew";
import { sceneDuration, sceneStart } from "./timing";

const Sc: React.FC<{ k: Parameters<typeof sceneStart>[0]; strength?: number; children: React.ReactNode }> = ({ k, strength = 1, children }) => (
  <Sequence from={sceneStart(k)} durationInFrames={sceneDuration(k)}>
    <CameraFrame duration={sceneDuration(k)} strength={strength}>{children}</CameraFrame>
  </Sequence>
);

const ScRaw: React.FC<{ k: Parameters<typeof sceneStart>[0]; children: React.ReactNode }> = ({ k, children }) => (
  <Sequence from={sceneStart(k)} durationInFrames={sceneDuration(k)}>{children}</Sequence>
);

export const BuyPilotLaunchFilm: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill className="film">
      <Backdrop />

      <Sc k="opening" strength={0.8}><Opening /></Sc>
      <Sc k="hook"><DemoHook /></Sc>

      <Sc k="d0Intro"><DemoClarify /></Sc>
      <ScRaw k="d0Screen"><D0Screen /></ScRaw>

      <Sc k="d1Intro"><DemoCriteriaEvidence /></Sc>
      <ScRaw k="d1Screen"><D1Screen /></ScRaw>

      <Sc k="d3Intro"><DemoNegation /></Sc>
      <ScRaw k="d3Screen"><D3Screen /></ScRaw>

      <Sc k="d2Intro"><DemoMultimodalNew /></Sc>
      <ScRaw k="d2Screen"><D2Screen /></ScRaw>

      <Sc k="dsIntro"><DemoScenic /></Sc>
      <ScRaw k="dsScreen"><DsScreen /></ScRaw>

      <Sc k="dxIntro"><DemoCompareNew /></Sc>
      <ScRaw k="dxScreen"><DXScreen /></ScRaw>

      <Sc k="d4Intro"><DemoCartNew /></Sc>
      <ScRaw k="d4Screen"><D4Screen /></ScRaw>

      <Sc k="techHallucination" strength={0.9}><TechHallucination /></Sc>
      <Sc k="techMultimodal" strength={0.9}><TechMultimodal /></Sc>
      <Sc k="techDecision" strength={0.9}><TechDecision /></Sc>
      <Sc k="techPrompt" strength={0.9}><TechPromptObs /></Sc>

      <Sc k="proof" strength={0.85}><ProofNew /></Sc>
      <Sc k="checklist"><ChecklistNew /></Sc>
      <Sc k="closing" strength={0.7}><ClosingNew /></Sc>

      <SceneTransitions frame={frame} />
    </AbsoluteFill>
  );
};
