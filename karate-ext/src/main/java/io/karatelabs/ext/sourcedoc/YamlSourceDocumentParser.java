package io.karatelabs.ext.sourcedoc;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.events.Event.ID;

final class YamlSourceDocumentParser implements SourceDocumentParser {
   private static final int CODE_POINT_LIMIT = 67108864;
   private static final int MAX_DEPTH = 200;

   @Override
   public SourceDocument parse(String var1, String var2) {
      LoaderOptions var3 = new LoaderOptions();
      var3.setCodePointLimit(67108864);
      var3.setProcessComments(true);
      ArrayList<org.yaml.snakeyaml.events.Event> var4 = new ArrayList();

      try {
         for (Event var10 : new Yaml(var3).parse(new StringReader(var2))) {
            var4.add(var10);
         }
      } catch (MarkedYAMLException var7) {
         Mark var6 = var7.getProblemMark();
         throw new SourceDocumentException(var1 + (var6 == null ? "" : ":" + (var6.getLine() + 1) + ":" + (var6.getColumn() + 1)) + " — " + var7.getProblem(), var7);
      } catch (RuntimeException var8) {
         throw new SourceDocumentException("could not parse '" + var1 + "' as YAML: " + var8.getMessage(), var8);
      } catch (StackOverflowError var9) {
         throw new SourceDocumentException("could not parse '" + var1 + "' as YAML: nesting too deep");
      }

      return new io.karatelabs.ext.sourcedoc.YamlSourceDocumentParser.ParserState(var1, var2, var4).parseDocument();
   }

   private static final class ParserState {
      private final String sourceName;
      private final String sourceText;
      private final SourceLineIndex lineIndex;
      private final List<Event> events;
      private final Map<String, io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry> pointerEntries = new LinkedHashMap<>();
      private final List<SourceComment> comments = new ArrayList<>();
      private final List<CommentEvent> pendingBlockComments = new ArrayList<>();
      private final Map<String, Object> anchors = new HashMap<>();
      private final Map<String, io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan> anchorSpans = new HashMap<>();
      private String currentPointer = "";
      private int eventIndex;
      private int depth;

      ParserState(String var1, String var2, List<Event> var3) {
         this.sourceName = var1;
         this.sourceText = var2;
         this.lineIndex = io.karatelabs.ext.sourcedoc.SourceLineIndex.create(var2);
         this.events = var3;
      }

      SourceDocument parseDocument() {
         this.expectEvent(ID.StreamStart);
         if (this.peekEvent() != null && !this.peekEvent().is(ID.StreamEnd)) {
            this.expectEvent(ID.DocumentStart);
            Object var1 = this.parseValue("");
            this.expectEvent(ID.DocumentEnd);
            this.flushRemainingBlockComments();
            if (this.peekEvent() != null && this.peekEvent().is(ID.DocumentStart)) {
               throw new SourceDocumentException(this.sourceName + " carries more than one YAML document — a spec is one document, and silently grading the first would grade half the file");
            } else {
               return new SourceDocument(this.sourceName, this.sourceText, var1, this.pointerEntries, this.comments);
            }
         } else {
            this.flushRemainingBlockComments();
            return new SourceDocument(this.sourceName, this.sourceText, null, this.pointerEntries, this.comments);
         }
      }

      private Object parseValue(String var1) {
         Event var2 = this.peekEvent();
         if (var2 == null) {
            throw new SourceDocumentException(this.sourceName + ": the document ended where a value was expected");
         } else if (++this.depth > 200) {
            throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — nesting deeper than 200 levels; this is not an API description");
         } else {
            Object var3;
            try {
               var3 = this.parseEvent(var1, var2);
            } finally {
               this.depth--;
            }

            return var3;
         }
      }

      private Object parseEvent(String var1, Event var2) {
         if (var2 instanceof AliasEvent) {
            return this.parseAlias(var1, (AliasEvent)this.nextEvent());
         } else if (var2 instanceof ScalarEvent) {
            return this.parseScalar(var1, (ScalarEvent)this.nextEvent());
         } else if (var2 instanceof SequenceStartEvent) {
            return this.parseSequence(var1, (SequenceStartEvent)this.nextEvent());
         } else if (var2 instanceof MappingStartEvent) {
            return this.parseMapping(var1, (MappingStartEvent)this.nextEvent());
         } else {
            throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — unexpected " + var2.getEventId());
         }
      }

      private Object parseAlias(String var1, AliasEvent var2) {
         String var3 = var2.getAnchor();
         if (!this.anchors.containsKey(var3)) {
            throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — undefined alias '*" + var3 + "'");
         } else {
            io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var4 = this.pointerEntryFor(var1);
            var4.valueSpan = this.spanFromMarks(var2.getStartMark(), var2.getEndMark());
            var4.aliasAnchorSpan = this.anchorSpans.get(var3);
            return this.anchors.get(var3);
         }
      }

      private Object parseScalar(String var1, ScalarEvent var2) {
         io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var3 = this.pointerEntryFor(var1);
         var3.valueSpan = this.spanFromMarks(var2.getStartMark(), var2.getEndMark());
         var3.scalarText = var2.isPlain() ? this.sourceText.substring(var3.valueSpan.offset(), var3.valueSpan.endOffset()) : var2.getValue();
         Object var4 = this.parseScalarValue(var2);
         this.recordAnchor(var2.getAnchor(), var4, var3.valueSpan);
         return var4;
      }

      private Object parseSequence(String var1, SequenceStartEvent var2) {
         ArrayList<java.lang.Object> var3 = new ArrayList();
         io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var4 = this.pointerEntryFor(var1);
         this.recordAnchor(var2.getAnchor(), var3, this.spanFromMarks(var2.getStartMark(), var2.getEndMark()));
         int var5 = 0;

         while (this.peekEvent() != null && !this.peekEvent().is(ID.SequenceEnd)) {
            String var6 = var1 + "/" + var5++;
            this.flushBlockCommentsBeforeLine(var6, this.peekEvent().getStartMark().getLine());
            this.currentPointer = var6;
            var3.add(this.parseValue(var6));
         }

         Event var7 = this.expectEvent(ID.SequenceEnd);
         var4.valueSpan = this.spanFromMarks(var2.getStartMark(), var7.getEndMark());
         this.recordAnchor(var2.getAnchor(), var3, var4.valueSpan);
         return var3;
      }

      private Object parseMapping(String var1, MappingStartEvent var2) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var4 = this.pointerEntryFor(var1);
         this.recordAnchor(var2.getAnchor(), var3, this.spanFromMarks(var2.getStartMark(), var2.getEndMark()));
         ArrayList<java.lang.Object> var5 = null;

         while (this.peekEvent() != null && !this.peekEvent().is(ID.MappingEnd)) {
            Event var6 = this.peekEvent();
            String var7 = this.mappingKey(var6);
            boolean var8 = "<<".equals(var7) && var6 instanceof ScalarEvent var9 && var9.isPlain();
            if (var8) {
               this.nextEvent();
               if (var5 == null) {
                  var5 = new ArrayList();
               }

               var5.add(this.parseValue(var1 + "/<<"));
            } else {
               String var13 = var1 + "/" + io.karatelabs.ext.sourcedoc.SourceDocument.escapePointerSegment(var7);
               io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry var10 = this.pointerEntryFor(var13);
               this.flushBlockCommentsBeforeLine(var13, var6.getStartMark().getLine());
               this.currentPointer = var13;
               this.nextEvent();
               var10.keySpan = this.spanFromMarks(var6.getStartMark(), var6.getEndMark());
               if (var6 instanceof ScalarEvent var11) {
                  this.recordAnchor(var11.getAnchor(), var7, var10.keySpan);
               }

               var3.put(var7, this.parseValue(var13));
            }
         }

         Event var12 = this.expectEvent(ID.MappingEnd);
         var4.valueSpan = this.spanFromMarks(var2.getStartMark(), var12.getEndMark());
         this.recordAnchor(var2.getAnchor(), var3, var4.valueSpan);
         if (var5 != null) {
            this.applyMergeAliases(var3, var5);
         }

         return var3;
      }

      // $VF: One or more variable merging failures!
      // $VF: Could not properly define all variable types!
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      private void applyMergeAliases(Map<String, Object> var1, List<Object> var2) {
         for (Object var4 : var2) {
            for (Object var8 : var4 instanceof Collection ? (Collection)var4 : List.of(var4)) {
               if (!(var8 instanceof Map var7)) {
                  throw new SourceDocumentException(this.sourceName + ": a '<<' merge takes a mapping or a list of mappings, got " + (var8 == null ? "null" : var8.getClass().getSimpleName()));
               }

               var7.forEach((key, value) -> var1.putIfAbsent((String)key, value));
            }
         }
      }

      private String mappingKey(Event var1) {
         if (var1 instanceof ScalarEvent var4) {
            return var4.getValue();
         } else if (var1 instanceof AliasEvent var2) {
            Object var3 = this.anchors.get(var2.getAnchor());
            if (!this.anchors.containsKey(var2.getAnchor())) {
               throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — undefined alias '*" + var2.getAnchor() + "' used as a mapping key");
            } else if (!(var3 instanceof Map) && !(var3 instanceof List)) {
               return String.valueOf(var3);
            } else {
               throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — the alias '*" + var2.getAnchor() + "' names a collection and cannot be a mapping key");
            }
         } else {
            throw new SourceDocumentException(
               this.sourceName
                  + this.formatMark(var1.getStartMark())
                  + " — a mapping key here is a "
                  + var1.getEventId()
                  + "; a complex key has no JSON Pointer, so it cannot be governed"
            );
         }
      }

      private Object parseScalarValue(ScalarEvent var1) {
         String var2 = var1.getTag();
         if (var2 != null && !"!".equals(var2)) {
            return this.parseTaggedScalar(var1, var2);
         } else {
            return var1.isPlain() ? io.karatelabs.ext.sourcedoc.YamlScalarParser.parse(var1.getValue()) : var1.getValue();
         }
      }

      private Object parseTaggedScalar(ScalarEvent var1, String var2) {
         String var3 = var1.getValue();

         return switch (var2) {
            case "tag:yaml.org,2002:str" -> var3;
            case "tag:yaml.org,2002:null" -> null;
            case "tag:yaml.org,2002:bool" -> {
               if (!"true".equalsIgnoreCase(var3) && !"false".equalsIgnoreCase(var3)) {
                  throw new SourceDocumentException(this.sourceName + this.formatMark(var1.getStartMark()) + " — '" + var3 + "' is tagged " + var2 + " but is not true or false");
               }

               yield "true".equalsIgnoreCase(var3);
            }
            case "tag:yaml.org,2002:int", "tag:yaml.org,2002:float" -> {
               Object var6 = io.karatelabs.ext.sourcedoc.YamlScalarParser.parse(var3);
               if (!(var6 instanceof Number)) {
                  throw new SourceDocumentException(this.sourceName + this.formatMark(var1.getStartMark()) + " — '" + var3 + "' is tagged " + var2 + " but is not a number");
               }

               yield var6;
            }
            default -> throw new SourceDocumentException(this.sourceName + this.formatMark(var1.getStartMark()) + " — the tag '" + var2 + "' is not supported in a governed document (core types only)");
         };
      }

      private void recordAnchor(String var1, Object var2, io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan var3) {
         if (var1 != null) {
            this.anchors.put(var1, var2);
            this.anchorSpans.put(var1, var3);
         }
      }

      private void flushBlockCommentsBeforeLine(String var1, int var2) {
         if (!this.pendingBlockComments.isEmpty()) {
            int var3 = var2 - 1;

            int var4;
            for (var4 = this.pendingBlockComments.size(); var4 > 0 && this.pendingBlockComments.get(var4 - 1).getStartMark().getLine() == var3; var3--) {
               var4--;
            }

            for (int var5 = 0; var5 < this.pendingBlockComments.size(); var5++) {
               this.comments.add(this.createComment(this.pendingBlockComments.get(var5), io.karatelabs.ext.sourcedoc.SourceComment.Kind.BLOCK, var5 < var4 ? "" : var1));
            }

            this.pendingBlockComments.clear();
         }
      }

      private void flushRemainingBlockComments() {
         for (CommentEvent var2 : this.pendingBlockComments) {
            this.comments.add(this.createComment(var2, io.karatelabs.ext.sourcedoc.SourceComment.Kind.BLOCK, ""));
         }

         this.pendingBlockComments.clear();
      }

      private SourceComment createComment(CommentEvent var1, io.karatelabs.ext.sourcedoc.SourceComment.Kind var2, String var3) {
         Mark var4 = var1.getStartMark();
         return new SourceComment(var1.getValue(), var2, var3, var4.getLine() + 1, var4.getColumn() + 1);
      }

      private void consumeComments() {
         while (this.eventIndex < this.events.size() && this.events.get(this.eventIndex) instanceof CommentEvent var1) {
            this.eventIndex++;
            switch (var1.getCommentType()) {
               case BLANK_LINE:
                  this.flushRemainingBlockComments();
                  break;
               case BLOCK:
                  this.pendingBlockComments.add(var1);
                  break;
               case IN_LINE:
                  this.comments.add(this.createComment(var1, io.karatelabs.ext.sourcedoc.SourceComment.Kind.INLINE, this.currentPointer));
            }
         }
      }

      private Event peekEvent() {
         this.consumeComments();
         return this.eventIndex < this.events.size() ? this.events.get(this.eventIndex) : null;
      }

      private Event nextEvent() {
         this.consumeComments();
         if (this.eventIndex >= this.events.size()) {
            throw new SourceDocumentException(this.sourceName + ": the document ended unexpectedly");
         } else {
            return this.events.get(this.eventIndex++);
         }
      }

      private Event expectEvent(ID var1) {
         Event var2 = this.nextEvent();
         if (!var2.is(var1)) {
            throw new SourceDocumentException(this.sourceName + this.formatMark(var2.getStartMark()) + " — expected " + var1 + ", found " + var2.getEventId());
         } else {
            return var2;
         }
      }

      private io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry pointerEntryFor(String var1) {
         return this.pointerEntries.computeIfAbsent(var1, var0 -> new io.karatelabs.ext.sourcedoc.SourceDocument.PointerEntry());
      }

      private io.karatelabs.ext.sourcedoc.SourceDocument.SourceSpan spanFromMarks(Mark var1, Mark var2) {
         int var3 = var1.getIndex();
         int var4 = Math.max(var3, Math.min(var2.getIndex(), this.sourceText.length()));

         while (var4 > var3 && Character.isWhitespace(this.sourceText.charAt(var4 - 1))) {
            var4--;
         }

         return this.lineIndex.spanBetween(var3, var4);
      }

      private String formatMark(Mark var1) {
         return var1 == null ? "" : ":" + (var1.getLine() + 1) + ":" + (var1.getColumn() + 1);
      }
   }
}
