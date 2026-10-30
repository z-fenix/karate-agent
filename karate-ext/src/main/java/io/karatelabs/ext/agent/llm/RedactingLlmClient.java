package io.karatelabs.ext.agent.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class RedactingLlmClient implements LlmClient {
   private final LlmClient a;
   private final UnaryOperator<String> b;
   private final Consumer<LlmRequest> c;
   private final String d;
   private String e;

   public RedactingLlmClient(LlmClient var1, UnaryOperator<String> var2) {
      this(var1, var2, null, "loop");
   }

   public RedactingLlmClient(LlmClient var1, UnaryOperator<String> var2, Consumer<LlmRequest> var3) {
      this(var1, var2, var3, "loop");
   }

   private RedactingLlmClient(LlmClient var1, UnaryOperator<String> var2, Consumer<LlmRequest> var3, String var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }

   public RedactingLlmClient e() {
      return new RedactingLlmClient(this.a, this.b, this.c, "draft");
   }

   @Override
   public LlmResponse a(List<LlmMessage> var1, String var2, Consumer<String> var3) {
      ArrayList<io.karatelabs.ext.agent.llm.LlmMessage> var4 = new ArrayList(var1.size());

      for (LlmMessage var6 : var1) {
         var4.add(var6.mapText(this.b));
      }

      String var9 = var2 == null ? null : this.b.apply(var2);
      if (this.c != null) {
         synchronized (this) {
            this.c.accept(new LlmRequest(this.d, Objects.equals(var9, this.e) ? null : var9, List.copyOf(var4)));
            this.e = var9;
         }
      }

      return this.a.a(var4, var9, var3);
   }

   @Override
   public LlmProvider a() {
      return this.a.a();
   }

   @Override
   public String b() {
      return this.a.b();
   }

   @Override
   public void c() {
      this.a.c();
   }

   @Override
   public void close() throws Exception {
      this.a.close();
   }
}
