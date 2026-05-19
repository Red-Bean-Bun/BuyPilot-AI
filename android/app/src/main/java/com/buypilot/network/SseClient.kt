package com.buypilot.network

// TODO: 实现 SseClient
// 职责：
//   - OkHttp SSE 连接 FastAPI /chat/stream
//   - 解析 SSE 事件并回调
//   - 处理断线重连、超时、取消
// 调用链：SseClient → FastAPI → ChatViewModel
