package su.nightexpress.excellentshop.shop.menu.context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.excellentshop.api.product.Product;
import su.nightexpress.nightcore.util.BukkitThing;

@NullMarked
public class SellContext {

    private final int                   maxSize;
    private final List<SellItemContext> items;
    private final List<ItemStack>       itemStackCache;

    public SellContext(int maxSize) {
        this.maxSize = maxSize;
        this.items = new ArrayList<>(this.maxSize);
        this.itemStackCache = new ArrayList<>();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public void clearAll() {
        this.items.clear();
        this.clearCache();
    }

    public void clearCache() {
        this.itemStackCache.clear();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<SellItemContext> getItems() {
        return items;
    }

    public boolean hasMaxItems() {
        return this.items.size() >= this.maxSize;
    }

    public boolean hasFreeSpace() {
        return this.items.size() < this.maxSize;
    }

    public void cachePlayerItem(ItemStack itemStack) {
        this.itemStackCache.add(itemStack);
    }

    public void addItem(SellItemContext itemContext) {
        this.items.add(itemContext);
    }

    public void mergeCache() {
        Map<ItemStack, Integer> distincts = new HashMap<>();

        this.itemStackCache.forEach(itemStack -> {
            int itemAmount = itemStack.getAmount();
            if (itemAmount <= 0) return;

            ItemStack keyStack = new ItemStack(itemStack);
            keyStack.setAmount(1);

            int distinctAmount = distincts.getOrDefault(keyStack, 0);
            int newAmount = distinctAmount + itemAmount;

            distincts.put(keyStack, newAmount);
        });


        this.itemStackCache.clear();

        distincts.forEach((itemStack, amount) -> {
            int accumulated = amount;
            int maxStackSize = itemStack.getMaxStackSize();

            while (accumulated > 0) {
                int reduce = accumulated >= maxStackSize ? maxStackSize : accumulated;

                ItemStack reduced = new ItemStack(itemStack);
                reduced.setAmount(reduce);
                this.cachePlayerItem(reduced);

                accumulated -= reduce;
            }
        });

        this.itemStackCache.sort(Comparator.comparing(stack -> BukkitThing.getValue(stack.getType())));
    }

    public @Nullable SellItemContext findItemToMerge(Product product) {
        for (SellItemContext itemContext : this.items) {
            if (itemContext.isFullfilled()) continue;

            Product otherProduct = itemContext.getProduct();
            if (otherProduct == product) {
                return itemContext;
            }
        }

        return null;
    }

    public List<ItemStack> getItemStackCache() {
        return itemStackCache;
    }
}