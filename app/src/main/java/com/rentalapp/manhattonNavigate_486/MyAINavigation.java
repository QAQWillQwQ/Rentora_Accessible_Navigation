package com.rentalapp.manhattonNavigate_486;


import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

// Handles AI-based pathfinding using A* and Hill Climbing.
// Supports different modes for normal, wheelchair, and blind users.
public class MyAINavigation {

    public enum Mode {
        NORMAL, WHEELCHAIR, BLIND
    }

    public static class Node {
        LatLng position;
        double gCost;  // cost from start
        double hCost;  // heuristic to goal
        double fCost;
        Node parent;

        public Node(LatLng pos) {
            this.position = pos;
        }

        public void computeFCost() {
            fCost = gCost + hCost;
        }
    }

    /**
     * Runs the A* search algorithm between two points using all nearby nodes.
     * It finds the shortest path considering both actual distance and estimated distance to the goal.
     *
     * @param start The starting location.
     * @param goal The destination.
     * @param allNodes All the points that can be visited in the map area.
     * @param mode The user type (normal, blind, wheelchair), which can affect future logic.
     * @return A list of LatLng points that make up the best path found.
     */
    public static List<LatLng> aStarSearch(LatLng start, LatLng goal, List<LatLng> allNodes, Mode mode) {
        Map<LatLng, Node> nodes = new HashMap<>();
        for (LatLng p : allNodes) {
            nodes.put(p, new Node(p));
        }

        Node startNode = nodes.get(start);
        startNode.gCost = 0;
        startNode.hCost = haversine(start, goal);
        startNode.computeFCost();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<Node> closedSet = new HashSet<>();
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            closedSet.add(current);

            if (current.position.equals(goal)) {
                return reconstructPath(current);
            }

            for (LatLng neighborPos : getNeighbors(current.position, allNodes)) {
                Node neighbor = nodes.get(neighborPos);
                if (closedSet.contains(neighbor)) continue;

                double tentativeG = current.gCost + haversine(current.position, neighbor.position);

                if (!openSet.contains(neighbor) || tentativeG < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeG;
                    neighbor.hCost = haversine(neighbor.position, goal);
                    neighbor.computeFCost();
                    if (!openSet.contains(neighbor)) openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>(); // no path found
    }

    /**
     * Finds a path using a hill climbing approach. Always chooses the next closest point to the goal.
     * It’s simpler and faster than A*, but it can get stuck if it can't find a better neighbor.
     *
     * @param start The starting point.
     * @param goal The ending point.
     * @param allNodes The list of known points in the area.
     * @return A list of LatLng that shows the path found using hill climbing.
     */
    public static List<LatLng> hillClimb(LatLng start, LatLng goal, List<LatLng> allNodes) {
        List<LatLng> path = new ArrayList<>();
        LatLng current = start;
        path.add(current);

        while (!current.equals(goal)) {
            List<LatLng> neighbors = getNeighbors(current, allNodes);
            LatLng next = null;
            double minH = Double.MAX_VALUE;

            for (LatLng neighbor : neighbors) {
                double h = haversine(neighbor, goal);
                if (h < minH) {
                    minH = h;
                    next = neighbor;
                }
            }

            if (next == null || path.contains(next)) break;

            path.add(next);
            current = next;
        }

        return path;
    }

    /**
     * Finds nearby points that are close enough (within 100 meters) to be considered connected.
     *
     * @param node The current point.
     * @param all A list of all points to compare with.
     * @return A list of nearby points that are valid neighbors.
     */
    private static List<LatLng> getNeighbors(LatLng node, List<LatLng> all) {
        List<LatLng> result = new ArrayList<>();
        for (LatLng other : all) {
            if (!node.equals(other) && haversine(node, other) < 0.1) { // ~100 meters
                result.add(other);
            }
        }
        return result;
    }

    /**
     * Builds the full path by following back from the goal to the start using parent pointers.
     *
     * @param node The ending node of the path.
     * @return The full path in the correct order from start to goal.
     */
    private static List<LatLng> reconstructPath(Node node) {
        List<LatLng> path = new LinkedList<>();
        while (node != null) {
            path.add(0, node.position);
            node = node.parent;
        }
        return path;
    }

    /**
     * Calculates the distance between two points using the Haversine formula.
     * This gives a realistic curve-based distance over the Earth’s surface.
     *
     * @param a First point.
     * @param b Second point.
     * @return Distance in kilometers.
     */
    private static double haversine(LatLng a, LatLng b) {
        double R = 6371; // Radius of earth in km
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);

        double aVal = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(aVal), Math.sqrt(1 - aVal));
        return R * c;
    }

    /**
     * Combines A* and hill climbing to generate multiple path options for the user.
     * Reads business site locations from Firestore as extra path nodes.
     *
     * @param origin Starting location.
     * @param destination Ending location.
     * @param mode User type (wheelchair/blind/normal) for future scoring.
     * @param callback What to do when all paths are ready.
     */
    public static void generatePathsUsingAI(LatLng origin, LatLng destination, Mode mode, PathCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<LatLng> knownPoints = new ArrayList<>();
        knownPoints.add(origin);
        knownPoints.add(destination);

        db.collection("Business Site_csv").get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            Double lat = doc.getDouble("Latitude");
                            Double lon = doc.getDouble("Longitude");
                            if (lat != null && lon != null) {
                                LatLng point = new LatLng(lat, lon);
                                if (!knownPoints.contains(point)) {
                                    knownPoints.add(point);
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    List<LatLng> bestPath = aStarSearch(origin, destination, knownPoints, mode);
                    List<LatLng> mediumPath = hillClimb(origin, destination, knownPoints);

                    List<LatLng> dummy = new ArrayList<>();
                    dummy.add(origin);
                    dummy.add(new LatLng(origin.latitude + 0.001, origin.longitude + 0.001));
                    dummy.add(destination);

                    List<List<LatLng>> all = new ArrayList<>();
                    callback.onPathsReady(all);

                })
                .addOnFailureListener(e -> {
                    callback.onPathsReady(Collections.emptyList());
                });
    }

    /**
     * This is used to return multiple paths once they're ready.
     * It helps keep the method asynchronous.
     */
    public interface PathCallback {
        void onPathsReady(List<List<LatLng>> paths);
    }

}
