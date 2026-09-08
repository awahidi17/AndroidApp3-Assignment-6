# PlaceBook Feature Mapping

| PlaceBook topic | Windsor Quest implementation |
| --- | --- |
| Google Maps | Vibrant hunt map with current and completed stop markers |
| User location and permissions | Fine/coarse permission handling, My Location layer, and fused current location |
| Places and map information | Fixed assignment route of 20 Windsor places with custom marker information cards |
| Room persistence | Saves visited status, notes, and a photo URI for every stop |
| Repository architecture | Activities use a repository, DAO, Room database, and data model |
| Detail Activity | Shows the clue, address, status, notes, photo, distance, and map action |
| Navigation | Drawer navigation, map/list/detail screens, and external map Intent |
| Photos | Full-resolution camera capture, gallery selection, retained access, and removal |
| Categories | Visited, current, and locked status categories with distinct colours |
| Search | Filters revealed stops without exposing locked business information |
| Delete/reset | Removes a photo or resets all hunt progress after confirmation |
| Sharing | Shares current progress or the completed-hunt message through an implicit Intent |
| Colour scheme | Consistent purple, aqua, coral, and gold Material styling |
| Progress indicator | Animated 0–20 progress bar, summary card, and completion dialog |

The tutorial's open-ended bookmarking concept was adapted to the assignment's required sequential route. Future places remain hidden until the previous stop is verified.
