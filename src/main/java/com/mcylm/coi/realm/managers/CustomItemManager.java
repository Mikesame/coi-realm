package com.mcylm.coi.realm.managers;

import com.mcylm.coi.realm.Entry;
import com.mcylm.coi.realm.item.COICustomItem;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CustomItemManager implements Listener {
    private final Map<String, COICustomItem> items = new LinkedHashMap<>();


    public void registerCustomItem(COICustomItem item) {
        if (this.items.containsKey(item.getNamespaceKey())) {
            return;
        }
        if (item.getEventListener() != null) {
            try {
                Listener listener = item.getEventListener().getConstructor(COICustomItem.class).newInstance(item);
                Entry.getInstance().getServer().getPluginManager().registerEvents(listener, Entry.getInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.items.put(item.getNamespaceKey(), item);
    }
    
    public @NonNull List<COICustomItem> getCustomItems() {
        return new ArrayList<>(this.items.values());
    }

    
    public COICustomItem getCustomItem(@NonNull String namespaceKey) {

        return items.get(namespaceKey);
    }

    public ItemStack getItemStack(@NonNull String namespaceKey) {
        COICustomItem item = this.getCustomItem(namespaceKey);
        return item == null ? null : item.getItemStack();
    }



    @EventHandler
    public void playerInteractEvent(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() == null) {
                return;
            }
            if (event.getItem().getItemMeta() == null) {
                return;
            }
            if (!event.getItem().getItemMeta().hasCustomModelData()) {
                return;
            }

            @NotNull PersistentDataContainer pdc = event.getItem().getItemMeta().getPersistentDataContainer();

            if (!pdc.has(COICustomItem.COI_CUSTOM_ITEM_NAMESPACEDKEY)) {
                return;
            }

            COICustomItem customItem = this.items.get(pdc.get(COICustomItem.COI_CUSTOM_ITEM_NAMESPACEDKEY, PersistentDataType.STRING));
            if (customItem == null) {
                return;
            }

            Consumer<PlayerInteractEvent> consumer = customItem.getItemUseEvent();
            if (consumer != null) {
                consumer.accept(event);
            }
        }
    }

    @EventHandler
    public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getItemMeta() == null) {
                return;
            }
            if (!item.getItemMeta().hasCustomModelData()) {
                return;
            }

            @NotNull PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

            if (!pdc.has(COICustomItem.COI_CUSTOM_ITEM_NAMESPACEDKEY)) {
                return;
            }

            COICustomItem customItem = this.items.get(pdc.get(COICustomItem.COI_CUSTOM_ITEM_NAMESPACEDKEY, PersistentDataType.STRING));
            if (customItem == null) {
                return;
            }

            Consumer<EntityDamageByEntityEvent> consumer = customItem.getPlayerHitEntityEvent();

            if (consumer != null) {
                consumer.accept(event);
            }

        }
    }
    
    
}
