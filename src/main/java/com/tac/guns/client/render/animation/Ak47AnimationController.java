package com.tac.guns.client.render.animation;


import com.tac.guns.GunMod;
import com.tac.guns.init.ModItems;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class Ak47AnimationController extends GunAnimationController {
    public static int INDEX_BODY = 7;
    public static int INDEX_LEFT_HAND = 9;
    public static int INDEX_RIGHT_HAND = 1;
    public static int INDEX_MAGAZINE = 5;
    public static int INDEX_BOLT = 3;
    public static final AnimationMeta RELOAD_NORM = new AnimationMeta(new ResourceLocation("tac","animations/ak47_reload_norm.gltf"));
    public static final AnimationMeta INSPECT = new AnimationMeta(new ResourceLocation("tac","animations/ak47_inspect.gltf"));
    private static final Ak47AnimationController instance = new Ak47AnimationController();

    private Ak47AnimationController() {
        try {
            Animations.load(RELOAD_NORM);
            Animations.load(INSPECT);
        } catch (IOException e) {
            GunMod.LOGGER.fatal(e.getStackTrace());
        }
        GunAnimationController.setAnimationControllerMap(ModItems.AK47.getId(),this);
    }

    public static Ak47AnimationController getInstance(){
        return instance;
    }

    @Override
    protected AnimationMeta getAnimationFromLabel(AnimationLabel label) {
        switch (label){
            case INSPECT: return INSPECT;
            case RELOAD_NORMAL: return RELOAD_NORM;
            default: return null;
        }
    }

    @Override
    protected int getAttachmentsNodeIndex() {
        return INDEX_BODY;
    }

    @Override
    protected int getRightHandNodeIndex() {
        return INDEX_RIGHT_HAND;
    }

    @Override
    protected int getLeftHandNodeIndex() {
        return INDEX_LEFT_HAND;
    }
}
