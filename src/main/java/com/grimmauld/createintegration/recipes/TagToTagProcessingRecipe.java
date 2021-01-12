package com.grimmauld.createintegration.recipes;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.grimmauld.createintegration.CreateIntegration;
import net.minecraft.item.crafting.Ingredient;
import com.simibubi.create.content.contraptions.processing.ProcessingOutput;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeSerializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Objects;

public class TagToTagProcessingRecipe<T extends ProcessingRecipe<?>> extends ProcessingRecipeSerializer<T> {

    public TagToTagProcessingRecipe(ProcessingRecipeFactory<T> factory, String type) {
        super(factory);
        this.setRegistryName(new ResourceLocation(CreateIntegration.modid, type));
    }

    @Override
    public T readFromJson(ResourceLocation recipeId, JsonObject json) {
        ProcessingRecipeBuilder<T> builder = new ProcessingRecipeBuilder<>(this.getFactory(), recipeId);

        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (JsonElement e : JSONUtils.getJsonArray(json, "ingredients")) {
            int count = 1;
            if (JSONUtils.hasField((JsonObject) e, "count")) {
                count = JSONUtils.getInt(e.getAsJsonObject().get("count"), "count");
            }
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.deserialize(e.getAsJsonObject()));
            }
        }

        NonNullList<ProcessingOutput> results = NonNullList.create();
        for (JsonElement e : JSONUtils.getJsonArray(json, "results")) {
            int i = JSONUtils.getInt(e.getAsJsonObject().get("count"), "count");
            Item item = null;

            if (e.getAsJsonObject().has("item")) {
                String s1 = JSONUtils.getString(e.getAsJsonObject().get("item"), "item");
                item = Registry.ITEM.getOrDefault(new ResourceLocation(s1));
            } else if (e.getAsJsonObject().has("tag")) {
                ResourceLocation s1 = new ResourceLocation(JSONUtils.getString(e.getAsJsonObject().get("tag"), "tag"));
                if (!Objects.requireNonNull(ItemTags.getCollection().get(s1)).getAllElements().isEmpty()) {
                    item = Lists.newArrayList(Objects.requireNonNull(ItemTags.getCollection().get(s1)).getAllElements()).get(0);
                }
            }
            if (item == null)
                return null;
            ItemStack itemstack = new ItemStack(item, i);
            float chance = 1;
            if (JSONUtils.hasField((JsonObject) e, "chance"))
                chance = JSONUtils.getFloat(e.getAsJsonObject().get("chance"), "chance");
            results.add(new ProcessingOutput(itemstack, chance));
        }

        builder.withItemIngredients(ingredients)
			.withItemOutputs(results);

        if (JSONUtils.hasField(json, "processingTime"))
            builder.duration(JSONUtils.getInt(json, "processingTime"));
        
        return builder.build();
    }
}
