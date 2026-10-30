package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContractCoverageSource implements io.karatelabs.ext.coverage.CoverageSource {
   private final Contract a;
   private final String b;
   private final String c;
   private final AddressMatcher d;
   private final List<CoverageItem> e;
   private final Map<String, Object> f;

   public ContractCoverageSource(Contract var1) {
      this(var1, null);
   }

   public ContractCoverageSource(Contract var1, String var2) {
      this.a = var1;
      this.b = a(var1.protocol());
      this.c = var2 != null && !var2.isBlank() ? var2 : null;
      this.d = io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry.a(var1);
      this.e = Collections.unmodifiableList(this.d.buildItems(var1));
      this.f = Collections.unmodifiableMap(this.d.buildMetadata(var1, this.e));
   }

   public Contract h() {
      return this.a;
   }

   static String a(Protocol var0) {
      return switch (var0) {
         case OPENAPI -> "openapi";
         case GRPC -> "grpc";
         case KAFKA -> "kafka";
         case ASYNCAPI -> "asyncapi";
         case WEBSOCKET -> "websocket";
      };
   }

   @Override
   public List<CoverageItem> a() {
      return this.e;
   }

   @Override
   public String b() {
      return this.b;
   }

   @Override
   public String f() {
      return this.c == null ? this.b : this.b + "." + this.c;
   }

   @Override
   public Map<String, Object> c() {
      return this.f;
   }

   @Override
   public boolean a(Action var1) {
      return this.d.accepts(var1);
   }

   @Override
   public CoverageItem b(Action var1) {
      return this.d.match(var1, this.e);
   }

   @Override
   public List<Warning> a(Action var1, CoverageItem var2) {
      return this.d.warnings(var1, var2, this.e);
   }

   public List<CoverageItem> c(Action var1) {
      if (this.d instanceof OpenApiAddressMatcher var4) {
         return var4.a(var1, this.e);
      } else {
         CoverageItem var2 = this.b(var1);
         return var2 == null ? List.of() : List.of(var2);
      }
   }

   @Override
   public MatchKind b(Action var1, CoverageItem var2) {
      return this.d.matchConfidence(var1, var2, this.e);
   }

   @Override
   public List<Warning> g() {
      return this.d.loadWarnings(this.e);
   }

   @Override
   public Map<String, Object> a(List<Action> var1, CoverageItem var2) {
      return this.d.aggregate(var1, var2);
   }
}
