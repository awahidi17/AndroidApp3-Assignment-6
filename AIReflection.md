# AI Usage and Reflection

## 1. How Did You Use AI in This Assignment?

- I used AI as a starting point and reference while changing the PlaceBook idea into a Windsor treasure hunt.
- One example was asking how to save a short note and a selected photo URI with each Room record. I used the pattern, then changed the field names and screen layout to fit my app.
- I also asked for help organizing the 20-stop unlocking logic. I kept the idea of finding the first unvisited stop because it was simple to follow.
- `OpenDocument` and retained URI permission were new to me, so I compared the explanation with Android documentation before adding the photo feature.

## 2. How Did You Understand, Verify, and Adapt the Code?

- I traced the order from the database to the map, list, and detail screen. I checked that only the current stop is unlocked and that finishing it reveals the next one.
- I tested the reset logic, notes field, photo picker, location permission, and map Intent separately.
- I changed the sample route to Windsor locations and moved the code into my own package. I also hid future names and addresses so the treasure hunt does not reveal the route early.

## 3. What Did You Learn or Get Better At Through This Work?

- I understand Room better because one saved record now affects several parts of the app.
- I improved at connecting Activities with Intents and passing a place ID to the detail screen.
- The map and database logic went well once I tested each part separately. The harder part was keeping the map, list, notes, and photo information synchronized after returning to a screen.
