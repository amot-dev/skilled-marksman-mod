package dev.amot.skilledMarksman.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.item.BowItem.getPullProgress;

@Mixin(BowItem.class)
public class BowItemMixin {
    @Unique
    private static final int BOW_CHARGE_TIME = 15;

    // Store divergence values per player
    @Unique
    private final HashMap<LivingEntity, Float> divergenceMap = new HashMap<>();

    @Inject(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;shootAll(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/entity/LivingEntity;)V"))
    private void stopCounter(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof PlayerEntity)) return;

        int maxUseTime = ((BowItem) (Object) this).getMaxUseTime(stack, user);
        int elapsedTicks = maxUseTime - remainingUseTicks;
        float pullProgress = getPullProgress(elapsedTicks);

        float divergence = 1.0F; // Default divergence value
        if (pullProgress == 1.0F) {
            int ticksAfterFullCharge = elapsedTicks - BOW_CHARGE_TIME;
            ticksAfterFullCharge = Math.max(0, ticksAfterFullCharge);
            divergence = Math.min(1.0F, ticksAfterFullCharge * 0.01F); // Divergence increases over time after full charge (5 seconds)
        }

        // Store divergence for use in redirect
        divergenceMap.put(user, divergence);
    }

    @Redirect(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BowItem;shootAll(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/item/ItemStack;Ljava/util/List;FFZLnet/minecraft/entity/LivingEntity;)V"))
    private void redirectShootAll(BowItem instance, ServerWorld serverWorld, LivingEntity shooter, Hand hand, ItemStack stack, List<ItemStack> projectiles, float speed, float divergence, boolean critical, LivingEntity target) {
        // Retrieve stored divergence
        if (divergenceMap.containsKey(shooter)) {
            divergence = divergenceMap.get(shooter);
            divergenceMap.remove(shooter); // Clean up after use
        }

        try {
            Method shootAllMethod = RangedWeaponItem.class.getDeclaredMethod("shootAll", ServerWorld.class, LivingEntity.class, Hand.class, ItemStack.class, List.class, float.class, float.class, boolean.class, LivingEntity.class);
            shootAllMethod.setAccessible(true);
            shootAllMethod.invoke(instance, serverWorld, shooter, hand, stack, projectiles, speed, divergence, critical, target);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}