/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui;

import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.commands.RCCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by lukas on 10.10.16.
 */

@SideOnly(Side.CLIENT)
public class GuiHider
{
    protected static GuiScreen hiddenGUI;
    protected static Visualizer visualizer;

    public static boolean canHide()
    {
        return hiddenGUI == null;
    }

    public static boolean hideGUI()
    {
        return hideGUI(null);
    }

    public static boolean hideGUI(Visualizer visualizer)
    {
        if (!canHide())
            return false;

        Minecraft mc = Minecraft.getMinecraft();
        hiddenGUI = mc.currentScreen;
        GuiHider.visualizer = visualizer;

        if (hiddenGUI == null)
            return false;

        mc.displayGuiScreen(null);

        ITextComponent reopen = new TextComponentString("/" + RCCommands.reopen.getName());
        reopen.getStyle().setColor(TextFormatting.GREEN);
        reopen.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + RCCommands.reopen.getName()));
        reopen.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, RecurrentComplex.translations.get("commands.rcreopen.run")));

        mc.player.sendMessage(RecurrentComplex.translations.format("commands.rc.didhide", reopen));

        return true;
    }

    public static boolean canReopen()
    {
        return hiddenGUI != null;
    }

    public static boolean reopenGUI()
    {
        if (!canReopen())
            return false;

        GuiScreen hiddenGUI = GuiHider.hiddenGUI;
        GuiHider.hiddenGUI = null;
        visualizer = null;

        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(hiddenGUI);

        if (mc.currentScreen == null)
        {
            GuiHider.hiddenGUI = hiddenGUI;
            return false;
        }

        return true;
    }

    public static void tryReopenGUI()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (!canReopen())
        {
            mc.player.sendMessage(RecurrentComplex.translations.get("commands.rcreopen.nogui"));
            return;
        }

        if (!reopenGUI())
            mc.player.sendMessage(RecurrentComplex.translations.get("commands.rcreopen.fail"));
    }

    public static void draw(Entity renderEntity, float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (renderEntity == mc.player && visualizer != null)
            visualizer.draw(renderEntity, partialTicks);
    }

    public interface Visualizer
    {
        void draw(Entity renderEntity, float partialTicks);
    }
}
