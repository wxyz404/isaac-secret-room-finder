# The Binding of Isaac — Secret Room Finder

A small, dependency-free Java Swing prototype for mapping a completed floor and ranking likely locations for a Secret Room, Super Secret Room, or Ultra Secret Room.

> This is an unofficial fan-made tool and is not affiliated with or endorsed by Nicalis, Inc. or *The Binding of Isaac*.

## Requirements

- JDK 17 or newer. The project is source-compatible with Java 17 and runs on Java 27.
- Maven 3.9 or newer for the documented build commands.

## Run

```powershell
mvn test
mvn package
java -cp target/classes com.example.isaacfinder.SecretRoomFinderApp
```

On Windows, double-click `run.bat` to build and open the prototype.

## Map input

The editor represents the full 13 × 13 floor boundary. Choose a palette entry, then click a cell to paint it:

- **Empty** — an unoccupied location that may be ranked as a candidate.
- **Normal room** — an ordinary completed-map room.
- **Boss Room** — enables Boss-proximity scoring for Super Secret Rooms.
- **Shop** — enables Shop-proximity scoring for Super Secret Rooms.
- **Blocked** — a position that cannot contain a room.

## Ranking rules

This is a transparent heuristic, not a reimplementation of the game’s seed or RNG.

- **Secret Room:** an empty cell touching at least two rooms. More touching rooms rank higher.
- **Super Secret Room:** an empty cell touching exactly one room, preserving the expected dead-end-like shape. It receives additional score bonuses for being close to the Boss Room, close to the Shop, and near the shortest Boss–Shop path.
- **Ultra Secret Room:** an empty cell touching exactly one room with at least two open sides, representing a plausible Red Key-style expansion slot.

Every result includes its score and the evidence used to calculate it, making the behavior straightforward to inspect and tune.

## Development

Run the complete automated test suite:

```powershell
mvn test
```

GitHub Actions runs the same command on Java 21 for pushes and pull requests.

