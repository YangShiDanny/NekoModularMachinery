package cn.howxu.mmcr.nekojs.event;

import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;

/** Event object received by MMCREvents.client callbacks. */
public final class ClientInfoEventJS {
    public void addRecipePoolInfo(String poolId, String translationKey, Object... arguments) {
        DeclarationStore.addRecipeInfo(true, poolId, translationKey, arguments);
    }
    public void addRecipeInfo(String recipeId, String translationKey, Object... arguments) {
        DeclarationStore.addRecipeInfo(false, recipeId, translationKey, arguments);
    }
}
