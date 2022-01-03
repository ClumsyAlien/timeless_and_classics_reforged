package com.tac.guns.client.render.gun.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.tac.guns.Reference;
import com.tac.guns.client.GunRenderType;
import com.tac.guns.client.handler.AimingHandler;
import com.tac.guns.client.render.animation.Animations;
import com.tac.guns.client.render.animation.GunAnimationController;
import com.tac.guns.client.render.gun.IOverrideModel;
import com.tac.guns.client.util.RenderUtil;
import com.tac.guns.item.attachment.IAttachment;
import com.tac.guns.util.OptifineHelper;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * Author: MrCrayfish
 */
public class LongRange8xScopeModel implements IOverrideModel
{
    private static final ResourceLocation RED_DOT_RETICLE = new ResourceLocation(Reference.MOD_ID, "textures/items/timeless_scopes/bushnell_revised.png");
    //private static final ResourceLocation RED_DOT_RETICLE_GLOW = new ResourceLocation(Reference.MOD_ID, "textures/effect/red_dot_reticle_glow.png");
    //private static final ResourceLocation VIGNETTE = new ResourceLocation(Reference.MOD_ID, "textures/effect/scope_vignette.png");

    @Override
    public void render(float partialTicks, ItemCameraTransforms.TransformType transformType, ItemStack stack, ItemStack parent, LivingEntity entity, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int light, int overlay) {

        if (OptifineHelper.isShadersEnabled()) {
            double transition = 1.0D - Math.pow(1.0D - AimingHandler.get().getNormalisedAdsProgress(), 2.0D);
            double zScale = 0.05D + 0.95D * (1.0D - transition);
            matrixStack.scale(1.0F, 1.0F, (float)zScale);
        }

        Animations.getExtraMatrixStack().translate(0,-0.15,-0.42);
        RenderUtil.renderModel(stack, parent, matrixStack, renderTypeBuffer, light, overlay);
        Animations.getExtraMatrixStack().translate(0,0.15,0.42);
        Animations.applyExtraTransform(matrixStack);
        GunAnimationController controller = GunAnimationController.fromItem(parent.getItem());
        if(controller!=null ) if(Animations.isAnimationRunning(controller.animationRunning())) return;

        if(transformType.isFirstPerson() && entity.equals(Minecraft.getInstance().player))
        {

            if(entity.getPrimaryHand() == HandSide.LEFT)
            {
                matrixStack.scale(-1, 1, 1);
            }

            float scopeSize = 1.20F;
            float size = scopeSize / 16.0F;
            float crop = 0.410F;
            Minecraft mc = Minecraft.getInstance();
            MainWindow window = mc.getMainWindow();


            float texU = ((window.getWidth() - window.getHeight() + window.getHeight() * crop * 2.0F) / 2.0F) / window.getWidth();

            matrixStack.push();
            {
                Matrix4f matrix = matrixStack.getLast().getMatrix();
                Matrix3f normal = matrixStack.getLast().getNormal();

                matrixStack.translate(-size / 2, 0.0595 , 4.55 * 0.0625);

                float color = (float) AimingHandler.get().getNormalisedAdsProgress() * 0.8F + 0.2F;

                IVertexBuilder builder;

                if(!OptifineHelper.isShadersEnabled())
                {
                    builder = renderTypeBuffer.getBuffer(GunRenderType.getScreen());
                    builder.pos(matrix, 0, size, 0).color(color, color, color, 1.0F).tex(texU, 1.0F - crop).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                    builder.pos(matrix, 0, 0, 0).color(color, color, color, 1.0F).tex(texU, crop).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                    builder.pos(matrix, size, 0, 0).color(color, color, color, 1.0F).tex(1.0F - texU, crop).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                    builder.pos(matrix, size, size, 0).color(color, color, color, 1.0F).tex(1.0F - texU, 1.0F - crop).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                }

                matrixStack.translate(0, 0, 0.0001);

                double invertProgress = (1.0 - AimingHandler.get().getNormalisedAdsProgress());
                matrixStack.translate(-0.04 * invertProgress, 0.01 * invertProgress, 0);

                double scale = 8.0;
                matrixStack.translate(size / 2, size / 2, 0);
                matrixStack.translate(-(size / scale) / 2, -(size / scale) / 2, 0);
                matrixStack.translate(0, 0, 0.0001);

                int reticleGlowColor = RenderUtil.getItemStackColor(stack, parent, IAttachment.Type.SCOPE_RETICLE_COLOR, 1);

                float red = ((reticleGlowColor >> 16) & 0xFF) / 255F;
                float green = ((reticleGlowColor >> 8) & 0xFF) / 255F;
                float blue = ((reticleGlowColor >> 0) & 0xFF) / 255F;
                float alpha = (float) AimingHandler.get().getNormalisedAdsProgress();

                alpha = (float) (0.75F * AimingHandler.get().getNormalisedAdsProgress());

                matrixStack.scale(10.0f,10.0f,10.0f);
                matrixStack.translate(-0.00455715, -0.00456, 0.0000);
                builder = renderTypeBuffer.getBuffer(RenderType.getEntityTranslucent(RED_DOT_RETICLE));
                builder.pos(matrix, 0, (float) (size / scale), 0).color(red, green, blue, alpha).tex(0.0F, 0.9375F).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                builder.pos(matrix, 0, 0, 0).color(red, green, blue, alpha).tex(0.0F, 0.0F).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                builder.pos(matrix, (float) (size / scale), 0, 0).color(red, green, blue, alpha).tex(0.9375F, 0.0F).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
                builder.pos(matrix, (float) (size / scale), (float) (size / scale), 0).color(red, green, blue, alpha).tex(0.9375F, 0.9375F).overlay(overlay).lightmap(15728880).normal(normal, 0.0F, 1.0F, 0.0F).endVertex();


            }
            matrixStack.pop();
        }//float scopeSize = 1.095F;matrixStack.translate(-size / 2, 0.0645 , 3.45 * 0.0625);
    }
}
