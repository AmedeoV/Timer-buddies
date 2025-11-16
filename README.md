# Timer Buddies 🎉⏰

A fun and engaging Android timer app designed to help kids transition between activities. As time counts down, a reward image is gradually revealed, motivating children to complete their tasks.

## Features

- **Kid-Friendly Interface**: Large, colorful buttons and simple navigation
- **Visual Countdown**: Circular progress indicator showing remaining time
- **Preset Timers**: Quick selection for 5, 10, 15, 20, 30, and 45 minutes
- **Image Reveal Animation**: Gradually uncovers a reward image as the timer counts down
- **Celebration Screen**: Fun completion message with animations
- **Pause & Resume**: Full timer control for flexibility

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with ViewModel
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Project Structure

```
app/src/main/java/com/timerbuddies/app/
├── MainActivity.kt                 # Main entry point
├── ui/
│   ├── screens/
│   │   └── TimerScreen.kt         # Main timer screen composable
│   ├── components/
│   │   ├── CircularTimerDisplay.kt # Circular timer with reveal animation
│   │   └── TimeSelectionButton.kt  # Time selection button component
│   └── theme/
│       ├── Color.kt               # App color palette
│       ├── Theme.kt               # Material theme configuration
│       └── Type.kt                # Typography settings
└── viewmodel/
    └── TimerViewModel.kt          # Timer state management
```

## Building the App

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 8 or higher
- Android SDK with API 34

### Steps

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an Android device or start an emulator
4. Click Run or press Shift+F10

## How to Use

1. **Select Time**: Choose a preset time duration (5-45 minutes)
2. **Start Timer**: Tap the "Start" button to begin the countdown
3. **Watch the Magic**: As time passes, the reward image gradually reveals
4. **Completion**: When time is up, see a celebration screen!
5. **Reset or New Timer**: Start over or select a different duration

## Future Enhancements

- [ ] Custom image upload/selection
- [ ] Multiple reward image themes
- [ ] Sound effects and music
- [ ] Custom timer durations
- [ ] Parent settings and controls
- [ ] Progress tracking and statistics
- [ ] Multiple timer profiles
- [ ] Notification support
- [ ] Widget for quick access

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests.

## License

This project is open source and available under the MIT License.

## Acknowledgments

Designed with love for kids and parents looking for better transition tools! 💙
