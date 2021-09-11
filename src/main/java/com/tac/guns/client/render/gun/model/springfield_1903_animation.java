package com.tac.guns.client.render.gun.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.tac.guns.client.SpecialModels;
import com.tac.guns.client.render.gun.IOverrideModel;
import com.tac.guns.client.util.RenderUtil;
import com.tac.guns.common.Gun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.math.vector.Vector3f;

/*
 * Because the revolver has a rotating chamber, we need to render it in a
 * different way than normal items. In this case we are overriding the model.
 */

/**
 * Author: ClumsyAlien, codebase and design based off Mr.Pineapple's original addon
 */
public class springfield_1903_animation implements IOverrideModel {

    /*
        I plan on making a very comprehensive description on my render / rendering methods, currently I am unable to give a good explanation on each part and will be supplying one later one in development!

        If you are just starting out I don't recommend attempting to create an animated part of your weapon is as much as I can comfortably give at this point!
    */
    @Override
    public void render(float v, ItemCameraTransforms.TransformType transformType, ItemStack stack, ItemStack parent, LivingEntity entity, MatrixStack matrices, IRenderTypeBuffer renderBuffer, int light, int overlay) {

            RenderUtil.renderModel(SpecialModels.SPRINGFIELD_1903.getModel(), stack, matrices, renderBuffer, light, overlay);
            matrices.push();


            CooldownTracker tracker = Minecraft.getInstance().player.getCooldownTracker();
            float cooldownOg = tracker.getCooldown(stack.getItem(), Minecraft.getInstance().getRenderPartialTicks());
            float cooldown = (float) easeInOutBack(cooldownOg);

            if (cooldownOg != 0 && cooldownOg < 0.83)
            {
                matrices.translate(-0.039, -0.038, 0.00);
                matrices.rotate(Vector3f.ZN.rotationDegrees(-90F));

                // matrices.translate(0, 0, 0.318f * (-4.5 * Math.pow(cooldownOg +0.19 -0.5, 2) + 1));

                if (cooldownOg < 0.822 && cooldownOg > 0.433)
                {
                    matrices.translate(0, 0, -0.03 * -cooldown);
                    matrices.translate(0, 0, 0.318f * ((1.0 * -cooldown)+1));
                }
                if (cooldownOg < 0.433 && cooldownOg > 0.02)
                {
                    matrices.translate(0, 0, 0.798f * ((1.0 * cooldownOg-0.07)));
                }

            }

            RenderUtil.renderModel(SpecialModels.SPRINGFIELD_1903_BOLT.getModel(), stack, matrices, renderBuffer, light, overlay);
            matrices.pop();
    }
    //Same method from GrenadeLauncherModel, to make a smooth rotation of the chamber.
    private double easeInOutBack(double x) {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;
        return (x < 0.5 ? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2 : (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2);
    }
}
