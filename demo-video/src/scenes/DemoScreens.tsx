import React from "react";
import { PhoneFullscreen } from "../components/PhoneFullscreen";
import { sceneDuration } from "../timing";

export const D0Screen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("d0Screen")}
    recording="recordings/1_web.mp4"
    startFrom={5}
    caption="推荐一款手机 → 识别品类 → 追问预算 → 推荐结果"
    callouts={[
      { side: "left",  title: "意图识别",   body: "从模糊需求提取品类与商品类型",         y: 0.25, at: (12-5)*30 },
      { side: "right", title: "主动澄清",   body: "信息不足时先追问预算，不盲推",         y: 0.45, at: (17-5)*30 },
      { side: "left",  title: "候选召回",   body: "补齐预算后开始筛选候选商品",           y: 0.65, at: (23-5)*30 },
      { side: "right", title: "商品事实",   body: "商品图片、价格、规格来自结构化数据",   y: 0.35, at: (30-5)*30 },
      { side: "left",  title: "证据可追溯", body: "推荐理由对应商品资料与评价证据",       y: 0.55, at: (42-5)*30 },
    ]}
    summary={{
      headline: "Agent 不盲推：先识别品类，主动澄清关键信息",
      metrics: [{ value: "95%", label: "意图准确率" }, { value: "< 3s", label: "首屏响应" }],
    }}
  />
);

export const D1Screen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("d1Screen")}
    recording="recordings/2_web.mp4"
    startFrom={2}
    caption="推荐防晒霜，不要酒精也不要香精，200以内"
    callouts={[
      { side: "right", title: "否定约束",   body: "不要酒精、不要香精被识别为排除条件",   y: 0.30, at: 10 },
      { side: "left",  title: "条件筛选",   body: "品类、防晒、预算、成分约束同时生效",   y: 0.50, at: (7-2)*30 },
      { side: "right", title: "唯一匹配",   body: "硬条件收敛到唯一匹配商品",             y: 0.35, at: (11-2)*30 },
      { side: "left",  title: "推荐理由",   body: "预算内、成分安全、适合日常使用",       y: 0.60, at: (13-2)*30 },
    ]}
    summary={{
      headline: "条件推荐：硬过滤 + 可解释理由，每条都有原文出处",
      metrics: [{ value: "1292", label: "知识块" }, { value: "100%", label: "推荐可溯源" }],
    }}
  />
);

export const D3Screen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("d3Screen")}
    recording="recordings/2_web.mp4"
    startFrom={2}
    caption="不要含酒精也不要香精 → 约束写入检索层"
    callouts={[
      { side: "left",  title: "否定约束识别", body: "不要酒精、不要香精被识别为排除条件", y: 0.30, at: 10 },
      { side: "right", title: "条件筛选",     body: "品类、防晒、预算、成分约束同时生效", y: 0.52, at: (7-2)*30 },
      { side: "left",  title: "推荐理由",     body: "预算内、成分安全，理由来自知识库原文", y: 0.72, at: (13-2)*30 },
    ]}
    summary={{
      headline: "否定约束直接进入 SQL 硬过滤，不靠 LLM 记忆",
      metrics: [{ value: "100%", label: "多轮一致性" }, { value: "SQL", label: "硬过滤层" }],
    }}
  />
);

export const D2Screen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("d2Screen")}
    recording="recordings/1_web.mp4"
    startFrom={100}
    caption="拍一张照片 → 识别商品外观 → 图文联合召回"
    callouts={[
      { side: "left",  title: "图像输入",   body: "用户拍照上传商品图发起找货",           y: 0.28, at: 10 },
      { side: "right", title: "视觉理解",   body: "从图片识别商品外观与品类线索",         y: 0.48, at: (106-100)*30 },
      { side: "left",  title: "图文召回",   body: "图片线索与文字需求一起进入推荐链路",   y: 0.68, at: (118-100)*30 },
      { side: "right", title: "推荐解释",   body: "返回同类候选并说明匹配原因",           y: 0.45, at: (124-100)*30 },
    ]}
    summary={{
      headline: "多模态找货：视觉理解 + 图文联合召回，100张商品图预建索引",
      metrics: [{ value: "100", label: "预建视觉索引" }, { value: "多模态", label: "召回方式" }],
    }}
  />
);

export const DsScreen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("dsScreen")}
    recording="recordings/1_web.mp4"
    startFrom={144}
    caption="下周去海边，帮我搭配防晒穿搭方案"
    callouts={[
      { side: "left",  title: "场景意图",   body: "海边出行被理解为防晒与穿搭需求",       y: 0.30, at: 10 },
      { side: "right", title: "跨品类推荐", body: "同时召回防晒、服饰等不同品类",         y: 0.52, at: (154-144)*30 },
      { side: "left",  title: "组合方案",   body: "输出的是搭配逻辑，不是单品列表",       y: 0.72, at: (167-144)*30 },
    ]}
    summary={{
      headline: "场景化理解：跨品类联合推荐，防晒 + 穿搭一次搞定",
      metrics: [{ value: "跨品类", label: "联合推荐" }, { value: "场景化", label: "需求理解" }],
    }}
  />
);

export const DXScreen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("dxScreen")}
    recording="recordings/1_web.mp4"
    startFrom={188}
    caption="多商品对比 → 结构化分析 → 可视化结论"
    callouts={[
      { side: "left",  title: "偏好识别",     body: "主要用来拍照进入购买标准",             y: 0.28, at: 10 },
      { side: "right", title: "候选召回",     body: "按预算与拍照偏好生成手机候选",         y: 0.48, at: (188+20-188)*30 },
      { side: "left",  title: "结构化对比表", body: "按参数、性能、续航、价格横向比较",     y: 0.65, at: (203-188)*30 },
      { side: "right", title: "反馈收敛",     body: "用户调整条件后重新生成推荐解释",       y: 0.40, at: (220-188)*30 },
      { side: "left",  title: "取舍分析",     body: "多个候选之间说明优劣取舍",             y: 0.58, at: (246-188)*30 },
      { side: "right", title: "可视化结论",   body: "雷达图辅助解释最终取舍",               y: 0.35, at: (272-188)*30 },
    ]}
    summary={{
      headline: "4 个维度品类感知自动提取，带结构化对比与可视化结论",
      metrics: [{ value: "4", label: "对比维度" }, { value: "雷达图", label: "可视化" }],
    }}
  />
);

export const D4Screen: React.FC = () => (
  <PhoneFullscreen
    duration={sceneDuration("d4Screen")}
    recording="recordings/2_web.mp4"
    startFrom={20}
    caption="加入购物车 → 购物车明细 → 合计 ¥170"
    callouts={[
      { side: "left",  title: "cart_action", body: "点击加购后前端状态实时更新",           y: 0.32, at: 10 },
      { side: "right", title: "加购成功",     body: "商品已写入购物车状态",                 y: 0.52, at: (25-20)*30 },
      { side: "left",  title: "购物车明细",   body: "数量、SKU、价格合计可见",               y: 0.72, at: (26-20)*30 },
      { side: "right", title: "状态闭环",     body: "回到推荐页仍保留已加购状态",           y: 0.45, at: (31-20)*30 },
    ]}
    summary={{
      headline: "自然语言驱动购物车：加购响应 1.2s，状态持久化完整闭环",
      metrics: [{ value: "1.2s", label: "加购响应" }, { value: "¥170", label: "购物车合计" }],
    }}
  />
);
