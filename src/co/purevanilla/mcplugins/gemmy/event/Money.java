package co.purevanilla.mcplugins.gemmy.event;

import co.purevanilla.mcplugins.gemmy.Main;
import co.purevanilla.mcplugins.gemmy.util.Drop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static co.purevanilla.mcplugins.gemmy.Main.econ;

public class Money implements Listener {

    @EventHandler
    public void blockDestroy(final BlockBreakEvent e){

        final Material blockBroken = e.getBlock().getType();
        final Block blockBrokenObj = e.getBlock();
        Ageable crop = null;
        try{
            crop = (Ageable) blockBrokenObj.getBlockData();
        } catch(ClassCastException err){
            // do nothing
        }

        final Ageable finalCropParent = crop;
        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {

                try{
                    if(Main.settings.getBlocksHashMap().containsKey(blockBroken) || Main.settings.getProducts().contains(blockBroken) || Main.settings.getSeeds().contains(blockBroken)){

                        if(Main.playerPlaced.contains(blockBrokenObj)){
                            Main.playerPlaced.remove(blockBrokenObj);
                        } else {

                            final Ageable finalCrop = finalCropParent;

                            // already running on async, check if it has been turned into air

                            if(e.getBlock().getLocation().getWorld().getBlockAt(e.getBlock().getLocation()).getType() == Material.AIR){

                                if(Main.settings.getBlocks().contains(blockBroken)){

                                    new Drop(e.getBlock().getLocation(),Main.settings.getBlockRange(blockBroken).getAmount()).spawn();

                                } else if (finalCrop != null && (Main.settings.getProducts().contains(blockBroken) || Main.settings.getSeeds().contains(blockBroken))){

                                    if(finalCrop.getAge()== finalCrop.getMaximumAge()){

                                        new Drop(e.getBlock().getLocation(),Main.settings.getHarvest(blockBroken).getHarvest().getAmount()).spawn();

                                        // expect harvesting change
                                        Main.expectedReplants.put(blockBrokenObj.getLocation(),Main.settings.getHarvest(blockBroken));

                                    }

                                }


                            }


                        }

                    }

                } catch(NullPointerException err) {
                    // item already removed
                }

            }
        });

    }

    @EventHandler
    public void onDrop(final EntityDropItemEvent e){
        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {
                try{
                    Drop drop = new Drop(e.getItemDrop().getItemStack());
                    if(drop.hasQuantity()){
                        e.getItemDrop().remove();
                        drop.setLocation(e.getItemDrop().getLocation());
                        drop.spawn();
                    }
                } catch(NullPointerException err){

                }
            }
        });
    }

    @EventHandler
    public void inventoryEvent(final InventoryClickEvent e){

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {

                try{
                    List<ItemStack> itemStacks = new ArrayList<>();
                    if(e.getAction()== InventoryAction.PLACE_ALL||e.getAction()==InventoryAction.PLACE_ONE ||e.getAction()==InventoryAction.PLACE_SOME){
                        if(e.getClickedInventory().getType()==InventoryType.PLAYER){
                            ItemStack[] Slots = e.getClickedInventory().getStorageContents();
                            for (ItemStack item:Slots) {
                                if(item!=null){
                                    itemStacks.add(item);
                                }
                            }
                        }
                    } else if(e.getAction()==InventoryAction.MOVE_TO_OTHER_INVENTORY){
                        for (HumanEntity entity :e.getViewers()) {
                            ItemStack[] Slots = entity.getInventory().getStorageContents();
                            for (ItemStack item:Slots) {
                                if(item != null){
                                    itemStacks.add(item);
                                }
                            }
                        }
                    }

                    for (ItemStack item:itemStacks) {
                        try{
                            Drop drop = new Drop(item);
                            if(drop.hasQuantity()){
                                econ.depositPlayer((OfflinePlayer) e.getWhoClicked(),(float) drop.getQuantity());
                                item.setAmount(0);
                            }
                        } catch (NullPointerException err){
                            // invalid drop
                        }
                    }
                } catch(NullPointerException err){

                }
            }
        });

    }

    @EventHandler
    public void entityDeath(final EntityDeathEvent e){

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {
                try{
                    if(e.getEntity().getKiller()!=null){
                        if(Main.settings.entityTypeRangeHashMap().containsKey(e.getEntityType())){
                            if(Main.spawnerEntities.contains(e.getEntity().getEntityId())){
                                Main.spawnerEntities.remove(Integer.valueOf(e.getEntity().getEntityId()));
                            } else {
                                Drop drop = new Drop(e.getEntity().getLocation(),Main.settings.entityTypeRangeHashMap().get(e.getEntityType()).getAmount());
                                drop.spawn();
                            }
                        }
                    }
                } catch(NullPointerException err){

                }
            }
        });

    }

    @EventHandler
    public void pickupGem(final EntityPickupItemEvent e){

        if(e.getEntity() instanceof Player){

            try{
                if(e.getItem().getItemStack().getType()==Main.settings.getGem()||e.getItem().getItemStack().getType()==Main.settings.getLargeGem()) {

                    Drop drop = new Drop(e.getItem().getItemStack());
                    if(drop.hasQuantity()){
                        e.setCancelled(true);
                        e.getItem().remove();
                        econ.depositPlayer((OfflinePlayer) e.getEntity(),(float) drop.getQuantity());
                    }

                }
            } catch(NullPointerException err){
                // fix it later I guess?
            }

        }

    }

    @EventHandler
    public void entityMating(final EntityBreedEvent e){

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {

                try{
                    Drop drop = new Drop(e.getFather().getLocation(),Main.settings.getBreedingRange().getAmount());
                    drop.spawn();
                } catch(NullPointerException err){
                    // fix it later I guess?
                }

            }
        });

    }

    @EventHandler
    public void blockPlace(final BlockPlaceEvent e){

            Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
                @Override
                public void run() {

                    try{
                        if(Main.settings.getBlocksHashMap().containsKey(e.getBlock().getType()) && !(Main.settings.getProducts().contains(e.getBlock().getType()) || Main.settings.getSeeds().contains(e.getBlock().getType()))){
                            Main.playerPlaced.add(e.getBlockPlaced());
                        } else if(Main.expectedReplants.containsKey(e.getBlock().getLocation()) && (Main.settings.getProducts().contains(e.getBlock().getType()) || Main.settings.getSeeds().contains(e.getBlock().getType()))){

                            final Material originalType = e.getBlock().getType();
                            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.plugin, new Runnable() {
                                @Override
                                public void run() {

                                    if(e.getBlock().getLocation().getWorld().getBlockAt(e.getBlock().getLocation()).getType() == originalType){

                                        Main.expectedReplants.remove(e.getBlock().getLocation());
                                        new Drop(e.getBlock().getLocation(),Main.settings.getHarvest(originalType).getReplant().getAmount()).spawn();

                                    }

                                }
                            },(long) 1/2);
                        }
                    } catch (NullPointerException err){
                        // Fix it later I guess?
                    }

                }
            });

    }

    @EventHandler
    public void entitySpawn(final SpawnerSpawnEvent e){

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {

                try{
                    Main.spawnerEntities.add(e.getEntity().getEntityId());
                } catch (NullPointerException err){
                    // Fix it later I guess?
                }
            }
        });

    }

    @EventHandler
    public void playerDeath(final PlayerDeathEvent e){

        Bukkit.getScheduler().runTaskAsynchronously(Main.plugin, new Runnable() {
            @Override
            public void run() {

                try{
                    if(Main.settings.isDeathEnabled()){
                        int deathPercent = Main.settings.getDeathPercent();
                        for (int i = 100; i > -1 ; i--) {
                            if(Objects.requireNonNull(e.getEntity().getPlayer()).hasPermission("gemmy.death."+i)){
                                deathPercent=i;
                            }
                        }

                        int amountToRemove = (int) (econ.getBalance(e.getEntity())*((float) deathPercent/100));
                        econ.withdrawPlayer(e.getEntity(),amountToRemove);

                        Drop drop = new Drop(e.getEntity().getLocation(),amountToRemove);
                        drop.spawn();
                    }
                } catch (NullPointerException err){
                    // Fix it later I guess?
                }
            }
        });

    }

}
