package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.coverage.Action;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ExecutionFragment(io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind component1, String component2, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome c, Long d, Long e, String f, boolean g, boolean h, String i, String j, String k, int l, Set<String> m, List<Action> n, List<io.karatelabs.ext.coverage.graph.CoverageHit> o, List<Map<String, Object>> p) {
   public static final String a = "fragment";
   public static final String b = "exec";


   public io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind a() {
      return this.component1;
   }

   public String b() {
      return this.component2;
   }

   public io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome c() {
      return this.c;
   }

   public Long d() {
      return this.d;
   }

   public Long e() {
      return this.e;
   }

   public String f() {
      return this.f;
   }

   public boolean g() {
      return this.g;
   }

   public boolean h() {
      return this.h;
   }

   public String i() {
      return this.i;
   }

   public String j() {
      return this.j;
   }

   public String k() {
      return this.k;
   }

   public int l() {
      return this.l;
   }

   public Set<String> m() {
      return this.m;
   }

   public List<Action> n() {
      return this.n;
   }

   public List<io.karatelabs.ext.coverage.graph.CoverageHit> o() {
      return this.o;
   }

   public List<Map<String, Object>> p() {
      return this.p;
   }

   public static enum ExecutionFragmentKind {
      ORIGINAL,
      CHECK_BLOCK,
      TRANSCRIPT,
      CONTAINER,
      REPLAY,
      STREAM;

      public static io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind ofTag(String var0) {
         if (var0 == null) {
            return null;
         } else {
            return switch (var0) {
               case "check-block" -> CHECK_BLOCK;
               case "replay" -> REPLAY;
               case "container" -> CONTAINER;
               default -> null;
            };
         }
      }

      public String tag() {
         return switch (this) {
            case CHECK_BLOCK -> "check-block";
            default -> this.name().toLowerCase();
            case CONTAINER -> "container";
            case REPLAY -> "replay";
         };
      }
   }
}
