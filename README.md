# OST Zones

This is an Android app that allows you to geofence areas on a Google map, then assign those zones their own playlist rules via Spotify. Perfect for adding thematic music to an area like, similar to video games or movies!

## Screenshots

![1](https://github.com/amingola/OST-Zones/assets/133837563/c1b83c5b-1054-4eb4-869e-d3af13676d25)
![2](https://github.com/amingola/OST-Zones/assets/133837563/35facf0a-314e-4945-9c0e-aa1f00e2c41f)
![3](https://github.com/amingola/OST-Zones/assets/133837563/8de03b82-4187-4f77-8e8d-ac173702031c)
![4](https://github.com/amingola/OST-Zones/assets/133837563/3daba022-e707-4504-a549-db1544689463)
![5](https://github.com/amingola/OST-Zones/assets/133837563/c4e4c978-c88f-4b21-aa2c-dd03f1eebf8f)

## Status

This demo is functional, in that
- The Google Map is rendered and the user can authenticate to Spotify
- Freeform polygons can be drawn (and redrawn for existing zones)
- Playlists can be assigned to play when inside a polygon
- The user's GPS location can be detected inside or outside a polygon (raycasting algorithm)
- An asynchronous task controls play/pause of Spotify tracks

However, there is much to be improved before it is fit for release to the Play store.

### Next Steps
- Metadata (like the app name) needs to be updated
- A cohesive theme for the UI
  - Default button UI should not be used
  - Icons are missing
  - Colors don't match
  - Lists are very plain
- Only the user's Playlists are loaded from the library (not individual songs, albums, etc.)
- Refactoring the MapsActivity into Fragments would be ideal
- The async task that plays/pauses music doesn't respect when Spotify is controlled by another device (e.g. will pause music if not in an OST Zone even if played from another device)

  There are all loads of potential useful features (granular playlist editing functionality, loop/shuffle/cross-fading options, etc.) that I may add in the future.
