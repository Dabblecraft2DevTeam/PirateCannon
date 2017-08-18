package com.github.keough99.main;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Cannon
{
  private static PirateCannon plugin;
  
  public static void init(PirateCannon piratecannon)
  {
    plugin = piratecannon;
  }
  
  public static void checkConfig()
  {
    if (plugin.getConfig().getInt("mincannonsize") < 1)
    {
      plugin.getConfig().set("mincannonsize", Integer.valueOf(1));
      plugin.getLogger().info("Error in plugins/PirateCannon/config.yml: mincannonsize cant be smaller than 1!");
    }
    if (plugin.getConfig().getInt("mincannonsize") >= plugin.getConfig().getInt("maxcannonsize"))
    {
      plugin.getConfig().set("maxcannonsize", Integer.valueOf(plugin.getConfig().getInt("mincannonsize") + 1));
      plugin.getLogger().info("Error in plugins/PirateCannon/config.yml: maxcannonsize cant be smaller or the same as mincannonsize!");
    }
  }
  
  public static boolean checkPermission(String playername)
  {
    boolean playerpermitted = false;
    if ((Bukkit.getPlayer(playername).isOp()) || (Bukkit.getPlayer(playername).hasPermission("cannon.use")) || (plugin.getConfig().getString("permissions").equalsIgnoreCase("all"))) {
      playerpermitted = true;
    } else {
      Bukkit.getPlayer(playername).sendMessage(ChatColor.RED + "You dont have permission.");
    }
    return playerpermitted;
  }
  
  public static int checkCannonId(Location loc, ArrayList<Integer> unusedids)
  {
    int cannonid = 0;
    Sign sign = (Sign)loc.getWorld().getBlockAt(loc).getState();
    if (plugin.getConfig().getBoolean("usecannoncooldown")) {
      try
      {
        cannonid = Integer.parseInt(sign.getLine(1));
      }
      catch (Exception e)
      {
        boolean changedsth = true;
        int i;
        for (; changedsth; i < unusedids.size() - 1)
        {
          changedsth = false;
          i = 0; continue;
          int swap = 0;
          if (((Integer)unusedids.get(i)).intValue() > ((Integer)unusedids.get(i + 1)).intValue())
          {
            swap = ((Integer)unusedids.get(i)).intValue();
            unusedids.set(i, (Integer)unusedids.get(i + 1));
            unusedids.set(i + 1, Integer.valueOf(swap));
            changedsth = true;
          }
          i++;
        }
        sign.setLine(0, "[Cannon]");
        sign.setLine(1, unusedids.get(0));
        sign.setLine(2, "");
        sign.setLine(3, "");
        sign.update(true);
        
        cannonid = ((Integer)unusedids.get(0)).intValue();
        if (unusedids.size() <= 1) {
          unusedids.add(Integer.valueOf(((Integer)unusedids.get(0)).intValue() + 1));
        }
        unusedids.remove(0);
      }
    }
    return cannonid;
  }
  
  public static boolean getPlayerCooldown(String playername, Long playerdelay)
  {
    Date date = new Date();
    boolean cooleddown = false;
    if (plugin.getConfig().getBoolean("useplayercooldown"))
    {
      long playerdelaytime = plugin.getConfig().getInt("playercooldown") + 1;
      try
      {
        playerdelaytime = date.getTime() - playerdelay.longValue();
      }
      catch (Exception localException) {}
      if (playerdelaytime >= plugin.getConfig().getInt("playercooldown")) {
        cooleddown = true;
      }
    }
    else
    {
      cooleddown = true;
    }
    return cooleddown;
  }
  
  public static boolean getCannonCooldown(int cannonid, Long cannondelay, String playername)
  {
    Date date = new Date();
    boolean cooleddown = false;
    if (plugin.getConfig().getBoolean("usecannoncooldown"))
    {
      long cannondelaytime = plugin.getConfig().getInt("cannoncooldown") + 1;
      try
      {
        cannondelaytime = date.getTime() - cannondelay.longValue();
      }
      catch (Exception localException) {}
      if (cannondelaytime >= plugin.getConfig().getInt("cannoncooldown")) {
        cooleddown = true;
      } else if (playername != null) {
        if (plugin.getConfig().getString("cannoncooldownoutput").equalsIgnoreCase("message+double")) {
          Bukkit.getPlayer(playername).sendMessage(ChatColor.DARK_AQUA + "This cannon is still cooling down. Remaining time: " + ChatColor.AQUA + (plugin.getConfig().getInt("cannoncooldown") - cannondelaytime) / 1000.0D + "s");
        } else if (plugin.getConfig().getString("cannoncooldownoutput").equalsIgnoreCase("message+int")) {
          Bukkit.getPlayer(playername).sendMessage(ChatColor.DARK_AQUA + "This cannon is still cooling down. Remaining time: " + ChatColor.AQUA + (plugin.getConfig().getInt("cannoncooldown") - cannondelaytime) / 1000L + "s");
        } else if (plugin.getConfig().getString("cannoncooldownoutput").equalsIgnoreCase("double")) {
          Bukkit.getPlayer(playername).sendMessage(ChatColor.AQUA + (plugin.getConfig().getInt("cannoncooldown") - cannondelaytime) / 1000.0D + "s");
        } else if (plugin.getConfig().getString("cannoncooldownoutput").equalsIgnoreCase("int")) {
          Bukkit.getPlayer(playername).sendMessage(ChatColor.AQUA + (plugin.getConfig().getInt("cannoncooldown") - cannondelaytime) / 1000L + "s");
        }
      }
    }
    else
    {
      cooleddown = true;
    }
    return cooleddown;
  }
  
  public static boolean checkAmmo(String playername)
  {
    boolean playerhasammo = true;
    if (plugin.getConfig().getBoolean("ammo.use"))
    {
      Inventory inv = Bukkit.getPlayer(playername).getInventory();
      List<Integer> ammo = plugin.getConfig().getIntegerList("ammo.types");
      List<Integer> countofammo = plugin.getConfig().getIntegerList("ammo.number");
      for (int i = 0; i < ammo.size(); i++) {
        if (!inv.containsAtLeast(new ItemStack(Material.getMaterial(((Integer)ammo.get(i)).intValue())), ((Integer)countofammo.get(i)).intValue())) {
          playerhasammo = false;
        }
      }
      if (!playerhasammo)
      {
        Bukkit.getPlayer(playername).sendMessage(ChatColor.DARK_AQUA + "You need some munition to fire this Cannon!");
        Bukkit.getPlayer(playername).sendMessage(ChatColor.DARK_AQUA + "Munition type ids: ");
        for (int i = 0; i < ammo.size(); i++) {
          Bukkit.getPlayer(playername).sendMessage(ChatColor.AQUA + countofammo.get(i) + ChatColor.DARK_AQUA + " x " + ChatColor.AQUA + Material.getMaterial(((Integer)ammo.get(i)).intValue()).name() + ChatColor.DARK_AQUA + " (id: " + ChatColor.AQUA + ammo.get(i) + ChatColor.DARK_AQUA + ")");
        }
      }
    }
    if (!plugin.getConfig().getBoolean("ammo.use")) {
      playerhasammo = true;
    }
    return playerhasammo;
  }
  
  public static boolean removeAmmo(String playername)
  {
    if (plugin.getConfig().getBoolean("ammo.use"))
    {
      List<Integer> ammo = plugin.getConfig().getIntegerList("ammo.types");
      List<Integer> countofammo = plugin.getConfig().getIntegerList("ammo.number");
      for (int i = 0; i < ammo.size(); i++)
      {
        ItemStack ammoitem = new ItemStack(((Integer)ammo.get(i)).intValue(), ((Integer)countofammo.get(i)).intValue());
        Bukkit.getPlayer(playername).getInventory().removeItem(new ItemStack[] { ammoitem });
        Bukkit.getPlayer(playername).updateInventory();
      }
    }
    return true;
  }
  
  public static boolean checkTool(Player player)
  {
    boolean playerhastool = true;
    if (plugin.getConfig().getBoolean("tool.use")) {
      if (player.getItemInHand().getTypeId() == plugin.getConfig().getInt("tool.type"))
      {
        ItemStack inhand = player.getItemInHand();
        if (inhand.getType().getMaxDurability() - inhand.getDurability() <= plugin.getConfig().getInt("tool.damage")) {
          playerhastool = false;
        } else {
          playerhastool = true;
        }
      }
      else
      {
        player.sendMessage(ChatColor.DARK_AQUA + "You need a tool to fire this cannon: " + ChatColor.AQUA + Material.getMaterial(plugin.getConfig().getInt("tool.type")).name());
        player.sendMessage(ChatColor.DARK_AQUA + "One shot will damage it " + ChatColor.AQUA + plugin.getConfig().getInt("tool.damage") + ChatColor.DARK_AQUA + " uses.");
        playerhastool = false;
      }
    }
    return playerhastool;
  }
  
  public static void removeDurability(Player player)
  {
    if ((plugin.getConfig().getBoolean("tool.use")) && 
      (player.getItemInHand().getTypeId() == plugin.getConfig().getInt("tool.type")))
    {
      ItemStack inhand = player.getItemInHand();
      if (inhand.getType().getMaxDurability() - inhand.getDurability() <= plugin.getConfig().getInt("tool.damage"))
      {
        inhand = new ItemStack(0, 0);
        player.setItemInHand(inhand);
      }
      else
      {
        int damage = inhand.getDurability() + plugin.getConfig().getInt("tool.damage");
        inhand.setDurability((short)damage);
        player.setItemInHand(inhand);
      }
    }
  }
  
  public static Entity shoot(Location loc, Vector launch, Vector correction, int smokedir)
  {
    World world = loc.getWorld();
    Location barrelend = loc.toVector().add(launch).toLocation(world);
    barrelend = barrelend.toVector().add(correction).toLocation(world);
    for (int i = 0; i < plugin.getConfig().getInt("smokeeffect"); i++) {
      world.playEffect(barrelend, Effect.SMOKE, smokedir);
    }
    world.playSound(barrelend, Sound.EXPLODE, 3.0F, 1.0F);
    world.playSound(barrelend, Sound.BLAZE_HIT, 2.0F, 1.0F);
    Entity projectile = null;
    if ((plugin.getConfig().getString("projectile").equalsIgnoreCase("snowball")) || (plugin.getConfig().getString("projectile").equalsIgnoreCase("snow")))
    {
      projectile = world.spawn(barrelend, Snowball.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if (plugin.getConfig().getString("projectile").equalsIgnoreCase("arrow"))
    {
      projectile = world.spawn(barrelend, Arrow.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if (plugin.getConfig().getString("projectile").equalsIgnoreCase("egg"))
    {
      projectile = world.spawn(barrelend, Egg.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if ((plugin.getConfig().getString("projectile").equalsIgnoreCase("thrownexpbottle")) || (plugin.getConfig().getString("projectile").equalsIgnoreCase("expbottle")))
    {
      projectile = world.spawn(barrelend, ThrownExpBottle.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if ((plugin.getConfig().getString("projectile").equalsIgnoreCase("thrownpotion")) || (plugin.getConfig().getString("projectile").equalsIgnoreCase("potion")))
    {
      projectile = world.spawn(barrelend, ThrownPotion.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if (plugin.getConfig().getString("projectile").equalsIgnoreCase("tnt"))
    {
      projectile = world.spawn(barrelend, TNTPrimed.class);
      projectile.setVelocity(launch.multiply(1));
      return projectile;
    }
    if (plugin.getConfig().getString("projectile").equalsIgnoreCase("tnt"))
    {
      TNTPrimed tnt = (TNTPrimed)projectile;
      PirateCannon.pid = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable()
      {
        public void run()
        {
          if ((Cannon.this.getVelocity().getX() * Cannon.this.getVelocity().getX() < 0.25D) && (Cannon.this.getVelocity().getZ() * Cannon.this.getVelocity().getZ() < 0.25D))
          {
            Location loc = Cannon.this.getLocation();
            Cannon.this.remove();
            int power = Cannon.plugin.getConfig().getInt("explosion.power");
            boolean setfire = Cannon.plugin.getConfig().getBoolean("explosion.setfire");
            boolean blockdamage = Cannon.plugin.getConfig().getBoolean("explosion.blockdamage");
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setfire, blockdamage);
            if (Cannon.plugin.getConfig().getDouble("explosion.playerdamage") > 0.0D)
            {
              List<Entity> nearby = Cannon.this.getNearbyEntities(Cannon.plugin.getConfig().getDouble("explosion.playerdamagerange"), Cannon.plugin.getConfig().getDouble("explosion.playerdamagerange") + 1.0D, Cannon.plugin.getConfig().getDouble("explosion.playerdamagerange"));
              for (int i = 0; i < nearby.size(); i++) {
                if (((nearby.get(i) instanceof Player)) && (((Player)nearby.get(i)).getGameMode() != GameMode.CREATIVE)) {
                  if (((CraftPlayer)nearby.get(i)).getHealth() - Cannon.plugin.getConfig().getDouble("explosion.playerdamage") <= 0.0D) {
                    ((CraftPlayer)nearby.get(i)).setHealth(0.0D);
                  } else {
                    ((CraftPlayer)nearby.get(i)).setHealth(((CraftPlayer)nearby.get(i)).getHealth() - Cannon.plugin.getConfig().getDouble("explosion.playerdamage"));
                  }
                }
              }
            }
            Bukkit.getScheduler().cancelTask(PirateCannon.pid.getTaskId());
          }
        }
      }, 0L, 10L);
    }
    return null;
  }
  
  protected Location getLocation() {
	// TODO Auto-generated method stub
	return null;
}

protected Location getVelocity() {
	// TODO Auto-generated method stub
	return null;
}

protected void remove() {
	// TODO Auto-generated method stub
	
}

protected List<Entity> getNearbyEntities(double double1, double d, double double2) {
	// TODO Auto-generated method stub
	return null;
}

public static Vector getDirection(Location loc)
  {
    Vector direction = new Vector(0, 0, 0);
    byte damage = loc.getBlock().getData();
    switch (damage)
    {
    case 2: 
      direction = new Vector(0, 0, 1);
      break;
    case 3: 
      direction = new Vector(0, 0, -1);
      break;
    case 4: 
      direction = new Vector(1, 0, 0);
      break;
    case 5: 
      direction = new Vector(-1, 0, 0);
    }
    return direction;
  }
  
  public static Vector getCorrection(Location loc)
  {
    Vector correction = new Vector(0.0D, 0.5D, 0.0D);
    byte damage = loc.getBlock().getData();
    switch (damage)
    {
    case 2: 
      correction.setX(0.5D);
      correction.setZ(1.5D);
      break;
    case 3: 
      correction.setX(0.5D);
      correction.setZ(-0.5D);
      break;
    case 4: 
      correction.setX(1.5D);
      correction.setZ(0.5D);
      break;
    case 5: 
      correction.setX(-0.5D);
      correction.setZ(0.5D);
    }
    return correction;
  }
  
  public static int getSmokeDirection(Location loc)
  {
    int smokedirection = 0;
    byte damage = loc.getBlock().getData();
    switch (damage)
    {
    case 2: 
      smokedirection = 7;
      break;
    case 3: 
      smokedirection = 1;
      break;
    case 4: 
      smokedirection = 5;
      break;
    case 5: 
      smokedirection = 3;
    }
    return smokedirection;
  }
  
  public static int getLength(Location loc, Vector direction)
  {
    Vector savedir = direction;
    byte damage = loc.getBlock().getData();
    int length = 0;
    Location testloc = loc;
    for (int i = 1; i <= plugin.getConfig().getInt("maxcannonsize") + 1; i++)
    {
      direction = savedir;
      testloc = testloc.toVector().add(direction).toLocation(testloc.getWorld());
      if (!plugin.getConfig().getList("cannontypes").contains(Integer.valueOf(testloc.getBlock().getTypeId())))
      {
        length = i - 1;
        i = plugin.getConfig().getInt("maxcannonsize") + 1;
      }
    }
    return length;
  }
  
  public static Vector getLaunchVector(Vector direction, int length)
  {
    Vector launchvector = direction.multiply(length);
    launchvector.setY(0.25D);
    return launchvector;
  }
  
  public static Vector getTurnTo(Vector direction, String turn)
  {
    Vector turnto = new Vector(0, 0, 0);
    if (direction.normalize().equals(new Vector(1, 0, 0))) {
      turnto = new Vector(0, 0, 1);
    } else if (direction.normalize().equals(new Vector(-1, 0, 0))) {
      turnto = new Vector(0, 0, -1);
    } else if (direction.normalize().equals(new Vector(0, 0, 1))) {
      turnto = new Vector(-1, 0, 0);
    } else if (direction.normalize().equals(new Vector(0, 0, -1))) {
      turnto = new Vector(1, 0, 0);
    }
    if (turn.equals("left")) {
      turnto = turnto.multiply(-1);
    }
    return turnto;
  }
  
  public static byte getTurnedSignDirection(Vector turnto)
  {
    byte signdirection = 0;
    if (turnto.normalize().equals(new Vector(1, 0, 0))) {
      signdirection = 4;
    } else if (turnto.normalize().equals(new Vector(-1, 0, 0))) {
      signdirection = 5;
    } else if (turnto.normalize().equals(new Vector(0, 0, 1))) {
      signdirection = 2;
    } else if (turnto.normalize().equals(new Vector(0, 0, -1))) {
      signdirection = 3;
    }
    return signdirection;
  }
}