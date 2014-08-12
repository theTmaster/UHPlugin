package me.azenet.UHPlugin.listeners;

import java.util.ArrayList;
import java.util.List;

import me.azenet.UHPlugin.UHPlugin;
import me.azenet.UHPlugin.i18n.I18n;
import me.azenet.UHPlugin.task.CancelBrewTask;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class UHGameplayListener implements Listener {

	private UHPlugin p = null;
	private I18n i = null;
	
	public UHGameplayListener(UHPlugin p) {
		this.p = p;
		this.i = p.getI18n();
	}
	
	
	/**
	 * Used to replace ghast tears with gold (if needed).
	 * 
	 * @param ev
	 */
	@EventHandler(ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent ev) {
		if (ev.getEntity() instanceof Ghast && p.getConfig().getBoolean("gameplay-changes.replaceGhastTearsWithGold")) {
			List<ItemStack> drops = new ArrayList<ItemStack>(ev.getDrops());
			ev.getDrops().clear(); 
			for (ItemStack i : drops) {
				if (i.getType() == Material.GHAST_TEAR) {
					ev.getDrops().add(new ItemStack(Material.GOLD_INGOT,i.getAmount()));
				} else {
					ev.getDrops().add(i);
				}
			}
		}
	}
	
	/**
	 * Used to prevent the user to get a ghast tear, if forbidden by the config.
	 * 
	 * @param ev
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPickupItem(PlayerPickupItemEvent ev) {
		if (ev.getItem().getItemStack().getType() == Material.GHAST_TEAR && ev.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && p.getConfig().getBoolean("gameplay-changes.replaceGhastTearsWithGold")) {
			ev.setCancelled(true);
		}
	}
	
	
	/**
	 * Used to disable power-II potions.
	 * 
	 * @param ev
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent ev) {
		if(p.getConfig().getBoolean("gameplay-changes.disableLevelIIPotions") && ev.getInventory() instanceof BrewerInventory) {
			BukkitRunnable cancelBrewTask = new CancelBrewTask((BrewerInventory) ev.getInventory(), ev.getWhoClicked());
			cancelBrewTask.runTaskLater(p, 1l);
		}
	}
	
	/**
	 * Used to disable power-II potions.
	 * 
	 * @param ev
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent ev) {
		if(ev.getInventory() instanceof BrewerInventory) {
			if(p.getConfig().getBoolean("gameplay-changes.disableLevelIIPotions")) {
				BukkitRunnable cancelBrewTask = new CancelBrewTask((BrewerInventory) ev.getInventory(), ev.getWhoClicked());
				cancelBrewTask.runTaskLater(p, 1l);
			}
		}
	}
	
	
	/**
	 * Used to disable enderpearl damages (if needed)
	 * 
	 * @param ev
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerTeleport(final PlayerTeleportEvent ev) {
		if(p.getConfig().getBoolean("gameplay-changes.disableEnderpearlsDamages")) {
			if(ev.getCause() == TeleportCause.ENDER_PEARL) {
				ev.setCancelled(true);
				ev.getPlayer().teleport(ev.getTo());
			}
		}
	}
	
	
	/**
	 * Used to change the amount of regenerated hearts from a golden apple.
	 * 
	 * @param ev
	 */
	@EventHandler
	public void onPlayerItemConsume(final PlayerItemConsumeEvent ev) {
		
		final int TICKS_BETWEEN_EACH_REGENERATION = 50;
		final int DEFAULT_NUMBER_OF_HEARTS_REGEN = 4;
		final int DEFAULT_NUMBER_OF_HEARTS_REGEN_NOTCH = 180;
		final int REGENERATION_LEVEL_GOLDEN_APPLE = 2;
		final int REGENERATION_LEVEL_NOTCH_GOLDEN_APPLE = 5;
		
		if(ev.getItem().getType() == Material.GOLDEN_APPLE) {
			ItemMeta meta = ev.getItem().getItemMeta();
			short dataValue = ev.getItem().getDurability();
			int halfHearts = 0;
			int level = 0;
			
			if(meta.hasDisplayName()
					&& (meta.getDisplayName().equals(ChatColor.RESET + i.t("craft.goldenApple.nameGoldenAppleFromHeadNormal"))
					|| meta.getDisplayName().equals(ChatColor.RESET + i.t("craft.goldenApple.nameGoldenAppleFromHeadNotch")))) {
				
				if(dataValue == 0) { // Normal golden apple from a head
					halfHearts = p.getConfig().getInt("gameplay-changes.goldenApple.regeneration.fromNormalHead", DEFAULT_NUMBER_OF_HEARTS_REGEN);
					level = REGENERATION_LEVEL_GOLDEN_APPLE;
				}
				else { // Notch golden apple from a head
					halfHearts = p.getConfig().getInt("gameplay-changes.goldenApple.regeneration.fromNotchHead", DEFAULT_NUMBER_OF_HEARTS_REGEN_NOTCH);
					level = REGENERATION_LEVEL_NOTCH_GOLDEN_APPLE;
				}
			}
			else if(dataValue == 0) { // Normal golden apple from an apple
				halfHearts = p.getConfig().getInt("gameplay-changes.goldenApple.regeneration.normal", DEFAULT_NUMBER_OF_HEARTS_REGEN);
				level = REGENERATION_LEVEL_GOLDEN_APPLE;
			}
			else { // Notch golden apple from an apple
				halfHearts = p.getConfig().getInt("gameplay-changes.goldenApple.regeneration.notch", DEFAULT_NUMBER_OF_HEARTS_REGEN_NOTCH);
				level = REGENERATION_LEVEL_NOTCH_GOLDEN_APPLE;
			}
			
			// Technically, a level-I effect is « level 0 ».
			final int realLevel = level - 1;
			
			
			// What is needed to do?
			if((dataValue == 0 && halfHearts == DEFAULT_NUMBER_OF_HEARTS_REGEN)
					|| (dataValue == 1 && halfHearts == DEFAULT_NUMBER_OF_HEARTS_REGEN_NOTCH)) {
				
				// Default behavior, nothing to do.
			}
			else if((dataValue == 0 && halfHearts > DEFAULT_NUMBER_OF_HEARTS_REGEN)
					|| (dataValue == 1 && halfHearts > DEFAULT_NUMBER_OF_HEARTS_REGEN_NOTCH)) {
				
				// If the heal needs to be increased, the effect can be applied immediately.
				
				int duration = ((int) Math.floor(TICKS_BETWEEN_EACH_REGENERATION / (Math.pow(2, realLevel)))) * halfHearts;
				
				new PotionEffect(PotionEffectType.REGENERATION, duration, realLevel).apply(ev.getPlayer());
			}
			else {
				// The heal needs to be decreased.
				// We can't apply the effect immediately, because the server will just ignore it.
				// So, we apply it two ticks later, with one half-heart less (because in two ticks, 
				// one half-heart is given to the player).
				
				final int healthApplied = halfHearts - 1;
				
				Bukkit.getScheduler().runTaskLater(this.p, new BukkitRunnable() {
					@Override
					public void run() {		
						// The original, vanilla, effect is removed
						ev.getPlayer().removePotionEffect(PotionEffectType.REGENERATION);
						
						
						int duration = ((int) Math.floor(TICKS_BETWEEN_EACH_REGENERATION / (Math.pow(2, realLevel)))) * healthApplied;
						
						p.getLogger().info(String.valueOf(healthApplied));
						p.getLogger().info(String.valueOf(Math.floor(TICKS_BETWEEN_EACH_REGENERATION / (Math.pow(2, realLevel)))));
						p.getLogger().info(String.valueOf(duration));
						
						new PotionEffect(PotionEffectType.REGENERATION, duration, realLevel).apply(ev.getPlayer());
					}
				}, 2l);
			}
		}
	}
	
	
	/**
	 * Used to update the compass.
	 * 
	 * @param ev
	 */
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent ev) {
		if ((ev.getAction() == Action.RIGHT_CLICK_AIR || ev.getAction() == Action.RIGHT_CLICK_BLOCK) && ev.getPlayer().getItemInHand().getType() == Material.COMPASS && p.getConfig().getBoolean("gameplay-changes.compass") && !p.getGameManager().isPlayerDead(ev.getPlayer().getName())) {
			Player player1 = ev.getPlayer();
			
			Boolean foundRottenFlesh = false;
			for (ItemStack item : player1.getInventory().getContents()) {
				if (item != null && item.getType() == Material.ROTTEN_FLESH) {
					if (item.getAmount() != 1) {
						item.setAmount(item.getAmount()-1);
					}
					else {
						player1.getInventory().removeItem(item); 
					}
					
					player1.updateInventory();
					foundRottenFlesh = true;
					break;
				}
			}
			
			if (!foundRottenFlesh) {
				player1.sendMessage(i.t("compass.noRottenFlesh"));
				player1.playSound(player1.getLocation(), Sound.STEP_WOOD, 1F, 1F);
				return;
			}
			
			player1.playSound(player1.getLocation(), Sound.BURP, 1F, 1F);
			
			Player nearest = null;
			Double distance = 99999D;
			for (String player2Name : p.getGameManager().getAlivePlayers()) {
				Player player2 = p.getServer().getPlayer(player2Name);
				
				try {	
					Double calc = player1.getLocation().distance(player2.getLocation());
					
					if (calc > 1 && calc < distance) {
						distance = calc;
						if (player2.getName() != player1.getName() && !this.p.getTeamManager().inSameTeam(player1, player2)) {
							nearest = player2.getPlayer();
						}
					}
				} catch (Exception ignored) {
					
				}
			}
			
			if (nearest == null) {
				player1.sendMessage(i.t("compass.nothingFound"));
				return;
			}
			
			player1.sendMessage(i.t("compass.success"));
			player1.setCompassTarget(nearest.getLocation());
		}
	}
	
	
	/**
	 * Used to disable the "bad" weather (aka non-clear weather).
	 * The weather is initially clear.
	 * 
	 * @param ev
	 */
	@EventHandler
	public void onWeatherChange(WeatherChangeEvent ev) {		
		if (!p.getConfig().getBoolean("gameplay-changes.weather")) {
			ev.setCancelled(true);
		}
	}
}
