package io.github.TcFoxy.ArenaTOW.Plugin;

import io.github.TcFoxy.ArenaTOW.API.MobType;
import io.github.TcFoxy.ArenaTOW.API.TOWEntity;
import io.github.TcFoxy.ArenaTOW.Plugin.Serializable.PersistInfo;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.util.Log;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.*;

public class TugListener implements Listener {


    private TugArena tug;


    public TugListener(TugArena tug) {
        this.tug = tug;
    }



    /*
     * used to make sure that entities
     * of the same team will not pathfind towards friendly targets
     * or targets that are invisible.
     */
	@EventHandler
	private void sameTeamTarget(EntityTargetEvent event){
		if (event.getTarget() == null) return;


	}

    /*
     * when a player kills a minion, tower, or player,
     * their team level increase, and they get money.
     */
//	@EventHandler 
//	public void minionDeath(EntityDeathEvent event){
//		if(event.getEntity() instanceof IronGolem || event.getEntity() instanceof Zombie){
//			EntityLiving el = (EntityLiving) ((CraftEntity) event.getEntity().getLastDamageCause().getEntity()).getHandle();
//			String entityclass = el.getClass().getName();
//			ArenaTeam team;
//			Integer q = 0;
//			switch(entityclass){
//			case NMSConstants.customRedZombie:
//				team = tug.blueTeam;
//				q = 1;
//				break;
//			case NMSConstants.customRedGolem:
//				team = tug.blueTeam;
//				q = 50;
//				break;
//			case NMSConstants.customBlueZombie:
//				team = tug.redTeam;
//				q = 1;
//				break;
//			case NMSConstants.customBlueGolem:
//				team = tug.redTeam;
//				q = 50;
//				break;
//			default:
//				return;
//			}
//			if(team == null){
//				return;
//			}
//			Set<Player> players = team.getBukkitPlayers();
//			for(Player p: players){
//				ArenaEcon.addCash(p, q);
//				if(tug.sh == null){
//					Bukkit.broadcastMessage("sh == null");
//				}
//				tug.sh.refreshScore(p);
//			}
//			tug.teamLevel.addTeamPoint(q, team);
//			
//		}
//				
//	}

    /*
     * golem's fireballs shouldnt hurt same team
     */

    @EventHandler
    private void noFireBallDmg(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof TOWEntity &&
                ((TOWEntity) event.getDamager()).getMobType() == MobType.FIREBALL) {

            TOWEntity fireball = (TOWEntity) event.getDamager();

            if (fireball.isSameTeam(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }


    /*
     * used to prevent any annoying loot drop by entities.
     */
    @EventHandler
    private void noLootDrop(EntityDeathEvent event) {
        event.setDroppedExp(0);
        event.getDrops().clear();
    }

    /*
     * no breaking blocks
     */
    @EventHandler
    private void noBreakBlocks(BlockBreakEvent event) {
        for (ArenaPlayer ap : tug.arena.getMatch().getAlivePlayers()) {
            if (event.getPlayer() == ap.getPlayer()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "Spamy Spam Spam - That's what you get for breaking blocks in the Arena!");
            }
        }
    }

    /*
     * used to stop incendiary fireballs from igniting blocks
     */
    @EventHandler
    private void noFireballFire(BlockIgniteEvent event) {
        if (event.getCause().equals(IgniteCause.FIREBALL)) event.setCancelled(true);
    }


    /*
     * used to set the victors if one of the nexi is destroyed
     */
    @EventHandler
    private void nexusDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.GUARDIAN)
            return;
        for (PersistInfo b : tug.activeInfo.values()) {
            if (b.hasMob() && b.getMob().isAlive()) {
                if (b.getMob().getMobType() == MobType.NEXUS) {
                    tug.arena.getMatch().setVictor(tug.redTeam);
                    return;
                } else if (b.getMob().getMobType() == MobType.NEXUS) {
                    tug.arena.getMatch().setVictor(tug.blueTeam);
                    return;
                }
            }
        }
    }

    /*
     * disable the explosions and fire from wizard fireballs
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity ent = event.getEntity();

        if (ent instanceof Creeper || ent instanceof Fireball) {
            event.setCancelled(true); //Removes block damage
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        event.setFire(false); //Only really needed for fireballs

        Entity ent = event.getEntity();
        if (ent instanceof Fireball)
            event.setRadius(2); //Increased from default(1), since the fireball now doesn't cause fire
    }


    /*
     * used to activate item upgrade chest
     */
//	@EventHandler
//	public void itemUpgrades(PlayerInteractEvent event){
//		if(event.getClickedBlock() == null){
//			return;
//		}
//		if(event.getClickedBlock().getType() == Material.ANVIL){
//			event.setCancelled(true);
//			tug.uGUI.openGUI(event);
//		}
//	}

}