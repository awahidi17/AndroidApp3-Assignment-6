# AndroidApp3 — Windsor Quest

Windsor Quest is a location-based treasure-hunt application created for **MWD3B Android Development — Assignment 5**.

The hunt starts at Windsor City Hall and guides participants through 19 additional local businesses. Completing the current stop unlocks the next clue. Room stores progress, personal notes, and the selected photo for each stop.

## Core Features

- Google Map with a custom colourful style
- Fine and coarse location permission handling
- Current-device location layer and map controls
- 20 sequential Windsor stops beginning at City Hall
- Current and completed map markers
- Future locations hidden until unlocked
- Room database persistence
- RecyclerView progress list
- Place detail Activity
- Editable personal notes saved in Room
- Photo selection with retained document access
- External map Intent for directions
- Navigation drawer, reset option, and completion dialog
- Vibrant Material Components interface

## Project Setup

1. Open the `AndroidApp3` folder in Android Studio.
2. Allow Gradle to sync.
3. Open or create `local.properties` in the project root.
4. Add your Google Maps SDK key:

```properties
MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
```

5. Enable **Maps SDK for Android** for that key in Google Cloud.
6. Run the app on an emulator or Android device with Google Play services.
7. Accept location permission when prompted.

`local.properties` is ignored by Git so the private API key is not uploaded.

## Hunt Flow

1. Begin at Windsor City Hall.
2. Read the current clue.
3. Visit the displayed business.
4. Add an optional note or photo.
5. Mark the stop as visited.
6. Continue until all 20 stops are complete.

## GitHub

The configured repository is:

`https://github.com/awahidi17/AndroidApp3.git`

## Author

**Ahmad Wahidi**
