package io.github.TcFoxy.ArenaTOW.Plugin.Serializable;

import io.github.TcFoxy.ArenaTOW.API.MobType;
import io.github.TcFoxy.ArenaTOW.API.TOWEntity;
import io.github.TcFoxy.ArenaTOW.Plugin.ArenaTOW;
import io.github.TcFoxy.ArenaTOW.Plugin.TugArena;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map.Entry;

public class Spawner extends PersistInfo {

    private HashMap<Integer, Location> paths = new HashMap<Integer, Location>();
    private HashMap<TOWEntity, Integer> zombies;


    public Spawner(String key, Color teamColor, Location loc, String info) {
        super(key, teamColor, loc, info);
        getSpawnerInfo();
        zombies = new HashMap<TOWEntity, Integer>();
    }

    @Override
    public TOWEntity spawnMob(TugArena arena) {
        Location spawn = getSpawnLoc();
        setMob(arena.getEntityHandler().spawnMob(arena.getEntityHandler(), MobType.ZOMBIE, getTeamColor(),
                spawn.getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
        return getMob();
    }

    public void addPp(Location loc, Player p) {
        if (paths.containsValue(loc)) {
            p.sendMessage(ChatColor.DARK_RED + "Already created a pathpoint in this position. No duplicate created");
            return;
        }
        if (ppIsTooFar(loc)) {
            p.sendMessage(ChatColor.DARK_RED + "PathPoint is too far from previous point. Get closer and try again");
            return;
        }
        paths.put(paths.size(), loc);
        this.saveSpawnerInfo();
        p.sendMessage("Pathpoint #" + paths.size() + " created for " +
                PersistInfo.getTeamColorStringReadable(this.getKey()) + " Spawner #" +
                PersistInfo.getObjectId(this));
    }

    private boolean ppIsTooFar(Location loc) {
        Location origin;
        if (paths.isEmpty()) {
            origin = this.getSpawnLoc();
        } else {
            origin = paths.get(paths.size() - 1);
        }
        return origin.distance(loc) > 10;
    }

    public void listPp() {
        for (int i = 0; i < paths.size(); i++) {
            Bukkit.broadcastMessage(paths.get(i) + "");
        }
    }

    public boolean hasPps() {
        if (paths.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void clearPath() {
        paths.clear();
    }


    public Location newPathDest(TOWEntity ent) {
        if (zombies.containsKey(ent)) {
            Integer buf = zombies.get(ent);
            zombies.put(ent, buf + 1);
            return paths.get(buf + 1);
        }
        return null;
    }

    public Location getPathDest(TOWEntity ent) {
        if (zombies.containsKey(ent)) {
            Integer buf = zombies.get(ent);
            if (paths.containsKey(buf)) {
                return paths.get(buf);
            }
        }
        return null;
    }

    public boolean containsPps(Location loc) {
        return paths.containsValue(loc);
    }


    public void addMob(TOWEntity ent) {
        zombies.put(ent, 0);
    }

    public void killMobs() {
        for (TOWEntity zombie : zombies.keySet()) {
            if (zombie.isAlive()) {
                zombie.setHealth(0);
            }
        }
    }

    public void removeMob(TOWEntity ent) {
        zombies.remove(ent);
    }

    public HashMap<TOWEntity, Integer> getZombies() {
        return zombies;
    }

    public Location getcurrentLocation(TOWEntity e) {
        if (e.isAlive()) {
            return e.getLocation();
        } else {
            return null;
        }
    }

    public void printLocations() {
        String buf = "";
        for (Location l : paths.values()) {
            buf += l.toString() + "\n";
        }
        Bukkit.broadcastMessage("Path locations of Spawner " + getKey() + ":\n" + buf);
    }

    public void getSpawnerInfo() {
        if (getInfo().equalsIgnoreCase("nopaths")) {
            return;
        }
        String[] rawparts = getInfo().split(";");
        for (int i = 0; i < rawparts.length; i++) {
            paths.put(i, stringToLocation(rawparts[i]));
        }
    }

    public void saveSpawnerInfo() {
        String buf = "nopaths";
        if (paths != null) {
            buf = "";
            for (Entry<Integer, Location> entry : paths.entrySet()) {
                buf += locationToString(entry.getValue());
                if (paths.get(entry.getKey() + 1) != null) {
                    buf += ";";
                }
            }
        }
        setInfo(buf);
    }

}