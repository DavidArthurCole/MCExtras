package net.minecraft.client.gui.screens.multiplayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class Realms32bitWarningScreen extends WarningScreen {
   private static final Component TITLE;
   private static final Component CONTENT;
   private static final Component CHECK;
   private static final Component NARRATION;

   public Realms32bitWarningScreen(Screen var1) {
      super(TITLE, CONTENT, CHECK, NARRATION, var1);
   }

   protected void initButtons(int var1) {
      this.addRenderableWidget(new Button(this.width / 2 - 75, 100 + var1, 150, 20, CommonComponents.GUI_DONE, (var1x) -> {
         if (this.stopShowing.selected()) {
            this.minecraft.options.skipRealms32bitWarning = true;
            this.minecraft.options.save();
         }

         this.minecraft.setScreen(this.previous);
      }));
   }

   static {
      TITLE = (new TranslatableComponent("title.32bit.deprecation.realms.header")).withStyle(ChatFormatting.BOLD);
      CONTENT = new TranslatableComponent("title.32bit.deprecation.realms");
      CHECK = new TranslatableComponent("title.32bit.deprecation.realms.check");
      NARRATION = TITLE.copy().append("\n").append(CONTENT);
   }
}
