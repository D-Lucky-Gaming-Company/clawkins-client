package github.dluckycompany.clawkins.ui;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.Wallet;

/**
 * Merchant shop UI for selling items.
 * Displays player inventory with sell prices and quantity slider.
 * Uses fixed virtual coordinate system (800x600) - resolution-independent rendering.
 *
 * Usage:
 *   MerchantShopUI shop = new MerchantShopUI(inventory, wallet, "Merchant", skin, font);
 *   stage.addActor(shop);
 */
public class MerchantShopUI extends Dialog {
    private final Inventory inventory;
    private final Wallet wallet;
    private final Skin skin;

    // UI components
    private Label walletLabel;
    private Label selectedItemLabel;
    private Label pricePerLabel;
    private Label quantityLabel;
    private Label totalPriceLabel;
    private Slider quantitySlider;
    private Item selectedItem;
    private int selectedQuantity = 1;
    private Consumer<SellAction> onItemSold;
    private Runnable onClose;
    
    // Fixed virtual UI dimensions (constant, independent of physical resolution)
    private static final float SHOP_WIDTH = 500f;
    private static final float SHOP_HEIGHT = 350f;
    private static final float ITEM_BUTTON_WIDTH = 280f;

    public MerchantShopUI(Inventory inventory, Wallet wallet, String merchantName, Skin skin, BitmapFont font) {
        super(merchantName, skin);
        this.inventory = inventory;
        this.wallet = wallet;
        this.skin = skin;
        buildUI();
    }

    private void buildUI() {
        // Fixed virtual UI dimensions - independent of physical screen resolution
        Table content = new Table();

        // Top: Wallet display
        walletLabel = new Label("Money: " + wallet.getMoney(), skin);
        walletLabel.setColor(Color.GOLD);
        content.add(walletLabel).padBottom(20).row();

        // Middle: Item list and details
        Table itemSelectionPanel = buildItemSelectionPanel();
        content.add(itemSelectionPanel).expandX().fillX().height(SHOP_HEIGHT * 0.55f).padBottom(20).row();

        // Bottom: Slider and sell button
        Table sellPanel = buildSellPanel();
        content.add(sellPanel).expandX().fillX().height(SHOP_HEIGHT * 0.25f).row();

        getContentTable().add(content).expandX().fillX();

        // Close button
        button("Done", false);
        setWidth(SHOP_WIDTH);
        setHeight(SHOP_HEIGHT);
        setPosition(400 - SHOP_WIDTH / 2, 300 - SHOP_HEIGHT / 2);  // Center in 800x600 virtual space
    }

    private Table buildItemSelectionPanel() {
        Table panel = new Table();

        // Left: Item list
        Table itemListTable = new Table();
        List<Item> items = inventory.getAllItems();

        if (items.isEmpty()) {
            itemListTable.add(new Label("No items to sell", skin));
        } else {
            for (Item item : items) {
                Button itemButton = createItemButton(item);
                itemListTable.add(itemButton).width(ITEM_BUTTON_WIDTH).height(40).padBottom(5).row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(itemListTable, skin);
        scrollPane.setFadeScrollBars(true);
        panel.add(scrollPane).expand().fill().padRight(10);

        // Right: Item details
        Table detailsTable = new Table();
        detailsTable.add(new Label("Item:", skin)).left().padBottom(5).row();
        selectedItemLabel = new Label("---", skin);
        selectedItemLabel.setColor(Color.GOLD);
        detailsTable.add(selectedItemLabel).left().padBottom(10).row();

        detailsTable.add(new Label("Price each:", skin)).left().padBottom(5).row();
        pricePerLabel = new Label("0", skin);
        pricePerLabel.setColor(Color.YELLOW);
        detailsTable.add(pricePerLabel).left().padBottom(20).row();

        panel.add(detailsTable).width(150);

        return panel;
    }

    private Button createItemButton(Item item) {
        int quantity = inventory.getQuantity(item);
        String buttonText = item.getName() + " ×" + quantity;
        Button button = new TextButton(buttonText, skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectItem(item);
            }
        });
        return button;
    }

    private Table buildSellPanel() {
        Table panel = new Table();

        // Quantity slider
        panel.add(new Label("Quantity:", skin)).padRight(10);
        quantitySlider = new Slider(1, 99, 1, false, skin);
        quantitySlider.setValue(1);
        quantitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                selectedQuantity = (int) quantitySlider.getValue();
                updatePriceDisplay();
            }
        });
        panel.add(quantitySlider).width(100).padRight(20);

        // Quantity label
        quantityLabel = new Label("1", skin);
        panel.add(quantityLabel).width(30).padRight(10).row();

        // Price display
        panel.add(new Label("Total:", skin)).padRight(10);
        totalPriceLabel = new Label("0", skin);
        totalPriceLabel.setColor(Color.YELLOW);
        panel.add(totalPriceLabel).width(100).padRight(20);

        // Sell button
        Button sellButton = new TextButton("Sell", skin);
        sellButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sellItem();
            }
        });
        panel.add(sellButton).width(80).height(40).padLeft(10);

        return panel;
    }

    private void selectItem(Item item) {
        selectedItem = item;
        int quantity = inventory.getQuantity(item);
        
        selectedItemLabel.setText(item.getName());
        pricePerLabel.setText(String.valueOf(item.getSellPrice()));
        
        // Update slider range
        quantitySlider.setRange(1, quantity);
        quantitySlider.setValue(1);
        selectedQuantity = 1;
        updatePriceDisplay();
    }

    private void updatePriceDisplay() {
        if (selectedItem == null) {
            quantityLabel.setText("0");
            totalPriceLabel.setText("0");
            return;
        }

        quantityLabel.setText(String.valueOf(selectedQuantity));
        long totalPrice = (long) selectedItem.getSellPrice() * selectedQuantity;
        totalPriceLabel.setText(String.valueOf(totalPrice));
    }

    private void sellItem() {
        if (selectedItem == null || selectedQuantity <= 0) {
            return;
        }

        // Check if player has enough items
        if (!inventory.hasItem(selectedItem, selectedQuantity)) {
            return;
        }

        long totalPrice = (long) selectedItem.getSellPrice() * selectedQuantity;
        
        // Remove items and add money
        inventory.removeItem(selectedItem, selectedQuantity);
        wallet.addMoney(totalPrice);

        if (onItemSold != null) {
            onItemSold.accept(new SellAction(selectedItem, selectedQuantity, totalPrice));
        }

        // Update displays
        updateWalletDisplay();
        selectedItem = null;
        selectedItemLabel.setText("---");
        pricePerLabel.setText("0");
        quantityLabel.setText("0");
        totalPriceLabel.setText("0");
    }

    private void updateWalletDisplay() {
        walletLabel.setText("Money: " + wallet.getMoney());
    }

    /**
     * Set callback for when an item is sold.
     *
     * @param callback receives SellAction with item, quantity, and total price
     */
    public void setOnItemSoldCallback(Consumer<SellAction> callback) {
        this.onItemSold = callback;
    }

    /**
     * Set callback for when shop closes.
     *
     * @param callback the callback to execute
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onClose = callback;
    }

    @Override
    protected void result(Object object) {
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Data class representing a sell action.
     */
    public static class SellAction {
        public final Item item;
        public final int quantity;
        public final long totalPrice;

        public SellAction(Item item, int quantity, long totalPrice) {
            this.item = item;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
        }
    }
}
