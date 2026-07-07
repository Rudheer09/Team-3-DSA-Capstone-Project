import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.*;

public class GUI {

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

    static class Radar3DVisualizer extends JPanel {
        private final List<Drone> drones;
        private final double tx, ty, tz, tRadius;
        private double angleX = 20;
        private double angleY = -45;
        private double scale = 2.0;
        private Point mousePt;

        public Radar3DVisualizer(List<Drone> drones, double tx, double ty, double tz, double tRadius) {
            this.drones = drones;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.tRadius = tRadius;

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    mousePt = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    int dx = e.getX() - mousePt.x;
                    int dy = e.getY() - mousePt.y;
                    angleY += dx * 0.5;
                    angleX += dy * 0.5;
                    mousePt = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getWheelRotation() < 0) {
                        scale *= 1.1;
                    } else {
                        scale /= 1.1;
                    }
                    repaint();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
            addMouseWheelListener(mouseHandler);
        }

        public static void createAndShowGUI(List<Drone> drones, double tx, double ty, double tz, double tRadius) {
            JFrame frame = new JFrame("🛸 3D AIRSPACE COGNITIVE RADAR DISPLAY");
            Radar3DVisualizer visualizer = new Radar3DVisualizer(drones, tx, ty, tz, tRadius);
            frame.add(visualizer);
            frame.setSize(1024, 768);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        private Point project(double x, double y, double z) {
            double radX = Math.toRadians(angleX);
            double radY = Math.toRadians(angleY);

            // Y-Axis Rotation
            double x1 = x * Math.cos(radY) + z * Math.sin(radY);
            double z1 = -x * Math.sin(radY) + z * Math.cos(radY);

            // X-Axis Rotation
            double y2 = y * Math.cos(radX) - z1 * Math.sin(radX);
            double z2 = y * Math.sin(radX) + z1 * Math.cos(radX);

            // Perspective factor based on transformed depth coordinates
            double distance = 1000.0;
            double perspective = distance / (distance + z2);

            int sx = (getWidth() / 2) + (int) (x1 * scale * perspective);
            int sy = (getHeight() / 2) - (int) (y2 * scale * perspective);
            return new Point(sx, sy);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setColor(new Color(0, 80, 0, 100));
            Point origin = project(0, 0, 0);
            Point xAxis = project(200, 0, 0);
            Point yAxis = project(0, 200, 0);
            Point zAxis = project(0, 0, 200);
            g2d.drawLine(origin.x, origin.y, xAxis.x, xAxis.y);
            g2d.drawLine(origin.x, origin.y, yAxis.x, yAxis.y);
            g2d.drawLine(origin.x, origin.y, zAxis.x, zAxis.y);

            for (Drone d : drones) {
                Point p = project(d.x, d.y, d.z);
                int r = (int) (d.safetyRadius * scale);

                g2d.setColor(new Color(30, 144, 255, 40));
                g2d.fillOval(p.x - r, p.y - r, r * 2, r * 2);
                g2d.setColor(Color.BLUE);
                g2d.drawOval(p.x - r, p.y - r, r * 2, r * 2);

                g2d.setColor(Color.CYAN);
                g2d.fillOval(p.x - 4, p.y - 4, 8, 8);

                g2d.setColor(Color.WHITE);
                g2d.drawString(d.id, p.x + 8, p.y + 4);
            }

            Point tp = project(tx, ty, tz);
            int tr = (int) (tRadius * scale);

            g2d.setColor(new Color(220, 20, 60, 60));
            g2d.fillOval(tp.x - tr, tp.y - tr, tr * 2, tr * 2);
            g2d.setColor(Color.RED);
            g2d.drawOval(tp.x - tr, tp.y - tr, tr * 2, tr * 2);

            g2d.setColor(Color.RED);
            g2d.fillOval(tp.x - 5, tp.y - 5, 10, 10);
            g2d.drawString("⚠️ INTRUDER TARGET", tp.x + 12, tp.y + 4);

            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2d.drawString("MOUSE CONTROLS: Left-Click & Drag to rotate | Scroll Wheel to Zoom", 15, 25);
        }
    }

    public static void main(String[] args) {
        GUI airspace = new GUI();
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
        double maxDroneRadius = 0.0;
        for (Drone d : initialDronesList) {
            d.updatePosition(timeStep);
            airspace.insert(d);
            if (d.safetyRadius > maxDroneRadius) {
                maxDroneRadius = d.safetyRadius;
            }
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
        double searchRange = tRadius + maxDroneRadius;
        List<Drone> threats = airspace.findNeighbors3D(tx, ty, tz, searchRange);
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

        SwingUtilities.invokeLater(() -> Radar3DVisualizer.createAndShowGUI(initialDronesList, tx, ty, tz, tRadius));
        scanner.close();
    }
}
