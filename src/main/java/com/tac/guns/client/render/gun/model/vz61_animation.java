package com.tac.guns.client.render.gun.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.Config;
import com.tac.guns.client.SpecialModels;
import com.tac.guns.client.render.gun.IOverrideModel;
import com.tac.guns.client.render.gun.ModelOverrides;
import com.tac.guns.client.util.RenderUtil;
import com.tac.guns.common.Gun;
import com.tac.guns.init.ModEnchantments;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import com.tac.guns.item.attachment.IAttachment;
import com.tac.guns.util.GunEnchantmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemCooldowns;
import com.mojang.math.Vector3f;

/*
 * Because the revolver has a rotating chamber, we need to render it in a
 * different way than normal items. In this case we are overriding the model.
 */

/**
 * Author: Timeless Development, and associates.
 */
public class vz61_animation implements IOverrideModel
{
    @Override
    public void render(float partialTicks, ItemTransforms.TransformType transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrices, MultiBufferSource renderBuffer, int light, int overlay)
    {
        if(ModelOverrides.hasModel(stack) && transformType.equals(ItemTransforms.TransformType.GUI) && Config.CLIENT.quality.reducedGuiWeaponQuality.get())
        {
            matrices.pushPose();
            matrices.mulPose(Vector3f.XP.rotationDegrees(-60.0F));
            matrices.mulPose(Vector3f.YP.rotationDegrees(225.0F));
            matrices.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
            matrices.translate(0.9,0,0);
            matrices.scale(1.5F,1.5F,1.5F);
            RenderUtil.renderModel(stack, stack, matrices, renderBuffer, light, overlay);
            matrices.popPose();
            return;
        }


        if(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.OVER_CAPACITY.get(), stack) > 0)
        {
            RenderUtil.renderModel(SpecialModels.VZ61_EXTENDED_MAG.getModel(), stack, matrices, renderBuffer, light, overlay);
        }
        else
        {
            RenderUtil.renderModel(SpecialModels.VZ61_STANDARD_MAG.getModel(), stack, matrices, renderBuffer, light, overlay);
        }

        RenderUtil.renderModel(SpecialModels.VZ61.getModel(), stack, matrices, renderBuffer, light, overlay);

        //Always push
        matrices.pushPose();
        ItemCooldowns tracker = Minecraft.getInstance().player.getCooldowns();
        float cooldownOg = tracker.getCooldownPercent(stack.getItem(), Minecraft.getInstance().getFrameTime());
        GunItem gunItem = ((GunItem) stack.getItem());
        if(Gun.hasAmmo(stack))
        {
            // Math provided by Bomb787 on GitHub and Curseforge!!!
            if(GunEnchantmentHelper.getRate(stack, gunItem.getGun()) <= 1 && cooldownOg != 0)
                matrices.translate(0, 0, 0.385f * (-4.5 * Math.pow(0.5-0.5, 2) + 1.0));
            else
                matrices.translate(0, 0, 0.385f * (-4.5 * Math.pow(cooldownOg-0.5, 2) + 1.0));
        }
        else if(!Gun.hasAmmo(stack))
        {
            if(cooldownOg > 0.5){
                // Math provided by Bomb787 on GitHub and Curseforge!!!
                matrices.translate(0, 0, 0.385f * (-4.5 * Math.pow(cooldownOg-0.5, 2) + 1.0));
            }
            else
            {
                matrices.translate(0, 0, 0.385f * (-4.5 * Math.pow(0.5-0.5, 2) + 1.0));
            }
        }
        matrices.translate(0, 0, 0.025F);
        RenderUtil.renderModel(SpecialModels.VZ61_BOLT.getModel(), stack, matrices, renderBuffer, light, overlay);

        //Always pop
        matrices.popPose();
    }
     

    //TODO comments
}
