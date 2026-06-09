export const FPS = 30;

// Layout per demo:
//   intro = text intro (文字介绍)
//   screen = full-screen recording, last 5s has summary overlay
//
// Recording sources:
//   1_web.mp4 = 1.mp4 (4m43s)
//   2_web.mp4 = 2.mp4 (34s)

export const scenes = {
  opening:      [0,    12],
  hook:         [12,   40],   // App启动 1.mp4 00:00-00:05

  // Demo 0 — 主动反问
  d0Intro:      [40,   53],   // 13s intro
  d0Screen:     [53,   100],  // 47s: 1.mp4 00:05-00:52 (模糊需求→反问→预算→候选→evidence)

  // Demo 1 — 单轮条件推荐 + 证据
  d1Intro:      [100,  113],
  d1Screen:     [113,  140],  // 27s: 2.mp4 00:02-00:16 + 1.mp4 01:18-01:35

  // Demo 3 — 反选排除
  d3Intro:      [140,  153],
  d3Screen:     [153,  169],  // 16s: 2.mp4 00:02-00:16 (最干净的反选素材)

  // Demo 2 — 图片找同款
  d2Intro:      [169,  182],
  d2Screen:     [182,  214],  // 32s: 1.mp4 01:44-02:16

  // Demo 场景化 — 海边防晒穿搭
  dsIntro:      [214,  227],
  dsScreen:     [227,  257],  // 30s: 1.mp4 02:24-02:54

  // Demo X — 多商品对比
  dxIntro:      [257,  270],
  dxScreen:     [270,  310],  // 40s: 1.mp4 03:08-03:46 + 04:32-04:43

  // Demo 4 — 购物车
  d4Intro:      [310,  323],
  d4Screen:     [323,  355],  // 32s: 2.mp4 00:20-00:34 + 1.mp4 04:20-04:28

  // ACT 3 — Tech
  techHallucination: [355, 385],
  techMultimodal:    [385, 400],
  techDecision:      [400, 412],
  techPrompt:        [412, 430],

  // ACT 4
  proof:        [430, 440],
  checklist:    [440, 452],
  closing:      [452, 465],
} as const;

export type SceneKey = keyof typeof scenes;

export const DURATION_SECONDS = 465;
export const DURATION_FRAMES = DURATION_SECONDS * FPS;

export const frameOf = (seconds: number) => Math.round(seconds * FPS);
export const sceneStart = (key: SceneKey) => frameOf(scenes[key][0]);
export const sceneDuration = (key: SceneKey) => frameOf(scenes[key][1] - scenes[key][0]);
