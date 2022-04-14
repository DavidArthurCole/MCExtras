package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class WarningScreen extends Screen {
   private final Component titleComponent;
   private final Component content;
   private final Component check;
   private final Component narration;
   protected final Screen previous;
   @Nullable
   protected Checkbox stopShowing;
   private MultiLineLabel message;

   protected WarningScreen(Component var1, Component var2, Component var3, Component var4, Screen var5) {
      super(NarratorChatListener.NO_TITLE);
      this.message = MultiLineLabel.EMPTY;
      this.titleComponent = var1;
      this.content = var2;
      this.check = var3;
      this.narration = var4;
      this.previous = var5;
   }

   protected abstract void initButtons(int var1);

   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.content, this.width - 50);
      int var10000 = this.message.getLineCount() + 1;
      Objects.requireNonNull(this.font);
      int var1 = var10000 * 9 * 2;
      this.stopShowing = new Checkbox(this.width / 2 - 155 + 80, 76 + var1, 150, 20, this.check, false);
      this.addRenderableWidget(this.stopShowing);
      this.initButtons(var1);
   }

   public Component getNarrationMessage() {
      return this.narration;
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.renderDirtBackground(0);
      drawString(var1, this.font, this.titleComponent, 25, 30, 16777215);
      MultiLineLabel var10000 = this.message;
      Objects.requireNonNull(this.font);
      var10000.renderLeftAligned(var1, 25, 70, 9 * 2, 16777215);
      super.render(var1, var2, var3, var4);
   }
}
