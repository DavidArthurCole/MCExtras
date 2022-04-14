package net.minecraft.client.gui.screens.recipebook;

import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class BlastingRecipeBookComponent extends AbstractFurnaceRecipeBookComponent {
   private static final Component FILTER_NAME = new TranslatableComponent("gui.recipebook.toggleRecipes.blastable");

   public BlastingRecipeBookComponent() {
   }

   protected Component getRecipeFilterName() {
      return FILTER_NAME;
   }

   protected Set<Item> getFuelItems() {
      return AbstractFurnaceBlockEntity.getFuel().keySet();
   }
}