package io.karatelabs.markup;

class MarkupHintException extends RuntimeException {
   MarkupHintException(String hint, Throwable cause) {
      super(hint, cause);
   }
}
