package com.example.multiaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class PiglinBarterModifier extends LootModifier {

    public static final Codec<PiglinBarterModifier> CODEC =
            RecordCodecBuilder.create(instance ->
                    codecStart(instance).apply(
                            instance,
                            PiglinBarterModifier::new
                    )
            );

    public PiglinBarterModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        /*
         * Условие в JSON ограничивает модификатор таблицей
         * minecraft:gameplay/piglin_bartering.
         */
        if (generatedLoot.isEmpty()) {
            return generatedLoot;
        }

        ServerLevel level = context.getLevel();

        // Отдельный случайный результат пиглина: от x0 до x16.
        int multiplier = level.random.nextInt(17);

        if (context.getParamOrNull(LootContextParams.THIS_ENTITY)
                instanceof Piglin piglin) {
            MultiplierDisplay.show(piglin, multiplier);
        }

        if (multiplier == 0) {
            generatedLoot.clear();
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

            while (remaining > 0) {
                int count = (int) Math.min(
                        remaining,
                        original.getMaxStackSize()
                );

                ItemStack copy = original.copy();
                copy.setCount(count);
                multiplied.add(copy);

                remaining -= count;
            }
        }

        return multiplied;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
