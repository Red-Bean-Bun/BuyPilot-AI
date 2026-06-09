import "./index.css";
import { Composition } from "remotion";
import { BuyPilotLaunchFilm } from "./Composition";
import { DURATION_FRAMES, FPS } from "./timing";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="BuyPilotLaunchFilm"
        component={BuyPilotLaunchFilm}
        durationInFrames={DURATION_FRAMES}
        fps={FPS}
        width={1920}
        height={1080}
      />
    </>
  );
};
