package com.tac.guns.client.render.pose;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.tac.guns.client.handler.ReloadHandler;
import com.tac.guns.client.render.animation.Animations;
import com.tac.guns.client.render.animation.GunAnimationController;
import com.tac.guns.client.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;


/**
 * Author: ClumsyAlien, codebase and design based off Mr.Crayfish's class concept
 */
public class TwoHandedPoseHighRes_ak47 extends TwoHandedPose {
	@Override
	public void renderFirstPersonArms(ClientPlayerEntity player, HandSide hand, ItemStack stack, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, float partialTicks) {
		MatrixStack extraMatrixStack = Animations.getExtraMatrixStack();
		extraMatrixStack.push();
		extraMatrixStack.translate(0, 0, -1);
		extraMatrixStack.rotate(Vector3f.YP.rotationDegrees(180F));
		
		extraMatrixStack.push();
		
		//float reloadProgress = ReloadHandler.get().getReloadProgress(partialTicks);
		//extraMatrixStack.translate(reloadProgress * 1.25, -reloadProgress, -reloadProgress * 1.5);
		
		int side = hand.opposite() == HandSide.RIGHT ? 1 : -1;
		extraMatrixStack.translate(8.5 * side * 0.0625, -1.015, -0.04);
		
		if (Minecraft.getInstance().player.getSkinType().equals("slim") && hand.opposite() == HandSide.LEFT) {
			extraMatrixStack.translate(0.03125F * -side, 0, 0);
		}
		
		extraMatrixStack.rotate(Vector3f.XP.rotationDegrees(80F));
		extraMatrixStack.rotate(Vector3f.YP.rotationDegrees(15F * -side));
		extraMatrixStack.rotate(Vector3f.ZP.rotationDegrees(15F * -side));
		extraMatrixStack.rotate(Vector3f.XP.rotationDegrees(-35F));
		extraMatrixStack.scale(1.0F, 1.0F, 1.0F);

		GunAnimationController controller = GunAnimationController.fromItem(stack.getItem());
		if(controller != null) controller.pushLeftHandNode();
		matrixStack.push();
		Animations.applyAnimationTransform(stack, player, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,matrixStack);
		RenderUtil.renderFirstPersonArm(player, hand.opposite(), matrixStack, buffer, light);
		matrixStack.pop();
		if(controller != null) Animations.popNode();
		
		extraMatrixStack.pop();
		
		double centerOffset = 2.5;
		if (Minecraft.getInstance().player.getSkinType().equals("slim")) {
			centerOffset += hand == HandSide.RIGHT ? 0.2 : 0.8;
		}
		centerOffset = hand == HandSide.RIGHT ? -centerOffset : centerOffset;
		extraMatrixStack.translate(centerOffset * 0.0405, -0.745, -1.075);
		
		extraMatrixStack.rotate(Vector3f.XP.rotationDegrees(80F));
		extraMatrixStack.scale(1F, 1F, 1F);
		if(controller != null) controller.pushRightHandNode();
		Animations.applyAnimationTransform(stack, player, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,matrixStack);
		RenderUtil.renderFirstPersonArm(player, hand, matrixStack, buffer, light);
		if(controller != null) Animations.popNode();
		extraMatrixStack.pop();

	}
}
