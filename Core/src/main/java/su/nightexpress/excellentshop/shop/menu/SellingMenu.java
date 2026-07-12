package su.nightexpress.excellentshop.shop.menu;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import su.nightexpress.excellentshop.ShopPlaceholders;
import su.nightexpress.excellentshop.ShopPlugin;
import su.nightexpress.excellentshop.api.BalanceHolder;
import su.nightexpress.excellentshop.api.UnitUtils;
import su.nightexpress.excellentshop.api.menu.SellingMenuAdapter;
import su.nightexpress.excellentshop.api.menu.SellingMenuProvider;
import su.nightexpress.excellentshop.api.product.Product;
import su.nightexpress.excellentshop.api.product.TradeType;
import su.nightexpress.excellentshop.api.shop.Shop;
import su.nightexpress.excellentshop.api.transaction.EPreparedTransaction;
import su.nightexpress.excellentshop.core.Lang;
import su.nightexpress.excellentshop.shop.AbstractShopModule;
import su.nightexpress.excellentshop.shop.menu.context.SellContext;
import su.nightexpress.excellentshop.shop.menu.context.SellItemContext;
import su.nightexpress.excellentshop.shop.menu.context.SellMenuContext;
import su.nightexpress.excellentshop.util.PacketUtils;
import su.nightexpress.excellentshop.util.ShopUtils;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.ui.inventory.MenuRegistry;
import su.nightexpress.nightcore.ui.inventory.action.ActionContext;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

@NullMarked
public class SellingMenu extends AbstractObjectMenu<SellMenuContext> implements SellingMenuProvider {

    @Nullable
    private SellingMenuAdapter adapter;

    private int[]     productSlots;
    private NightItem lockedIcon;
    private String    worthText;
    private String    amountText;

    public SellingMenu(ShopPlugin plugin) {
        super(plugin, MenuType.GENERIC_9X6, "Selling", SellMenuContext.class);
    }

    @Override
    public void load(FileConfig config) {
        super.load(config);

        PacketUtils.library().ifPresent(lib -> {
            this.adapter = lib.createSellingMenuAdapter(this.plugin, this);
            this.adapter.register();
        });
    }

    public void cleanUp() {
        this.close(); // Force close to return items.

        if (this.adapter != null) {
            this.adapter.unregister();
            this.adapter = null;
        }
    }

    public boolean show(Player player, AbstractShopModule module, @Nullable Shop targetShop,
                        @Nullable Product targetProduct, int shopPage) {
        return this.show(player,
            new SellMenuContext(module, new SellContext(this.productSlots.length), targetShop, targetProduct, shopPage));
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(45, 54).toArray());

        this.addDefaultButton("sellout", MenuItem.button()
            .defaultState(ItemState.builder()
                .icon(NightItem.fromType(Material.LIME_DYE)
                    .setDisplayName(TagWrappers.GREEN.and(TagWrappers.BOLD).wrap("Sell Out"))
                    .setLore(Lists.newList(
                        TagWrappers.GRAY.wrap("Total worth: " + TagWrappers.WHITE.wrap(ShopPlaceholders.GENERIC_PRICE)),
                        "",
                        TagWrappers.GREEN.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to sell"))
                    ))
                    .hideAllComponents()
                )
                .condition(context -> !this.getObject(context).sellContext().isEmpty())
                .displayModifier((context, item) -> item.replace(builder -> builder
                    .with(ShopPlaceholders.GENERIC_PRICE, () -> this.getObject(context).worth(context.getPlayer())
                        .format(Lang.OTHER_PRICE_DELIMITER.text()))
                ))
                .action(this::handleSellOut)
                .build()
            )
            .state("empty", ItemState.builder()
                .icon(NightItem.fromType(Material.GRAY_DYE)
                    .setDisplayName(TagWrappers.WHITE.and(TagWrappers.BOLD).wrap("Sell Out"))
                    .setLore(Lists.newList(
                        TagWrappers.GRAY.wrap("Nothing to sell.")
                    ))
                    .hideAllComponents()
                )
                .condition(context -> this.getObject(context).sellContext().isEmpty())
                .build()
            )
            .slots(53)
            .build()
        );

        this.addDefaultButton("cancel", MenuItem.button()
            .defaultState(ItemState.builder()
                .icon(NightItem.fromType(Material.RED_DYE)
                    .setDisplayName(TagWrappers.RED.and(TagWrappers.BOLD).wrap("Cancel"))
                    .setLore(Lists.newList(
                        TagWrappers.GRAY.wrap("Click to go back.")
                    ))
                    .hideAllComponents()
                )
                .action(this::handleCancel)
                .build()
            )
            .slots(45)
            .build()
        );
    }

    @Override
    protected void onLoad(FileConfig config) {
        this.productSlots = config.get(ConfigTypes.INT_ARRAY, "Item.Sell-Slots", IntStream.range(0, 36).toArray());

        this.lockedIcon = config.get(ConfigTypes.NIGHT_ITEM, "Item.Locked-Icon", NightItem.fromType(Material.BARRIER)
            .setDisplayName(TagWrappers.RED.and(TagWrappers.BOLD).wrap("Unsellable"))
            .setLore(Lists.newList(
                TagWrappers.GRAY.wrap("This item can not be sold here.")
            ))
        );

        this.worthText = config.get(ConfigTypes.STRING, "Item.Info.Worth", TagWrappers.GREEN.wrap("Worth: " +
            ShopPlaceholders.GENERIC_WORTH));
        this.amountText = config.get(ConfigTypes.STRING, "Item.Info.Amount", TagWrappers.RED.wrap(
            "Min. amount to sell: " + ShopPlaceholders.GENERIC_AMOUNT));
    }

    @Override
    protected void onClick(ViewerContext context, InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        int rawSlot = event.getRawSlot();
        boolean isPlayerSlot = rawSlot >= inventory.getSize();
        ClickType clickType = event.getClick();

        context.getViewer().setNextClickIn(0L); // Allow fast clicks to make sure visuals update always triggers.

        if (clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        if (isPlayerSlot && this.handlePlayerSlotClick(context, event)) {
            return;
        }

        if (this.isProductSlot(rawSlot) && this.handleMenuSlotClick(context, event)) {
            return;
        }

        // This prevents item visual glitch when using shift+double click.
        this.plugin.runTask(() -> this.triggerSlotUpdates(context.getPlayer()));
    }

    private boolean handlePlayerSlotClick(ViewerContext context, InventoryClickEvent event) {
        int realSlot = event.getSlot();
        Player player = context.getPlayer();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            // Force trigger packet to add item worth lore.
            this.plugin.runTask(() -> this.triggerSlotUpdate(player, realSlot));
            event.setCancelled(false);
            return true;
        }

        if (!this.isSellable(context, clickedItem)) {
            // This prevents item visual glitch when using shift+double click.
            this.plugin.runTask(() -> this.triggerSlotUpdate(player, realSlot));
            return true;
        }

        if (event.isShiftClick()) {
            ItemStack copyStack = new ItemStack(clickedItem);
            // Set zero here so this item in player inventory doesnt count when we put player items into fake inventory
            clickedItem.setAmount(0);

            // Cache cursor so it don't get lost on MenuViewer#refresh
            ItemStack cursor = event.getCursor();
            ItemStack saveCursor = cursor == null ? null : new ItemStack(cursor);

            if (!this.addItem(context, copyStack, leftAmount -> clickedItem.setAmount(leftAmount))) return false;

            ItemStack saveClicked = new ItemStack(clickedItem);
            // Prevent possible duplications using client mods with fast clicks due to 1 tick delay below
            clickedItem.setAmount(0);

            this.plugin.runTask(player, () -> {
                context.getViewer().refresh();
                InventoryView view = context.getViewer().getCurrentView();
                if (view != null) {
                    view.setCursor(saveCursor);
                }
                player.getInventory().setItem(realSlot, saveClicked);
            });

            return true;
        }

        event.setCancelled(false);
        return false;
    }

    private boolean handleMenuSlotClick(ViewerContext context, InventoryClickEvent event) {
        Player player = context.getPlayer();

        ItemStack cursor = event.getCursor();
        if (!cursor.getType().isAir()) {
            ItemStack cursorCopy = new ItemStack(cursor);
            //cursor.setAmount(0);

            if (!this.addItem(context, cursorCopy, leftAmount -> cursor.setAmount(leftAmount))) return false;

            ItemStack saveCursor = cursor == null ? null : new ItemStack(cursor);
            cursor.setAmount(0);

            this.plugin.runTask(player, () -> {
                context.getViewer().refresh();
                InventoryView view = context.getViewer().getCurrentView();
                if (view != null) {
                    view.setCursor(saveCursor);
                }
            });
            return true;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && !clickedItem.getType().isAir()) {
            ItemStack copyStack = new ItemStack(clickedItem);
            clickedItem.setAmount(0);

            if (!this.removeItem(context, copyStack)) return true;

            this.plugin.runTask(player, () -> {
                context.getViewer().refresh();
                InventoryView view = context.getViewer().getCurrentView();

                if (event.isShiftClick() || view == null) {
                    Players.addItem(player, copyStack);
                }
                else {
                    view.setCursor(new ItemStack(copyStack));
                }
            });
        }

        return true;
    }

    @Override
    protected void onDrag(ViewerContext context, InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        Set<Integer> slots = event.getRawSlots();

        if (slots.stream().allMatch(slot -> slot >= inventory.getSize())) {
            Player player = context.getPlayer();

            event.setCancelled(false);
            context.getViewer().setNextClickIn(0L);

            // Force trigger packet to add item worth lore.
            this.plugin.runTask(() -> {
                event.getInventorySlots().forEach(slot -> this.triggerSlotUpdate(player, slot));
            });
        }
    }

    @Override
    protected void onClose(ViewerContext context, InventoryCloseEvent event) {
        Player player = context.getPlayer();
        SellMenuContext menuContext = this.getObject(context);
        SellContext sellContext = menuContext.sellContext();

        sellContext.getItemStackCache().forEach(itemStack -> Players.addItem(player, itemStack));
        sellContext.clearAll();
    }

    @Override
    public void handleClose(Player player, InventoryCloseEvent event,
                            MenuRegistry menuRegistry) {
        super.handleClose(player, event, menuRegistry);
        this.triggerSlotUpdates(player); // A small workaround to not use #runTask in #onClose due to possible exception on server shutdown.
    }

    @Override
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory,
                          List<MenuItem> items) {

    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {
        Player player = context.getPlayer();
        SellMenuContext menuContext = this.getObject(context);
        SellContext sellContext = menuContext.sellContext();

        int index = 0;
        for (ItemStack itemStack : sellContext.getItemStackCache()) {
            if (index >= this.productSlots.length) break;

            inventory.setItem(this.productSlots[index], new ItemStack(itemStack));
            index++;
        }

        this.plugin.runTask(() -> this.triggerSlotUpdates(player));
    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public boolean isImmuneSlot(Player player, int slot) {
        if (this.isProductSlot(slot)) return false;

        MenuViewer viewer = this.getViewer(player);
        if (viewer == null) return true;

        InventoryView view = viewer.getCurrentView();

        return view == null || slot < view.getTopInventory().getSize();
    }

    @Override
    public boolean isProductSlot(int slot) {
        return Lists.contains(this.productSlots, slot);
    }

    private boolean addItem(ViewerContext context,
                            ItemStack clickedItem,
                            Consumer<Integer> callback) {
        Product product = this.findProduct(context, clickedItem);
        if (product == null) return false;

        int itemAmount = clickedItem.getAmount();
        int itemUnits = UnitUtils.amountToUnits(product, itemAmount);
        if (itemUnits <= 0) return false;

        SellMenuContext menuContext = this.getObject(context);
        SellContext sellContext = menuContext.sellContext();

        Player player = context.getPlayer();

        // Create virtual inventory and fill it with current items to get the max sellable unit amount for the current item/
        Inventory fakeInventory = this.plugin.getServer().createInventory(null, 54);

        // Add player items.
        for (ItemStack contents : player.getInventory().getStorageContents()) {
            if (contents != null && !contents.getType().isAir()) {
                fakeInventory.addItem(new ItemStack(contents));
            }
        }

        for (ItemStack cachedStack : sellContext.getItemStackCache()) {
            fakeInventory.addItem(new ItemStack(cachedStack));
        }

        // Add the current item.
        fakeInventory.addItem(new ItemStack(clickedItem));

        // Don't allow to put item for sell if limit(s) reached.
        int maxSellableUnits = product.getMaxSellableUnitAmount(player, fakeInventory);
        if (maxSellableUnits == 0) {
            callback.accept(itemAmount);
            return false;
        }

        int amountToReturn;
        int unitsToSell;

        // No sell limits, continue with original values, nothing to return back
        if (maxSellableUnits < 0) {
            unitsToSell = itemUnits;
            amountToReturn = 0;
        }
        // Otherwise calculate units amount based on product limit settings and currently added items for sell
        else {
            int alreadyForSellUnits = sellContext.getItems()
                .stream()
                .filter(item -> item.getProduct() == product)
                .mapToInt(SellItemContext::getUnits)
                .sum();
            int sellLimitUnits = Math.max(0, maxSellableUnits - alreadyForSellUnits);

            if (sellLimitUnits == 0) {
                callback.accept(itemAmount);
                return false;
            }

            unitsToSell = Math.min(itemUnits, sellLimitUnits);
            int deltaUnits = Math.max(0, itemUnits - unitsToSell);
            amountToReturn = UnitUtils.amountToUnits(product, deltaUnits);
        }

        // Cache item with the limited/reduced amount based on limit calculations above
        ItemStack itemToCache = new ItemStack(clickedItem);
        itemToCache.setAmount(UnitUtils.unitsToAmount(product, unitsToSell));

        int mergeResult = this.mergeWithTarget(sellContext, product, unitsToSell);
        if (mergeResult == 0) {
            // The merge was successful, cache item and apply leftovers back to player inventory.
            sellContext.cachePlayerItem(itemToCache);
            sellContext.mergeCache();
            callback.accept(amountToReturn);
        }
        else if (mergeResult == unitsToSell) {
            // Nothing merged/added, no free space for new items, cancel immediately.
            if (!sellContext.hasFreeSpace()) {
                return false;
            }

            SellItemContext itemContext = new SellItemContext(product, unitsToSell);
            sellContext.addItem(itemContext);
            sellContext.cachePlayerItem(itemToCache);
            sellContext.mergeCache();

            callback.accept(amountToReturn);
        }
        else {
            if (sellContext.hasFreeSpace()) {
                SellItemContext itemContext = new SellItemContext(product, mergeResult);
                sellContext.addItem(itemContext);
                sellContext.cachePlayerItem(itemToCache);
                sellContext.mergeCache();
                callback.accept(amountToReturn);
            }
            // Partially merged/added, no free space for new items, apply leftovers and cance.
            else {
                int amountLeft = UnitUtils.unitsToAmount(mergeResult, product.getUnitSize());

                callback.accept(amountToReturn + amountLeft);
            }
        }

        return true;
    }

    private int mergeWithTarget(SellContext sellContext, Product product, int leftUnits) {
        SellItemContext targetItem = sellContext.findItemToMerge(product);
        if (targetItem == null || targetItem.isFullfilled()) return leftUnits;

        int maxUnitsPerStack = product.getMaxUnitsInStack();
        int targetUnits = targetItem.getUnits();

        // How much units can be added to the target item that is already added for sell
        int availableUnits = maxUnitsPerStack - targetUnits;
        if (availableUnits <= 0) return leftUnits;

        if (availableUnits >= leftUnits) {
            targetItem.setUnits(targetUnits + leftUnits);
            return 0;
        }

        targetItem.setUnits(maxUnitsPerStack);
        int result = leftUnits - availableUnits;

        return this.mergeWithTarget(sellContext, product, result);
    }

    private boolean removeItem(ViewerContext context, ItemStack clickedItem) {
        Product product = this.findProduct(context, clickedItem);
        if (product == null) return false;

        SellMenuContext menuContext = this.getObject(context);
        SellContext sellContext = menuContext.sellContext();

        int itemAmount = clickedItem.getAmount();
        if (itemAmount <= 0) return false;

        int amountToRemove = itemAmount;

        for (ItemStack cachedStack : sellContext.getItemStackCache()) {
            if (!cachedStack.isSimilar(clickedItem)) continue;

            int cachedAmount = cachedStack.getAmount();
            if (cachedAmount >= amountToRemove) {
                cachedStack.setAmount(cachedAmount - amountToRemove);
                break;
            }

            cachedStack.setAmount(0);
            amountToRemove -= cachedAmount;
        }

        sellContext.mergeCache();

        int unitsToRemove = UnitUtils.amountToUnits(itemAmount, product.getUnitSize());
        for (SellItemContext itemContext : sellContext.getItems()) {
            if (itemContext.getProduct() != product) continue;

            int sellUnits = itemContext.getUnits();
            if (sellUnits >= unitsToRemove) {
                itemContext.setUnits(sellUnits - unitsToRemove);
                break;
            }

            itemContext.setUnits(0);
            unitsToRemove -= sellUnits;
        }
        sellContext.getItems().removeIf(i -> i.getUnits() <= 0);

        return true;
    }

    private void triggerSlotUpdates(Player player) {
        if (this.adapter == null) return;

        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            this.triggerSlotUpdate(player, slot);
        }
    }

    private void triggerSlotUpdate(Player player, int slot) {
        if (this.adapter == null) return;

        ItemStack itemStack = player.getInventory().getItem(slot);
        if (itemStack == null || itemStack.getType().isAir()) return;

        this.adapter.callPlayerInventoryPacket(player, slot, itemStack);
    }

    @Nullable
    private Product findProduct(ViewerContext context, ItemStack itemStack) {
        Player player = context.getPlayer();
        SellMenuContext data = this.getObject(context);

        Product targetProduct = data.targetProduct();
        if (targetProduct != null) return targetProduct.getContent().isItemMatches(itemStack) ? targetProduct : null;

        Shop targetShop = data.targetShop();
        if (targetShop != null) return targetShop.getBestProduct(itemStack, TradeType.SELL);

        Set<? extends Shop> shops = data.module().getShops(player);
        return ShopUtils.findBestProduct(itemStack, TradeType.SELL, shops);
    }

    private boolean isSellable(ViewerContext context, ItemStack itemStack) {
        Product product = this.findProduct(context, itemStack);
        return product != null && product.canTrade(context.getPlayer());
    }

    private void handleSellOut(ActionContext context) {
        SellMenuContext data = this.getObject(context);
        if (data.sellContext().isEmpty()) return;

        Player player = context.getPlayer();
        AbstractShopModule module = data.module();

        Inventory inventory = this.plugin.getServer().createInventory(null, 54);
        EPreparedTransaction.Builder transaction = EPreparedTransaction.builder(player, TradeType.SELL)
            .setStrict(false)
            .setUserInventory(inventory);

        SellContext sellContext = data.sellContext();

        sellContext.getItemStackCache().forEach(itemStack -> inventory.addItem(new ItemStack(itemStack)));
        sellContext.getItems().forEach(itemContext -> {
            Product product = itemContext.getProduct();
            int units = itemContext.getUnits();

            transaction.addProduct(product, units);
        });
        sellContext.clearAll();

        module.proceedTransaction(transaction.build(), completed -> {
            // Add back to player all items that were not sold.
            for (ItemStack content : inventory.getContents()) {
                if (content != null && !content.getType().isAir()) {
                    Players.addItem(player, content);
                }
            }

            this.goBack(context);
        });
    }

    private void handleCancel(ActionContext context) {
        this.goBack(context);
    }

    private void goBack(ViewerContext context) {
        Player player = context.getPlayer();
        SellMenuContext data = this.getObject(context);

        if (data.targetShop() != null) {
            data.targetShop().open(player, data.shopPage());
        }
        else if (data.targetProduct() != null) {
            data.targetProduct().getShop().open(player, data.shopPage());
        }
        else {
            context.getViewer().closeMenu();
        }
    }

    @Nullable
    public ItemStack onSlotRender(Player player, ItemStack itemStack) {
        MenuViewer viewer = this.getViewer(player);
        if (viewer == null) return itemStack;

        if (itemStack.getType().isAir()) return itemStack;

        ViewerContext context = viewer.createContext();

        Product product = this.findProduct(context, itemStack);
        if (product == null) {
            return this.lockedIcon.getItemStack();
        }

        int amount = itemStack.getAmount();
        int units = UnitUtils.amountToUnits(product, amount);

        ItemStack modified = new ItemStack(itemStack);
        PlaceholderContext.Builder builder = PlaceholderContext.builder();
        List<String> lore = ItemUtil.getLoreSerialized(modified);

        if (this.isPlaceholderIntegrationEnabled()) {
            builder.andThen(CommonPlaceholders.forPlaceholderAPI(player));
        }

        if (units <= 0) {
            lore.add(this.amountText);
            builder.with(ShopPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(product.getUnitSize()));
        }
        else {
            BalanceHolder worth = new BalanceHolder();
            worth.store(product.getCurrency(), product.getFinalSellPrice(player, units));

            lore.add(this.worthText);
            builder.with(ShopPlaceholders.GENERIC_WORTH, () -> worth.format(Lang.OTHER_PRICE_DELIMITER.text()));
        }

        ItemUtil.setLore(modified, builder.build().apply(lore));
        return modified;
    }
}
