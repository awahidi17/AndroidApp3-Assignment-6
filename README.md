# AndroidApp3 — Windsor Quest

Windsor Quest is a completed location-based treasure-hunt application created for **MWD3B Android Development — Assignment 6**. Assignment 6 polishes and completes the project started in Assignment 5.

The hunt starts at Windsor City Hall and guides participants through 19 additional local businesses. Completing the current stop unlocks the next clue. Room stores progress, personal notes, and the selected photo for each stop.

## Core Features

- Google Map with a custom colourful style
- Fine and coarse location permission handling
- Current-device location layer and map controls
- 20 sequential Windsor stops beginning at City Hall
- Current and completed map markers
- Custom vibrant information cards for map markers
- Future locations hidden until unlocked
- Room database persistence
- RecyclerView progress list
- Search for revealed stops without exposing locked businesses
- Place detail Activity
- Editable personal notes saved in Room
- Live proximity verification within 250 metres of each stop
- Current-distance feedback using the fused location provider
- Full-resolution camera photos stored through FileProvider
- Gallery photo selection with retained document access
- Change or remove a saved place photo
- External map Intent for directions
- Shareable progress and completion messages
- Navigation drawer, reset option, and completion dialog
- Vibrant Material Components interface
- Haversine distance tests and 20-stop route tests

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
4. Move within 250 metres and verify your live location.
5. Add an optional note, take a photo, or choose a saved image.
6. Complete the stop to reveal the next clue.
7. Continue until all 20 stops are complete.

## Assignment 6 Improvements

- Stops can no longer be completed from an unrelated location.
- A live distance card shows whether the participant has arrived.
- Participants can take a new place photo in addition to choosing one.
- Photo removal, progress sharing, and completion sharing were added.
- Custom marker cards and privacy-safe stop search were added.
- Permission errors, unavailable GPS, missing camera apps, and missing map apps show clear feedback.
- The project version was updated to 2.0 and the code remains separated into Activities, repository, DAO, and model layers.

See [`PLACEBOOK_FEATURES.md`](PLACEBOOK_FEATURES.md) for the complete tutorial-to-project feature mapping.

## Testing Location in the Emulator

Open the emulator controls, select **Location**, and enter the latitude and longitude of the current stop. Send the position to the emulator, return to Windsor Quest, and tap **Check My Distance** or **Verify Location & Complete**.

## GitHub

The configured repository is:

`https://github.com/awahidi17/AndroidApp3.git`

## Author

**Ahmad Wahidi**
