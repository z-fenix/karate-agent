package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.ProtocolProvider;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProtocolProviderRegistry {
   private static final Logger a = LoggerFactory.getLogger(ProtocolProviderRegistry.class);
   private static volatile Map<Protocol, ProtocolProvider> b;

   private ProtocolProviderRegistry() {
   }

   private static Map<Protocol, ProtocolProvider> a() {
      var var0 = b;
      if (var0 == null) {
         synchronized (ProtocolProviderRegistry.class) {
            var0 = b;
            if (var0 == null) {
               var0 = b();
               b = var0;
            }
         }
      }

      return var0;
   }

   private static Map<Protocol, ProtocolProvider> b() {
      EnumMap var0 = new EnumMap<>(Protocol.class);
      a(var0, new io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry.OpenApiProtocolProvider());

      for (ProtocolProvider var2 : ServiceLoader.load(ProtocolProvider.class, ProtocolProviderRegistry.class.getClassLoader())) {
         a(var0, var2);
      }

      return var0;
   }

   private static void a(Map<Protocol, ProtocolProvider> var0, ProtocolProvider var1) {
      ProtocolProvider var2 = var0.put(var1.protocol(), var1);
      if (var2 != null) {
         a.debug("coverage: protocol provider for {} overridden by {}", var1.protocol(), var1.getClass().getName());
      }
   }

   public static AddressMatcher a(Contract var0) {
      ProtocolProvider var1 = a().get(var0.protocol());
      return (AddressMatcher)(var1 != null ? var1.matcher(var0) : new GenericContractAddressMatcher(var0));
   }

   public static ContractLoader a(Protocol var0) {
      ProtocolProvider var1 = a().get(var0);
      return var1 != null ? var1.loader() : null;
   }

   public static ScaffoldEmitter b(Protocol var0) {
      ProtocolProvider var1 = a().get(var0);
      return var1 != null ? var1.scaffoldEmitter() : null;
   }

   public static boolean c(Protocol var0) {
      return a().containsKey(var0);
   }

   private static final class OpenApiProtocolProvider implements ProtocolProvider {
      @Override
      public Protocol protocol() {
         return Protocol.OPENAPI;
      }

      @Override
      public AddressMatcher matcher(Contract var1) {
         return new OpenApiAddressMatcher(var1);
      }
   }
}
