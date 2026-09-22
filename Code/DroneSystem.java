import java.util.*;

class Drone {
    String id;
    double x, y, z;
    double vx, vy, vz;
    double radius = 10.0;  // Collision boundary

    public Drone(String id, double x, double y, double z, double vx, double vy, double vz) {
        this.id = id;
        this.x = x; this.y = y; this.z = z;
        this.vx = vx; this.vy = vy; this.vz = vz;
    }

    public void updateTrajectory(double dt) {
        x += vx * dt;
        y += vy * dt;
        z += vz * dt;
    }

    public double distance3D(Drone other) {
        return Math.sqrt(Math.pow(x - other.x, 2) +
                         Math.pow(y - other.y, 2) +
                         Math.pow(z - other.z, 2));
    }

    @Override
    public String toString() {
        return String.format("%s at [%.1f, %.1f, %.1f]", id, x, y, z);
    }
}

class NoFlyZone {
    String id;
    double cx, cy, cz, radius;

    public NoFlyZone(String id, double cx, double cy, double cz, double radius) {
        this.id = id;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.radius = radius;
    }

    public boolean violates(Drone d) {
        double dist = Math.sqrt(Math.pow(cx - d.x, 2) +
                                Math.pow(cy - d.y, 2) +
                                Math.pow(cz - d.z, 2));
        return dist <= radius;
    }
}

// ==========================================
// ALGORITHM 1: 3D KD-TREE (Nearest Neighbor)
// ==========================================

class KDNode {
    Drone drone;
    KDNode left, right;
    int axis; // 0=X, 1=Y, 2=Z

    public KDNode(Drone drone, int axis) {
        this.drone = drone;
        this.axis = axis;
    }
}

class KDTree {
    private KDNode root;
    private Drone bestNode;
    private double bestDist;

    public void build(List<Drone> drones) {
        this.root = buildRec(new ArrayList<>(drones), 0);
    }

    private KDNode buildRec(List<Drone> drones, int depth) {
        if (drones.isEmpty()) return null;

        int axis = depth % 3;

        drones.sort(Comparator.comparingDouble(d -> getAxisVal(d, axis)));

        int mid = drones.size() / 2;

        KDNode node = new KDNode(drones.get(mid), axis);

        node.left = buildRec(drones.subList(0, mid), depth + 1);
        node.right = buildRec(drones.subList(mid + 1, drones.size()), depth + 1);

        return node;
    }

    public Drone getNearestNeighbor(double tx, double ty, double tz) {
        bestNode = null;
        bestDist = Double.MAX_VALUE;

        searchRec(root, tx, ty, tz);

        return bestNode;
    }

    private void searchRec(KDNode node, double tx, double ty, double tz) {
        if (node == null) return;

        double d = Math.sqrt(
                Math.pow(node.drone.x - tx, 2) +
                Math.pow(node.drone.y - ty, 2) +
                Math.pow(node.drone.z - tz, 2)
        );

        if (d < bestDist) {
            bestDist = d;
            bestNode = node.drone;
        }

        double targetAxisVal = getAxisVal(tx, ty, tz, node.axis);
        double nodeAxisVal = getAxisVal(node.drone, node.axis);

        KDNode next = (targetAxisVal < nodeAxisVal)
                ? node.left
                : node.right;

        KDNode other = (targetAxisVal < nodeAxisVal)
                ? node.right
                : node.left;

        searchRec(next, tx, ty, tz);

        if (Math.abs(targetAxisVal - nodeAxisVal) < bestDist) {
            searchRec(other, tx, ty, tz);
        }
    }

    private double getAxisVal(Drone d, int axis) {
        return axis == 0 ? d.x : (axis == 1 ? d.y : d.z);
    }

    private double getAxisVal(double x, double y, double z, int axis) {
        return axis == 0 ? x : (axis == 1 ? y : z);
    }
}

// ==========================================
// ALGORITHM 2: 2D QUAD TREE (Point Location & Range)
// ==========================================

class Boundary {
    double x, y, width, height; // x,y is center

    public Boundary(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    public boolean contains(Drone d) {
        return (d.x >= x - width &&
                d.x <= x + width &&
                d.y >= y - height &&
                d.y <= y + height);
    }

    public boolean intersects(Boundary range) {
        return !(range.x - range.width > x + width ||
                 range.x + range.width < x - width ||
                 range.y - range.height > y + height ||
                 range.y + range.height < y - height);
    }
}

class QuadTree {
    private Boundary boundary;
    private int capacity;
    private List<Drone> drones;
    private boolean divided;
    private QuadTree nw, ne, sw, se;

    public QuadTree(Boundary boundary, int capacity) {
        this.boundary = boundary;
        this.capacity = capacity;
        this.drones = new ArrayList<>();
        this.divided = false;
    }

    public boolean insert(Drone d) {
        if (!boundary.contains(d)) return false;

        if (drones.size() < capacity) {
            drones.add(d);
            return true;
        }

        if (!divided) subdivide();

        return nw.insert(d) ||
               ne.insert(d) ||
               sw.insert(d) ||
               se.insert(d);
    }

    private void subdivide() {
        double x = boundary.x;
        double y = boundary.y;
        double w = boundary.width / 2;
        double h = boundary.height / 2;

        ne = new QuadTree(new Boundary(x + w, y + h, w, h), capacity);
        nw = new QuadTree(new Boundary(x - w, y + h, w, h), capacity);
        se = new QuadTree(new Boundary(x + w, y - h, w, h), capacity);
        sw = new QuadTree(new Boundary(x - w, y - h, w, h), capacity);

        divided = true;
    }

    public List<Drone> query2DRange(Boundary range, List<Drone> found) {
        if (!boundary.intersects(range)) return found;

        for (Drone d : drones) {
            if (range.contains(d)) {
                found.add(d);
            }
        }

        if (divided) {
            nw.query2DRange(range, found);
            ne.query2DRange(range, found);
            sw.query2DRange(range, found);
            se.query2DRange(range, found);
        }

        return found;
    }
}

// ==========================================
// ALGORITHM 3: SWEEP LINE (2D Broad-Phase Collisions)
// ==========================================

class SweepLineAlgorithm {

    public static void detectCollisions(List<Drone> drones) {
        if (drones.size() < 2) return;

        drones.sort(Comparator.comparingDouble(d -> (d.x - d.radius)));

        List<Drone> activeList = new ArrayList<>();

        int potentialCollisions = 0;

        for (Drone current : drones) {

            activeList.removeIf(d ->
                    (d.x + d.radius) < (current.x - current.radius));

            for (Drone active : activeList) {

                boolean overlapY =
                        !((active.y + active.radius) <
                          (current.y - current.radius) ||
                          (active.y - active.radius) >
                          (current.y + current.radius));

                if (overlapY) {

                    double dist = current.distance3D(active);

                    if (dist <= (current.radius + active.radius)) {

                        System.out.printf(
                                "🚨 [COLLISION ALERT via Sweep Line] %s and %s are critically close (%.2fm)!\n",
                                current.id,
                                active.id,
                                dist
                        );

                        potentialCollisions++;
                    }
                }
            }

            activeList.add(current);
        }

        if (potentialCollisions == 0) {
            System.out.println(
                    "✅ Sweep Line Check: Airspace is clear of collisions."
            );
        }
    }
}

// ==========================================
// MAIN DRONE SYSTEM
// ==========================================

public class DroneSystem {

    private static Map<String, Drone> droneMap = new HashMap<>();
    private static List<NoFlyZone> zones = new ArrayList<>();

    // Added: Simulation time
    private static double currentTime = 0.0;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        zones.add(new NoFlyZone(
                "Airport_Runway",
                0,
                0,
                0,
                50
        ));

        System.out.println("==================================================");
        System.out.println("✈️ REAL-TIME DRONE AIRSPACE MONITORING SYSTEM ✈️");
        System.out.println("==================================================");

        while (true) {

            System.out.println("\n--- MAIN MENU ---");

            // Added: Display current simulation time
            System.out.printf(
                    "Current Simulation Time: %.2f seconds%n",
                    currentTime
            );

            System.out.println("1. Add Drone (Dynamic Insertion)");
            System.out.println("2. Remove Drone (Dynamic Deletion)");
            System.out.println("3. Advance Time / Update Trajectories");
            System.out.println("4. Run Sweep Line Collision Detection (2D/3D)");
            System.out.println("5. Query Nearest Neighbor (KD-Tree)");
            System.out.println("6. Query 2D Point Location / Range (Quad Tree)");
            System.out.println("7. View All Drones");
            System.out.println("8. Exit");

            System.out.print("Select an option: ");

            String choice = scanner.nextLine();

            try {

                switch (choice) {

                    // ==========================================
                    // 1. ADD DRONE
                    // ==========================================

                    case "1":

                        System.out.print(
                                "Enter ID, x, y, z, vx, vy, vz (space-separated): "
                        );

                        String[] inputs =
                                scanner.nextLine().split(" ");

                        Drone d = new Drone(
                                inputs[0],
                                Double.parseDouble(inputs[1]),
                                Double.parseDouble(inputs[2]),
                                Double.parseDouble(inputs[3]),
                                Double.parseDouble(inputs[4]),
                                Double.parseDouble(inputs[5]),
                                Double.parseDouble(inputs[6])
                        );

                        droneMap.put(d.id, d);

                        System.out.println(
                                "✅ Drone dynamically inserted."
                        );

                        break;

                    // ==========================================
                    // 2. REMOVE DRONE
                    // ==========================================

                    case "2":

                        System.out.print(
                                "Enter Drone ID to remove: "
                        );

                        String id = scanner.nextLine();

                        if (droneMap.remove(id) != null) {

                            System.out.println(
                                    "✅ Drone deleted."
                            );

                        } else {

                            System.out.println(
                                    "❌ Drone not found."
                            );
                        }

                        break;

                    // ==========================================
                    // 3. ADVANCE TIME / UPDATE TRAJECTORIES
                    // ==========================================

                    case "3":

                        System.out.print(
                                "Enter time step (dt in seconds): "
                        );

                        double dt =
                                Double.parseDouble(scanner.nextLine());

                        // Added: Advance simulation clock
                        currentTime += dt;

                        for (Drone drone : droneMap.values()) {

                            drone.updateTrajectory(dt);

                            for (NoFlyZone zone : zones) {

                                if (zone.violates(drone)) {

                                    System.out.println(
                                            "⚠️ NO-FLY ZONE VIOLATION: "
                                                    + drone.id
                                    );
                                }
                            }
                        }

                        System.out.printf(
                                "✅ Trajectories updated. Current simulation time: %.2f seconds.%n",
                                currentTime
                        );

                        break;

                    // ==========================================
                    // 4. SWEEP LINE COLLISION DETECTION
                    // ==========================================

                    case "4":

                        System.out.println(
                                "\nExecuting Sweep Line Collision Algorithm..."
                        );

                        SweepLineAlgorithm.detectCollisions(
                                new ArrayList<>(droneMap.values())
                        );

                        break;

                    // ==========================================
                    // 5. KD-TREE NEAREST NEIGHBOR
                    // ==========================================

                    case "5":

                        System.out.print(
                                "Enter target coordinates X Y Z (space-separated): "
                        );

                        String[] coords =
                                scanner.nextLine().split(" ");

                        KDTree kdTree = new KDTree();

                        kdTree.build(
                                new ArrayList<>(droneMap.values())
                        );

                        Drone nearest =
                                kdTree.getNearestNeighbor(
                                        Double.parseDouble(coords[0]),
                                        Double.parseDouble(coords[1]),
                                        Double.parseDouble(coords[2])
                                );

                        if (nearest != null) {

                            System.out.println(
                                    "🎯 Nearest Drone (KD-Tree): "
                                            + nearest
                            );

                        } else {

                            System.out.println(
                                    "No drones in airspace."
                            );
                        }

                        break;

                    // ==========================================
                    // 6. QUAD TREE RANGE QUERY
                    // ==========================================

                    case "6":

                        System.out.print(
                                "Enter search center X Y and Area Radius (space-separated): "
                        );

                        String[] qCoords =
                                scanner.nextLine().split(" ");

                        Boundary screenBounds =
                                new Boundary(
                                        0,
                                        0,
                                        10000,
                                        10000
                                );

                        QuadTree qt =
                                new QuadTree(
                                        screenBounds,
                                        4
                                );

                        for (Drone dr : droneMap.values()) {
                            qt.insert(dr);
                        }

                        Boundary searchBox =
                                new Boundary(
                                        Double.parseDouble(qCoords[0]),
                                        Double.parseDouble(qCoords[1]),
                                        Double.parseDouble(qCoords[2]),
                                        Double.parseDouble(qCoords[2])
                                );

                        List<Drone> found =
                                qt.query2DRange(
                                        searchBox,
                                        new ArrayList<>()
                                );

                        System.out.println(
                                "📍 Drones found in area (Quad Tree): "
                                        + found.size()
                        );

                        for (Drone fd : found) {

                            System.out.println(
                                    "  - " + fd.id
                            );
                        }

                        break;

                    // ==========================================
                    // 7. VIEW ALL DRONES
                    // ==========================================

                    case "7":

                        droneMap.values().forEach(
                                System.out::println
                        );

                        break;

                    // ==========================================
                    // 8. EXIT
                    // ==========================================

                    case "8":

                        System.out.println(
                                "System shutting down..."
                        );

                        scanner.close();

                        System.exit(0);

                        break;

                    default:

                        System.out.println(
                                "❌ Invalid choice. Try again."
                        );
                }

            } catch (Exception e) {

                System.out.println(
                        "❌ Input error. Please use correct formatting."
                );
            }
        }
    }
}