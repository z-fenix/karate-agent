package io.karatelabs.ext.rules.analysis;

import io.karatelabs.parser.Node;
import java.util.LinkedHashMap;
import java.util.List;

public sealed interface AbstractValue permits io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue, AbstractValue.ArrayAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue, io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue {
   long a();

   public record ArrayAbstractValue(List<AbstractValue> b) implements AbstractValue {

      public ArrayAbstractValue(List<AbstractValue> b) {
         this.b = b;
      }

      @Override
      public long a() {
         long var1 = 0L;

         for (AbstractValue var4 : this.b) {
            var1 |= var4.a();
         }

         return var1;
      }

      public List<AbstractValue> b() {
         return this.b;
      }
   }

   public record FunctionAbstractValue(Node b) implements AbstractValue {

      public FunctionAbstractValue(Node b) {
         this.b = b;
      }

      @Override
      public long a() {
         return 0L;
      }

      public Node b() {
         return this.b;
      }
   }

   public record SymbolicReferenceValue(String b) implements AbstractValue {

      public SymbolicReferenceValue(String b) {
         this.b = b;
      }

      @Override
      public long a() {
         return 0L;
      }

      public String b() {
         return this.b;
      }
   }

   public record ScalarSetAbstractValue(List<Object> d, long a) implements AbstractValue {

      public ScalarSetAbstractValue(List<Object> d, long a) {
         d = List.copyOf(d);
         if (d.size() > 1) {
            Class var4 = d.get(0).getClass();

            for (Object var6 : d) {
               if (var6.getClass() != var4) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("MEMBERS_MIXED", -1, "a set of " + var4.getSimpleName() + " and " + var6.getClass().getSimpleName());
               }
            }
         }

         if (!d.isEmpty() && !(d.get(0) instanceof String) && !(d.get(0) instanceof Boolean)) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("MEMBERS_MIXED", -1, "a set of " + d.get(0).getClass().getSimpleName());
         } else {
            this.d = d;
            this.a = a;
         }
      }

      static io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue a(Object var0) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(List.of(var0), 0L);
      }

      boolean b() {
         return this.d.size() == 1;
      }

      boolean c() {
         return !this.d.isEmpty() && this.d.get(0) instanceof Boolean;
      }

      public List<Object> d() {
         return this.d;
      }

      @Override
      public long a() {
         return this.a;
      }
   }

   public record NullAbstractValue() implements AbstractValue {
      @Override
      public long a() {
         return 0L;
      }
   }

   public record NumericIntervalAbstractValue(io.karatelabs.ext.rules.analysis.NumericInterval b, long a) implements AbstractValue {

      public NumericIntervalAbstractValue(io.karatelabs.ext.rules.analysis.NumericInterval b, long a) {
         this.b = b;
         this.a = a;
      }

      static io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue a(double var0) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(io.karatelabs.ext.rules.analysis.NumericInterval.a(var0), 0L);
      }

      public io.karatelabs.ext.rules.analysis.NumericInterval b() {
         return this.b;
      }

      @Override
      public long a() {
         return this.a;
      }
   }

   public record ObjectAbstractValue(LinkedHashMap<String, AbstractValue> b, long c, boolean d) implements AbstractValue {

      public ObjectAbstractValue(LinkedHashMap<String, AbstractValue> b, long c, boolean d) {
         this.b = b;
         this.c = c;
         this.d = d;
      }

      static io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue a(LinkedHashMap<String, AbstractValue> var0) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue(var0, 0L, false);
      }

      @Override
      public long a() {
         long var1 = this.c;

         for (AbstractValue var4 : this.b.values()) {
            var1 |= var4.a();
         }

         return var1;
      }

      public LinkedHashMap<String, AbstractValue> b() {
         return this.b;
      }

      public long c() {
         return this.c;
      }

      public boolean d() {
         return this.d;
      }
   }

   public record DerivedStringAbstractValue(long a) implements AbstractValue {

      public DerivedStringAbstractValue(long a) {
         this.a = a;
      }
   }

   public record UndefinedAbstractValue() implements AbstractValue {
      @Override
      public long a() {
         return 0L;
      }
   }
}
