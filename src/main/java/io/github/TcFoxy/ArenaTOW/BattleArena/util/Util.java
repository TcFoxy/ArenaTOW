package io.github.TcFoxy.ArenaTOW.BattleArena.util;


import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import io.github.TcFoxy.ArenaTOW.BattleArena.Defaults;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.spawns.SpawnLocation;
import io.github.TcFoxy.ArenaTOW.updater.Version;

public class Util {

	static public void printStackTrace(){
		/// I've left in this accidentally too many times,
		/// make sure DEBUGGING is now on before printing
		if (Defaults.DEBUG_MSGS)
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				System.out.println(ste);}
	}

    static public String getLocString(Location l){
        return l.getWorld().getName() +"," + (int)l.getX() + "," + (int)l.getY() + "," + (int)l.getZ();
    }

    static public String getLocString(SpawnLocation l){
        return l.getLocation().getWorld().getName() +"," + (int)l.getLocation().getX() +
                "," + (int)l.getLocation().getY() + "," + (int)l.getLocation().getZ();
    }

	/**
	 * Returns the version of craftbukkit
	 * @return version or 0 if the craftbukkit version is pre 1.4.5
	 */
	public static Version getCraftBukkitVersion(){
		final String pkg = Bukkit.getServer().getClass().getPackage().getName();
		String version = pkg.substring(pkg.lastIndexOf('.') + 1);
		if (version.equalsIgnoreCase("craftbukkit")){
			return new Version("0");
		}
		return new Version(version);
	}

    /**
     * Mimic the Object toString Method
     * @param o Object
     * @return Object toString
     */
    public static String toString(Object o) {
        return o == null ? null : o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    public static UUID fromString(String name){
        try {
            return UUID.fromString(name);
        } catch (Exception e) {
            return new UUID(0, name.hashCode());
        }
    }

}
