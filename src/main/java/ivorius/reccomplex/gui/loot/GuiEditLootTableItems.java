/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.loot;

import ivorius.ivtoolkit.gui.*;
import ivorius.ivtoolkit.network.PacketGuiAction;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.gui.GuiHider;
import ivorius.reccomplex.gui.InventoryWatcher;
import ivorius.reccomplex.gui.RCGuiHandler;
import ivorius.reccomplex.utils.SaveDirectoryData;
import ivorius.reccomplex.utils.scale.Scale;
import ivorius.reccomplex.utils.scale.Scales;
import ivorius.reccomplex.world.storage.loot.GenericLootTable;
import ivorius.reccomplex.world.storage.loot.GenericLootTable.Component;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 26.05.14.
 */

@SideOnly(Side.CLIENT)
public class GuiEditLootTableItems extends GuiContainer implements InventoryWatcher
{
    public static ResourceLocation textureBackground = new ResourceLocation(RecurrentComplex.MOD_ID, RecurrentComplex.filePathTextures + "gui_edit_inventory_gen.png");
    public static Scale WEIGHT_SCALE = Scales.pow(5);

    public String key;
    public Component component;
    public SaveDirectoryData saveDirectoryData;

    private GuiButton backBtn;

    private GuiButton nextPageBtn;
    private GuiButton prevPageBtn;

    private List<GuiSlider> weightSliders = new ArrayList<>();
    private List<GuiSliderRange> minMaxSliders = new ArrayList<>();

    private int currentPage;

    public GuiEditLootTableItems(EntityPlayer player, GenericLootTable.Component component, String key, SaveDirectoryData saveDirectoryData)
    {
        super(new ContainerEditLootTableItems(player, key, component));

        this.key = key;
        this.component = component;
        this.saveDirectoryData = saveDirectoryData;

        this.xSize = ContainerEditLootTableItems.SEGMENT_WIDTH * ContainerEditLootTableItems.ITEM_COLUMNS + 20;
        this.ySize = 219;
        ((ContainerEditLootTableItems) inventorySlots).inventory.addWatcher(this);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        weightSliders.clear();
        minMaxSliders.clear();

        int leftEdge = width / 2 - xSize / 2;
        int topEdge = height / 2 - ySize / 2;
        int shiftRightPage = leftEdge + ContainerEditLootTableItems.ITEM_COLUMNS * ContainerEditLootTableItems.SEGMENT_WIDTH;

        this.buttonList.add(this.backBtn = new GuiButton(0, leftEdge, this.height / 2 + 90, xSize / 3 - 1, 20, IvTranslations.get("gui.back")));

        GuiButton saveButton = new GuiButton(1, leftEdge + xSize / 3 + 1, this.height / 2 + 90, xSize / 3 - 2, 20, TextFormatting.GREEN + IvTranslations.get("reccomplex.gui.save"));
        saveButton.enabled = false;
        this.buttonList.add(saveButton);

        GuiButton hideButton = new GuiButton(2, leftEdge + xSize / 3 * 2 + 1, this.height / 2 + 90, xSize / 3 - 1, 20, IvTranslations.get("reccomplex.gui.hidegui"));
        hideButton.enabled = false; // Can't yet with container gui
        this.buttonList.add(hideButton);

        this.buttonList.add(this.nextPageBtn = new GuiButton(10, shiftRightPage, this.height / 2 - 80, 20, 20, ">"));
        this.buttonList.add(this.prevPageBtn = new GuiButton(11, shiftRightPage, this.height / 2 - 50, 20, 20, "<"));

        for (int col = 0; col < ContainerEditLootTableItems.ITEM_COLUMNS; ++col)
        {
            for (int row = 0; row < ContainerEditLootTableItems.ITEM_ROWS; ++row)
            {
                int availableSize = ContainerEditLootTableItems.SEGMENT_WIDTH - 22 - 4;
                int baseX = leftEdge + 20 + col * ContainerEditLootTableItems.SEGMENT_WIDTH;
                int onePart = availableSize / 5;

                GuiSliderRange minMaxSlider = new GuiSliderRange(100, baseX, topEdge + 18 + row * 18, onePart * 2 - 2, 18, "");
                minMaxSlider.addListener(slider -> {
                    List<GenericLootTable.RandomizedItemStack> chestContents = component.items;
                    if (slider.id < 300)
                    {
                        int stackIndex = slider.id - 200;

                        if (stackIndex < chestContents.size())
                        {
                            GenericLootTable.RandomizedItemStack chestContent = chestContents.get(stackIndex);
                            IntegerRange intRange = Ranges.roundedIntRange(minMaxSlider.getRange());
                            chestContent.min = intRange.getMin();
                            chestContent.max = intRange.getMax();

                            minMaxSlider.setRange(new FloatRange(chestContent.min, chestContent.max));

                            updateAllItemSliders();
                        }
                    }
                });
                minMaxSlider.setMinValue(1);
                this.buttonList.add(minMaxSlider);
                minMaxSliders.add(minMaxSlider);

                GuiSlider weightSlider = new GuiSlider(200, baseX + onePart * 2, topEdge + 18 + row * 18, onePart * 3, 18, IvTranslations.get("reccomplex.gui.random.weight"));
                weightSlider.addListener(slider -> {
                    List<GenericLootTable.RandomizedItemStack> chestContents = component.items;
                    if (slider.id < 200 && slider.id >= 100)
                    {
                        int stackIndex = slider.id - 100;
                        if (stackIndex < chestContents.size())
                        {
                            chestContents.get(stackIndex).weight = WEIGHT_SCALE.in(slider.getValue());
                            updateAllItemSliders();
                        }
                    }
                });
                weightSlider.setMinValue(WEIGHT_SCALE.out(0));
                weightSlider.setMaxValue(WEIGHT_SCALE.out(100));
                this.buttonList.add(weightSlider);
                weightSliders.add(weightSlider);
            }
        }

        this.setPage(currentPage);
    }

    public void setPage(int colShift)
    {
        currentPage = colShift;
        ((ContainerEditLootTableItems) inventorySlots).scrollTo(colShift);

        updateAllItemSliders();
        updatePageButtons();
    }

    private void updateAllItemSliders()
    {
        List<GenericLootTable.RandomizedItemStack> chestContents = component.items;

        for (int i = 0; i < weightSliders.size(); i++)
        {
            GuiSlider weightSlider = weightSliders.get(i);
            GuiSliderRange minMaxSlider = minMaxSliders.get(i);

            int index = i + currentPage * ContainerEditLootTableItems.ITEMS_PER_PAGE;
            weightSlider.id = index + 100;
            minMaxSlider.id = index + 200;
            if (index < chestContents.size())
            {
                GenericLootTable.RandomizedItemStack chestContent = chestContents.get(index);
                minMaxSlider.setRange(new FloatRange(chestContent.min, chestContent.max));
                minMaxSlider.setMaxValue(chestContent.itemStack.getMaxStackSize());
                minMaxSlider.enabled = true;
                minMaxSlider.displayString = IvTranslations.format("reccomplex.gui.inventorygen.minMax", chestContent.min, chestContent.max);

                weightSlider.setValue(WEIGHT_SCALE.out((float) chestContent.weight));
                weightSlider.enabled = true;
                weightSlider.displayString = IvTranslations.format("reccomplex.gui.inventorygen.weightNumber", String.format("%.4f", WEIGHT_SCALE.in(weightSlider.getValue())));
            }
            else
            {
                minMaxSlider.setRange(new FloatRange(1, 1));
                minMaxSlider.setMaxValue(64);
                minMaxSlider.enabled = false;
                minMaxSlider.displayString = IvTranslations.get("reccomplex.gui.inventorygen.range");

                weightSlider.setValue(weightSlider.getMinValue());
                weightSlider.enabled = false;
                weightSlider.displayString = IvTranslations.format("reccomplex.gui.random.weight");
            }
        }
    }

    private void updatePageButtons()
    {
        List<GenericLootTable.RandomizedItemStack> chestContents = component.items;
        int neededSlots = chestContents.size() + 1;
        nextPageBtn.enabled = ((currentPage + 1) * ContainerEditLootTableItems.ITEMS_PER_PAGE) <= neededSlots;
        prevPageBtn.enabled = currentPage > 0;
    }

    @Override
    protected void actionPerformed(GuiButton button)
    {
        if (button.enabled)
        {
            if (button.id == 0)
            {
                RCGuiHandler.editLootTableComponent(mc.player, key, component, saveDirectoryData);
            }
            else if (button.id == 2)
            {
                GuiHider.hideGUI();
            }
            else if (button.id == 10)
            {
                setPage(currentPage + 1);
                RecurrentComplex.network.sendToServer(PacketGuiAction.packetGuiAction("igSelectCol", currentPage));
            }
            else if (button.id == 11)
            {
                setPage(currentPage - 1);
                RecurrentComplex.network.sendToServer(PacketGuiAction.packetGuiAction("igSelectCol", currentPage));
            }
        }
    }

    @Override
    protected void keyTyped(char keyChar, int keyCode) throws IOException
    {
        if (keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode())
            return;

        if (keyCode == Keyboard.KEY_ESCAPE) // Would otherwise close GUI
        {
            if (backBtn != null)
                actionPerformed(backBtn);

            return;
        }

        super.keyTyped(keyChar, keyCode);

        if (keyCode == Keyboard.KEY_LEFT && prevPageBtn.enabled)
        {
            actionPerformed(prevPageBtn);
        }
        else if (keyCode == Keyboard.KEY_RIGHT && nextPageBtn.enabled)
        {
            actionPerformed(nextPageBtn);
        }
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int mouseX, int mouseY)
    {
        mc.getTextureManager().bindTexture(textureBackground);
        GlStateManager.color(1.0f, 1.0f, 1.0f);
        drawTexturedModalRect(width / 2 - 176 / 2 - 20 / 2 - 1, height / 2 - 13, 0, 0, 176, 90);

        for (int i = 0; i < ContainerEditLootTableItems.ITEM_ROWS; i++)
        {
            drawTexturedModalRect(width / 2 - ContainerEditLootTableItems.SEGMENT_WIDTH / 2 - 11, height / 2 - 91 + i * 18, 7, 7, 18, 18);
        }
//        for (int i = 0; i < ContainerEditInventoryGen.ITEM_COLUMNS; i++)
//        {
//            int baseX = width / 2 + i * ContainerEditInventoryGen.SEGMENT_WIDTH;
//            drawCenteredString(fontRendererObj, IvTranslations.format("reccomplex.gui.inventorygen.min"), baseX + 20, this.height / 2 - 75, 0xffffffff);
//            drawCenteredString(fontRendererObj, IvTranslations.format("reccomplex.gui.inventorygen.max"), baseX + 40, this.height / 2 - 75, 0xffffffff);
//            drawCenteredString(fontRendererObj, IvTranslations.format("reccomplex.gui.inventorygen.weight"), baseX + 60, this.height / 2 - 75, 0xffffffff);
//        }
    }

    private void drawPlaceholderString(GuiTextField textField, String string)
    {
        if (StringUtils.isNullOrEmpty(textField.getText()))
            drawString(fontRenderer, string, textField.x + 5, textField.y + 7, 0xff888888);
    }

    @Override
    public void inventoryChanged(IInventory inventory)
    {
        updateAllItemSliders();
        updatePageButtons();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);

        if (state == 0)
        {
            for (Object object : buttonList)
            {
                GuiButton button = (GuiButton) object;
                button.mouseReleased(mouseX, mouseY);
            }
        }
    }

//    public void updateSaveButtonEnabled()
//    {
//        backBtn.enabled = key.trim().length() > 0 && component.tableID.trim().length() > 0;
//    }
}
