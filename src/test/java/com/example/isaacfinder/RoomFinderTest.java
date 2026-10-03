package com.example.isaacfinder;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomFinderTest {
    @Test
    void floorMapUsesTheFullThirteenByThirteenBoundary() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        assertTrue(map.columns() == 13 && map.rows() == 13);
    }

    @Test
    void secretRoomNeedsAtLeastTwoAdjacentRooms() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.ROOM);
        map.paint(4, 5, SecretRoomFinderApp.CellState.ROOM);
        assertTrue(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.SECRET), 5, 5));
    }

    @Test
    void superSecretRoomRequiresExactlyOneAdjacentRoom() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.ROOM);
        assertTrue(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 5));
        map.paint(4, 5, SecretRoomFinderApp.CellState.ROOM);
        assertFalse(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 5));
    }

    @Test
    void secretAndSuperSecretRoomsCannotBeNextToABossRoom() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.BOSS);
        map.paint(5, 3, SecretRoomFinderApp.CellState.ROOM);
        map.paint(4, 5, SecretRoomFinderApp.CellState.ROOM);

        assertFalse(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.SECRET), 5, 5));
        assertFalse(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 5));
    }

    @Test
    void superSecretCandidatesNearBossAndShopScoreHigher() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.ROOM);
        map.paint(3, 4, SecretRoomFinderApp.CellState.ROOM);
        map.paint(7, 5, SecretRoomFinderApp.CellState.BOSS);
        map.paint(9, 5, SecretRoomFinderApp.CellState.SHOP);
        int nearScore = candidateAt(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 5).score();
        int farScore = candidateAt(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 3, 5).score();
        assertTrue(nearScore > farScore);
    }

    @Test
    void superSecretExplainsItsBossShopPathBonus() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 2, SecretRoomFinderApp.CellState.ROOM);
        map.paint(3, 3, SecretRoomFinderApp.CellState.BOSS);
        map.paint(7, 3, SecretRoomFinderApp.CellState.SHOP);
        String reason = candidateAt(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 3).reason();
        assertTrue(reason.contains("Boss–Shop path"));
    }

    @Test
    void superSecretSearchWorksWhenBossAndShopAreUnknown() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.ROOM);
        SecretRoomFinderApp.Candidate candidate = candidateAt(find(map, SecretRoomFinderApp.TargetRoom.SUPER_SECRET), 5, 5);
        assertFalse(candidate.reason().contains("Boss distance"));
        assertFalse(candidate.reason().contains("Shop distance"));
    }

    @Test
    void ultraSecretRoomCannotDirectlyTouchANormalRoom() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        map.paint(5, 4, SecretRoomFinderApp.CellState.ROOM);
        assertFalse(hasCandidate(find(map, SecretRoomFinderApp.TargetRoom.ULTRA_SECRET), 5, 5));
    }

    @Test
    void ultraSecretRoomRanksThreeRedRoomConnectionsAboveLowerTiers() {
        SecretRoomFinderApp.FloorMap map = new SecretRoomFinderApp.FloorMap(13, 13);
        // Each normal room is two steps away, leaving one empty Red Room bridge square.
        map.paint(5, 3, SecretRoomFinderApp.CellState.ROOM);
        map.paint(3, 5, SecretRoomFinderApp.CellState.ROOM);
        map.paint(5, 7, SecretRoomFinderApp.CellState.SHOP);

        SecretRoomFinderApp.Candidate candidate = candidateAt(
                find(map, SecretRoomFinderApp.TargetRoom.ULTRA_SECRET), 5, 5);

        assertTrue(candidate.reason().contains("3 non-red room(s)"));
        assertTrue(candidate.reason().contains("3+ connection tier"));
        assertTrue(candidate.score() >= 13_225);
    }

    private static List<SecretRoomFinderApp.Candidate> find(SecretRoomFinderApp.FloorMap map, SecretRoomFinderApp.TargetRoom target) {
        return SecretRoomFinderApp.RoomFinder.find(map, target);
    }

    private static boolean hasCandidate(List<SecretRoomFinderApp.Candidate> candidates, int column, int row) {
        return candidates.stream().anyMatch(candidate -> candidate.column() == column && candidate.row() == row);
    }

    private static SecretRoomFinderApp.Candidate candidateAt(List<SecretRoomFinderApp.Candidate> candidates, int column, int row) {
        return candidates.stream().filter(candidate -> candidate.column() == column && candidate.row() == row).findFirst().orElseThrow();
    }
}

