import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

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

    public void clear() {
        root = null;
    }

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
        if (node == null)
            return;

        double distance = Math
                .sqrt(Math.pow(node.drone.x - tx, 2) + Math.pow(node.drone.y - ty, 2) + Math.pow(node.drone.z - tz, 2));
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
        if (axis == 0)
            return drone.x;
        if (axis == 1)
            return drone.y;
        return drone.z;
    }

    static class Building {
        double x, z, w, d, h;
        Building(double x, double z, double w, double d, double h) {
            this.x = x;
            this.z = z;
            this.w = w;
            this.d = d;
            this.h = h;
        }
    }
    
    static class Tree {
        double x, z, h, r;
        Tree(double x, double z, double h, double r) {
            this.x = x;
            this.z = z;
            this.h = h;
            this.r = r;
        }
    }
    
    static class Bird {
        double x, y, z;
        double vx, vz;
        double angle;
        double flapSpeed;
        Bird(double x, double y, double z, double vx, double vz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vz = vz;
            this.flapSpeed = 0.1 + Math.random() * 0.15;
            this.angle = Math.random() * Math.PI * 2;
        }
        
        void update() {
            x += vx;
            z += vz;
            if (x > 600) x = -600;
            if (x < -600) x = 600;
            if (z > 600) z = -600;
            if (z < -600) z = 600;
            angle += flapSpeed;
        }
    }

    static class RadarPanel extends JPanel {
        private List<Drone> drones;
        private double tx, ty, tz, tRadius;
        private GUI kdTree = new GUI();
        
        // Environment lists
        private List<Building> buildings = new ArrayList<>();
        private List<Tree> trees = new ArrayList<>();
        private List<Bird> birds = new ArrayList<>();
        
        // View state
        private double angleX = 20; // Pitch
        private double angleY = -45; // Yaw
        private double zoom = 1.0;
        private double translateX = 0;
        private double translateY = 0;
        
        // Mouse interaction
        private Point lastMousePt;
        
        public RadarPanel(List<Drone> drones, double tx, double ty, double tz, double tRadius) {
            this.drones = drones;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.tRadius = tRadius;
            
            // Populate KD-tree initially
            for (Drone d : drones) {
                kdTree.insert(d);
            }
            
            setBackground(new Color(10, 16, 26)); // Dark slate blue
            
            // Populate buildings
            buildings.add(new Building(-350, -200, 80, 80, 250));
            buildings.add(new Building(-300, 250, 100, 60, 180));
            buildings.add(new Building(350, -300, 70, 70, 300));
            buildings.add(new Building(250, 300, 90, 90, 220));
            buildings.add(new Building(0, -400, 120, 80, 150));
            
            // Populate trees
            for (int i = 0; i < 15; i++) {
                double txVal = -300 + Math.random() * 600;
                double tzVal = -300 + Math.random() * 600;
                if (Math.abs(txVal) > 50 || Math.abs(tzVal) > 50) {
                    trees.add(new Tree(txVal, tzVal, 25 + Math.random() * 20, 12 + Math.random() * 6));
                }
            }
            
            // Populate birds
            for (int i = 0; i < 8; i++) {
                double bx = -400 + Math.random() * 800;
                double by = -200 - Math.random() * 150;
                double bz = -400 + Math.random() * 800;
                double vx = -2 + Math.random() * 4;
                double vz = -2 + Math.random() * 4;
                if (Math.abs(vx) < 0.5) vx = 1.0;
                if (Math.abs(vz) < 0.5) vz = 1.0;
                birds.add(new Bird(bx, by, bz, vx, vz));
            }
            
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMousePt = e.getPoint();
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    Point currentPt = e.getPoint();
                    if (lastMousePt != null) {
                        double dx = currentPt.x - lastMousePt.x;
                        double dy = currentPt.y - lastMousePt.y;
                        
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            angleY += dx * 0.5;
                            angleX -= dy * 0.5;
                        } else if (SwingUtilities.isRightMouseButton(e) || e.isShiftDown()) {
                            translateX += dx;
                            translateY += dy;
                        }
                        repaint();
                    }
                    lastMousePt = currentPt;
                }
                
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double amount = e.getPreciseWheelRotation();
                    zoom *= Math.exp(-amount * 0.1);
                    zoom = Math.max(0.1, Math.min(zoom, 10.0));
                    repaint();
                }
            };
            
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
            addMouseWheelListener(mouseHandler);
        }
        
        // Helper class for painter's algorithm
        private static class Renderable {
            double depth;
            Runnable drawAction;
            
            Renderable(double depth, Runnable drawAction) {
                this.depth = depth;
                this.drawAction = drawAction;
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // Background gradient
            GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(10, 16, 26),
                0, h, new Color(18, 30, 49)
            );
            g2.setPaint(bgGradient);
            g2.fillRect(0, 0, w, h);
            
            // Screen center
            double centerX = w / 2.0 + translateX;
            double centerY = h / 2.0 + translateY;
            
            // We will collect all 3D objects, project them, and sort them by depth.
            List<Renderable> renderables = new ArrayList<>();
            
            // 1. Draw Grid on the Ground (at simulation ground, y=0)
            double gridMin = -500;
            double gridMax = 500;
            double gridStep = 100;
            
            // Grid lines parallel to X axis
            for (double gz = gridMin; gz <= gridMax; gz += gridStep) {
                final double gridZ = gz;
                for (double gx = gridMin; gx < gridMax; gx += gridStep) {
                    final double startX = gx;
                    final double endX = gx + gridStep;
                    
                    Point3D p1 = project(startX, 0, gridZ, centerX, centerY);
                    Point3D p2 = project(endX, 0, gridZ, centerX, centerY);
                    
                    double avgDepth = (p1.z + p2.z) / 2.0;
                    
                    renderables.add(new Renderable(avgDepth, () -> {
                        g2.setColor(new Color(0, 255, 255, 30));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
                    }));
                }
            }
            
            // Grid lines parallel to Z axis
            for (double gx = gridMin; gx <= gridMax; gx += gridStep) {
                final double gridX = gx;
                for (double gz = gridMin; gz < gridMax; gz += gridStep) {
                    final double startZ = gz;
                    final double endZ = gz + gridStep;
                    
                    Point3D p1 = project(gridX, 0, startZ, centerX, centerY);
                    Point3D p2 = project(gridX, 0, endZ, centerX, centerY);
                    
                    double avgDepth = (p1.z + p2.z) / 2.0;
                    
                    renderables.add(new Renderable(avgDepth, () -> {
                        g2.setColor(new Color(0, 255, 255, 30));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
                    }));
                }
            }
            
            // Add Buildings
            for (Building b : buildings) {
                addBuilding(b, renderables, centerX, centerY, g2);
            }
            
            // Add Trees
            for (Tree t : trees) {
                addTree(t, renderables, centerX, centerY, g2);
            }
            
            // Add Birds
            for (Bird b : birds) {
                addBird(b, renderables, centerX, centerY, g2);
            }
            
            // Add Drones
            for (Drone d : drones) {
                // Project drone coordinates (X=d.x, Y=-d.y, Z=d.z)
                Point3D dp = project(d.x, -d.y, d.z, centerX, centerY);
                
                // Drop line to ground (at y=0, i.e., projected point at d.x, 0, d.z)
                Point3D dGround = project(d.x, 0, d.z, centerX, centerY);
                
                double lineDepth = (dp.z + dGround.z) / 2.0;
                renderables.add(new Renderable(lineDepth, () -> {
                    g2.setColor(new Color(0, 191, 255, 80));
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 5f}, 0f));
                    g2.drawLine((int)dp.x, (int)dp.y, (int)dGround.x, (int)dGround.y);
                }));
                
                // Add safety bubble (radius of circle on screen scaled by perspective)
                double scale = getPerspectiveScale(dp.z);
                double screenRadius = d.safetyRadius * scale * zoom;
                
                renderables.add(new Renderable(dp.z - d.safetyRadius, () -> {
                    g2.setColor(new Color(30, 144, 255, 35));
                    g2.fill(new Ellipse2D.Double(dp.x - screenRadius, dp.y - screenRadius, screenRadius * 2, screenRadius * 2));
                    
                    g2.setColor(new Color(30, 144, 255, 120));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new Ellipse2D.Double(dp.x - screenRadius, dp.y - screenRadius, screenRadius * 2, screenRadius * 2));
                }));
                
                // Add core point (radius = 4)
                final double coreScreenRadius = Math.max(2.0, 4.0 * scale * zoom);
                
                renderables.add(new Renderable(dp.z, () -> {
                    g2.setColor(new Color(0, 191, 255));
                    g2.fill(new Ellipse2D.Double(dp.x - coreScreenRadius, dp.y - coreScreenRadius, coreScreenRadius * 2, coreScreenRadius * 2));
                    
                    g2.setColor(new Color(0, 191, 255, 100));
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(new Ellipse2D.Double(dp.x - coreScreenRadius - 2, dp.y - coreScreenRadius - 2, (coreScreenRadius + 2) * 2, (coreScreenRadius + 2) * 2));
                    
                    // Text label
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.drawString(d.id, (int)(dp.x + coreScreenRadius + 6), (int)(dp.y + 4));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    g2.setColor(new Color(200, 220, 255));
                    g2.drawString(String.format("H: %.1fm", d.z), (int)(dp.x + coreScreenRadius + 6), (int)(dp.y + 15));
                }));
            }
            
            // Add Target
            Point3D tp = project(tx, -ty, tz, centerX, centerY);
            Point3D tpGround = project(tx, 0, tz, centerX, centerY);
            
            double targetLineDepth = (tp.z + tpGround.z) / 2.0;
            renderables.add(new Renderable(targetLineDepth, () -> {
                g2.setColor(new Color(220, 20, 60, 100));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g2.drawLine((int)tp.x, (int)tp.y, (int)tpGround.x, (int)tpGround.y);
            }));
            
            double tScale = getPerspectiveScale(tp.z);
            double targetScreenRadius = tRadius * tScale * zoom;
            renderables.add(new Renderable(tp.z - tRadius, () -> {
                g2.setColor(new Color(220, 20, 60, 60));
                g2.fill(new Ellipse2D.Double(tp.x - targetScreenRadius, tp.y - targetScreenRadius, targetScreenRadius * 2, targetScreenRadius * 2));
                
                g2.setColor(new Color(220, 20, 60, 200));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(tp.x - targetScreenRadius, tp.y - targetScreenRadius, targetScreenRadius * 2, targetScreenRadius * 2));
            }));
            
            final double targetCoreRadius = Math.max(3.0, 5.0 * tScale * zoom);
            renderables.add(new Renderable(tp.z, () -> {
                g2.setColor(Color.RED);
                g2.fill(new Ellipse2D.Double(tp.x - targetCoreRadius, tp.y - targetCoreRadius, targetCoreRadius * 2, targetCoreRadius * 2));
                
                long time = System.currentTimeMillis();
                double pulse = 2.0 + 3.0 * Math.sin(time * 0.005);
                g2.setColor(new Color(255, 0, 0, 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(tp.x - targetCoreRadius - pulse, tp.y - targetCoreRadius - pulse, (targetCoreRadius + pulse) * 2, (targetCoreRadius + pulse) * 2));
                
                g2.setColor(Color.RED);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("⚠️ INTRUDER", (int)(tp.x + targetCoreRadius + 8), (int)(tp.y + 4));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(new Color(255, 150, 150));
                g2.drawString(String.format("X:%.1f Y:%.1f Z:%.1f", tx, ty, tz), (int)(tp.x + targetCoreRadius + 8), (int)(tp.y + 16));
            }));
            
            // Sort by depth (descending order)
            Collections.sort(renderables, new Comparator<Renderable>() {
                @Override
                public int compare(Renderable o1, Renderable o2) {
                    return Double.compare(o2.depth, o1.depth);
                }
            });
            
            // Draw
            for (Renderable r : renderables) {
                r.drawAction.run();
            }
            
            // HUD
            drawHUD(g2, w, h);
        }
        
        private void drawHUD(Graphics2D g2, int w, int h) {
            int boxW = 280;
            int boxH = 140;
            int x = 20;
            int y = 20;
            
            g2.setColor(new Color(15, 23, 42, 200));
            g2.fillRoundRect(x, y, boxW, boxH, 15, 15);
            
            g2.setColor(new Color(255, 255, 255, 30));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, boxW, boxH, 15, 15);
            
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("🛸 3D RADAR SYSTEM HUD", x + 15, y + 25);
            
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(156, 163, 175));
            
            int textY = y + 45;
            g2.drawString("• Rotate view: Left Click + Drag", x + 15, textY);
            g2.drawString("• Pan view: Right Click + Drag (or Shift+Drag)", x + 15, textY + 18);
            g2.drawString("• Zoom view: Scroll Wheel", x + 15, textY + 36);
            
            g2.setColor(new Color(14, 165, 233));
            g2.drawString(String.format("Pitch: %.1f° | Yaw: %.1f° | Zoom: %.2fx", angleX, angleY, zoom), x + 15, textY + 60);
            
            double maxDroneRadius = 0.0;
            for (Drone d : drones) {
                if (d.safetyRadius > maxDroneRadius) {
                    maxDroneRadius = d.safetyRadius;
                }
            }
            double searchRange = tRadius + maxDroneRadius;
            List<Drone> nearDrones = kdTree.findNeighbors3D(tx, ty, tz, searchRange);

            boolean threatDetected = false;
            List<String> threatDrones = new ArrayList<>();
            for (Drone d : nearDrones) {
                double dist = Math.sqrt(Math.pow(d.x - tx, 2) + Math.pow(d.y - ty, 2) + Math.pow(d.z - tz, 2));
                if (dist < (tRadius + d.safetyRadius)) {
                    threatDetected = true;
                    threatDrones.add(String.format("%s (%.1fm separation)", d.id, dist));
                }
            }
            
            if (threatDetected) {
                int threatW = 320;
                int threatH = 40 + threatDrones.size() * 20;
                int txBox = w - threatW - 20;
                int tyBox = 20;
                
                g2.setColor(new Color(127, 29, 29, 200));
                g2.fillRoundRect(txBox, tyBox, threatW, threatH, 15, 15);
                g2.setColor(new Color(239, 68, 68, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(txBox, tyBox, threatW, threatH, 15, 15);
                
                g2.setColor(new Color(254, 226, 226));
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("⚠️ COLLISION THREAT ALERTS", txBox + 15, tyBox + 25);
                
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                int threatYPos = tyBox + 45;
                for (String threat : threatDrones) {
                    g2.drawString("COLLISION: " + threat, txBox + 15, threatYPos);
                    threatYPos += 20;
                }
            } else {
                int statusW = 220;
                int statusH = 50;
                int sxBox = w - statusW - 20;
                int syBox = 20;
                
                g2.setColor(new Color(6, 78, 59, 200));
                g2.fillRoundRect(sxBox, syBox, statusW, statusH, 15, 15);
                g2.setColor(new Color(16, 185, 129, 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(sxBox, syBox, statusW, statusH, 15, 15);
                
                g2.setColor(new Color(209, 250, 229));
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("✨ AIRSPACE SECURE", sxBox + 15, syBox + 30);
            }
        }
        
        private Point3D project(double x, double y, double z, double cx, double cy) {
            double radX = Math.toRadians(angleX);
            double radY = Math.toRadians(angleY);
            
            double x1 = x * Math.cos(radY) + z * Math.sin(radY);
            double y1 = y;
            double z1 = -x * Math.sin(radY) + z * Math.cos(radY);
            
            double x2 = x1;
            double y2 = y1 * Math.cos(radX) - z1 * Math.sin(radX);
            double z2 = y1 * Math.sin(radX) + z1 * Math.cos(radX);
            
            double cameraD = 800;
            double focalLength = 800;
            
            double sz = z2 + cameraD;
            if (sz < 1) sz = 1;
            
            double sx = cx + (x2 * focalLength * zoom) / sz;
            double sy = cy + (y2 * focalLength * zoom) / sz;
            
            return new Point3D(sx, sy, z2);
        }
        
        private double getPerspectiveScale(double rotatedZ) {
            double cameraD = 800;
            double focalLength = 800;
            double sz = rotatedZ + cameraD;
            if (sz < 1) sz = 1;
            return focalLength / sz;
        }
        
        public void updateBirds() {
            for (Bird b : birds) {
                b.update();
            }
        }

        public void updateDrones(double timeStep) {
            // 1. Move all drones and check grid boundaries, or destroy if hitting the ground
            for (int idx = drones.size() - 1; idx >= 0; idx--) {
                Drone d = drones.get(idx);
                d.updatePosition(timeStep);
                
                // Ground hit check
                if (d.y <= 0) {
                    System.out.println("[💥 CRASH] Drone " + d.id + " hit the ground and was destroyed!");
                    drones.remove(idx);
                    continue;
                }
                
                // Boundary checking: X [-500, 500], Y [0, 500], Z [-500, 500]
                if (d.x < -500) { d.x = -500; d.vx = -d.vx; }
                else if (d.x > 500) { d.x = 500; d.vx = -d.vx; }
                
                if (d.y > 500) { d.y = 500; d.vy = -d.vy; }
                
                if (d.z < -500) { d.z = -500; d.vz = -d.vz; }
                else if (d.z > 500) { d.z = 500; d.vz = -d.vz; }
            }

            // 2. Resolve obstacles (Buildings and Trees)
            for (Drone d : drones) {
                // Buildings (AABB boxes on the ground)
                for (Building b : buildings) {
                    double halfW = b.w / 2.0;
                    double halfD = b.d / 2.0;
                    
                    // Find closest point on building to drone center
                    double cx = Math.max(b.x - halfW, Math.min(d.x, b.x + halfW));
                    double cy = Math.max(0.0, Math.min(d.y, b.h));
                    double cz = Math.max(b.z - halfD, Math.min(d.z, b.z + halfD));
                    
                    double dx = d.x - cx;
                    double dy = d.y - cy;
                    double dz = d.z - cz;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    
                    if (dist < d.safetyRadius) {
                        double overlap = d.safetyRadius - dist;
                        double nx = (dist > 1e-5) ? dx / dist : 0.0;
                        double ny = (dist > 1e-5) ? dy / dist : 1.0;
                        double nz = (dist > 1e-5) ? dz / dist : 0.0;
                        
                        if (dist <= 1e-5) {
                            double distToX = Math.min(Math.abs(d.x - (b.x - halfW)), Math.abs(d.x - (b.x + halfW)));
                            double distToY = Math.min(Math.abs(d.y - 0.0), Math.abs(d.y - b.h));
                            double distToZ = Math.min(Math.abs(d.z - (b.z - halfD)), Math.abs(d.z - (b.z + halfD)));
                            if (distToX < distToY && distToX < distToZ) {
                                nx = (d.x > b.x) ? 1.0 : -1.0;
                                ny = 0.0;
                                nz = 0.0;
                            } else if (distToY < distToX && distToY < distToZ) {
                                nx = 0.0;
                                ny = (d.y > b.h / 2.0) ? 1.0 : -1.0;
                                nz = 0.0;
                            } else {
                                nx = 0.0;
                                ny = 0.0;
                                nz = (d.z > b.z) ? 1.0 : -1.0;
                            }
                            overlap = d.safetyRadius;
                        }
                        
                        d.x += nx * overlap;
                        d.y += ny * overlap;
                        d.z += nz * overlap;
                        
                        double dotVal = d.vx * nx + d.vy * ny + d.vz * nz;
                        if (dotVal < 0.0) {
                            d.vx = d.vx - 2.0 * dotVal * nx;
                            d.vy = d.vy - 2.0 * dotVal * ny;
                            d.vz = d.vz - 2.0 * dotVal * nz;
                        }
                    }
                }
                
                // Trees (Cylinders on the ground)
                for (Tree t : trees) {
                    if (d.y >= -d.safetyRadius && d.y <= t.h + d.safetyRadius) {
                        double dx = d.x - t.x;
                        double dz = d.z - t.z;
                        double distXZ = Math.sqrt(dx*dx + dz*dz);
                        double requiredDist = t.r + d.safetyRadius;
                        
                        if (distXZ < requiredDist) {
                            if (d.y > t.h) {
                                double overlap = (t.h + d.safetyRadius) - d.y;
                                d.y += overlap;
                                if (d.vy < 0) {
                                    d.vy = -d.vy;
                                }
                            } else {
                                double overlap = requiredDist - distXZ;
                                double nx = (distXZ > 1e-5) ? dx / distXZ : 1.0;
                                double nz = (distXZ > 1e-5) ? dz / distXZ : 0.0;
                                
                                d.x += nx * overlap;
                                d.z += nz * overlap;
                                
                                double dotVal = d.vx * nx + d.vz * nz;
                                if (dotVal < 0.0) {
                                    d.vx = d.vx - 2.0 * dotVal * nx;
                                    d.vz = d.vz - 2.0 * dotVal * nz;
                                }
                            }
                        }
                    }
                }
            }

            // 3. Resolve Drone-to-Drone collisions (Elastic impulse resolution)
            for (int i = 0; i < drones.size(); i++) {
                Drone d1 = drones.get(i);
                for (int j = i + 1; j < drones.size(); j++) {
                    Drone d2 = drones.get(j);
                    
                    double dx = d1.x - d2.x;
                    double dy = d1.y - d2.y;
                    double dz = d1.z - d2.z;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    double minDist = d1.safetyRadius + d2.safetyRadius;
                    
                    if (dist < minDist) {
                        double overlap = minDist - dist;
                        double nx = (dist > 1e-5) ? dx / dist : 1.0;
                        double ny = (dist > 1e-5) ? dy / dist : 0.0;
                        double nz = (dist > 1e-5) ? dz / dist : 0.0;
                        
                        d1.x += nx * overlap * 0.5;
                        d1.y += ny * overlap * 0.5;
                        d1.z += nz * overlap * 0.5;
                        
                        d2.x -= nx * overlap * 0.5;
                        d2.y -= ny * overlap * 0.5;
                        d2.z -= nz * overlap * 0.5;
                        
                        double rvx = d1.vx - d2.vx;
                        double rvy = d1.vy - d2.vy;
                        double rvz = d1.vz - d2.vz;
                        double velAlongNormal = rvx * nx + rvy * ny + rvz * nz;
                        
                        if (velAlongNormal < 0) {
                            double impulse = -velAlongNormal;
                            
                            d1.vx += impulse * nx;
                            d1.vy += impulse * ny;
                            d1.vz += impulse * nz;
                            
                            d2.vx -= impulse * nx;
                            d2.vy -= impulse * ny;
                            d2.vz -= impulse * nz;
                        }
                    }
                }
            }

            // 4. Resolve Drone-to-Intruder collisions (Intruder is static/infinite mass)
            for (Drone d : drones) {
                double dx = d.x - tx;
                double dy = d.y - ty;
                double dz = d.z - tz;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                double minDist = d.safetyRadius + tRadius;
                
                if (dist < minDist) {
                    double overlap = minDist - dist;
                    double nx = (dist > 1e-5) ? dx / dist : 1.0;
                    double ny = (dist > 1e-5) ? dy / dist : 0.0;
                    double nz = (dist > 1e-5) ? dz / dist : 0.0;
                    
                    d.x += nx * overlap;
                    d.y += ny * overlap;
                    d.z += nz * overlap;
                    
                    double dotVal = d.vx * nx + d.vy * ny + d.vz * nz;
                    if (dotVal < 0.0) {
                        d.vx = d.vx - 2.0 * dotVal * nx;
                        d.vy = d.vy - 2.0 * dotVal * ny;
                        d.vz = d.vz - 2.0 * dotVal * nz;
                    }
                }
            }

            // 5. Rebuild KD-tree
            kdTree.clear();
            for (Drone d : drones) {
                kdTree.insert(d);
            }
        }

        public void handleKeyPress(java.awt.event.KeyEvent e) {
            double speed = 5.0; // meters per keypress
            int keyCode = e.getKeyCode();
            if (keyCode == java.awt.event.KeyEvent.VK_W || keyCode == java.awt.event.KeyEvent.VK_UP) {
                tz += speed;
            } else if (keyCode == java.awt.event.KeyEvent.VK_S || keyCode == java.awt.event.KeyEvent.VK_DOWN) {
                tz -= speed;
            } else if (keyCode == java.awt.event.KeyEvent.VK_A || keyCode == java.awt.event.KeyEvent.VK_LEFT) {
                tx -= speed;
            } else if (keyCode == java.awt.event.KeyEvent.VK_D || keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
                tx += speed;
            } else if (keyCode == java.awt.event.KeyEvent.VK_Q || keyCode == java.awt.event.KeyEvent.VK_PAGE_UP) {
                ty += speed;
            } else if (keyCode == java.awt.event.KeyEvent.VK_E || keyCode == java.awt.event.KeyEvent.VK_PAGE_DOWN) {
                ty = Math.max(0.0, ty - speed);
            }
            repaint();
        }

        private void addBuilding(Building b, List<Renderable> renderables, double cx, double cy, Graphics2D g2) {
            double hw = b.w / 2;
            double hd = b.d / 2;
            
            final Point3D[] pts = new Point3D[8];
            pts[0] = project(b.x - hw, 0, b.z - hd, cx, cy);
            pts[1] = project(b.x + hw, 0, b.z - hd, cx, cy);
            pts[2] = project(b.x + hw, 0, b.z + hd, cx, cy);
            pts[3] = project(b.x - hw, 0, b.z + hd, cx, cy);
            pts[4] = project(b.x - hw, -b.h, b.z - hd, cx, cy);
            pts[5] = project(b.x + hw, -b.h, b.z - hd, cx, cy);
            pts[6] = project(b.x + hw, -b.h, b.z + hd, cx, cy);
            pts[7] = project(b.x - hw, -b.h, b.z + hd, cx, cy);
            
            int[][] faceIndices = {
                {4, 5, 6, 7}, // Top
                {3, 2, 6, 7}, // Front
                {0, 1, 5, 4}, // Back
                {0, 3, 7, 4}, // Left
                {1, 2, 6, 5}  // Right
            };
            
            Color[] faceColors = {
                new Color(40, 60, 90, 220),  // Top
                new Color(30, 45, 70, 220),  // Front
                new Color(20, 30, 50, 220),  // Back
                new Color(25, 38, 60, 220),  // Left
                new Color(35, 52, 80, 220)   // Right
            };
            
            for (int i = 0; i < faceIndices.length; i++) {
                final int faceIdx = i;
                final int[] idxs = faceIndices[i];
                
                double avgDepth = (pts[idxs[0]].z + pts[idxs[1]].z + pts[idxs[2]].z + pts[idxs[3]].z) / 4.0;
                
                renderables.add(new Renderable(avgDepth, () -> {
                    java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
                    path.moveTo(pts[idxs[0]].x, pts[idxs[0]].y);
                    for (int j = 1; j < 4; j++) {
                        path.lineTo(pts[idxs[j]].x, pts[idxs[j]].y);
                    }
                    path.closePath();
                    
                    g2.setColor(faceColors[faceIdx]);
                    g2.fill(path);
                    
                    g2.setColor(new Color(0, 191, 255, 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(path);
                    
                    if (faceIdx != 0) {
                        drawWindowsOnFace(g2, pts[idxs[0]], pts[idxs[1]], pts[idxs[2]], pts[idxs[3]]);
                    } else {
                        final Point3D topCenter = project(b.x, -b.h, b.z, cx, cy);
                        double beaconScale = getPerspectiveScale(topCenter.z);
                        double bRadius = 3 * beaconScale * zoom;
                        if (bRadius > 1) {
                            long time = System.currentTimeMillis();
                            double pulse = Math.abs(Math.sin(time * 0.005));
                            g2.setColor(new Color(255, 0, 0, (int)(100 + 155 * pulse)));
                            g2.fill(new Ellipse2D.Double(topCenter.x - bRadius, topCenter.y - bRadius, bRadius * 2, bRadius * 2));
                        }
                    }
                }));
            }
        }
        
        private void drawWindowsOnFace(Graphics2D g2, Point3D p0, Point3D p1, Point3D p2, Point3D p3) {
            int rows = 5;
            int cols = 3;
            g2.setColor(new Color(255, 255, 150, 100));
            for (int r = 1; r < rows; r++) {
                double fracY = (double)r / rows;
                double xa = p0.x + fracY * (p3.x - p0.x);
                double ya = p0.y + fracY * (p3.y - p0.y);
                double xb = p1.x + fracY * (p2.x - p1.x);
                double yb = p1.y + fracY * (p2.y - p1.y);
                
                for (int c = 1; c < cols; c++) {
                    double fracX = (double)c / cols;
                    double wx = xa + fracX * (xb - xa);
                    double wy = ya + fracX * (yb - ya);
                    g2.fillRect((int)wx - 1, (int)wy - 1, 3, 3);
                }
            }
        }
        
        private void addTree(Tree t, List<Renderable> renderables, double cx, double cy, Graphics2D g2) {
            final Point3D base = project(t.x, 0, t.z, cx, cy);
            final Point3D top = project(t.x, -t.h, t.z, cx, cy);
            
            final double trunkDepth = (base.z + top.z) / 2.0;
            double scale = getPerspectiveScale(top.z);
            double trunkThickness = 4 * scale * zoom;
            if (trunkThickness < 1) trunkThickness = 1;
            
            final double finalThickness = trunkThickness;
            renderables.add(new Renderable(trunkDepth, () -> {
                g2.setColor(new Color(101, 67, 33));
                g2.setStroke(new BasicStroke((float)finalThickness));
                g2.drawLine((int)base.x, (int)base.y, (int)top.x, (int)top.y);
            }));
            
            final double foliageRadius = t.r * scale * zoom;
            renderables.add(new Renderable(top.z - t.r, () -> {
                g2.setColor(new Color(34, 139, 34, 200));
                g2.fill(new Ellipse2D.Double(top.x - foliageRadius, top.y - foliageRadius, foliageRadius * 2, foliageRadius * 2));
                
                g2.setColor(new Color(46, 204, 113, 150));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Ellipse2D.Double(top.x - foliageRadius, top.y - foliageRadius, foliageRadius * 2, foliageRadius * 2));
            }));
        }
        
        private void addBird(Bird b, List<Renderable> renderables, double cx, double cy, Graphics2D g2) {
            final Point3D bp = project(b.x, b.y, b.z, cx, cy);
            double scale = getPerspectiveScale(bp.z);
            final double birdSize = 10 * scale * zoom;
            if (birdSize < 2) return;
            
            renderables.add(new Renderable(bp.z, () -> {
                double flap = Math.sin(b.angle);
                double wingY = flap * (birdSize * 0.4);
                
                g2.setColor(new Color(220, 220, 250, 200));
                g2.setStroke(new BasicStroke(1.5f));
                
                g2.drawLine((int)bp.x, (int)bp.y, (int)(bp.x - birdSize), (int)(bp.y - wingY));
                g2.drawLine((int)bp.x, (int)bp.y, (int)(bp.x + birdSize), (int)(bp.y - wingY));
            }));
        }
        
        private static class Point3D {
            double x, y, z;
            Point3D(double x, double y, double z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }
    }

    static class Radar3DVisualizer {
        public static void createAndShowGUI(List<Drone> drones, double tx, double ty, double tz, double tRadius) {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("🛸 NATIVE 3D JAVA SPACE MONITORING SYSTEM");
                RadarPanel radarPanel = new RadarPanel(drones, tx, ty, tz, tRadius);
                frame.add(radarPanel);
                frame.setSize(1024, 768);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                
                // Add key listener for manual control of the intruder target
                frame.addKeyListener(new java.awt.event.KeyAdapter() {
                    @Override
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        radarPanel.handleKeyPress(e);
                    }
                });
                
                frame.setVisible(true);
                
                Timer timer = new Timer(33, e -> {
                    radarPanel.updateBirds();
                    radarPanel.updateDrones(0.033);
                    radarPanel.repaint();
                });
                timer.start();
            });
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
            System.out.printf(" -> %s moved to updated coordinates: (X: %.1f, Y: %.1f, Z: %.1f)\n", d.id, d.x, d.y,
                    d.z);
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
                double dist = Math.sqrt(
                        Math.pow(potential.x - tx, 2) + Math.pow(potential.y - ty, 2) + Math.pow(potential.z - tz, 2));
                double limit = tRadius + potential.safetyRadius;

                if (dist < limit) {
                    System.out.println("[⚠️ COLLISION THREAT DETECTED]:");
                    System.out.printf(" -> Object Impact Profile: %s\n", potential.id);
                    System.out.printf(" -> Intercept Position: (X: %.1f, Y: %.1f, Z: %.1f)\n", potential.x, potential.y,
                            potential.z);
                    System.out.printf(" -> Calculated Separation: %.2fm (Required Safe Buffer: %.2fm)\n\n", dist,
                            limit);
                    collision = true;
                }
            }
            if (!collision) {
                System.out.println(
                        "[✨ SAFE PROXIMITY]: Objects identified nearby, but safe spatial indexing margins are secure.");
            }
        }

        Radar3DVisualizer.createAndShowGUI(initialDronesList, tx, ty, tz, tRadius);
        scanner.close();
    }
}