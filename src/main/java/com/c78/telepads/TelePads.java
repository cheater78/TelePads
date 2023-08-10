package com.c78.telepads;

import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TelePads extends JavaPlugin implements Listener, TabExecutor {
    class TelePadContainer{
        List<TelePad> pads;

        public TelePad getPad(Block b){
            Location bLoc =  b.getLocation();

            for(TelePad pad : pads){
                TelePad.BlockLocation loc = pad.loc;
                if(bLoc.getBlockX() == loc.x && bLoc.getBlockY() == loc.y && bLoc.getBlockZ() == loc.z){
                    return pad;
                }
            }
            return null;
        }

        public void teleport(Player p, Block b){
            Location bLoc =  b.getLocation();
            for(TelePad pad : pads){
                if(!Objects.equals(pad.loc.dim, p.getWorld().getEnvironment().name())) continue;
                TelePad.BlockLocation loc = pad.loc;
                if(bLoc.getBlockX() == loc.x && bLoc.getBlockY() == loc.y && bLoc.getBlockZ() == loc.z){
                    if(pad.dest == null) return;
                    Location dest = new Location(Bukkit.getServer().getWorld(pad.dest.dim),pad.dest.x, pad.dest.y, pad.dest.z, pad.dest.yaw, pad.dest.pitch);
                    p.teleport(dest);
                }
            }
        }

        public static class TelePad{
            BlockLocation loc;
            PlayerLocation dest = null;

            public String toString(){
                if(dest == null ) return ChatColor.GRAY + "Pad: at: " + ChatColor.GREEN + loc.toString() + ChatColor.GRAY + ", " + ChatColor.DARK_RED + "Not Activated";
                return ChatColor.GRAY + "Pad: at: " + ChatColor.GREEN + loc.toString() + ChatColor.GRAY + ", to: " + ChatColor.GREEN + dest.toString();
            }

            public static class PlayerLocation{
                public double x = 0;
                public double y = 0;
                public double z = 0;
                public float pitch = 0;
                public float yaw = 0;
                public String dim = "world";

                public String toString(){
                    return ("{dim:" + dim + "x:" + x + ",y:" + y + ",z:" + z + ", (pitch:" + pitch + ",yaw:" + yaw + ")}");
                }
            }

            public static class BlockLocation{
                public int x = 0;
                public int y = 0;
                public int z = 0;
                String dim = "world";

                public String toString(){
                    return ("{dim:" + dim + "x:" + x + ",y:" + y + ",z:" + z + "}");
                }

            }
        }

    }

    ItemStack m_Pad;
    File m_FileStorage;
    TelePadContainer m_RuntimeStorage;

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(this.getCommand("listpads")).setExecutor(this);

        //Custom Item
        m_Pad = new ItemStack(Material.LODESTONE);
        ItemMeta pMeta = m_Pad.getItemMeta();

        m_Pad.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        pMeta.setDisplayName(ChatColor.RED + "TelePad");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_PURPLE + "Telepad by c78");
        pMeta.setLore(lore);
        m_Pad.setItemMeta(pMeta);

        //Custom Recipe
        NamespacedKey nsKey = new NamespacedKey(this, "Telepad");
        ShapedRecipe tpadRecipe = new ShapedRecipe(nsKey, m_Pad);
        tpadRecipe.shape("EEE", "EDE", "STS");

        tpadRecipe.setIngredient('E',Material.ENDER_EYE);
        tpadRecipe.setIngredient('D',Material.DIAMOND_BLOCK);
        tpadRecipe.setIngredient('S',Material.END_STONE);
        tpadRecipe.setIngredient('T',Material.TNT);

        getServer().addRecipe(tpadRecipe);

        //Load Pads from File / Create File
        setUpStorage();

        Bukkit.getLogger().info("TelePads by C78 started...");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args){
        if(label.equals("listpads") && args.length == 0){
            if(m_RuntimeStorage.pads.size() == 0)
                if(s instanceof Player){
                    Player p = (Player) s;
                    p.sendMessage(ChatColor.RED + "No Pads yet");
                }else{
                    Bukkit.getLogger().info("No Pads yet");
                    return true;
                }
            for(TelePadContainer.TelePad pad : m_RuntimeStorage.pads){
                if(s instanceof Player){
                    Player p = (Player) s;
                    p.sendMessage(pad.toString());
                }else{
                    Bukkit.getLogger().info(pad.toString());
                    return true;
                }
            }
        }
        if(label.equals("givepad")){
            if(s instanceof Player){
                Player p = (Player) s;
                if(p.hasPermission("telepads.givepad"))
                    p.getInventory().addItem(m_Pad);
                else p.sendMessage(ChatColor.DARK_RED + "You dont have permission for that command!");
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> ret = new ArrayList<>();

        if(args.length == 0){
            ret.add("listPads");
            if(sender instanceof Player){
                Player p = (Player) sender;
                if(p.hasPermission("telepads.givepad")){
                    ret.add("givepad");
                }
            }

        }




        return ret;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){
        Player p = e.getPlayer();
        Block b = p.getLocation().add(0,-1,0).getBlock();

        m_RuntimeStorage.teleport(p,b);

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        if(e == null) return;
        Player p = e.getPlayer();
        ItemStack iStack = e.getItemInHand();
        Block b = e.getBlock();

        if(Objects.equals(iStack.getItemMeta(), m_Pad.getItemMeta()) && iStack.getType().equals(m_Pad.getType())){
            TelePadContainer.TelePad pad = new TelePadContainer.TelePad();
            TelePadContainer.TelePad.BlockLocation bLoc = new TelePadContainer.TelePad.BlockLocation();

            bLoc.dim = b.getWorld().getEnvironment().toString();
            bLoc.x = b.getX();
            bLoc.y = b.getY();
            bLoc.z = b.getZ();

            pad.loc = bLoc;

            b.getLocation().add(0, 1, 0).getBlock().setType(Material.CRIMSON_SIGN);
            BlockData bData = b.getLocation().add(0, 1, 0).getBlock().getBlockData();
            if(bData instanceof Rotatable rot){
                rot.setRotation(p.getFacing());
            }else{
                Bukkit.getLogger().warning("An Error occured while placing a TelePad");
            }
            b.getLocation().add(0, 1, 0).getBlock().setBlockData(bData);

            pad.dest = null;
            m_RuntimeStorage.pads.add(pad);

            try { saveStorage(); }
            catch (IOException io){}
            p.sendMessage(ChatColor.GREEN + "Pad placed(activate by editing the sign)");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e){
        Player p = e.getPlayer();
        Block b = e.getBlock();
        Block padB = b.getLocation().add(0, -1, 0).getBlock();

        if(b.getType() != Material.CRIMSON_SIGN) return;
        if((e.getLine(0) + e.getLine(1) + e.getLine(2)).equals("")) return;

        TelePadContainer.TelePad pad = m_RuntimeStorage.getPad(padB);
        if(pad !=null){
            try{
                if(pad.dest == null) pad.dest = new TelePadContainer.TelePad.PlayerLocation();
                pad.dest.x = Double.parseDouble(Objects.requireNonNull(e.getLine(0)));
                pad.dest.y = Double.parseDouble(Objects.requireNonNull(e.getLine(1)));
                pad.dest.z = Double.parseDouble(Objects.requireNonNull(e.getLine(2)));

                if(!e.getLine(3).equals("")){
                    String py = e.getLine(3);
                    if(py.contains("/")){
                        String[] ele = py.split("/");

                        if(!(ele[0].equals("")||ele[1].equals(""))){
                            try{
                                pad.dest.pitch = Float.parseFloat(ele[1]);
                                pad.dest.yaw = Float.parseFloat(ele[0]);
                            }catch (Exception exception){
                                p.sendMessage(ChatColor.RED + "Yaw/Pitch was illegal, defaulting(0,0)");
                            }

                        }
                    }else p.sendMessage(ChatColor.RED + "Yaw/Pitch was illegal, defaulting(0,0)");
                }else p.sendMessage(ChatColor.RED + "Yaw/Pitch was illegal, defaulting(0,0)");

                b.setType(Material.AIR);
                p.sendMessage(ChatColor.GREEN + "Pad activated");
                try { saveStorage(); }
                catch (IOException io){}
            }catch (Exception exception) {
                pad.dest = null;
                p.sendMessage(ChatColor.DARK_RED + "Wrong Syntax! Edit the Sign as follows:");
                p.sendMessage(ChatColor.DARK_RED + "      x");
                p.sendMessage(ChatColor.DARK_RED + "      y");
                p.sendMessage(ChatColor.DARK_RED + "      z");
                p.sendMessage(ChatColor.DARK_RED + "  yaw/pitch   (optional, can be left empty)");
                p.sendMessage(ChatColor.DARK_RED + "Destination removed");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        Player p = e.getPlayer();
        Block b = e.getBlock();

        if(b.getType().equals(Material.CRIMSON_SIGN)){
            Block below = b.getLocation().add(0, -1, 0).getBlock();
            TelePadContainer.TelePad signPad = m_RuntimeStorage.getPad(below);
            if(signPad != null){
                e.setCancelled(true);
                return;
            }
        }

        TelePadContainer.TelePad pad = m_RuntimeStorage.getPad(b);
        if(pad != null){
            p.sendMessage(ChatColor.GREEN + "Pad destroyed");
            m_RuntimeStorage.pads.remove(pad);
            e.setDropItems(false);
            e.getPlayer().getWorld().dropItemNaturally(e.getBlock().getLocation(), m_Pad);
            try {
                saveStorage();
            }catch (IOException io){

            }

        }

    }



    public void setUpStorage(){
        String path = "";
        try{                        path = TelePads.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(); }
        catch (Exception e){        throw new RuntimeException(e); }
        path = path.replace("TelePads.jar" , "");

        new File(path + "/TelePads").mkdirs();

        m_FileStorage = new File(path + "/TelePads/pads.json");
        if(!m_FileStorage.exists()) {
            try {
                m_FileStorage.createNewFile();
                m_RuntimeStorage = new TelePadContainer();
                saveStorage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            try {
                loadStorage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(m_RuntimeStorage == null) m_RuntimeStorage = new TelePadContainer();
        if(m_RuntimeStorage.pads == null) m_RuntimeStorage.pads = new ArrayList<>();
        if(m_FileStorage == null) {
            m_FileStorage = new File(path + "/TelePads/pads.json");
            m_FileStorage.mkdirs();
        }
    }

    private void saveStorage () throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(m_RuntimeStorage);

        if(m_FileStorage.canWrite())
            Files.write(m_FileStorage.toPath(), json.getBytes());
    }

    private void loadStorage() throws IOException {
        String json = Files.readString(m_FileStorage.toPath());
        m_RuntimeStorage = new Gson().fromJson(json, TelePadContainer.class);
        if(m_RuntimeStorage == null) m_RuntimeStorage = new TelePadContainer();
        if(m_RuntimeStorage.pads == null) m_RuntimeStorage.pads = new ArrayList<>();
    }
}
