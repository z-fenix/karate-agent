package io.karatelabs.common;

public class Pair<T> {
   public final T left;
   public final T right;

   private Pair(T left, T right) {
      this.left = left;
      this.right = right;
   }

   @Override
   public boolean equals(Object o) {
      return !(o instanceof Pair<?> other) ? false : this.left.equals(other.left) && this.right.equals(other.right);
   }

   @Override
   public String toString() {
      return this.left + ":" + this.right;
   }

   public static <T> Pair<T> of(T left, T right) {
      return new Pair<>(left, right);
   }
}
