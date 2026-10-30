package io.karatelabs.markup;

import java.util.ArrayDeque;
import java.util.Deque;
import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IStandaloneElementTag;
import org.thymeleaf.model.IText;

public class FragmentTraceHandler extends AbstractTemplateHandler {
   private final Deque<FragmentTraceHandler.Frame> stack = new ArrayDeque<>();
   private int depth = 0;

   public void handleOpenElement(IOpenElementTag tag) {
      if (!"ka-trace".equals(tag.getElementCompleteName())) {
         this.stack.push(new FragmentTraceHandler.Frame(false, null));
         super.handleOpenElement(tag);
      } else {
         String payload = tag.getAttributeValue("data-ka-trace");
         this.emitComment(formatBegin(payload, this.depth));
         this.stack.push(new FragmentTraceHandler.Frame(true, payload));
         this.depth++;
      }
   }

   public void handleCloseElement(ICloseElementTag tag) {
      if (!"ka-trace".equals(tag.getElementCompleteName())) {
         super.handleCloseElement(tag);
         if (!this.stack.isEmpty()) {
            this.stack.pop();
         }
      } else {
         FragmentTraceHandler.Frame frame = this.stack.isEmpty() ? null : this.stack.pop();
         if (frame != null && frame.traced) {
            this.depth--;
            this.emitComment(formatEnd(frame.payload, this.depth));
         }
      }
   }

   public void handleStandaloneElement(IStandaloneElementTag tag) {
      super.handleStandaloneElement(tag);
   }

   private void emitComment(String text) {
      IModelFactory factory = this.getContext().getModelFactory();
      IComment comment = factory.createComment(text);
      super.handleComment(comment);
      IText nl = factory.createText("\n");
      super.handleText(nl);
   }

   private static String formatBegin(String payload, int depth) {
      return formatComment("ka:fragment-begin", payload, depth);
   }

   private static String formatEnd(String payload, int depth) {
      return formatComment("ka:fragment-end", payload, depth);
   }

   private static String formatComment(String label, String payload, int depth) {
      String[] parts = payload == null ? new String[0] : payload.split("\\|", 3);
      String kind = parts.length > 0 ? parts[0] : "";
      String expr = parts.length > 1 ? parts[1] : "";
      String with = parts.length > 2 ? parts[2] : null;
      StringBuilder sb = new StringBuilder(" ").append(label);
      sb.append(" (").append(kind).append(") ").append(expr);
      sb.append(" depth=").append(depth);
      if (with != null && !with.isEmpty()) {
         sb.append(" with={").append(with).append("}");
      }

      sb.append(' ');
      return sb.toString();
   }

   private static final class Frame {
      final boolean traced;
      final String payload;

      Frame(boolean traced, String payload) {
         this.traced = traced;
         this.payload = payload;
      }
   }
}
