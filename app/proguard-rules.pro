# Project-specific R8 rules.
# Libraries used here (kotlinx.serialization, Retrofit, OkHttp, Room, Hilt, WorkManager, Coil,
# CameraX, ML Kit, Firebase, Credential Manager) ship their own consumer rules, so only rules
# for code that R8 cannot see being used reflectively belong in this file.
