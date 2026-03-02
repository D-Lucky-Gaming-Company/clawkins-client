# clawkins-client
This repository is the "brain" of the operation. It houses the Java source code and the Gradle build system that compiles your game into a runnable PC application (.jar or .exe).

Primary Purpose: To manage the game lifecycle, turn-based combat algorithms, and player input (Keyboard/Mouse).
Key Modules:
core/: The heart of the project. This contains the Java classes for your Clawkins, the AI logic for Dogs/Birds/Rats, and the combat state machine.
lwjgl3/: The PC-specific launcher. It handles window creation, resolution settings (like 1080p), and hardware communication via OpenGL.
PM Responsibility: You will manage Pull Requests here. Your goal is to ensure that no one breaks the "Build"—if the code doesn't compile, it shouldn't be merged into the main branch.
Technical Stack: Java 17+ (or 21), libGDX Framework, Gradle.
