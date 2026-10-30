package io.karatelabs.js;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Args {
   public static final Args.Arg<String> STR = new Args.Arg<>(String.class, null, false);
   public static final Args.Arg<Integer> INT = new Args.Arg<>(Integer.class, null, false);
   public static final Args.Arg<Double> NUM = new Args.Arg<>(Double.class, null, false);
   public static final Args.Arg<Boolean> BOOL = new Args.Arg<>(Boolean.class, null, false);
   public static final Args.Arg<Map> MAP = new Args.Arg<>(Map.class, null, false);
   public static final Args.Arg<List> LIST = new Args.Arg<>(List.class, null, false);

   public static <T> Args.Arg<T> type(Class<T> type) {
      return new Args.Arg<>(type, null, false);
   }

   public static JavaCallable call(JavaCallable fn) {
      return fn;
   }

   public static JavaCallable invoke(JavaInvokable fn) {
      return fn;
   }

   public static JavaCallable invoke(Supplier<?> fn) {
      return (ctx, args) -> fn.get();
   }

   public static <A> JavaCallable invoke(Args.Arg<A> a, Function<A, ?> fn) {
      return (ctx, args) -> fn.apply(a.extract(args, 0));
   }

   public static <A, B> JavaCallable invoke(Args.Arg<A> a, Args.Arg<B> b, BiFunction<A, B, ?> fn) {
      return (ctx, args) -> fn.apply(a.extract(args, 0), b.extract(args, 1));
   }

   public static <A, B, C> JavaCallable invoke(Args.Arg<A> a, Args.Arg<B> b, Args.Arg<C> c, Args.Fn3<A, B, C, ?> fn) {
      return (ctx, args) -> fn.apply(a.extract(args, 0), b.extract(args, 1), c.extract(args, 2));
   }

   public static class Arg<T> {
      final Class<T> type;
      final T defaultValue;
      final boolean optional;

      Arg(Class<T> type, T defaultValue, boolean optional) {
         this.type = type;
         this.defaultValue = defaultValue;
         this.optional = optional;
      }

      public Args.Arg<T> optional(T defaultValue) {
         return new Args.Arg<>(this.type, defaultValue, true);
      }

      T extract(Object[] args, int index) {
         if (index >= args.length) {
            if (this.optional) {
               return this.defaultValue;
            } else {
               throw new IllegalArgumentException("missing argument " + index);
            }
         } else {
            Object raw = args[index];
            if (this.type == String.class) {
               return (T)raw.toString();
            } else if (this.type == Integer.class) {
               return (T)(Object) ((Number)raw).intValue();
            } else {
               return (T)(this.type == Double.class ? ((Number)raw).doubleValue() : this.type.cast(raw));
            }
         }
      }
   }

   @FunctionalInterface
   public interface Fn3<A, B, C, R> {
      R apply(A var1, B var2, C var3);
   }
}
