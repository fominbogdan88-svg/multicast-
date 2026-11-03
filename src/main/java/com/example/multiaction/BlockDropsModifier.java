package com.example.multiaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class BlockDropsModifier extends LootModifier {

    public static final Codec<BlockDropsModifier> CODEC =
            RecordCodecBuilder.create(instance ->
                    codecStart(instance).apply(
                            instance,
                            BlockDropsModifier::new
                    )
            );

    public BlockDropsModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        // Не меняем добычу сундуков, мобов и других источников.
        if (!context.hasParam(LootContextParams.BLOCK_STATE)) {
            return generatedLoot;
        }

        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY)
                instanceof ServerPlayer player)) {
            return generatedLoot;
        }

        if (generatedLoot.isEmpty()) {
            return generatedLoot;
        }

        int multiplier = MulticastManager.roll(player);

        if (multiplier == 0) {
            generatedLoot.clear();

            player.displayClientMessage(
                    Component.literal("§c✘ Дроп блока сорвался!"),
                    true
            );

            return generatedLoot;
        }

        if (multiplier == 1) {
            return generatedLoot;
        }

        ObjectArrayList<ItemStack> multiplied = new ObjectArrayList<>();

        for (ItemStack original : generatedLoot) {
            if (original.isEmpty()) {
                continue;
            }

            long remaining = (long) original.getCount() * multiplier;
            int maxStackSize = original.getMaxStackSize();

            // Разделяем результат на допустимые стаки.
            while (remaining > 0) {
                int count = (int) Math.min(remaining, maxStackSize);

                ItemStack copy = original.copy();
                copy.setCount(count);

                multiplied.add(copy);
                remaining -= count;
            }
        }

        player.displayClientMessage(
                Component.literal("§6✦ Дроп блока: x" + multiplier),
                true
        );

        return multiplied;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
