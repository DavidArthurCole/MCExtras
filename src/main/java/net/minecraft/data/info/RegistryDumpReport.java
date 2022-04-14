package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;

public class RegistryDumpReport implements DataProvider {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private final DataGenerator generator;

   public RegistryDumpReport(DataGenerator var1) {
      this.generator = var1;
   }

   public void run(HashCache var1) throws IOException {
      JsonObject var2 = new JsonObject();
      Registry.REGISTRY.holders().forEach((var1x) -> {
         var2.add(var1x.key().location().toString(), dumpRegistry((Registry)var1x.value()));
      });
      Path var3 = this.generator.getOutputFolder().resolve("reports/registries.json");
      DataProvider.save(GSON, var1, var2, var3);
   }

   private static <T> JsonElement dumpRegistry(Registry<T> var0) {
      JsonObject var1 = new JsonObject();
      if (var0 instanceof DefaultedRegistry) {
         ResourceLocation var2 = ((DefaultedRegistry)var0).getDefaultKey();
         var1.addProperty("default", var2.toString());
      }

      int var4 = Registry.REGISTRY.getId(var0);
      var1.addProperty("protocol_id", var4);
      JsonObject var3 = new JsonObject();
      var0.holders().forEach((var2x) -> {
         Object var3x = var2x.value();
         int var4 = var0.getId(var3x);
         JsonObject var5 = new JsonObject();
         var5.addProperty("protocol_id", var4);
         var3.add(var2x.key().location().toString(), var5);
      });
      var1.add("entries", var3);
      return var1;
   }

   public String getName() {
      return "Registry Dump";
   }
}
