package com.example.isaacfinder;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A deliberately small, dependency-free prototype.
 *
 * The placement rules live in RoomFinder so they can be replaced with a more
 * game-accurate implementation later without changing the user interface.
 */
public final class SecretRoomFinderApp {
    private static final int COLUMNS = 13;
    // Isaac floor layouts use a 13 by 13 map boundary.
    private static final int ROWS = 13;

    private final FloorMap floorMap = FloorMap.createSample(COLUMNS, ROWS);
    private final JButton[][] cells = new JButton[ROWS][COLUMNS];
    private final JList<Candidate> candidates = new JList<>();
    private final JLabel status = new JLabel("Choose a tile from the palette, then paint the completed map.");
    private TargetRoom target = TargetRoom.SECRET;
    private CellState paintState = CellState.ROOM;
    private Candidate selectedCandidate;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SecretRoomFinderApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("The Binding of Isaac — Secret Room Finder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(createContent());
        frame.pack();
        frame.setMinimumSize(new Dimension(900, 760));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                findCandidates();
            }
        });
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(35, 31, 29));

        JLabel heading = new JLabel("Secret Room Finder");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        heading.setForeground(new Color(244, 232, 209));

        JLabel subheading = new JLabel("Build the completed floor map, then inspect transparent placement candidates.");
        subheading.setForeground(new Color(205, 193, 178));
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        header.add(heading);
        header.add(subheading);
        root.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createMapPanel(), createControlPanel());
        split.setResizeWeight(0.68);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        status.setForeground(new Color(205, 193, 178));
        root.add(status, BorderLayout.SOUTH);
        return root;
    }

    private JPanel createMapPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Completed map"));
        panel.setBackground(new Color(48, 43, 39));

        JPanel grid = new JPanel(new GridLayout(ROWS, COLUMNS, 4, 4));
        grid.setBorder(new EmptyBorder(12, 12, 12, 12));
        grid.setBackground(new Color(48, 43, 39));
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int finalRow = row;
                int finalColumn = column;
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setFocusPainted(false);
                // Some Windows button themes ignore custom fills unless the
                // content area is explicitly painted and opaque.
                cell.setContentAreaFilled(true);
                cell.setOpaque(true);
                cell.setBorderPainted(true);
                cell.setMargin(new java.awt.Insets(0, 0, 0, 0));
                cell.addActionListener(event -> {
                    floorMap.paint(finalColumn, finalRow, paintState);
                    selectedCandidate = null;
                    refreshMap();
                    findCandidates();
                });
                cells[row][column] = cell;
                grid.add(cell);
            }
        }
        panel.add(grid, BorderLayout.CENTER);
        refreshMap();
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 12, 8, 0));
        panel.setBackground(new Color(48, 43, 39));

        JPanel top = new JPanel(new GridLayout(0, 1, 4, 4));
        top.setOpaque(false);
        top.add(label("Find a likely:"));
        ButtonGroup group = new ButtonGroup();
        for (TargetRoom value : TargetRoom.values()) {
            JRadioButton button = new JRadioButton(value.displayName());
            button.setOpaque(false);
            button.setForeground(new Color(244, 232, 209));
            button.setSelected(value == target);
            button.addActionListener(event -> {
                target = value;
                findCandidates();
            });
            group.add(button);
            top.add(button);
        }
        JButton find = new JButton("Find candidates");
        find.addActionListener(event -> findCandidates());
        top.add(find);
        top.add(label("Map palette:"));
        ButtonGroup palette = new ButtonGroup();
        for (CellState value : CellState.values()) {
            JRadioButton button = new JRadioButton(value.displayName());
            button.setOpaque(false);
            button.setForeground(new Color(244, 232, 209));
            button.setSelected(value == paintState);
            button.addActionListener(event -> paintState = value);
            palette.add(button);
            top.add(button);
        }
        panel.add(top, BorderLayout.NORTH);

        candidates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        candidates.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectedCandidate = candidates.getSelectedValue();
                refreshMap();
            }
        });
        JScrollPane scrollPane = new JScrollPane(candidates);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Ranked candidates"));
        panel.add(scrollPane, BorderLayout.CENTER);

        JLabel note = new JLabel("Select a palette tile, then click a map cell to paint it.\nSelect a result to inspect its map position.");
        note.setForeground(new Color(205, 193, 178));
        note.setVerticalAlignment(SwingConstants.TOP);
        panel.add(note, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(244, 232, 209));
        return label;
    }

    private void findCandidates() {
        List<Candidate> results = RoomFinder.find(floorMap, target);
        candidates.setListData(results.toArray(Candidate[]::new));
        if (results.isEmpty()) {
            selectedCandidate = null;
            status.setText("No eligible " + target.displayName().toLowerCase() + " candidates on this map.");
        } else {
            candidates.setSelectedIndex(0);
            selectedCandidate = results.get(0);
            status.setText(results.size() + " candidate(s). The top result is highlighted in gold.");
        }
        refreshMap();
    }

    private void refreshMap() {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                JButton cell = cells[row][column];
                CellState state = floorMap.at(column, row);
                boolean isSelected = selectedCandidate != null
                        && selectedCandidate.column() == column && selectedCandidate.row() == row;
                cell.setText(isSelected ? "?" : state.symbol());
                cell.setToolTipText("(" + column + ", " + row + "): " + state.displayName());
                cell.setBackground(isSelected ? new Color(218, 171, 54) : state.color());
                cell.setForeground(isSelected ? new Color(35, 31, 29) : new Color(244, 232, 209));
                cell.setBorder(BorderFactory.createLineBorder(isSelected ? new Color(255, 226, 126) : new Color(90, 82, 75)));
            }
        }
    }

    enum TargetRoom {
        SECRET("Secret Room"),
        SUPER_SECRET("Super Secret Room"),
        ULTRA_SECRET("Ultra Secret Room");

        private final String displayName;

        TargetRoom(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }

    enum CellState {
        EMPTY("Empty", "", new Color(65, 58, 52)),
        ROOM("Normal room", "■", new Color(112, 76, 61)),
        BOSS("Boss Room", "B", new Color(148, 67, 57)),
        SHOP("Shop", "$", new Color(95, 130, 94)),
        BLOCKED("Blocked", "×", new Color(39, 36, 34));

        private final String displayName;
        private final String symbol;
        private final Color color;

        CellState(String displayName, String symbol, Color color) {
            this.displayName = displayName;
            this.symbol = symbol;
            this.color = color;
        }

        String displayName() { return displayName; }
        String symbol() { return symbol; }
        Color color() { return color; }
        boolean isRoom() { return this == ROOM || this == BOSS || this == SHOP; }
    }

    static final class FloorMap {
        private final CellState[][] states;

        FloorMap(int columns, int rows) {
            states = new CellState[rows][columns];
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    states[row][column] = CellState.EMPTY;
                }
            }
        }

        static FloorMap createSample(int columns, int rows) {
            FloorMap map = new FloorMap(columns, rows);
            int[][] rooms = {{6, 4}, {5, 4}, {4, 4}, {3, 4}, {6, 3}, {6, 2}, {7, 4}, {8, 4}, {8, 5}, {5, 5}, {4, 5}, {4, 6}, {3, 5}};
            for (int[] room : rooms) map.states[room[1]][room[0]] = CellState.ROOM;
            map.states[5][3] = CellState.BOSS;
            map.states[5][8] = CellState.SHOP;
            return map;
        }

        CellState at(int column, int row) { return states[row][column]; }
        int columns() { return states[0].length; }
        int rows() { return states.length; }

        void paint(int column, int row, CellState state) {
            states[row][column] = state;
        }
    }

    record Candidate(int column, int row, int score, String reason) {
        @Override
        public String toString() {
            return "(" + column + ", " + row + ")  score " + score + " — " + reason;
        }
    }

    static final class RoomFinder {
        private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

        static List<Candidate> find(FloorMap map, TargetRoom target) {
            List<Candidate> results = new ArrayList<>();
            for (int row = 0; row < map.rows(); row++) {
                for (int column = 0; column < map.columns(); column++) {
                    if (map.at(column, row) != CellState.EMPTY) {
                        continue;
                    }
                    int adjacentRooms = adjacentRooms(map, column, row);
                    int adjacentBossRooms = adjacentRoomsOfType(map, column, row, CellState.BOSS);
                    int openSides = openSides(map, column, row);
                    Candidate candidate = evaluate(map, target, column, row, adjacentRooms, adjacentBossRooms, openSides);
                    if (candidate != null) {
                        results.add(candidate);
                    }
                }
            }
            results.sort(Comparator.comparingInt(Candidate::score).reversed()
                    .thenComparingInt(Candidate::row).thenComparingInt(Candidate::column));
            return results;
        }

        private static Candidate evaluate(FloorMap map, TargetRoom target, int column, int row,
                                          int adjacentRooms, int adjacentBossRooms, int openSides) {
            return switch (target) {
                // The Boss Room has one normal-room entrance. Secret variants cannot occupy its other sides.
                case SECRET -> adjacentBossRooms == 0 && adjacentRooms >= 2
                        ? new Candidate(column, row, adjacentRooms * 10 + openSides,
                        "touches " + adjacentRooms + " rooms; " + openSides + " open side(s)")
                        : null;
                case SUPER_SECRET -> adjacentBossRooms == 0
                        ? superSecretCandidate(map, column, row, adjacentRooms, openSides)
                        : null;
                case ULTRA_SECRET -> ultraSecretCandidate(map, column, row, adjacentRooms);
            };
        }

        /**
         * Super Secret Rooms are placed early, after Boss but before Shop.  A
         * one-room slot keeps the usual dead-end shape; distance bonuses make
         * late-map positions near Boss/Shop appear first without hiding other
         * structurally valid possibilities.
         */
        private static Candidate superSecretCandidate(FloorMap map, int column, int row,
                                                       int adjacentRooms, int openSides) {
            if (adjacentRooms != 1) return null;

            int bossDistance = closestDistance(map, column, row, CellState.BOSS);
            int shopDistance = closestDistance(map, column, row, CellState.SHOP);
            int bossShopDistance = distanceBetween(map, CellState.BOSS, CellState.SHOP);
            int bossBonus = bossDistance < 0 ? 0 : Math.max(0, 20 - 2 * bossDistance);
            int shopBonus = shopDistance < 0 ? 0 : Math.max(0, 12 - shopDistance);
            boolean betweenBossAndShop = bossDistance >= 0 && shopDistance >= 0
                    && bossDistance + shopDistance <= bossShopDistance + 2;
            int betweenBonus = betweenBossAndShop ? 8 : 0;
            int score = 10 + openSides + bossBonus + shopBonus + betweenBonus;

            String reason = "touches exactly 1 room; " + openSides + " open side(s)";
            if (bossDistance >= 0) reason += "; Boss distance " + bossDistance + " (bonus " + bossBonus + ")";
            if (shopDistance >= 0) reason += "; Shop distance " + shopDistance + " (bonus " + shopBonus + ")";
            if (betweenBossAndShop) reason += "; near the Boss–Shop path (bonus 8)";
            return new Candidate(column, row, score, reason);
        }

        /**
         * An Ultra Secret Room is not directly connected to a normal room.
         * Instead, each empty adjacent cell is treated as a possible Red Room
         * bridge. We count the distinct non-red map tiles reachable through
         * those bridges; separate painted cells also represent separate parts
         * of an L-shaped room.
         */
        private static Candidate ultraSecretCandidate(FloorMap map, int column, int row, int adjacentRooms) {
            if (adjacentRooms != 0) return null;

            Set<String> connectedRooms = new HashSet<>();
            for (int[] direction : DIRECTIONS) {
                int bridgeColumn = column + direction[0];
                int bridgeRow = row + direction[1];
                if (!inside(map, bridgeColumn, bridgeRow) || map.at(bridgeColumn, bridgeRow) != CellState.EMPTY) {
                    continue;
                }
                addRoomsBeyondBridge(map, column, row, bridgeColumn, bridgeRow, connectedRooms);
            }

            int connections = connectedRooms.size();
            if (connections == 0) return null;

            int likelihoodWeight;
            String tier;
            if (connections >= 3) {
                likelihoodWeight = 13_225;
                tier = "3+ connection tier (11.5× a 2-room location)";
            } else if (connections == 2) {
                likelihoodWeight = 1_150;
                tier = "2 connection tier (11.5× a 1-room location)";
            } else {
                likelihoodWeight = 100;
                tier = "1 connection tier";
            }
            return new Candidate(column, row, likelihoodWeight + connections,
                    "reached through Red Room bridge(s) from " + connections + " non-red room(s); " + tier);
        }

        private static void addRoomsBeyondBridge(FloorMap map, int ultraColumn, int ultraRow,
                                                  int bridgeColumn, int bridgeRow, Set<String> connectedRooms) {
            for (int[] direction : DIRECTIONS) {
                int roomColumn = bridgeColumn + direction[0];
                int roomRow = bridgeRow + direction[1];
                if (roomColumn == ultraColumn && roomRow == ultraRow) continue;
                if (inside(map, roomColumn, roomRow) && map.at(roomColumn, roomRow).isRoom()) {
                    connectedRooms.add(roomColumn + ":" + roomRow);
                }
            }
        }

        private static int adjacentRooms(FloorMap map, int column, int row) {
            int count = 0;
            for (int[] direction : DIRECTIONS) {
                int x = column + direction[0];
                int y = row + direction[1];
                if (inside(map, x, y) && map.at(x, y).isRoom()) {
                    count++;
                }
            }
            return count;
        }

        private static int adjacentRoomsOfType(FloorMap map, int column, int row, CellState roomType) {
            int count = 0;
            for (int[] direction : DIRECTIONS) {
                int x = column + direction[0];
                int y = row + direction[1];
                if (inside(map, x, y) && map.at(x, y) == roomType) count++;
            }
            return count;
        }

        private static int openSides(FloorMap map, int column, int row) {
            int count = 0;
            for (int[] direction : DIRECTIONS) {
                int x = column + direction[0];
                int y = row + direction[1];
                if (!inside(map, x, y) || map.at(x, y) == CellState.EMPTY) {
                    count++;
                }
            }
            return count;
        }

        private static boolean inside(FloorMap map, int column, int row) {
            return column >= 0 && column < map.columns() && row >= 0 && row < map.rows();
        }

        private static int closestDistance(FloorMap map, int column, int row, CellState target) {
            int closest = Integer.MAX_VALUE;
            for (int y = 0; y < map.rows(); y++) {
                for (int x = 0; x < map.columns(); x++) {
                    if (map.at(x, y) == target) closest = Math.min(closest, manhattan(column, row, x, y));
                }
            }
            return closest == Integer.MAX_VALUE ? -1 : closest;
        }

        private static int distanceBetween(FloorMap map, CellState first, CellState second) {
            int closest = Integer.MAX_VALUE;
            for (int y = 0; y < map.rows(); y++) for (int x = 0; x < map.columns(); x++) {
                if (map.at(x, y) != first) continue;
                int distance = closestDistance(map, x, y, second);
                if (distance >= 0) closest = Math.min(closest, distance);
            }
            return closest == Integer.MAX_VALUE ? -1 : closest;
        }

        private static int manhattan(int x1, int y1, int x2, int y2) {
            return Math.abs(x1 - x2) + Math.abs(y1 - y2);
        }
    }
}

