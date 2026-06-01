# Android Runtime Flags

`Debug` builds use the real backend by default:

```bash
./gradlew :app:installDebug
```

The backend URL is read from `BUY_PILOT_BASE_URL` in the project-root `.env`, or from the environment variable with the same name. For physical-device observability, use the tunnel URL, for example:

```bash
BUY_PILOT_BASE_URL=https://api.lzjyyds.top ./gradlew :app:installDebug
```

To run the local mock chat UI without calling `/chat/stream`, opt in explicitly:

```bash
./gradlew :app:installDebug -PUSE_MOCK_CHAT=true
```
