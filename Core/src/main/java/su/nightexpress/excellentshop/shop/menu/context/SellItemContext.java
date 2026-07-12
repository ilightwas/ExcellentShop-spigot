package su.nightexpress.excellentshop.shop.menu.context;

import su.nightexpress.excellentshop.api.product.Product;

public class SellItemContext {

    private final Product product;
    private int           units;

    public SellItemContext(Product product, int units) {
        this.product = product;
        this.units = units;
    }

    public boolean isFullfilled() {
        return this.units >= this.product.getMaxUnitsInStack();
    }

    public Product getProduct() {
        return this.product;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return "SellItemContext [product=" + product + ", units=" + units + "]";
    }
}