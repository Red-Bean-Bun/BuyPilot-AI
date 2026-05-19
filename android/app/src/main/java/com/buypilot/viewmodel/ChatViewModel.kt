package com.buypilot.viewmodel

// TODO: 实现 ChatViewModel
// 职责：
//   - 管理 ChatUiNode 列表的 StateFlow
//   - 发送用户消息、接收 SSE 事件
//   - 处理 SSE → ChatUiNode 转换
//   - 暴露 loading / error / 阶段状态
// 调用链：ChatViewModel → SseClient → ChatScreen
