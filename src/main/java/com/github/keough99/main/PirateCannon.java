package com.github.keough99.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class PirateCannon
  extends JavaPlugin
  implements Listener
{
  public static BukkitTask pid;
  public static FileConfiguration idConfig = null;
  public static File idConfigFile = null;
  HashMap<String, Long> playerlastshot = new HashMap();
  ArrayList<Entity> projectiles = new ArrayList();
  ArrayList<Integer> unusedids = new ArrayList();
  HashMap<Integer, Long> cannonlastshot = new HashMap();
  
  public void onEnable()
  {
    Cannon.init(this);
    getLogger().info("PirateCannons Enabled!");
    Bukkit.getPluginManager().registerEvents(this, this);
    getConfig().options().copyDefaults(true);
    saveDefaultConfig();
    idConfigFile = new File("plugins/PirateCannon/idConfig.yml");
    idConfig = YamlConfiguration.loadConfiguration(idConfigFile);
    idConfig.options().header("This is a list of unused cannon id's, please do not edit this!");
    
    this.unusedids = stringToList(idConfig.getString("unusedids"));
    if (this.unusedids.size() < 1)
    {
      this.unusedids.add(Integer.valueOf(1));
      this.unusedids.add(Integer.valueOf(2));
      idConfig.set("unusedids", listToString(this.unusedids));
      saveIDConfig();
      this.unusedids = stringToList(idConfig.getString("unusedids"));
    }
    Cannon.checkConfig();
  }
  
  public void onDisable()
  {
    idConfig.set("unusedids", listToString(this.unusedids));
    saveIDConfig();
    saveDefaultConfig();
    getLogger().info("PirateCannons Disabled!");
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
  {
    if (cmd.getName().equalsIgnoreCase("cannon"))
    {
      sender.sendMessage(ChatColor.AQUA + "PirateCannons are enabled on this Server.");
      return true;
    }
    return false;
  }
  
  @EventHandler
  public void PlayerInteract(PlayerInteractEvent event)
  {
    if ((event.getAction() == Action.RIGHT_CLICK_BLOCK) && (event.getClickedBlock().getType() == Material.WALL_SIGN))
    {
      Sign sign = (Sign)event.getClickedBlock().getState();
      if (sign.getLine(0).contains("[Cannon]"))
      {
        int cannonid = Cannon.checkCannonId(event.getClickedBlock().getLocation(), this.unusedids);
        
        boolean playerpermitted = Cannon.checkPermission(event.getPlayer().getName());
        
        Date date = new Date();
        boolean playerdelay = false;
        boolean cannondelay = false;
        
        playerdelay = Cannon.getPlayerCooldown(event.getPlayer().getName(), (Long)this.playerlastshot.get(event.getPlayer().getName()));
        
        cannondelay = Cannon.getCannonCooldown(cannonid, (Long)this.cannonlastshot.get(Integer.valueOf(cannonid)), event.getPlayer().getName());
        
        boolean playerhasammo = Cannon.checkAmmo(event.getPlayer().getName());
        
        boolean playerhastool = Cannon.checkTool(event.getPlayer());
        if ((sign.getLine(0).contains("[Cannon]")) && (playerdelay) && (cannondelay) && (playerhasammo) && (playerhastool) && (playerpermitted))
        {
          Location loc = sign.getLocation();
          Vector direction = Cannon.getDirection(loc);
          Vector correction = Cannon.getCorrection(loc);
          int smokedirection = Cannon.getSmokeDirection(loc);
          int length = Cannon.getLength(loc, direction);
          Vector launch = Cannon.getLaunchVector(direction, length);
          if ((getConfig().getInt("mincannonsize") <= length) && (length <= getConfig().getInt("maxcannonsize")))
          {
            this.projectiles.add(Cannon.shoot(loc, launch, correction, smokedirection));
            
            Cannon.removeAmmo(event.getPlayer().getName());
            Cannon.removeDurability(event.getPlayer());
            this.playerlastshot.put(event.getPlayer().getName(), Long.valueOf(date.getTime()));
            this.cannonlastshot.put(Integer.valueOf(cannonid), Long.valueOf(date.getTime()));
          }
        }
        event.setCancelled(true);
      }
    }
    if (((event.getAction() == Action.RIGHT_CLICK_BLOCK) || (event.getAction() == Action.LEFT_CLICK_BLOCK)) && 
      (event.getClickedBlock().getType() == Material.getMaterial(getConfig().getInt("rotationblock"))) && (getConfig().getBoolean("allowrotation")) && (
      (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) || (event.getAction().equals(Action.LEFT_CLICK_BLOCK)))) {
      for (int i = 0; i < 4; i++)
      {
        Location loc = event.getClickedBlock().getLocation();
        switch (i)
        {
        case 0: 
          loc.setX(loc.getX() - 1.0D);
          loc.setY(loc.getY() - 1.0D);
          break;
        case 1: 
          loc.setX(loc.getX() + 1.0D);
          loc.setY(loc.getY() - 1.0D);
          break;
        case 2: 
          loc.setZ(loc.getZ() - 1.0D);
          loc.setY(loc.getY() - 1.0D);
          break;
        case 3: 
          loc.setZ(loc.getZ() + 1.0D);
          loc.setY(loc.getY() - 1.0D);
        }
        if (loc.getBlock().getType().equals(Material.WALL_SIGN))
        {
          Sign sign = (Sign)loc.getBlock().getState();
          if (sign.getLine(0).contains("[Cannon]"))
          {
            Vector direction = Cannon.getDirection(loc);
            int length = Cannon.getLength(loc, direction);
            String turn = "";
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
              turn = "right";
            } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
              turn = "left";
            }
            Vector targetdirection = Cannon.getTurnTo(direction, turn);
            byte signdirection = Cannon.getTurnedSignDirection(targetdirection);
            
            Block signfrom = loc.getBlock();
            Vector dir = new Vector(direction.getX(), direction.getY(), direction.getZ());
            Vector targetdir = new Vector(targetdirection.getX(), targetdirection.getY(), targetdirection.getZ());
            Block signto = loc.toVector().add(dir.add(targetdir.multiply(-1))).toLocation(loc.getWorld()).getBlock();
            ArrayList<Block> turnfrom = new ArrayList();
            ArrayList<Block> turnto = new ArrayList();
            loc = event.getClickedBlock().getLocation();
            Location saveloc = loc;
            loc.setY(loc.getY() - 1.0D);
            for (int j = 1; j <= length; j++)
            {
              dir = new Vector(direction.getX(), direction.getY(), direction.getZ());
              saveloc = loc;
              Block from = loc.toVector().add(dir.multiply(j)).toLocation(loc.getWorld()).getBlock();
              saveloc = loc;
              targetdir = new Vector(targetdirection.getX(), targetdirection.getY(), targetdirection.getZ());
              Block to = loc.toVector().add(targetdir.multiply(j)).toLocation(loc.getWorld()).getBlock();
              turnfrom.add(from);
              turnto.add(to);
            }
            boolean enoughspace = true;
            for (Block block : turnto) {
              if (block.getType() != Material.AIR) {
                enoughspace = false;
              }
            }
            if (signto.getType() != Material.AIR) {
              enoughspace = false;
            }
            if (enoughspace)
            {
              for (int j = 0; j < turnfrom.size(); j++)
              {
                ((Block)turnto.get(j)).setType(((Block)turnfrom.get(j)).getType());
                ((Block)turnfrom.get(j)).setType(Material.AIR);
              }
              String[] lines = ((Sign)signfrom.getState()).getLines();
              signfrom.setType(Material.AIR);
              signto.setType(Material.WALL_SIGN);
              signto.setData(signdirection);
              Sign turnsign = (Sign)signto.getState();
              turnsign.setLine(0, lines[0]);
              turnsign.setLine(1, lines[1]);
              turnsign.setLine(2, lines[2]);
              turnsign.setLine(3, lines[3]);
              turnsign.update(true);
            }
          }
          i = 10;
          event.setCancelled(true);
        }
      }
    }
  }
  
  @EventHandler
  public void Sign(SignChangeEvent event)
  {
    if (event.getPlayer().hasPermission("cannon.create"))
    {
      String line0 = event.getLine(0);
      if ((line0.equalsIgnoreCase("c")) && (getConfig().getBoolean("useplayercooldown")))
      {
        event.getPlayer().sendMessage(ChatColor.AQUA + "Cannon created successfully.");
        event.setLine(0, "[Cannon]");
        event.setLine(1, "kaboom");
        event.setLine(2, ":D");
        event.setLine(3, "");
      }
      if ((line0.equalsIgnoreCase("[Cannon]")) && (getConfig().getBoolean("useplayercooldown")))
      {
        event.getPlayer().sendMessage(ChatColor.AQUA + "Cannon created successfully.");
        event.setLine(0, "[Cannon]");
        event.setLine(1, "kaboom");
        event.setLine(2, ":D");
        event.setLine(3, "");
      }
      if ((line0.equalsIgnoreCase("c")) && (getConfig().getBoolean("usecannoncooldown")))
      {
        boolean changedsth = true;
        int i;
        for (; changedsth; i < this.unusedids.size() - 1)
        {
          changedsth = false;
          i = 0; continue;
          int swap = 0;
          if (((Integer)this.unusedids.get(i)).intValue() > ((Integer)this.unusedids.get(i + 1)).intValue())
          {
            swap = ((Integer)this.unusedids.get(i)).intValue();
            this.unusedids.set(i, (Integer)this.unusedids.get(i + 1));
            this.unusedids.set(i + 1, Integer.valueOf(swap));
            changedsth = true;
          }
          i++;
        }
        event.getPlayer().sendMessage(ChatColor.AQUA + "Cannon created successfully.");
        event.setLine(0, "[Cannon]");
        event.setLine(1,""+ this.unusedids.get(0));
        event.setLine(2, "");
        event.setLine(3, "");
        if (this.unusedids.size() <= 1) {
          this.unusedids.add(Integer.valueOf(((Integer)this.unusedids.get(0)).intValue() + 1));
        }
        this.unusedids.remove(0);
      }
      if ((line0.equalsIgnoreCase("[Cannon]")) && (getConfig().getBoolean("usecannoncooldown")))
      {
        boolean changedsth = true;
        int i;
        for (; changedsth; i < this.unusedids.size() - 1)
        {
          changedsth = false;
          i = 0; continue;
          int swap = 0;
          if (((Integer)this.unusedids.get(i)).intValue() > ((Integer)this.unusedids.get(i + 1)).intValue())
          {
            swap = ((Integer)this.unusedids.get(i)).intValue();
            this.unusedids.set(i, (Integer)this.unusedids.get(i + 1));
            this.unusedids.set(i + 1, Integer.valueOf(swap));
            changedsth = true;
          }
          i++;
        }
        event.getPlayer().sendMessage(ChatColor.AQUA + "Cannon created successfully.");
        event.setLine(0, "[Cannon]");
        event.setLine(1,""+ this.unusedids.get(0));
        event.setLine(2, "");
        event.setLine(3, "");
        if (this.unusedids.size() <= 1) {
          this.unusedids.add(Integer.valueOf(((Integer)this.unusedids.get(0)).intValue() + 1));
        }
        this.unusedids.remove(0);
      }
    }
  }
  
  @EventHandler
  public void onProjectileHit(ProjectileHitEvent event)
  {
    if (this.projectiles.contains(event.getEntity()))
    {
      Location loc = event.getEntity().getLocation();
      event.getEntity().remove();
      int power = getConfig().getInt("explosion.power");
      boolean setfire = getConfig().getBoolean("explosion.setfire");
      boolean blockdamage = getConfig().getBoolean("explosion.blockdamage");
      loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setfire, blockdamage);
      if (getConfig().getDouble("explosion.playerdamage") > 0.0D)
      {
        List<Entity> nearby = event.getEntity().getNearbyEntities(getConfig().getDouble("explosion.playerdamagerange"), getConfig().getDouble("explosion.playerdamagerange") + 1.0D, getConfig().getDouble("explosion.playerdamagerange"));
        for (int i = 0; i < nearby.size(); i++) {
          if (((nearby.get(i) instanceof Player)) && (((Player)nearby.get(i)).getGameMode() != GameMode.CREATIVE) && (((CraftPlayer)nearby.get(i)).getHealth() > 0.0D)) {
            if (((CraftPlayer)nearby.get(i)).getHealth() - getConfig().getDouble("explosion.playerdamage") <= 0.0D) {
              ((CraftPlayer)nearby.get(i)).setHealth(0.0D);
            } else {
              ((CraftPlayer)nearby.get(i)).setHealth(((CraftPlayer)nearby.get(i)).getHealth() - getConfig().getDouble("explosion.playerdamage"));
            }
          }
        }
      }
      if (getConfig().getDouble("explosion.entitydamage") > 0.0D)
      {
        List<Entity> nearby = event.getEntity().getNearbyEntities(getConfig().getDouble("explosion.entitydamagerange"), getConfig().getDouble("explosion.entitydamagerange") + 1.0D, getConfig().getDouble("explosion.entitydamagerange"));
        for (int i = 0; i < nearby.size(); i++) {
          if (((nearby.get(i) instanceof LivingEntity)) && (!(nearby.get(i) instanceof Player))) {
            if (((CraftLivingEntity)nearby.get(i)).getHealth() - getConfig().getDouble("explosion.entitydamage") <= 0.0D) {
              ((CraftLivingEntity)nearby.get(i)).setHealth(0.0D);
            } else {
              ((CraftLivingEntity)nearby.get(i)).setHealth(((CraftLivingEntity)nearby.get(i)).getHealth() - getConfig().getDouble("explosion.entitydamage"));
            }
          }
        }
      }
      this.projectiles.remove(event.getEntity());
    }
  }
  
  @EventHandler
  public void onBlockBreak(BlockBreakEvent event)
  {
    Block block = event.getBlock();
    if (block.getType().equals(Material.WALL_SIGN))
    {
      Sign sign = (Sign)block.getState();
      if ((sign.getLine(0).contains("[Cannon]")) && (getConfig().getBoolean("usecannoncooldown"))) {
        try
        {
          this.unusedids.add(Integer.valueOf(Integer.parseInt(sign.getLine(1))));
        }
        catch (Exception localException) {}
      }
    }
  }
  
  public String listToString(ArrayList<Integer> list)
  {
    String string = "";
    for (Integer i : list) {
      if (string.equals("")) {
        string = ""+ i;
      } else {
        string = string + "," + i;
      }
    }
    return string;
  }
  
  public ArrayList<Integer> stringToList(String string)
  {
    try
    {
      ArrayList<Integer> list = new ArrayList();
      String[] stringlist = string.split(",");
      String[] arrayOfString1;
      int j = (arrayOfString1 = stringlist).length;
      for (int i = 0; i < j; i++)
      {
        String s = arrayOfString1[i];
        list.add(Integer.valueOf(Integer.parseInt(s)));
      }
      return list;
    }
    catch (Exception e)
    {
      getLogger().info("Error in config.yml: Unused Cannon id's could not be loaded!");
      return new ArrayList();
    }
  }
  
  public static void saveIDConfig()
  {
    if ((idConfig == null) || (idConfigFile == null)) {
      return;
    }
    try
    {
      idConfig.save(idConfigFile);
    }
    catch (IOException ex)
    {
      Bukkit.getLogger().log(Level.SEVERE, "Could not save idconfig to " + idConfigFile, ex);
    }
  }
  
  @EventHandler
  public void onRedstoneChange(BlockRedstoneEvent event)
  {
    if (getConfig().getList("cannontypes").contains(Integer.valueOf(event.getBlock().getRelative(BlockFace.UP).getTypeId())))
    {
      Sign sign = null;
      for (int i = 0; i <= 3; i++) {
        switch (i)
        {
        case 0: 
          if (event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.EAST).getType() == Material.WALL_SIGN) {
            sign = (Sign)event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.EAST).getState();
          }
          break;
        case 1: 
          if (event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).getType() == Material.WALL_SIGN) {
            sign = (Sign)event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.NORTH).getState();
          }
          break;
        case 2: 
          if (event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.WEST).getType() == Material.WALL_SIGN) {
            sign = (Sign)event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.WEST).getState();
          }
          break;
        case 3: 
          if (event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).getType() == Material.WALL_SIGN) {
            sign = (Sign)event.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH).getState();
          }
          break;
        }
      }
      if ((sign != null) && (sign.getLine(0).equalsIgnoreCase("[Cannon]")))
      {
        int cannonid = Cannon.checkCannonId(sign.getLocation(), this.unusedids);
        
        Date date = new Date();
        boolean cannondelay = Cannon.getCannonCooldown(cannonid, (Long)this.cannonlastshot.get(Integer.valueOf(cannonid)), null);
        if (cannondelay) {
          this.cannonlastshot.put(Integer.valueOf(cannonid), Long.valueOf(date.getTime()));
        }
        if ((getConfig().getBoolean("useredstonetriggers")) && (sign.getLine(0).contains("[Cannon]")) && (cannondelay))
        {
          Location loc = sign.getLocation();
          Vector direction = Cannon.getDirection(loc);
          Vector correction = Cannon.getCorrection(loc);
          int smokedirection = Cannon.getSmokeDirection(loc);
          int length = Cannon.getLength(loc, direction);
          Vector launch = Cannon.getLaunchVector(direction, length);
          if ((getConfig().getInt("mincannonsize") <= length) && (length <= getConfig().getInt("maxcannonsize"))) {
            this.projectiles.add(Cannon.shoot(loc, launch, correction, smokedirection));
          }
        }
      }
    }
  }
}