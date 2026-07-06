import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    static class Drone {
        String id;
        double x, y, z;
        double vx, vy, vz;
        double safetyRadius;

        public Drone(String id, double x, double y, double z, double vx, double vy, double vz, double safetyRadius) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.safetyRadius = safetyRadius;
        }

        public void updatePosition(double timeStep) {
            this.x += this.vx * timeStep;
            this.y += this.vy * timeStep;
            this.z += this.vz * timeStep;
        }
    }

    static class KDNode {
        Drone drone;
        KDNode left, right;
        int axis;

        public KDNode(Drone drone, int axis) {
            this.drone = drone;
            this.axis = axis;
        }
    }

    private KDNode root;

    public void insert(Drone drone) {
        root = insertRec(root, drone, 0);
    }

    private KDNode insertRec(KDNode root, Drone drone, int depth) {
        if (root == null) {
            return new KDNode(drone, depth % 3);
        }

        int axis = root.axis;
        double currentCoord = getCoord(drone, axis);
        double nodeCoord = getCoord(root.drone, axis);

        if (currentCoord < nodeCoord) {
            root.left = insertRec(root.left, drone, depth + 1);
        } else {
            root.right = insertRec(root.right, drone, depth + 1);
        }

        return root;
    }

    public List<Drone> findNeighbors3D(double x, double y, double z, double range) {
        List<Drone> neighbors = new ArrayList<>();
        searchRec(root, x, y, z, range, neighbors);
        return neighbors;
    }

    private void searchRec(KDNode node, double tx, double ty, double tz, double range, List<Drone> result) {
        if (node == null) return;

        double distance = Math.sqrt(Math.pow(node.drone.x - tx, 2) + Math.pow(node.drone.y - ty, 2) + Math.pow(node.drone.z - tz, 2));
        if (distance <= range) {
            result.add(node.drone);
        }

        int axis = node.axis;
        double nodeCoord = getCoord(node.drone, axis);
        double targetCoord = (axis == 0) ? tx : (axis == 1) ? ty : tz;

        if (targetCoord - range < nodeCoord) {
            searchRec(node.left, tx, ty, tz, range, result);
        }
        if (targetCoord + range > nodeCoord) {
            searchRec(node.right, tx, ty, tz, range, result);
        }
    }

    private double getCoord(Drone drone, int axis) {
        if (axis == 0) return drone.x;
        if (axis == 1) return drone.y;
        return drone.z;
    }

    public static void main(String[] args) {
        Main airspace = new Main();
        Scanner scanner = new Scanner(System.in);
        List<Drone> initialDronesList = new ArrayList<>();

        System.out.println("==================================================");
        System.out.println("   🛸 AIRSPACE INITIALIZATION WIZARD 🛸          ");
        System.out.println("==================================================");
        
        int droneCounter = 1;
        while (true) {
            System.out.println("\n--- Adding Drone #" + droneCounter + " ---");
            System.out.print("Enter Drone ID name (or type '-1' to stop adding drones): ");
            String id = scanner.next();
            if (id.equals("-1")) {
                break;
            }

            System.out.print("Enter Initial X coordinate: ");
            double x = scanner.nextDouble();
            System.out.print("Enter Initial Y coordinate: ");
            double y = scanner.nextDouble();
            System.out.print("Enter Initial Z coordinate (Altitude): ");
            double z = scanner.nextDouble();

            System.out.print("Enter Velocity vector VX (m/s): ");
            double vx = scanner.nextDouble();
            System.out.print("Enter Velocity vector VY (m/s): ");
            double vy = scanner.nextDouble();
            System.out.print("Enter Velocity vector VZ (m/s): ");
            double vz = scanner.nextDouble();

            System.out.print("Enter Drone Safety Bubble Radius (meters): ");
            double radius = scanner.nextDouble();

            initialDronesList.add(new Drone(id, x, y, z, vx, vy, vz, radius));
            droneCounter++;
        }

        if (initialDronesList.isEmpty()) {
            System.out.println("[!] No drones added. Terminating radar module.");
            scanner.close();
            return;
        }

        System.out.println("\n==================================================");
        System.out.println("   ⏰ TIME-STEP SIMULATION CLOCK ⏰               ");
        System.out.println("==================================================");
        System.out.print("Enter simulation time step increment (seconds): ");
        double timeStep = scanner.nextDouble();

        System.out.println("\n[Action] Updating drone vectors by " + timeStep + "s...");
        for (Drone d : initialDronesList) {
            d.updatePosition(timeStep);
            airspace.insert(d);
            System.out.printf(" -> %s moved to updated coordinates: (X: %.1f, Y: %.1f, Z: %.1f)\n", d.id, d.x, d.y, d.z);
        }
        System.out.println("[Status] 3D KD-Tree Partitioning Index constructed successfully.\n");

        System.out.println("==================================================");
        System.out.println("   🎯 INTRUDER RADAR SCAN CONSOLE 🎯              ");
        System.out.println("==================================================");
        System.out.println("Enter details for the rogue intruder vehicle to scan airspace:");
        System.out.print("Enter Target X: ");
        double tx = scanner.nextDouble();
        System.out.print("Enter Target Y: ");
        double ty = scanner.nextDouble();
        System.out.print("Enter Target Z (Altitude): ");
        double tz = scanner.nextDouble();
        System.out.print("Enter Target Safety Radius: ");
        double tRadius = scanner.nextDouble();

        long startTime = System.nanoTime();
        List<Drone> threats = airspace.findNeighbors3D(tx, ty, tz, tRadius + 5.0);
        long endTime = System.nanoTime();

        System.out.println("\n=========================================");
        System.out.println("        3D RADAR ALGO SCAN ALERTS        ");
        System.out.println("=========================================");
        System.out.printf("Search Latency: %.4f ms\n", (endTime - startTime) / 1000000.0);
        System.out.println("-----------------------------------------");

        if (threats.isEmpty()) {
            System.out.println("[✨ CLEAR AIRSPACE]: No dynamic coordinates overlap in your custom environment.");
        } else {
            boolean collision = false;
            for (Drone potential : threats) {
                double dist = Math.sqrt(Math.pow(potential.x - tx, 2) + Math.pow(potential.y - ty, 2) + Math.pow(potential.z - tz, 2));
                double limit = tRadius + potential.safetyRadius;

                if (dist < limit) {
                    System.out.println("[⚠️ COLLISION THREAT DETECTED]:");
                    System.out.printf(" -> Object Impact Profile: %s\n", potential.id);
                    System.out.printf(" -> Intercept Position: (X: %.1f, Y: %.1f, Z: %.1f)\n", potential.x, potential.y, potential.z);
                    System.out.printf(" -> Calculated Separation: %.2fm (Required Safe Buffer: %.2fm)\n\n", dist, limit);
                    collision = true;
                }
            }
            if (!collision) {
                System.out.println("[✨ SAFE PROXIMITY]: Objects identified nearby, but safe spatial indexing margins are secure.");
            }
        }

        scanner.close();
    }
}