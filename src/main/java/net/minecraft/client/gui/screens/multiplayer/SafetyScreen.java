package net.minecraft.client.gui.screens.multiplayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class SafetyScreen extends WarningScreen {
   private static final Component TITLE;
   private static final Component CONTENT;
   private static final Component CHECK;
   private static final Component NARRATION;

   public SafetyScreen(Screen var1) {
      super(TITLE, CONTENT, CHECK, NARRATION, var1);
   }

   protected void initButtons(int var1) {
      this.addRenderableWidget(new Button(this.width / 2 - 155, 100 + var1, 150, 20, CommonComponents.GUI_PROCEED, (var1x) -> {
         if (this.stopShowing.selected()) {
            this.minecraft.options.skipMultiplayerWarning = true;
            this.minecraft.options.save();
         }

         this.minecraft.setScreen(new JoinMultiplayerScreen(this.previous));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, 100 + var1, 150, 20, CommonComponents.GUI_BACK, (var1x) -> {
         this.minecraft.setScreen(this.previous);
      }));
   }

   static {
      TITLE = (new TranslatableComponent("multiplayerWarning.header")).withStyle(ChatFormatting.BOLD);
      CONTENT = new TranslatableComponent("multiplayerWarning.message");
      CHECK = new TranslatableComponent("multiplayerWarning.check");
      NARRATION = TITLE.copy().append("\n").append(CONTENT);
   }
}
