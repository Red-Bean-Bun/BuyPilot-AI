# BuyPilot-AI Demo Film

This Remotion project contains the animated shell for the competition demo video.

## Preview

```bash
npm run dev
```

Open the Remotion Studio URL printed by the command and choose:

```text
BuyPilotLaunchFilm
```

## Render

```bash
npx remotion render BuyPilotLaunchFilm
```

The composition is 1920x1080, 30fps, and 5m00s.

## Replace Phone Placeholders

Current phone screens are animated placeholders. Put Android screen recordings in:

```text
public/recordings/
```

Suggested filenames are listed in `public/recordings/README.md`.

When recordings are ready, pass `recording="recordings/...mp4"` to the relevant
`PhonePlaceholder` in `src/scenes/`. Until a recording is explicitly passed, the
film renders the polished placeholder and does not try to load missing files.


## Motion Rules

- Use `useCurrentFrame()`, `interpolate()`, and `Easing`.
- Do not use CSS transitions or CSS animations for render-critical motion.
- Keep demo sections truthful: loading/thinking may be accelerated, but use the visible speed badges.
