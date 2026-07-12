package su.nightexpress.excellentshop.shop.menu.context;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import su.nightexpress.excellentshop.api.BalanceHolder;
import su.nightexpress.excellentshop.api.product.Product;
import su.nightexpress.excellentshop.api.shop.Shop;
import su.nightexpress.excellentshop.shop.AbstractShopModule;

public record SellMenuContext(AbstractShopModule module,
                              SellContext sellContext,
                              @Nullable Shop targetShop,
                              @Nullable Product targetProduct,
                              int shopPage) {


    public BalanceHolder worth(Player player) {
        BalanceHolder holder = new BalanceHolder();

        this.sellContext.getItems().forEach(quantified -> {
            Product product = quantified.getProduct();

            holder.store(product.getCurrency(), product.getFinalSellPrice(player, quantified.getUnits()));
        });

        return holder;
    }
}