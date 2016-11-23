package io.github.TcFoxy.ArenaTOW.BattleArena.serializers;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import io.github.TcFoxy.ArenaTOW.BattleArena.BattleArena;
import io.github.TcFoxy.ArenaTOW.BattleArena.Defaults;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.BattleArenaController;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.ParamController;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.RoomController;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.Scheduler;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.containers.AreaContainer;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.containers.RoomContainer;
import io.github.TcFoxy.ArenaTOW.BattleArena.controllers.plugins.WorldGuardController;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.ArenaParams;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.LocationType;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.MatchParams;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.StateGraph;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.arenas.Arena;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.arenas.ArenaControllerInterface;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.arenas.ArenaType;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.arenas.Persistable;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.exceptions.RegionNotFound;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.options.EventOpenOptions;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.options.TransitionOption;
import io.github.TcFoxy.ArenaTOW.BattleArena.objects.spawns.SpawnLocation;
import io.github.TcFoxy.ArenaTOW.BattleArena.util.Log;
import io.github.TcFoxy.ArenaTOW.BattleArena.util.MinMax;
import io.github.TcFoxy.ArenaTOW.BattleArena.util.SerializerUtil;

public class ArenaSerializer extends BaseConfig{
    static BattleArenaController arenaController;
    static HashMap<Plugin, Set<ArenaSerializer>> configs = new HashMap<Plugin, Set<ArenaSerializer>>();

    /// Which plugin does this ArenaSerializer belong to
    Plugin plugin;

    public static void setBAC(BattleArenaController bac){
        arenaController = bac;
    }

    public ArenaSerializer(Plugin plugin, File file){
        setConfig(file);
        this.plugin = plugin;

        config = new YamlConfiguration();
        Set<ArenaSerializer> paths = configs.get(plugin);
        if (paths == null){
            paths = new HashSet<ArenaSerializer>();
            configs.put(plugin, paths);
        } else { /// check to see if we have this path already
            for (ArenaSerializer as : paths){
                if (as.file.getPath().equals(this.file.getPath())){
                    return;}
            }
        }
        paths.add(this);
    }

    public static void loadAllArenas(){
        for (Plugin plugin: configs.keySet()){
            loadAllArenas(plugin);
        }
    }

    public static void loadAllArenas(Plugin plugin){
        for (ArenaSerializer serializer: configs.get(plugin)){
            serializer.loadArenas(plugin);
        }
    }

    public static void loadAllArenas(Plugin plugin, ArenaType arenaType){
        Set<ArenaSerializer> serializers = configs.get(plugin);
        if (serializers == null || serializers.isEmpty()){
            Log.err(plugin.getName() +" has no arenas to load");
            return;
        }

        for (ArenaSerializer serializer: serializers){
            serializer.loadArenas(plugin,arenaType);
        }
    }

    public void loadArenas(Plugin plugin){
        try {config.load(file);} catch (Exception e){Log.printStackTrace(e);}
        loadArenas(plugin, BattleArena.getBAController(), config,null);
    }

    public void loadArenas(Plugin plugin, ArenaType arenaType){
        try {config.load(file);} catch (Exception e){Log.printStackTrace(e);}
        loadArenas(plugin, BattleArena.getBAController(), config, arenaType);
    }

    protected void loadArenas(Plugin plugin, BattleArenaController bac, //ArenaSerializer arenaSerializer,
                              ConfigurationSection cs, ArenaType arenaType){
        final String pname = "["+plugin.getName()+"] ";
        if (cs == null){
            Log.info(pname+" " + arenaType + " has no arenas, cs is null");
            return;
        }

        ConfigurationSection as = cs.getConfigurationSection("arenas");
        ConfigurationSection bks = cs.getConfigurationSection("brokenArenas");
        if (as == null && bks == null){
            if (Defaults.DEBUG) Log.info(pname+" " + arenaType + " has no arenas, configSectionPath=" + cs.getCurrentPath());
            return;
        }

        List<String> keys = (as == null) ? new ArrayList<String>() : new ArrayList<String>(as.getKeys(false));
        int oldGoodSize = keys.size();
        Set<String> brokenKeys = bks == null ? new HashSet<String>() : bks.getKeys(false);
        int oldBrokenSize = brokenKeys.size();
        keys.addAll(brokenKeys);

        Set<String> brokenArenas = new HashSet<String>();
        Set<String> loadedArenas = new HashSet<String>();
        for (String name : keys){
            if (loadedArenas.contains(name) || brokenArenas.contains(name)) /// We already tried to load this arena
                continue;
            boolean broken = brokenKeys.contains(name);
            String section = broken ? "brokenArenas" : "arenas";
            if (arenaType != null){ /// Are we looking for 1 particular arena type to load
                String path = section+"."+name;
                String atype = cs.getString(path+".type",null);
                if (atype == null || !ArenaType.isSame(atype,arenaType)){
                    /// Its not the same type.. so don't let it affect the sizes of the arena counts
                    if (brokenArenas.remove(name)){
                        oldBrokenSize--;
                    } else{
                        oldGoodSize--;
                    }
                    continue;
                }
            }
            Arena arena = null;
            try{
                arena = loadArena(plugin, bac,cs.getConfigurationSection(section+"."+name));
                if (arena != null){
                    loadedArenas.add(arena.getName());
                    if (broken){
                        transfer(cs,"brokenArenas."+name, "arenas."+name);}
                }
            } catch(IllegalArgumentException e){
                Log.err(e.getMessage());
            } catch(Exception e){
                Log.printStackTrace(e);
            }
            if (arena == null){
                brokenArenas.add(name);
                if (!broken){
                    transfer(cs,"arenas."+name, "brokenArenas."+name);}
            }
        }
        if (!loadedArenas.isEmpty()) {
            Log.info(pname+"Loaded "+arenaType+" arenas: " + StringUtils.join(loadedArenas,", "));
        } else if (Defaults.DEBUG){
            Log.info(pname+"No arenas found for " + cs.getCurrentPath() +"  arenatype="+arenaType +"  cs="+cs.getName());
        }
        if (!brokenArenas.isEmpty()){
            Log.warn("&c"+pname+"&eFailed loading arenas: " + StringUtils.join(brokenArenas, ", ") + " arenatype="+arenaType +" cs="+cs.getName());
        }
        if (oldGoodSize != loadedArenas.size() || oldBrokenSize != brokenArenas.size()){
            try {
                config.save(file);
            } catch (IOException e) {
                Log.printStackTrace(e);
            }
        }
    }

    private static void transfer(ConfigurationSection cs, String string, String string2) {
        try{
            Map<String,Object> map = new HashMap<String,Object>(cs.getConfigurationSection(string).getValues(false));
            cs.createSection(string2, map);
            cs.set(string,null);
        } catch(Exception e){
            Log.printStackTrace(e);
        }
    }

    public static Arena loadArena(Plugin plugin, final BattleArenaController bac, ConfigurationSection cs) {
        String name = cs.getName().toLowerCase();

        ArenaType atype = ArenaType.fromString(cs.getString("type"));
        if (atype==null){
            Log.err(" Arena type not found for " + name);
            return null;
        }
        MatchParams mp = new MatchParams(atype);
        try {
            if (cs.contains("params"))
                mp = ConfigSerializer.loadMatchParams(plugin, atype, name, cs.getConfigurationSection("params"),true);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
        /// Get from the "old" way of specifying teamSize and nTeams
        if (cs.contains("teamSize")) {
            MinMax mm = MinMax.valueOf(cs.getString("teamSize"));
            mp.setTeamSize(mm);
        }
        if (cs.contains("nTeams")) {
            MinMax mm = MinMax.valueOf(cs.getString("nTeams"));
            mp.setNTeams(mm);
        }

        if (!mp.valid()){
            Log.err( name + " This arena is not valid arenaq=" + mp.toString());
            return null;
        }

        final Arena arena = ArenaType.createArena(name, mp,false);
        if (arena == null){
            Log.err("Couldnt load the Arena " + name);
            return null;
        }


        /// Spawns
        Map<Integer, List<SpawnLocation>> locs =
                SerializerUtil.parseLocations(cs.getConfigurationSection("locations"));
        if (locs != null){
            setSpawns(arena,locs);
        }

        /// Wait room spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("waitRoomLocations"));
        if (locs != null){
            RoomContainer rc = RoomController.getOrCreateRoom(arena,LocationType.WAITROOM);
            setSpawns(rc, locs);
        }

        /// Spectate spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("spectateLocations"));
        if (locs != null){
            RoomContainer rc = RoomController.getOrCreateRoom(arena,LocationType.SPECTATE);
            setSpawns(rc, locs);
        }

        /// Visitor spawns
        locs = SerializerUtil.parseLocations(cs.getConfigurationSection("visitorLocations"));
        if (locs != null) {
            RoomContainer rc = RoomController.getOrCreateRoom(arena,LocationType.VISITOR);
            setSpawns(rc, locs);
        }

//        /// Item/mob/group spawns
//        ConfigurationSection spawncs = cs.getConfigurationSection("spawns");
//        if (spawncs != null){
//            for (String spawnStr : spawncs.getKeys(false)){
//                ConfigurationSection scs = spawncs.getConfigurationSection(spawnStr);
//                TimedSpawn s;
//                try {
//                    s = parseSpawnable(scs);
//                } catch (Exception e) {
//                    Log.printStackTrace(e);
//                    continue;
//                }
//                if (s == null)
//                    continue;
//                arena.putTimedSpawn(Long.parseLong(spawnStr), s);
//            }
//        }
        cs = cs.getConfigurationSection("persistable");
        Persistable.yamlToObjects(arena, cs,Arena.class);
        try {
			updateRegions(arena);
		} catch (RegionNotFound e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        ArenaControllerInterface aci = new ArenaControllerInterface(arena);
        aci.init();
        bac.addArena(arena);

        if (arena.getParams().hasAnyOption(TransitionOption.ALWAYSOPEN)) {
            Scheduler.scheduleSynchronousTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        EventOpenOptions eoo = EventOpenOptions.parseOptions(
                                new String[]{"COPYPARAMS"}, null, arena.getParams());
                        Arena a = bac.reserveArena(arena);
                        if (a == null) {
                            Log.warn("&cArena &6" + arena.getName() + " &cwas set to always open but could not be reserved");
                        } else {
                            eoo.setSecTillStart(0);
                            bac.createAndAutoMatch(arena, eoo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return arena;
    }

    private static void setSpawns(AreaContainer rc, Map<Integer, List<SpawnLocation>> locs) {
        for (Entry<Integer, List<SpawnLocation>> entry: locs.entrySet()) {
            try {
                List<SpawnLocation> list = entry.getValue();
                for (int i = 0; i <list.size(); i++){
                    rc.setSpawnLoc(entry.getKey(), i, list.get(i));
                }
            } catch (IllegalStateException e) {
                Log.printStackTrace(e);
            }
        }
    }

    private static void updateRegions(Arena arena) throws RegionNotFound {
        if (!WorldGuardController.hasWorldGuard())
            return;
        if (!arena.hasRegion())
            return;
        if (!WorldGuardController.hasRegion(arena.getWorldGuardRegion())){
            Log.err("Arena " + arena.getName() +" has a world guard region defined but it no longer exists inside of WorldGuard."+
                    "You will have to remake the region.  /arena alter <arena name> addregion");}
        MatchParams mp = ParamController.getMatchParamCopy(arena.getArenaType().getName());
        if (mp == null)
            return;
        StateGraph trans = mp.getThisStateGraph();
        if (trans == null)
            return;
        WorldGuardController.setFlag(arena.getWorldGuardRegion(), "entry", !trans.hasAnyOption(TransitionOption.WGNOENTER));
        WorldGuardController.trackRegion(arena.getWorldGuardRegion());
    }

    private void saveArenas(boolean log) {
        ArenaSerializer.saveArenas(BattleArena.getBAController().getArenas().values(), file, config, plugin,log);
        try {
            config.save(file);
        } catch (IOException e) {
            Log.printStackTrace(e);
        }
    }

    private static void saveArenas(Collection<Arena> arenas, File f, ConfigurationSection config, Plugin plugin, boolean log){
        ConfigurationSection maincs = config.createSection("arenas");
        config.createSection("brokenArenas");
        List<String> saved = new ArrayList<String>();
        for (Arena arena : arenas){
            String arenaname = null;
            try{
                arenaname = arena.getName();
                ArenaType at = arena.getArenaType();
                if (!at.getPlugin().getName().equals(plugin.getName()))
                    continue;
                ArenaParams parentParams = arena.getParams().getParent();
                arena.getParams().setParent(null);
                HashMap<String, Object> amap = new HashMap<String, Object>();
                amap.put("type", at.getName());

                /// Spawn locations
                Map<String, List<String>> locs = SerializerUtil.toSpawnMap(arena);
                if (locs != null){
                    amap.put("locations", locs);}

                /// Wait room spawns
                locs = SerializerUtil.toSpawnMap(arena.getWaitroom());
                if (locs != null) {
                    amap.put("waitRoomLocations", locs);}

                /// spectate locations
                locs = SerializerUtil.toSpawnMap(arena.getSpectatorRoom());
                if (locs != null) {
                    amap.put("spectateLocations", locs);}

                /// Visitor locations
                locs = SerializerUtil.toSpawnMap(arena.getVisitorRoom());
                if (locs != null) {
                    amap.put("visitorLocations", locs);}

                Map<String,Object> persisted = Persistable.objectsToYamlMap(arena, Arena.class);
                if (persisted != null && !persisted.isEmpty()){
                    amap.put("persistable", persisted);
                }
                saved.add(arenaname);

                ConfigurationSection arenacs = maincs.createSection(arenaname);
                SerializerUtil.expandMapIntoConfig(arenacs, amap);

                ConfigSerializer.saveParams(arena.getParams(), arenacs.createSection("params"), true);
                arena.getParams().setParent(parentParams);

                config.set("brokenArenas."+arenaname, null); /// take out any duplicate names in broken arenas
            } catch (Exception e){
                Log.printStackTrace(e);
                if (arenaname != null){
                    transfer(config, "arenas."+arenaname, "brokenArenas."+arenaname);
                }
            }
        }
        if (log)
            Log.info(plugin.getName() + " Saving arenas " + StringUtils.join(saved,",") +" to " +
                    f.getPath() + " configSection="+config.getCurrentPath()+"." + config.getName());
    }


    protected void saveArenas() {
        saveArenas(false);
    }
    @Override
    public void save() {
        this.saveArenas(true);
    }

    public static void saveAllArenas(boolean log){
        for (Plugin plugin: configs.keySet()){
            for (ArenaSerializer serializer: configs.get(plugin)){
                serializer.saveArenas(log);
            }
        }
    }

    public static void saveArenas(Plugin plugin){
        if (!configs.containsKey(plugin))
            return;
        for (ArenaSerializer serializer: configs.get(plugin)){
            serializer.saveArenas(true);
        }
    }
}
