package com.tac.guns.client.handler;

import com.tac.guns.Config;
import com.tac.guns.GunMod;
import com.tac.guns.common.Gun;
import com.tac.guns.event.GunFireEvent;
import com.tac.guns.item.GunItem;
import com.tac.guns.item.TransitionalTypes.TimelessGunItem;
import com.tac.guns.network.PacketHandler;
import com.tac.guns.network.message.MessageShoot;
import com.tac.guns.network.message.MessageShooting;
import com.tac.guns.util.GunEnchantmentHelper;
import com.tac.guns.util.GunModifierHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CooldownTracker;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Level;
import org.lwjgl.glfw.GLFW;

import static com.tac.guns.GunMod.LOGGER;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class  ShootingHandler
{
    private static ShootingHandler instance;

    public static ShootingHandler get()
    {
        if(instance == null)
        {
            instance = new ShootingHandler();
        }
        return instance;
    }

    public boolean isShooting() {
        return shooting;
    }

    public void setShooting(boolean shooting) {
        this.shooting = shooting;
    }
    public void setShootingError(boolean shootErr) {
        this.shootErr = shootErr;
    }

    private boolean shooting;

    private boolean shootErr;
    private boolean clickUp = false;
    private int burstTracker = 0;
    private int burstCooldown = 0;

    private ShootingHandler() {}

    private boolean isInGame()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.loadingGui != null)
            return false;
        if(mc.currentScreen != null)
            return false;
        if(!mc.mouseHelper.isMouseGrabbed())
            return false;
        return mc.isGameFocused();
    }

    @SubscribeEvent
    public void onKeyPressed(InputEvent.RawMouseEvent event)
    {
        if(!this.isInGame())
            return;

        if(event.getAction() != GLFW.GLFW_PRESS)
            return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if(player == null)
            return;

        if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT && AimingHandler.get().isLookingAtInteractableBlock())
        {
            if(player.getHeldItemMainhand().getItem() instanceof GunItem && !AimingHandler.get().isLookingAtInteractableBlock())
            {
                event.setCanceled(true);
            }
            return;
        }

        ItemStack heldItem = player.getHeldItemMainhand();
        if(heldItem.getItem() instanceof GunItem)
        {
            int button = event.getButton();
            if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            {
                event.setCanceled(true);
            }
            if(event.getAction() == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
            {
                fire(player, heldItem);
            }
        }
    }

    @SubscribeEvent
    public void onHandleShooting(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
            return;

        if(!this.isInGame())
            return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if(player != null)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.getItem() instanceof GunItem && (Gun.hasAmmo(heldItem) || player.isCreative()))
            {
                boolean shooting = GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS && !this.clickUp;
                if(GunMod.controllableLoaded)
                {
                    // shooting |= ControllerHandler.isShooting();
                }
                if(shooting)
                {
                    if(!this.shooting)
                    {
                        this.shooting = true;
                        PacketHandler.getPlayChannel().sendToServer(new MessageShooting(true));
                    }
                }
                else if(this.shooting)
                {
                    this.shooting = false;
                    PacketHandler.getPlayChannel().sendToServer(new MessageShooting(false));
                }
            }
            else if(this.shooting)
            {
                this.shooting = false;
                PacketHandler.getPlayChannel().sendToServer(new MessageShooting(false));
            }
        }
        else
        {
            this.shooting = false;
        }

        if (this.shooting)
            player.setSprinting(false);
    }

    @SubscribeEvent
    public void onHandleBurstShootonKeyPressed(InputEvent.RawMouseEvent event)
    {
        if(!this.isInGame())
            return;

        if(event.getAction() != GLFW.GLFW_PRESS)
            return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if(player == null)
            return;
        if(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            ItemStack heldItem = player.getHeldItemMainhand();
            if (heldItem.getItem() instanceof TimelessGunItem) {
                TimelessGunItem gunItem = (TimelessGunItem) heldItem.getItem();
                Gun gun = gunItem.getGun();
                if (heldItem.getTag().getInt("CurrentFireMode") == 3 && this.burstCooldown == 0) {
                CooldownTracker tracker = player.getCooldownTracker();
                    if (!tracker.hasCooldown(heldItem.getItem())) {
                        fire(player, heldItem);
                        this.burstTracker = gun.getGeneral().getBurstCount();
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public void onPostClientTick(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.END)
            return;

        if(!isInGame())
            return;

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if(player != null)
        {
            ItemStack heldItem = player.getHeldItemMainhand();
            if(heldItem.getItem() instanceof TimelessGunItem)
            {
                TimelessGunItem gunItem = (TimelessGunItem) heldItem.getItem();
                if(heldItem.getTag().getInt("CurrentFireMode") == 3 && Config.CLIENT.controls.burstPress.get())
                {
                    Gun gun = ((TimelessGunItem) heldItem.getItem()).getModifiedGun(heldItem);
                    if(this.shootErr)
                    {
                        if(this.burstTracker > 0)
                            this.burstTracker++;
                        this.shootErr = false;
                    }
                    CooldownTracker tracker = player.getCooldownTracker();
                    if(this.burstTracker > 0) {
                        if(!tracker.hasCooldown(heldItem.getItem())) {
                            fire(player, heldItem);
                            this.burstTracker--;
                        }
                        LOGGER.log(Level.FATAL, "At least catching");
                    }
                    else if(heldItem.getTag().getInt("AmmoCount") > 0 && this.burstTracker > 0)
                    {
                        LOGGER.log(Level.FATAL, "Reset without second fire");
                        if(!tracker.hasCooldown(heldItem.getItem()))
                        {
                            this.burstTracker = 0;
                            this.clickUp = true;
                            this.burstCooldown = gun.getGeneral().getBurstRate();
                        }
                    }
                    return;
                }
                else if(GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS)
                {
                    Gun gun = ((TimelessGunItem) heldItem.getItem()).getModifiedGun(heldItem);
                    if(heldItem.getTag().getInt("CurrentFireMode") == 3 && !Config.CLIENT.controls.burstPress.get() && !this.clickUp && this.burstCooldown <= 0)
                    {
                        if(this.shootErr)
                        {
                            if(this.burstTracker > 0)
                                this.burstTracker--;
                            this.shootErr = false;
                        }
                        CooldownTracker tracker = player.getCooldownTracker();
                        if(this.burstTracker < 2) {
                            if(!tracker.hasCooldown(heldItem.getItem())) {
                                fire(player, heldItem);
                                this.burstTracker++;
                            }
                        }
                        else if(heldItem.getTag().getInt("AmmoCount") > 0)
                        {
                            if(!tracker.hasCooldown(heldItem.getItem()))
                            {
                                Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                                int rate = GunEnchantmentHelper.getRate(heldItem, modifiedGun);
                                this.burstTracker = 0;
                                this.clickUp = true;
                                this.burstCooldown = (int)(GunModifierHelper.getModifiedRate(heldItem, rate)*2.5);
                            }
                        }
                        return;
                    }
                    else if(gun.getGeneral().isAuto() && heldItem.getTag().getInt("CurrentFireMode") == 2)
                    {
                        fire(player, heldItem);
                        return;
                    }
                }
                else if(this.clickUp || GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_RELEASE)
                {
                    if(heldItem.getTag().getInt("CurrentFireMode") == 3 && this.burstTracker > 0) {
                        Gun modifiedGun = gunItem.getModifiedGun(heldItem);
                        this.burstCooldown = (modifiedGun.getGeneral().getBurstRate());
                    }
                    this.burstTracker = 0;
                    this.clickUp = false;
                }
                // CONTINUER FOR BURST. USING BURST TRACKER AS A REVERSE ITOR WHEN BURST IS ON PRESS MODE.
            }
            if(this.burstCooldown > 0)
                this.burstCooldown--;
        }
    }

    public void fire(PlayerEntity player, ItemStack heldItem)
    {
        if(!(heldItem.getItem() instanceof GunItem))
            return;

        if(!Gun.hasAmmo(heldItem) && !player.isCreative())
            return;
        
        if(player.isSpectator())
            return;

        CooldownTracker tracker = player.getCooldownTracker();

        if(!tracker.hasCooldown(heldItem.getItem()))
        {
            GunItem gunItem = (GunItem) heldItem.getItem();
            Gun modifiedGun = gunItem.getModifiedGun(heldItem);

            if(MinecraftForge.EVENT_BUS.post(new GunFireEvent.Pre(player, heldItem)))
                return;

            int rate = GunEnchantmentHelper.getRate(heldItem, modifiedGun);
            rate = GunModifierHelper.getModifiedRate(heldItem, rate);
            tracker.setCooldown(heldItem.getItem(), rate);
            PacketHandler.getPlayChannel().sendToServer(new MessageShoot(player));

            MinecraftForge.EVENT_BUS.post(new GunFireEvent.Post(player, heldItem));
        }
    }
}
