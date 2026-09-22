/// ============================================================================
// 1. SPA ROUTING & VIEW CONTROLLER
// ============================================================================
let currentView = 'cockpit';

function navigateTo(viewId) {
    if (currentView === viewId) return;

    // Reset all views
    document.querySelectorAll('.page-view').forEach(view => {
        view.classList.remove('active-view');
        view.style.display = 'none';
    });

    // Update Nav Link States
    document.querySelectorAll('.nav-links a').forEach(link => link.classList.remove('active-tab'));
    const activeLink = document.getElementById(`link-${viewId}`);
    if (activeLink) activeLink.classList.add('active-tab');

    // Display Target View
    const target = document.getElementById(`view-${viewId}`);
    target.style.display = (viewId === 'hero' || viewId === 'cockpit') ? 'flex' : 'block';

    // Force CSS reflow before animation
    void target.offsetWidth;
    target.classList.add('active-view');
    currentView = viewId;

    if (viewId === 'cockpit') {
        resizeRadarViewport();
    }
}

// ============================================================================
// 2. GLOBAL AMBIENT SMUDGE PARTICLES
// ============================================================================
const bgCanvas = document.getElementById('global-bg-canvas');
const bgCtx = bgCanvas.getContext('2d');
let bgW, bgH, mouseX = -1000, mouseY = -1000;

function resizeBg() {
    bgW = bgCanvas.width = window.innerWidth;
    bgH = bgCanvas.height = window.innerHeight;
}
window.addEventListener('resize', resizeBg);
resizeBg();

window.addEventListener('mousemove', e => {
    mouseX = e.clientX;
    mouseY = e.clientY;
});

const bgParticles = Array.from({ length: 60 }, () => ({
    x: Math.random() * bgW,
    y: Math.random() * bgH,
    vx: (Math.random() - 0.5) * 0.5,
    vy: (Math.random() - 0.5) * 0.5,
    size: Math.random() * 2 + 1
}));

function animateGlobalBg() {
    bgCtx.fillStyle = 'rgba(5, 5, 5, 0.2)';
    bgCtx.fillRect(0, 0, bgW, bgH);

    bgParticles.forEach(p => {
        p.x += p.vx;
        p.y += p.vy;
        if (p.x < 0) p.x = bgW;
        if (p.x > bgW) p.x = 0;
        if (p.y < 0) p.y = bgH;
        if (p.y > bgH) p.y = 0;

        const dx = p.x - mouseX;
        const dy = p.y - mouseY;
        const dist = Math.hypot(dx, dy);

        if (dist < 150) {
            p.x += (dx / dist) * 1.5;
            p.y += (dy / dist) * 1.5;
            bgCtx.fillStyle = '#ff6a00';
        } else {
            bgCtx.fillStyle = 'rgba(0, 212, 255, 0.25)';
        }
        bgCtx.beginPath();
        bgCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        bgCtx.fill();
    });
    requestAnimationFrame(animateGlobalBg);
}
animateGlobalBg();

// ============================================================================
// 3. SECTION 2: DATA STRUCTURE CANVASES
// ============================================================================
function setupCanvas(id) {
    const c = document.getElementById(id);
    const resize = () => {
        c.width = c.parentElement.clientWidth;
        c.height = c.parentElement.clientHeight;
    };
    window.addEventListener('resize', resize);
    resize();
    return { canvas: c, ctx: c.getContext('2d') };
}

const kdCanvas = setupCanvas('canvas-kdtree');
const qtCanvas = setupCanvas('canvas-quadtree');
const slCanvas = setupCanvas('canvas-sweepline');
let dsTime = 0;

function animateDataStructures() {
    if (currentView !== 'structures') return requestAnimationFrame(animateDataStructures);
    dsTime += 0.02;

    // KD TREE
    kdCanvas.ctx.fillStyle = '#050505';
    kdCanvas.ctx.fillRect(0, 0, kdCanvas.canvas.width, kdCanvas.canvas.height);
    const cw = kdCanvas.canvas.width;
    const ch = kdCanvas.canvas.height;
    const splitX = cw / 2 + Math.sin(dsTime) * 50;

    kdCanvas.ctx.strokeStyle = 'rgba(0, 212, 255, 0.5)';
    kdCanvas.ctx.lineWidth = 2;
    kdCanvas.ctx.beginPath();
    kdCanvas.ctx.moveTo(splitX, 0);
    kdCanvas.ctx.lineTo(splitX, ch);
    kdCanvas.ctx.stroke();

    kdCanvas.ctx.strokeStyle = 'rgba(255, 106, 0, 0.5)';
    kdCanvas.ctx.beginPath();
    kdCanvas.ctx.moveTo(0, ch / 3);
    kdCanvas.ctx.lineTo(splitX, ch / 3);
    kdCanvas.ctx.stroke();

    // QUAD TREE
    qtCanvas.ctx.fillStyle = '#050505';
    qtCanvas.ctx.fillRect(0, 0, qtCanvas.canvas.width, qtCanvas.canvas.height);
    qtCanvas.ctx.strokeStyle = 'rgba(255, 106, 0, 0.4)';
    qtCanvas.ctx.lineWidth = 1;
    const qw = qtCanvas.canvas.width;
    const qh = qtCanvas.canvas.height;
    qtCanvas.ctx.strokeRect(10, 10, qw - 20, qh - 20);
    qtCanvas.ctx.strokeRect(10, 10, qw / 2 - 10, qh / 2 - 10);
    qtCanvas.ctx.strokeRect(qw / 2, qh / 2, qw / 2 - 10, qh / 2 - 10);
    qtCanvas.ctx.fillStyle = '#00d4ff';
    qtCanvas.ctx.beginPath();
    qtCanvas.ctx.arc(qw * 0.25, qh * 0.25, 4 + Math.sin(dsTime * 5) * 2, 0, Math.PI * 2);
    qtCanvas.ctx.fill();

    // SWEEP LINE
    slCanvas.ctx.fillStyle = '#050505';
    slCanvas.ctx.fillRect(0, 0, slCanvas.canvas.width, slCanvas.canvas.height);
    const slW = slCanvas.canvas.width;
    const slH = slCanvas.canvas.height;
    const sweepX = (dsTime * 50) % slW;
    slCanvas.ctx.strokeStyle = '#00d4ff';
    slCanvas.ctx.lineWidth = 3;
    slCanvas.ctx.beginPath();
    slCanvas.ctx.moveTo(sweepX, 0);
    slCanvas.ctx.lineTo(sweepX, slH);
    slCanvas.ctx.stroke();

    [[slW * 0.2, 50], [slW * 0.4, 80], [slW * 0.7, 40]].forEach(pt => {
        const isScanned = Math.abs(pt[0] - sweepX) < pt[1];
        slCanvas.ctx.fillStyle = isScanned ? 'rgba(239, 68, 68, 0.6)' : 'rgba(255, 106, 0, 0.3)';
        slCanvas.ctx.strokeStyle = isScanned ? '#ef4444' : '#ff6a00';
        slCanvas.ctx.beginPath();
        slCanvas.ctx.arc(pt[0], slH / 2, pt[1], 0, Math.PI * 2);
        slCanvas.ctx.fill();
        slCanvas.ctx.stroke();
    });

    requestAnimationFrame(animateDataStructures);
}
requestAnimationFrame(animateDataStructures);

// ============================================================================
// 4. SECTION 3: PROPRIETARY 3D VECTOR RADAR (JAVA Camera3D + RadarCanvas)
// ============================================================================
const rCanvas = document.getElementById('radar-viewport-canvas');
const rCtx = rCanvas.getContext('2d');

function resizeRadarViewport() {
    if (!rCanvas || !rCanvas.parentElement) return;
    rCanvas.width = rCanvas.parentElement.clientWidth;
    rCanvas.height = rCanvas.parentElement.clientHeight;
}
window.addEventListener('resize', resizeRadarViewport);

const camera3D = {
    azimuth: 0.8,
    elevation: 0.5,
    distance: 1200,
    fov: 650,
    targetX: 0,
    targetY: 0,
    targetZ: 0,
    isDragging: false,
    lastMouseX: 0,
    lastMouseY: 0,

    project(x, y, z) {
        const dx = x - this.targetX;
        const dy = y - this.targetY;
        const dz = z - this.targetZ;

        const x1 = dx * Math.cos(this.azimuth) - dz * Math.sin(this.azimuth);
        const z1 = dx * Math.sin(this.azimuth) + dz * Math.cos(this.azimuth);
        const y1 = dy;

        const y2 = y1 * Math.cos(this.elevation) - z1 * Math.sin(this.elevation);
        const z2 = y1 * Math.sin(this.elevation) + z1 * Math.cos(this.elevation);
        const x2 = x1;

        const depth = z2 + this.distance;
        if (depth <= 20) return null;

        const scale = this.fov / depth;
        return {
            x: rCanvas.width / 2 + x2 * scale,
            y: rCanvas.height / 2 - y2 * scale,
            scale: scale,
            depth: depth
        };
    }
};

rCanvas.addEventListener('mousedown', e => {
    camera3D.isDragging = false;
    camera3D.lastMouseX = e.clientX;
    camera3D.lastMouseY = e.clientY;
});

window.addEventListener('mousemove', e => {
    if (e.buttons !== 1 || currentView !== 'cockpit') return;
    const deltaX = e.clientX - camera3D.lastMouseX;
    const deltaY = e.clientY - camera3D.lastMouseY;

    if (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2) {
        camera3D.isDragging = true;
    }

    camera3D.azimuth += deltaX * 0.008;
    camera3D.elevation = Math.max(0.1, Math.min(1.4, camera3D.elevation + deltaY * 0.008));

    camera3D.lastMouseX = e.clientX;
    camera3D.lastMouseY = e.clientY;

    document.getElementById('cam-stats').innerText =
        `AZIMUTH: ${(camera3D.azimuth * 180 / Math.PI).toFixed(1)}° | ELEV: ${(camera3D.elevation * 180 / Math.PI).toFixed(1)}° | ZOOM: ${(1200 / camera3D.distance).toFixed(1)}x`;
});

rCanvas.addEventListener('wheel', e => {
    e.preventDefault();
    camera3D.distance = Math.max(400, Math.min(2500, camera3D.distance + e.deltaY * 0.8));
    document.getElementById('cam-stats').innerText =
        `AZIMUTH: ${(camera3D.azimuth * 180 / Math.PI).toFixed(1)}° | ELEV: ${(camera3D.elevation * 180 / Math.PI).toFixed(1)}° | ZOOM: ${(1200 / camera3D.distance).toFixed(1)}x`;
}, { passive: false });

document.getElementById('cam-iso').addEventListener('click', () => {
    camera3D.azimuth = 0.8;
    camera3D.elevation = 0.55;
    camera3D.distance = 1200;
});

document.getElementById('cam-top').addEventListener('click', () => {
    camera3D.azimuth = 0.0;
    camera3D.elevation = 1.45;
    camera3D.distance = 1400;
});

document.getElementById('cam-chase').addEventListener('click', () => {
    if (fleet.length > 0) {
        camera3D.targetX = fleet[0].x;
        camera3D.targetY = fleet[0].y;
        camera3D.targetZ = fleet[0].z;
        camera3D.distance = 600;
    }
});

let fleet = [];
let obstacles = [];
let noFlyZones = [];
let explosions = [];
let simMode = 1;
let lastTickTime = performance.now();
let rotorAngle = 0;
let selectedDroneId = null;

class SimulationDrone {
    constructor(id, x, y, z, vx, vy, vz) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.radius = 18;
        this.status = 'NOMINAL';
    }

    update(dt) {
        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.z += this.vz * dt;

        if (Math.abs(this.x) > 650) this.vx *= -1;
        if (Math.abs(this.z) > 650) this.vz *= -1;
        if (this.y > 350 || this.y < 20) this.vy *= -1;
    }

    distance3D(other) {
        return Math.hypot(this.x - other.x, this.y - other.y, this.z - other.z);
    }
}

function deleteDrone(id) {
    const index = fleet.findIndex(d => d.id.toLowerCase() === id.toLowerCase());
    if (index !== -1) {
        const removed = fleet.splice(index, 1)[0];
        if (selectedDroneId === removed.id) selectedDroneId = null;
        logAlert(`PURGED: Unit ${removed.id} removed from airspace.`, "log");
        document.getElementById('hud-delete-id').value = '';
    } else {
        logAlert(`DELETE FAILED: Unit ${id} not found in active fleet.`, "alert");
    }
}

function logAlert(msg, type = "log") {
    const stream = document.getElementById('hud-alert-stream');
    const div = document.createElement('div');
    div.className = `alert-box ${type}`;
    div.innerHTML = `[${new Date().toLocaleTimeString()}] ${msg}`;
    stream.prepend(div);
    if (stream.children.length > 8) stream.removeChild(stream.lastChild);
}

function updateSelectedTelemetry() {
    const container = document.getElementById('selected-drone-telemetry');
    if (!container) return;

    if (!selectedDroneId) {
        container.innerHTML = `<div class="telemetry-placeholder">Click a drone on radar to lock telemetry targeting.</div>`;
        return;
    }

    const drone = fleet.find(d => d.id === selectedDroneId);
    if (!drone) {
        selectedDroneId = null;
        container.innerHTML = `<div class="telemetry-placeholder">Target lost or destroyed.</div>`;
        return;
    }

    const speed = Math.hypot(drone.vx, drone.vy, drone.vz).toFixed(1);

    container.innerHTML = `
        <div class="telemetry-data-grid">
            <div class="telemetry-data-row"><span>CALLSIGN:</span> <span>${drone.id}</span></div>
            <div class="telemetry-data-row"><span>STATUS:</span> <span style="color: ${drone.status === 'DANGER' ? 'var(--accent-red)' : 'var(--accent-green)'};">${drone.status}</span></div>
            <div class="telemetry-data-row"><span>POS (X,Y,Z):</span> <span>${drone.x.toFixed(0)}, ${drone.y.toFixed(0)}, ${drone.z.toFixed(0)}</span></div>
            <div class="telemetry-data-row"><span>VEL VECTOR:</span> <span>[${drone.vx.toFixed(1)}, ${drone.vy.toFixed(1)}, ${drone.vz.toFixed(1)}]</span></div>
            <div class="telemetry-data-row"><span>SPEED:</span> <span>${speed} m/s</span></div>
            <button class="btn-solid-orange" onclick="deleteDrone('${drone.id}')" style="background: var(--accent-red); color: #fff; margin-top: 0.5rem;">PURGE THIS DRONE</button>
        </div>
    `;
}

function loadScenario(type) {
    fleet = [];
    obstacles = [];
    noFlyZones = [];
    explosions = [];
    selectedDroneId = null;
    camera3D.targetX = 0;
    camera3D.targetY = 0;
    camera3D.targetZ = 0;
    document.getElementById('hud-alert-stream').innerHTML = '';

    if (type === 'swarm') {
        for (let i = 0; i < 8; i++) {
            obstacles.push({
                x: (Math.random() - 0.5) * 600,
                z: (Math.random() - 0.5) * 600,
                w: 60 + Math.random() * 30,
                d: 60 + Math.random() * 30,
                h: 120 + Math.random() * 180
            });
        }
        noFlyZones.push({ x: 0, z: 0, r: 120, h: 250 });

        for (let i = 0; i < 28; i++) {
            fleet.push(new SimulationDrone(
                `SWARM-${i + 1}`,
                (Math.random() - 0.5) * 700,
                60 + Math.random() * 180,
                (Math.random() - 0.5) * 700,
                (Math.random() - 0.5) * 40,
                (Math.random() - 0.5) * 15,
                (Math.random() - 0.5) * 40
            ));
        }
        logAlert("SCENARIO LOADED: Dense Urban Swarm. Obstacles and NFZ active.", "log");
    } else if (type === 'intercept') {
        obstacles.push({ x: 0, z: 0, w: 90, d: 90, h: 220 });
        fleet.push(new SimulationDrone("ALPHA-1", -350, 120, 0, 75, 0, 0));
        fleet.push(new SimulationDrone("BRAVO-2", 350, 120, 0, -75, 0, 0));
        fleet.push(new SimulationDrone("CHARLIE-3", 0, 120, -350, 0, 0, 75));
        fleet.push(new SimulationDrone("DELTA-4", 0, 120, 350, 0, 0, -75));
        logAlert("SCENARIO LOADED: High-Speed Intercept Vectors.", "log");
    } else {
        logAlert("SCENARIO LOADED: Blank Slate. Awaiting manual injection.", "log");
    }
}

document.getElementById('btn-load-scenario').addEventListener('click', () => {
    loadScenario(document.getElementById('hud-scenario').value);
});

document.getElementById('hud-mode').addEventListener('change', e => {
    simMode = parseInt(e.target.value);
    const statusEl = document.getElementById('hud-val-status');
    statusEl.innerText = simMode === 1 ? "NOMINAL" : "UNREGULATED";
    statusEl.className = simMode === 1 ? "metric-value status-cyan" : "metric-value";
    if (simMode === 2) statusEl.style.color = "#ef4444";
    logAlert(`MODE SWITCH: Collision Avoidance is now ${simMode === 1 ? 'ONLINE' : 'OFFLINE'}`, "log");
});

document.getElementById('btn-inject-drone').addEventListener('click', () => {
    const id = document.getElementById('hud-id').value || `UAV-${fleet.length + 1}`;
    const x = parseFloat(document.getElementById('hud-x').value) || 0;
    const y = parseFloat(document.getElementById('hud-y').value) || 100;
    const z = parseFloat(document.getElementById('hud-z').value) || 0;
    const vx = parseFloat(document.getElementById('hud-vx').value) || 0;
    const vy = parseFloat(document.getElementById('hud-vy').value) || 0;
    const vz = parseFloat(document.getElementById('hud-vz').value) || 0;

    fleet.push(new SimulationDrone(id, x, y, z, vx, vy, vz));
    logAlert(`INJECTED: Drone ${id} launched into airspace.`, "log");
});

document.getElementById('btn-delete-drone').addEventListener('click', () => {
    const idToDelete = document.getElementById('hud-delete-id').value.trim();
    if (!idToDelete) {
        logAlert("DELETE FAILED: Please enter a Drone ID.", "alert");
        return;
    }
    deleteDrone(idToDelete);
});

document.getElementById('btn-inject-building').addEventListener('click', () => {
    const type = document.getElementById('hud-build-type').value;
    const x = parseFloat(document.getElementById('hud-bx').value) || 0;
    const z = parseFloat(document.getElementById('hud-bz').value) || 0;

    if (type === 'skyscraper') {
        obstacles.push({ x, z, w: 70, d: 70, h: 220 });
        logAlert(`STRUCTURE: Obstacle3D spawned at [${x}, ${z}].`, "log");
    } else {
        noFlyZones.push({ x, z, r: 100, h: 200 });
        logAlert(`RESTRICTION: No-Fly Zone active at [${x}, ${z}].`, "log");
    }
});

rCanvas.addEventListener('click', e => {
    if (camera3D.isDragging) return;

    const rect = rCanvas.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    let closestDrone = null;
    let minDistance = 25;

    fleet.forEach(d => {
        const proj = camera3D.project(d.x, d.y, d.z);
        if (proj) {
            const dist = Math.hypot(proj.x - clickX, proj.y - clickY);
            if (dist < minDistance) {
                minDistance = dist;
                closestDrone = d;
            }
        }
    });

    if (closestDrone) {
        if (selectedDroneId === closestDrone.id) {
            if (confirm(`Delete drone ${closestDrone.id}?`)) {
                deleteDrone(closestDrone.id);
            }
        } else {
            selectedDroneId = closestDrone.id;
            document.getElementById('hud-delete-id').value = closestDrone.id;
            logAlert(`TELEMETRY LOCK: Target acquired [${closestDrone.id}]. Click again to delete.`, "log");
        }
    } else {
        selectedDroneId = null;
    }
});

function drawAxis3D(origin, end, color, label) {
    if (!origin || !end) return;

    rCtx.strokeStyle = color;
    rCtx.fillStyle = color;
    rCtx.lineWidth = 3;

    rCtx.beginPath();
    rCtx.moveTo(origin.x, origin.y);
    rCtx.lineTo(end.x, end.y);
    rCtx.stroke();

    const angle = Math.atan2(end.y - origin.y, end.x - origin.x);
    const headLen = 10;

    rCtx.beginPath();
    rCtx.moveTo(end.x, end.y);
    rCtx.lineTo(end.x - headLen * Math.cos(angle - Math.PI / 6), end.y - headLen * Math.sin(angle - Math.PI / 6));
    rCtx.lineTo(end.x - headLen * Math.cos(angle + Math.PI / 6), end.y - headLen * Math.sin(angle + Math.PI / 6));
    rCtx.closePath();
    rCtx.fill();

    rCtx.font = 'bold 12px monospace';
    rCtx.fillText(label, end.x + 8, end.y + 4);
}

function renderTacticalRadar() {
    requestAnimationFrame(renderTacticalRadar);
    if (currentView !== 'cockpit') return;

    const now = performance.now();
    const dt = Math.min((now - lastTickTime) / 1000, 0.1);
    lastTickTime = now;
    rotorAngle += 0.8;

    rCtx.fillStyle = '#02040a';
    rCtx.fillRect(0, 0, rCanvas.width, rCanvas.height);

    rCtx.strokeStyle = 'rgba(0, 212, 255, 0.15)';
    rCtx.lineWidth = 1;
    for (let i = -700; i <= 700; i += 100) {
        const p1 = camera3D.project(i, 0, -700);
        const p2 = camera3D.project(i, 0, 700);
        if (p1 && p2) {
            rCtx.beginPath();
            rCtx.moveTo(p1.x, p1.y);
            rCtx.lineTo(p2.x, p2.y);
            rCtx.stroke();
        }
        const p3 = camera3D.project(-700, 0, i);
        const p4 = camera3D.project(700, 0, i);
        if (p3 && p4) {
            rCtx.beginPath();
            rCtx.moveTo(p3.x, p3.y);
            rCtx.lineTo(p4.x, p4.y);
            rCtx.stroke();
        }
    }

    const origin = camera3D.project(0, 0, 0);
    const xAxis = camera3D.project(250, 0, 0);
    const yAxis = camera3D.project(0, 250, 0);
    const zAxis = camera3D.project(0, 0, 250);

    drawAxis3D(origin, xAxis, '#ff2a2a', '+X');
    drawAxis3D(origin, yAxis, '#10b981', '+Y (ALT)');
    drawAxis3D(origin, zAxis, '#00d4ff', '+Z');

    obstacles.forEach(b => {
        const hw = b.w / 2;
        const hd = b.d / 2;
        const pts = [
            camera3D.project(b.x - hw, 0, b.z - hd),
            camera3D.project(b.x + hw, 0, b.z - hd),
            camera3D.project(b.x + hw, 0, b.z + hd),
            camera3D.project(b.x - hw, 0, b.z + hd),
            camera3D.project(b.x - hw, b.h, b.z - hd),
            camera3D.project(b.x + hw, b.h, b.z - hd),
            camera3D.project(b.x + hw, b.h, b.z + hd),
            camera3D.project(b.x - hw, b.h, b.z + hd)
        ];

        if (pts.every(p => p !== null)) {
            rCtx.fillStyle = 'rgba(10, 15, 24, 0.75)';
            rCtx.beginPath();
            rCtx.moveTo(pts[4].x, pts[4].y);
            rCtx.lineTo(pts[5].x, pts[5].y);
            rCtx.lineTo(pts[6].x, pts[6].y);
            rCtx.lineTo(pts[7].x, pts[7].y);
            rCtx.closePath();
            rCtx.fill();

            rCtx.strokeStyle = 'rgba(0, 212, 255, 0.5)';
            rCtx.lineWidth = 1.5;
            rCtx.beginPath();
            rCtx.moveTo(pts[0].x, pts[0].y);
            rCtx.lineTo(pts[1].x, pts[1].y);
            rCtx.lineTo(pts[2].x, pts[2].y);
            rCtx.lineTo(pts[3].x, pts[3].y);
            rCtx.closePath();
            rCtx.stroke();
            
            rCtx.beginPath();
            rCtx.moveTo(pts[4].x, pts[4].y);
            rCtx.lineTo(pts[5].x, pts[5].y);
            rCtx.lineTo(pts[6].x, pts[6].y);
            rCtx.lineTo(pts[7].x, pts[7].y);
            rCtx.closePath();
            rCtx.stroke();
            
            for (let i = 0; i < 4; i++) {
                rCtx.beginPath();
                rCtx.moveTo(pts[i].x, pts[i].y);
                rCtx.lineTo(pts[i + 4].x, pts[i + 4].y);
                rCtx.stroke();
            }
        }
    });

    noFlyZones.forEach(nf => {
        rCtx.strokeStyle = 'rgba(239, 68, 68, 0.5)';
        rCtx.fillStyle = 'rgba(239, 68, 68, 0.08)';
        rCtx.lineWidth = 1.5;

        const basePts = [];
        const roofPts = [];
        for (let a = 0; a < Math.PI * 2; a += Math.PI / 8) {
            basePts.push(camera3D.project(nf.x + Math.cos(a) * nf.r, 0, nf.z + Math.sin(a) * nf.r));
            roofPts.push(camera3D.project(nf.x + Math.cos(a) * nf.r, nf.h, nf.z + Math.sin(a) * nf.r));
        }

        if (basePts.every(p => p !== null)) {
            rCtx.beginPath();
            basePts.forEach((p, idx) => idx === 0 ? rCtx.moveTo(p.x, p.y) : rCtx.lineTo(p.x, p.y));
            rCtx.closePath();
            rCtx.fill();
            rCtx.stroke();

            rCtx.beginPath();
            roofPts.forEach((p, idx) => idx === 0 ? rCtx.moveTo(p.x, p.y) : rCtx.lineTo(p.x, p.y));
            rCtx.closePath();
            rCtx.stroke();
        }
    });

    fleet.forEach(d => {
        d.update(dt);
        d.status = 'NOMINAL';

        obstacles.forEach(b => {
            const hw = b.w / 2;
            const hd = b.d / 2;
            if (Math.abs(d.x - b.x) < hw + d.radius && Math.abs(d.z - b.z) < hd + d.radius && d.y < b.h) {
                if (simMode === 1) {
                    if (Math.abs(d.x - b.x) > Math.abs(d.z - b.z)) d.vx *= -1;
                    else d.vz *= -1;
                } else {
                    logAlert(`💥 CRITICAL IMPACT: ${d.id} collided with Obstacle.`, "alert");
                    explosions.push({ x: d.x, y: d.y, z: d.z, age: 0 });
                    fleet.splice(fleet.indexOf(d), 1);
                }
            }
        });

        noFlyZones.forEach(nf => {
            if (Math.hypot(d.x - nf.x, d.z - nf.z) < nf.r && d.y < nf.h) {
                d.status = 'DANGER';
                if (simMode === 1 && Math.random() < 0.03) {
                    logAlert(`⚠️ TRAJECTORY ALERT: ${d.id} breached restricted zone.`, "alert");
                }
            }
        });
    });

    if (simMode === 1) {
        const sorted = [...fleet].sort((a, b) => a.x - b.x);
        for (let i = 0; i < sorted.length; i++) {
            for (let j = i + 1; j < sorted.length; j++) {
                const d1 = sorted[i];
                const d2 = sorted[j];
                if (Math.abs(d1.x - d2.x) > 100) break;

                const dist = d1.distance3D(d2);
                if (dist < 90) {
                    d1.status = 'DANGER';
                    d2.status = 'DANGER';

                    const p1 = camera3D.project(d1.x, d1.y, d1.z);
                    const p2 = camera3D.project(d2.x, d2.y, d2.z);
                    if (p1 && p2) {
                        rCtx.strokeStyle = '#ef4444';
                        rCtx.lineWidth = 1.5;
                        rCtx.setLineDash([4, 4]);
                        rCtx.beginPath();
                        rCtx.moveTo(p1.x, p1.y);
                        rCtx.lineTo(p2.x, p2.y);
                        rCtx.stroke();
                        rCtx.setLineDash([]);
                    }

                    const force = (90 - dist) / 90;
                    const dx = (d1.x - d2.x) / dist;
                    const dy = (d1.y - d2.y) / dist;
                    const dz = (d1.z - d2.z) / dist;

                    d1.vx += dx * force * 15;
                    d1.vy += dy * force * 5;
                    d1.vz += dz * force * 15;

                    d2.vx -= dx * force * 15;
                    d2.vy -= dy * force * 5;
                    d2.vz -= dz * force * 15;
                }
            }
        }
    } else {
        for (let i = 0; i < fleet.length; i++) {
            for (let j = i + 1; j < fleet.length; j++) {
                const d1 = fleet[i];
                const d2 = fleet[j];
                if (d1.distance3D(d2) < (d1.radius + d2.radius)) {
                    logAlert(`🚨 FATAL COLLISION: ${d1.id} destroyed ${d2.id}.`, "alert");
                    explosions.push({ x: (d1.x + d2.x) / 2, y: (d1.y + d2.y) / 2, z: (d1.z + d2.z) / 2, age: 0 });
                    fleet.splice(j, 1);
                    fleet.splice(i, 1);
                    i--;
                    break;
                }
            }
        }
    }

    fleet.forEach(d => {
        const proj = camera3D.project(d.x, d.y, d.z);
        const shadow = camera3D.project(d.x, 0, d.z);

        if (proj && shadow) {
            rCtx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
            rCtx.lineWidth = 1;
            rCtx.beginPath();
            rCtx.moveTo(shadow.x, shadow.y);
            rCtx.lineTo(proj.x, proj.y);
            rCtx.stroke();

            rCtx.fillStyle = 'rgba(0, 212, 255, 0.2)';
            rCtx.beginPath();
            rCtx.ellipse(shadow.x, shadow.y, 10 * shadow.scale, 5 * shadow.scale, 0, 0, Math.PI * 2);
            rCtx.fill();

            if (d.id === selectedDroneId) {
                rCtx.strokeStyle = '#00d4ff';
                rCtx.lineWidth = 2;
                rCtx.setLineDash([4, 2]);
                rCtx.beginPath();
                rCtx.arc(proj.x, proj.y, Math.max(0.4, proj.scale * 45) * 1.3, 0, Math.PI * 2);
                rCtx.stroke();
                rCtx.setLineDash([]);
            }

            const s = Math.max(0.4, proj.scale * 45);
            const isDanger = d.status === 'DANGER';

            rCtx.strokeStyle = isDanger ? '#ef4444' : '#00d4ff';
            rCtx.lineWidth = 2;
            rCtx.beginPath();
            rCtx.moveTo(proj.x - s, proj.y - s * 0.5);
            rCtx.lineTo(proj.x + s, proj.y + s * 0.5);
            rCtx.moveTo(proj.x - s, proj.y + s * 0.5);
            rCtx.lineTo(proj.x + s, proj.y - s * 0.5);
            rCtx.stroke();

            rCtx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
            rCtx.lineWidth = 1.2;
            [[-s, -s * 0.5], [s, s * 0.5], [-s, s * 0.5], [s, -s * 0.5]].forEach(pos => {
                rCtx.beginPath();
                rCtx.ellipse(proj.x + pos[0], proj.y + pos[1], s * 0.45, s * 0.2, rotorAngle, 0, Math.PI * 2);
                rCtx.stroke();
            });

            rCtx.fillStyle = isDanger ? '#ef4444' : '#ff6a00';
            rCtx.beginPath();
            rCtx.arc(proj.x, proj.y, s * 0.35, 0, Math.PI * 2);
            rCtx.fill();

            rCtx.strokeStyle = '#ffffff';
            rCtx.lineWidth = 1.5;
            rCtx.beginPath();
            rCtx.moveTo(proj.x, proj.y);
            rCtx.lineTo(proj.x + (d.vx * 0.4), proj.y + (d.vz * 0.4));
            rCtx.stroke();

            rCtx.fillStyle = '#f8fafc';
            rCtx.font = '10px monospace';
            rCtx.fillText(`${d.id} [${d.y.toFixed(0)}m]`, proj.x + s + 2, proj.y - 4);
        }
    });

    explosions.forEach((exp, index) => {
        exp.age += dt;
        const ep = camera3D.project(exp.x, exp.y, exp.z);
        if (ep) {
            const rad = exp.age * 60 * ep.scale;
            rCtx.strokeStyle = `rgba(239, 68, 68, ${Math.max(0, 1 - exp.age)})`;
            rCtx.lineWidth = 2;
            rCtx.beginPath();
            rCtx.arc(ep.x, ep.y, rad, 0, Math.PI * 2);
            rCtx.stroke();
        }
        if (exp.age > 1) explosions.splice(index, 1);
    });

    document.getElementById('hud-val-fleet').innerText = fleet.length;
    document.getElementById('hud-val-lat').innerText = (performance.now() - now).toFixed(1) + "ms";
    updateSelectedTelemetry();
}

loadScenario('swarm');
resizeRadarViewport();
requestAnimationFrame(renderTacticalRadar);

// ============================================================================
// 5. SECTION 4: CLI BACKEND COMPILER (MATCHING DroneSystem.java 8-OPTION MENU)
// ============================================================================
const btnRunCli = document.getElementById('btn-run-java');
const termOutput = document.getElementById('term-window');
const cliInputArea = document.getElementById('cli-input-area');
const termInput = document.getElementById('term-input-field');

let termState = 0;
const termDroneMap = new Map();
const termZones = [{ id: "Airport_Runway", cx: 0, cy: 0, cz: 0, radius: 50 }];

function termPrint(text) {
    termOutput.innerHTML += `<div>${text}</div>`;
    termOutput.scrollTop = termOutput.scrollHeight;
}

btnRunCli.addEventListener('click', () => {
    btnRunCli.disabled = true;
    btnRunCli.innerText = "COMPILING...";
    termOutput.innerHTML = '';
    cliInputArea.style.display = 'none';
    termState = 0;

    termPrint("Starting JVM Environment...");
    termPrint("Compiling DroneSystem.java... [OK]");

    setTimeout(() => {
        termPrint("==================================================");
        termPrint("✈️ REAL-TIME DRONE AIRSPACE MONITORING SYSTEM ✈️");
        termPrint("==================================================");
        printMenuPrompt();
        btnRunCli.innerText = "▶ RUN COMPILER";
        btnRunCli.disabled = false;
        cliInputArea.style.display = 'flex';
        termState = 1;
    }, 1000);
});

function printMenuPrompt() {
    termPrint("<br>--- MAIN MENU ---");
    termPrint("1. Add Drone (Dynamic Insertion)");
    termPrint("2. Remove Drone (Dynamic Deletion)");
    termPrint("3. Advance Time / Update Trajectories");
    termPrint("4. Run Sweep Line Collision Detection (2D/3D)");
    termPrint("5. Query Nearest Neighbor (KD-Tree)");
    termPrint("6. Query 2D Point Location / Range (Quad Tree)");
    termPrint("7. View All Drones");
    termPrint("8. Exit");
    termPrint("Select an option: ");
}

termInput.addEventListener('keydown', e => {
    if (e.key === 'Enter' && termState > 0) {
        const val = termInput.value.trim();
        termPrint(`<span style="color: #cbd5e1;">${val}</span>`);
        termInput.value = '';

        if (termState === 1) {
            switch (val) {
                case "1":
                    termPrint("Enter ID, x, y, z, vx, vy, vz (space-separated): ");
                    termState = 2;
                    break;
                case "2":
                    termPrint("Enter Drone ID to remove: ");
                    termState = 3;
                    break;
                case "3":
                    termPrint("Enter time step (dt in seconds): ");
                    termState = 4;
                    break;
                case "4":
                    termPrint("<br>Executing Sweep Line Collision Algorithm...");
                    if (termDroneMap.size < 2) {
                        termPrint("✅ Sweep Line Check: Airspace is clear of collisions.");
                    } else {
                        const drones = Array.from(termDroneMap.values());
                        drones.sort((a, b) => (a.x - 10.0) - (b.x - 10.0));
                        let alertsCount = 0;
                        for (let i = 0; i < drones.length; i++) {
                            for (let j = i + 1; j < drones.length; j++) {
                                const d1 = drones[i], d2 = drones[j];
                                if ((d1.x + 10.0) < (d2.x - 10.0)) break;
                                const dy = !((d2.y + 10.0) < (d1.y - 10.0) || (d2.y - 10.0) > (d1.y + 10.0));
                                if (dy) {
                                    const dist = Math.sqrt(Math.pow(d1.x - d2.x, 2) + Math.pow(d1.y - d2.y, 2) + Math.pow(d1.z - d2.z, 2));
                                    if (dist <= 20.0) {
                                        termPrint(`🚨 [COLLISION ALERT via Sweep Line] ${d1.id} and ${d2.id} are critically close (${dist.toFixed(2)}m)!`);
                                        alertsCount++;
                                    }
                                }
                            }
                        }
                        if (alertsCount === 0) termPrint("✅ Sweep Line Check: Airspace is clear of collisions.");
                    }
                    printMenuPrompt();
                    break;
                case "5":
                    termPrint("Enter target coordinates X Y Z (space-separated): ");
                    termState = 5;
                    break;
                case "6":
                    termPrint("Enter search center X Y and Area Radius (space-separated): ");
                    termState = 6;
                    break;
                case "7":
                    if (termDroneMap.size === 0) {
                        termPrint("No drones in airspace.");
                    } else {
                        termDroneMap.forEach(d => {
                            termPrint(`${d.id} at [${d.x.toFixed(1)}, ${d.y.toFixed(1)}, ${d.z.toFixed(1)}]`);
                        });
                    }
                    printMenuPrompt();
                    break;
                case "8":
                    termPrint("System shutting down...");
                    cliInputArea.style.display = 'none';
                    termState = 0;
                    break;
                default:
                    termPrint("❌ Invalid choice. Try again.");
                    printMenuPrompt();
            }
        } else if (termState === 2) { // Option 1: Add Drone
            const p = val.split(" ");
            if (p.length >= 7) {
                const droneObj = {
                    id: p[0],
                    x: parseFloat(p[1]), y: parseFloat(p[2]), z: parseFloat(p[3]),
                    vx: parseFloat(p[4]), vy: parseFloat(p[5]), vz: parseFloat(p[6]),
                    radius: 10.0
                };
                termDroneMap.set(p[0], droneObj);
                termPrint("✅ Drone dynamically inserted.");
            } else {
                termPrint("❌ Input error. Please use correct formatting.");
            }
            termState = 1;
            printMenuPrompt();
        } else if (termState === 3) { // Option 2: Remove Drone
            if (termDroneMap.delete(val)) {
                termPrint("✅ Drone deleted.");
            } else {
                termPrint("❌ Drone not found.");
            }
            termState = 1;
            printMenuPrompt();
        } else if (termState === 4) { // Option 3: Advance Trajectories
            const dt = parseFloat(val) || 1.0;
            termDroneMap.forEach(d => {
                d.x += d.vx * dt;
                d.y += d.vy * dt;
                d.z += d.vz * dt;
                termZones.forEach(zone => {
                    const dist = Math.sqrt(Math.pow(zone.cx - d.x, 2) + Math.pow(zone.cy - d.y, 2) + Math.pow(zone.cz - d.z, 2));
                    if (dist <= zone.radius) {
                        termPrint(`⚠️ NO-FLY ZONE VIOLATION: ${d.id}`);
                    }
                });
            });
            termPrint("✅ Trajectories updated.");
            termState = 1;
            printMenuPrompt();
        } else if (termState === 5) { // Option 5: KD-Tree Nearest Neighbor
            const coords = val.split(" ");
            if (coords.length >= 3 && termDroneMap.size > 0) {
                const tx = parseFloat(coords[0]), ty = parseFloat(coords[1]), tz = parseFloat(coords[2]);
                let best = null;
                let bestDist = Number.MAX_VALUE;
                termDroneMap.forEach(d => {
                    const dist = Math.sqrt(Math.pow(d.x - tx, 2) + Math.pow(d.y - ty, 2) + Math.pow(d.z - tz, 2));
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = d;
                    }
                });
                if (best) {
                    termPrint(`🎯 Nearest Drone (KD-Tree): ${best.id} at [${best.x.toFixed(1)}, ${best.y.toFixed(1)}, ${best.z.toFixed(1)}]`);
                }
            } else {
                termPrint("No drones in airspace.");
            }
            termState = 1;
            printMenuPrompt();
        } else if (termState === 6) { // Option 6: Quad Tree Range Query
            const qCoords = val.split(" ");
            if (qCoords.length >= 3 && termDroneMap.size > 0) {
                const cx = parseFloat(qCoords[0]), cy = parseFloat(qCoords[1]), radius = parseFloat(qCoords[2]);
                const found = [];
                termDroneMap.forEach(d => {
                    if (d.x >= cx - radius && d.x <= cx + radius && d.y >= cy - radius && d.y <= cy + radius) {
                        found.push(d);
                    }
                });
                termPrint(`📍 Drones found in area (Quad Tree): ${found.length}`);
                found.forEach(fd => termPrint(`  - ${fd.id}`));
            } else {
                termPrint("📍 Drones found in area (Quad Tree): 0");
            }
            termState = 1;
            printMenuPrompt();
        }
    }
});