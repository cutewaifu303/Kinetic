package secret.kinetic.utils.player;

import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;















public final class ProjectileUtils implements IMinecraft {

    
    public static final double ARROW_DRAG = 0.99;
    
    public static final double ARROW_GRAVITY = 0.05;
    
    public static final double ARROW_SPAWN_OFFSET = 0.16;
    
    public static final double ARROW_SPAWN_DROP = 0.1;
    
    public static final double SPEED_PER_POWER = 3.0;
    
    public static final double ENTITY_GRAVITY = 0.08;
    
    public static final double ENTITY_VERTICAL_DRAG = 0.98;
    
    public static final double HIT_TOLERANCE = 0.3;
    
    public static final int MAX_SOLVER_PASSES = 10;
    
    public static final int MAX_FLIGHT_TICKS = 120;

    
    public enum PredictionMode {
        DIRECT("Direct"),        
        SIMPLE("Simple"),        
        ITERATIVE("Iterative");  

        public final String name;

        PredictionMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    
    public static final class Solution {
        
        public final float yaw, pitch;
        
        public final Vec3 impactPoint;
        
        public final List<Vec3> trajectory;
        
        public final double missDistance;
        
        public final double flightTicks;
        
        public final boolean precise;

        Solution(float yaw, float pitch, Vec3 impactPoint, List<Vec3> trajectory,
                 double missDistance, double flightTicks, boolean precise) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.impactPoint = impactPoint;
            this.trajectory = trajectory;
            this.missDistance = missDistance;
            this.flightTicks = flightTicks;
            this.precise = precise;
        }
    }

    
    private interface PitchObjective {
        double evaluate(float pitch);
    }

    
    private static final class Aim {
        final Vec3 start;
        final float yaw, pitch;
        final double flightTicks, miss;

        Aim(Vec3 start, float yaw, float pitch, double flightTicks, double miss) {
            this.start = start;
            this.yaw = yaw;
            this.pitch = pitch;
            this.flightTicks = flightTicks;
            this.miss = miss;
        }
    }

    private ProjectileUtils() {
    }

    
    
    

    



    public static float bowPower(int useDurationTicks) {
        float f = Math.min(useDurationTicks, 20) / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(1.0F, f);
    }

    
    public static double arrowSpeed(int useDurationTicks) {
        return bowPower(useDurationTicks) * SPEED_PER_POWER;
    }

    




    public static float closedFormPitch(double speed, double horizontalDistance, double deltaY) {
        if (speed <= 0) return Float.NaN;
        if (horizontalDistance < 0.05) {
            
            return MathHelper.clamp_float((float) -Math.toDegrees(Math.atan2(deltaY, 0.05)), -89f, 89f);
        }
        double v2 = speed * speed;
        double root = v2 * v2 - ARROW_GRAVITY * (ARROW_GRAVITY * horizontalDistance * horizontalDistance
                + 2.0 * deltaY * v2);
        if (root < 0) return Float.NaN;
        return (float) -Math.toDegrees(Math.atan((v2 - Math.sqrt(root)) / (ARROW_GRAVITY * horizontalDistance)));
    }

    
    public static double[] launchMotion(float yaw, float pitch, double speed) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRad);
        return new double[]{
                -Math.sin(yawRad) * horizontal * speed,
                -Math.sin(pitchRad) * speed,
                Math.cos(yawRad) * horizontal * speed};
    }

    
    public static Vec3 arrowStart(double shooterX, double eyeY, double shooterZ, float yaw) {
        double yawRad = Math.toRadians(yaw);
        return new Vec3(shooterX - Math.cos(yawRad) * ARROW_SPAWN_OFFSET,
                eyeY - ARROW_SPAWN_DROP,
                shooterZ - Math.sin(yawRad) * ARROW_SPAWN_OFFSET);
    }

    
    
    

    



    public static Vec3 smoothedVelocity(EntityLivingBase entity) {
        double vx = clamp((entity.motionX + (entity.posX - entity.lastTickPosX)) * 0.5, 1.0);
        double vy = clamp((entity.motionY + (entity.posY - entity.lastTickPosY)) * 0.5, 1.0);
        double vz = clamp((entity.motionZ + (entity.posZ - entity.lastTickPosZ)) * 0.5, 1.0);
        return new Vec3(vx, vy, vz);
    }

    private static double clamp(double value, double max) {
        return MathHelper.clamp_double(value, -max, max);
    }

    




    public static Vec3[] buildTargetTrack(EntityLivingBase target, Vec3 velocity, int ticks) {
        Vec3[] track = new Vec3[ticks + 1];
        double x = target.posX, z = target.posZ;
        double y = target.posY + target.height * 0.5;
        double startY = y;
        double vx = velocity.xCoord, vy = velocity.yCoord, vz = velocity.zCoord;
        boolean grounded = target.onGround && vy <= 0.0;
        boolean jumped = vy > 0.0;
        track[0] = new Vec3(x, y, z);

        for (int i = 1; i <= ticks; i++) {
            x += vx;
            z += vz;
            if (grounded) {
                
            } else if (vy >= 0.0) {
                
                y += vy;
                vy -= ENTITY_GRAVITY;
            } else {
                
                vy = (vy - ENTITY_GRAVITY) * ENTITY_VERTICAL_DRAG;
                y += vy;
                if (jumped && y <= startY) {
                    
                    y = startY;
                    grounded = true;
                }
            }
            track[i] = new Vec3(x, y, z);
        }
        return track;
    }

    
    public static Vec3 sampleTrack(Vec3[] track, double ticks) {
        if (ticks <= 0 || track.length == 1) return track[0];
        int i = (int) Math.floor(ticks);
        if (i >= track.length - 1) return track[track.length - 1];
        double f = ticks - i;
        Vec3 a = track[i], b = track[i + 1];
        return new Vec3(a.xCoord + (b.xCoord - a.xCoord) * f,
                a.yCoord + (b.yCoord - a.yCoord) * f,
                a.zCoord + (b.zCoord - a.zCoord) * f);
    }

    
    
    

    



    public static List<Vec3> simulateArrow(Vec3 start, float yaw, float pitch, double speed, int maxTicks, boolean collide) {
        List<Vec3> points = new ArrayList<Vec3>();
        points.add(start);
        double[] m = launchMotion(yaw, pitch, speed);
        double x = start.xCoord, y = start.yCoord, z = start.zCoord;
        for (int i = 0; i < maxTicks; i++) {
            double nx = x + m[0], ny = y + m[1], nz = z + m[2];
            if (collide && mc.theWorld != null) {
                MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(new Vec3(x, y, z), new Vec3(nx, ny, nz), false, true, false);
                if (hit != null) {
                    points.add(hit.hitVec);
                    break;
                }
            }
            x = nx;
            y = ny;
            z = nz;
            points.add(new Vec3(x, y, z));
            
            m[0] *= ARROW_DRAG;
            m[1] *= ARROW_DRAG;
            m[2] *= ARROW_DRAG;
            m[1] -= ARROW_GRAVITY;
        }
        return points;
    }

    



    private static double simulateMiss(Vec3 start, float yaw, float pitch, double speed, Vec3 point, double[] out) {
        double[] m = launchMotion(yaw, pitch, speed);
        double[] closest = new double[5];
        double px = start.xCoord, py = start.yCoord, pz = start.zCoord;
        double best = Double.MAX_VALUE;
        int growing = 0;
        for (int i = 1; i <= MAX_FLIGHT_TICKS; i++) {
            double x = px + m[0], y = py + m[1], z = pz + m[2];
            closestOnSegment(px, py, pz, x, y, z, point.xCoord, point.yCoord, point.zCoord, closest);
            if (closest[4] < best) {
                best = closest[4];
                growing = 0;
                if (out != null) out[0] = (i - 1) + closest[0];
            } else if (++growing > 3 && closest[4] > best + 5.0) {
                break; 
            }
            px = x;
            py = y;
            pz = z;
            m[0] *= ARROW_DRAG;
            m[1] *= ARROW_DRAG;
            m[2] *= ARROW_DRAG;
            m[1] -= ARROW_GRAVITY;
        }
        return best;
    }

    




    public static double simulateMovingMiss(Vec3 start, float yaw, float pitch, double speed, Vec3[] track, double[] out) {
        double[] m = launchMotion(yaw, pitch, speed);
        double[] closest = new double[5];
        double ax = start.xCoord, ay = start.yCoord, az = start.zCoord;
        double rx = ax - track[0].xCoord, ry = ay - track[0].yCoord, rz = az - track[0].zCoord;
        double best = Double.MAX_VALUE;
        int growing = 0;
        int maxTicks = Math.min(MAX_FLIGHT_TICKS, track.length - 1);
        for (int i = 1; i <= maxTicks; i++) {
            ax += m[0];
            ay += m[1];
            az += m[2];
            double nx = ax - track[i].xCoord, ny = ay - track[i].yCoord, nz = az - track[i].zCoord;
            closestOnSegment(rx, ry, rz, nx, ny, nz, 0, 0, 0, closest);
            if (closest[4] < best) {
                best = closest[4];
                growing = 0;
                if (out != null) out[0] = (i - 1) + closest[0];
            } else if (++growing > 3 && closest[4] > best + 5.0) {
                break;
            }
            rx = nx;
            ry = ny;
            rz = nz;
            m[0] *= ARROW_DRAG;
            m[1] *= ARROW_DRAG;
            m[2] *= ARROW_DRAG;
            m[1] -= ARROW_GRAVITY;
        }
        return best;
    }

    



    private static void closestOnSegment(double ax, double ay, double az,
                                         double bx, double by, double bz,
                                         double px, double py, double pz, double[] out) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = dx * dx + dy * dy + dz * dz;
        double t = length < 1e-12 ? 0 : ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / length;
        t = MathHelper.clamp_double(t, 0, 1);
        double cx = ax + dx * t, cy = ay + dy * t, cz = az + dz * t;
        double ex = px - cx, ey = py - cy, ez = pz - cz;
        out[0] = t;
        out[1] = cx;
        out[2] = cy;
        out[3] = cz;
        out[4] = Math.sqrt(ex * ex + ey * ey + ez * ez);
    }

    
    
    

    




    private static Aim aimAtPoint(double shooterX, double eyeY, double shooterZ, Vec3 point, double speed) {
        float yaw = yawTo(shooterX, shooterZ, point);
        Vec3 start = arrowStart(shooterX, eyeY, shooterZ, yaw);
        
        yaw = yawTo(start.xCoord, start.zCoord, point);
        start = arrowStart(shooterX, eyeY, shooterZ, yaw);

        double dx = point.xCoord - start.xCoord;
        double dy = point.yCoord - start.yCoord;
        double dz = point.zCoord - start.zCoord;
        double h = Math.sqrt(dx * dx + dz * dz);

        float seed = closedFormPitch(speed, h, dy);
        if (Float.isNaN(seed)) return null; 
        seed = MathHelper.clamp_float(seed, -89f, 89f);

        final Vec3 fStart = start;
        final float fYaw = yaw;
        final double fSpeed = speed;
        PitchObjective objective = new PitchObjective() {
            @Override
            public double evaluate(float pitch) {
                return simulateMiss(fStart, fYaw, pitch, fSpeed, point, null);
            }
        };

        float pitch = searchPitch(objective, seed, 6f);
        double miss = objective.evaluate(pitch);
        if (miss > HIT_TOLERANCE) {
            
            pitch = searchPitch(objective, pitch, 25f);
            miss = objective.evaluate(pitch);
        }

        double[] out = new double[1];
        simulateMiss(start, yaw, pitch, speed, point, out);
        return new Aim(start, yaw, pitch, out[0], miss);
    }

    
    private static float searchPitch(PitchObjective objective, float seed, float window) {
        float bestPitch = seed;
        double bestMiss = objective.evaluate(seed);
        for (float p = seed - window; p <= seed + window + 1e-3f; p += 0.5f) {
            if (p < -89f || p > 89f) continue;
            double miss = objective.evaluate(p);
            if (miss < bestMiss) {
                bestMiss = miss;
                bestPitch = p;
            }
        }
        for (float p = bestPitch - 0.5f; p <= bestPitch + 0.5f + 1e-4f; p += 0.05f) {
            if (p < -89f || p > 89f) continue;
            double miss = objective.evaluate(p);
            if (miss < bestMiss) {
                bestMiss = miss;
                bestPitch = p;
            }
        }
        return bestPitch;
    }

    private static float yawTo(double fromX, double fromZ, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(to.zCoord - fromZ, to.xCoord - fromX)) - 90.0F;
    }

    





    public static Solution solve(EntityLivingBase shooter, EntityLivingBase target, int useDurationTicks, PredictionMode mode) {
        if (shooter == null || target == null) return null;
        double speed = arrowSpeed(useDurationTicks);
        if (speed <= 0) return null;

        double shooterX = shooter.posX;
        double eyeY = shooter.posY + shooter.getEyeHeight();
        double shooterZ = shooter.posZ;
        Vec3 center = new Vec3(target.posX, target.posY + target.height * 0.5, target.posZ);
        Vec3 velocity = smoothedVelocity(target);

        Vec3 point;
        if (mode == PredictionMode.DIRECT) {
            point = center;
        } else if (mode == PredictionMode.SIMPLE) {
            
            Aim first = aimAtPoint(shooterX, eyeY, shooterZ, center, speed);
            double ticks = first != null ? first.flightTicks : shooter.getDistanceToEntity(target) / speed + 1.0;
            point = center.addVector(velocity.xCoord * ticks, velocity.yCoord * ticks, velocity.zCoord * ticks);
        } else {
            
            Vec3[] track = buildTargetTrack(target, velocity, MAX_FLIGHT_TICKS);
            point = center;
            for (int pass = 0; pass < MAX_SOLVER_PASSES; pass++) {
                Aim aim = aimAtPoint(shooterX, eyeY, shooterZ, point, speed);
                if (aim == null) return null;
                Vec3 next = sampleTrack(track, aim.flightTicks);
                if (next.squareDistanceTo(point) < 0.0025) {
                    point = next; 
                    break;
                }
                point = next;
            }
        }

        Aim aim = aimAtPoint(shooterX, eyeY, shooterZ, point, speed);
        if (aim == null) return null;

        int previewTicks = Math.min(MAX_FLIGHT_TICKS, (int) Math.ceil(aim.flightTicks) + 4);
        List<Vec3> trajectory = simulateArrow(aim.start, aim.yaw, aim.pitch, speed, previewTicks, true);
        return new Solution(aim.yaw, aim.pitch, point, trajectory, aim.miss, aim.flightTicks, aim.miss <= HIT_TOLERANCE);
    }
}
