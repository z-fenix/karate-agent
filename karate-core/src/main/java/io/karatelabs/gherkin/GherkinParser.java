package io.karatelabs.gherkin;

import io.karatelabs.common.Pair;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.parser.BaseLexer;
import io.karatelabs.parser.BaseParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GherkinParser extends BaseParser {
   private static final Logger logger = LoggerFactory.getLogger(GherkinParser.class);
   private static final TokenType[] T_STEP_LINE = new TokenType[]{TokenType.G_KEYWORD, TokenType.EQ, TokenType.IDENT, TokenType.DOT, TokenType.G_EXPR};
   private static final AtomicBoolean PARALLEL_TAG_WARNED = new AtomicBoolean();
   private Node ast;

   public GherkinParser(Resource resource) {
      super(resource, getTokens(resource), false);
   }

   public GherkinParser(Resource resource, boolean errorRecovery) {
      super(resource, getTokens(resource), errorRecovery);
   }

   public static List<Token> getTokens(Resource resource) {
      return BaseLexer.tokenize(new GherkinLexer(resource));
   }

   public Node getAst() {
      return this.ast;
   }

   public Feature parse() {
      this.ast = this.parseAst();
      return this.transformToFeature(this.ast);
   }

   private Node parseAst() {
      this.enter(NodeType.G_FEATURE);
      if (this.peek() == TokenType.EOF) {
         this.consume(TokenType.EOF);
         this.exit();
         return this.markerNode().getFirst();
      } else {
         this.tags();
         if (!this.consumeIf(TokenType.G_FEATURE)) {
            this.error(new TokenType[]{TokenType.G_FEATURE});
            this.recoverTo(new TokenType[]{TokenType.G_SCENARIO, TokenType.G_SCENARIO_OUTLINE, TokenType.G_BACKGROUND, TokenType.EOF});
         }

         this.nameDesc();
         this.background();

         while (this.peek() != TokenType.EOF) {
            if (!this.scenarioOrOutline()) {
               if (this.peek() == TokenType.G_PREFIX) {
                  this.error("step outside a Background, Scenario or Scenario Outline");
               } else {
                  this.error(new TokenType[]{TokenType.G_SCENARIO, TokenType.G_SCENARIO_OUTLINE});
               }

               this.enter(NodeType.ERROR);
               this.recoverTo(new TokenType[]{TokenType.G_TAG, TokenType.G_SCENARIO, TokenType.G_SCENARIO_OUTLINE, TokenType.G_PREFIX, TokenType.EOF});
               this.exit();
            }
         }

         this.consume(TokenType.EOF);
         this.exit();
         return this.markerNode().getFirst();
      }
   }

   private boolean tags() {
      if (!this.peekIf(TokenType.G_TAG)) {
         return false;
      } else {
         this.enter(NodeType.G_TAGS);

         while (this.consumeIf(TokenType.G_TAG)) {
         }

         return this.exit();
      }
   }

   private boolean nameDesc() {
      if (!this.peekIf(TokenType.G_DESC)) {
         return false;
      } else {
         this.enter(NodeType.G_NAME_DESC);

         while (this.consumeIf(TokenType.G_DESC)) {
         }

         return this.exit();
      }
   }

   private boolean background() {
      if (!this.enter(NodeType.G_BACKGROUND, TokenType.G_BACKGROUND)) {
         return false;
      } else {
         this.nameDesc();

         while (this.step()) {
         }

         return this.exit();
      }
   }

   private boolean scenarioOrOutline() {
      if (this.peekIf(TokenType.G_TAG)) {
         this.tags();
      }

      if (this.peekIf(TokenType.G_SCENARIO)) {
         return this.scenario();
      } else {
         return this.peekIf(TokenType.G_SCENARIO_OUTLINE) ? this.scenarioOutline() : false;
      }
   }

   private boolean scenario() {
      if (!this.enter(NodeType.G_SCENARIO, TokenType.G_SCENARIO)) {
         return false;
      } else {
         this.nameDesc();
         this.docString();

         while (this.step()) {
         }

         return this.exit();
      }
   }

   private boolean scenarioOutline() {
      if (!this.enter(NodeType.G_SCENARIO_OUTLINE, TokenType.G_SCENARIO_OUTLINE)) {
         return false;
      } else {
         this.nameDesc();
         this.docString();

         while (this.step()) {
         }

         while (this.examples()) {
         }

         return this.exit();
      }
   }

   private boolean examples() {
      if (this.peekIf(TokenType.G_TAG)) {
         this.enter(NodeType.G_EXAMPLES);
         this.tags();
         if (!this.consumeIf(TokenType.G_EXAMPLES)) {
            return this.exit(false, false);
         }
      } else if (!this.enter(NodeType.G_EXAMPLES, TokenType.G_EXAMPLES)) {
         return false;
      }

      this.nameDesc();
      this.table();
      return this.exit();
   }

   private boolean step() {
      if (!this.enter(NodeType.G_STEP, TokenType.G_PREFIX)) {
         return false;
      } else {
         this.consumeIf(TokenType.G_KEYWORD);
         this.stepLine();
         this.docString();
         this.table();
         return this.exit();
      }
   }

   private boolean stepLine() {
      if (!this.peekAnyOf(T_STEP_LINE)) {
         return false;
      } else {
         this.enter(NodeType.G_STEP_LINE);

         while (this.peekAnyOf(T_STEP_LINE)) {
            this.consumeNext();
         }

         return this.exit();
      }
   }

   private boolean docString() {
      if (!this.enter(NodeType.G_DOC_STRING, TokenType.G_TRIPLE_QUOTE)) {
         return false;
      } else {
         while (!this.peekIf(TokenType.G_TRIPLE_QUOTE) && !this.peekIf(TokenType.EOF)) {
            this.consumeNext();
         }

         if (!this.consumeIf(TokenType.G_TRIPLE_QUOTE)) {
            this.error(new TokenType[]{TokenType.G_TRIPLE_QUOTE});
         }

         return this.exit();
      }
   }

   private boolean table() {
      if (!this.peekIf(TokenType.G_PIPE)) {
         return false;
      } else {
         this.enter(NodeType.G_TABLE);

         while (this.tableRow()) {
         }

         return this.exit();
      }
   }

   private boolean tableRow() {
      if (!this.peekIf(TokenType.G_PIPE)) {
         return false;
      } else {
         int rowLine = this.peekToken().line;
         this.enter(NodeType.G_TABLE_ROW);
         this.consumeNext();

         while (true) {
            if (this.consumeIf(TokenType.G_TABLE_CELL)) {
               if (!this.consumeIf(TokenType.G_PIPE)) {
                  break;
               }
            } else {
               if (!this.peekIf(TokenType.G_PIPE) || this.peekToken().line != rowLine) {
                  break;
               }

               this.consumeNext();
            }
         }

         return this.exit();
      }
   }

   private Feature transformToFeature(Node ast) {
      Feature feature = new Feature(this.resource);
      feature.setCommentSpans(commentSpans(this.tokens));
      List<Tag> pendingTags = null;
      int i = 0;

      for (int n = ast.size(); i < n; i++) {
         Node child = ast.get(i);
         switch (child.type) {
            case G_TAGS:
               pendingTags = this.transformTags(child);
               break;
            case TOKEN:
               if (child.token.type == TokenType.G_FEATURE) {
                  feature.setLine(child.token.line + 1);
                  if (pendingTags != null) {
                     feature.setTags(pendingTags);
                     pendingTags = null;
                  }
               }
               break;
            case G_NAME_DESC:
               Pair<String> nd = this.transformNameDesc(child);
               feature.setName(nd.left);
               feature.setDescription(nd.right);
               break;
            case G_BACKGROUND:
               feature.setBackground(this.transformBackground(feature, child));
               break;
            case G_SCENARIO: {
               FeatureSection section = this.transformScenario(feature, child, pendingTags);
               feature.addSection(section);
               pendingTags = null;
               break;
            }
            case G_SCENARIO_OUTLINE: {
               FeatureSection section = this.transformScenarioOutline(feature, child, pendingTags);
               feature.addSection(section);
               pendingTags = null;
            }
            case ERROR:
         }
      }

      return feature;
   }

   private List<Tag> transformTags(Node tagsNode) {
      List<Tag> tags = new ArrayList<>();
      int i = 0;

      for (int n = tagsNode.size(); i < n; i++) {
         Node child = tagsNode.get(i);
         if (child.isToken() && child.token.type == TokenType.G_TAG) {
            Tag tag = new Tag(child.token.line + 1, child.token.getText(), SourceSpan.of(child.token, child.token));
            tags.add(tag);
            if ("parallel".equals(tag.getName()) && PARALLEL_TAG_WARNED.compareAndSet(false, true)) {
               String path = this.resource != null ? this.resource.getPrefixedPath() : "<unknown>";
               logger.warn(
                  "@parallel is no longer supported in v2 and is silently ignored (first seen at {}:{}). Use @lock=<name> for mutual exclusion or @lock=* for exclusive execution. See https://docs.karatelabs.io/getting-started/migration-from-v1#parallel-execution-control",
                  path,
                  tag.getLine()
               );
            }
         }
      }

      return tags;
   }

   private Pair<String> transformNameDesc(Node node) {
      String name = null;
      StringBuilder desc = new StringBuilder();
      boolean first = true;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (child.isToken() && child.token.type == TokenType.G_DESC) {
            String text = StringUtils.trimToNull(child.token.getText());
            if (first) {
               name = text;
               first = false;
            } else if (text != null) {
               if (!desc.isEmpty()) {
                  desc.append('\n');
               }

               desc.append(child.token.getText());
            }
         }
      }

      String description = desc.isEmpty() ? null : desc.toString();
      return Pair.of(name, description);
   }

   private Background transformBackground(Feature feature, Node node) {
      Background bg = new Background();
      List<Step> steps = new ArrayList<>();
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         switch (child.type) {
            case TOKEN:
               if (child.token.type == TokenType.G_BACKGROUND) {
                  bg.setLine(child.token.line + 1);
                  bg.setSpan(SourceSpan.of(child.token, lastToken(node)));
               }
               break;
            case G_STEP:
               steps.add(this.transformStep(feature, null, steps.size(), child));
         }
      }

      bg.setSteps(steps);
      return bg;
   }

   private FeatureSection transformScenario(Feature feature, Node node, List<Tag> tags) {
      FeatureSection section = new FeatureSection();
      Scenario scenario = new Scenario(feature, section, -1);
      section.setScenario(scenario);
      List<Step> steps = new ArrayList<>();
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         switch (child.type) {
            case G_TAGS:
               if (tags == null) {
                  tags = this.transformTags(child);
               } else {
                  tags.addAll(this.transformTags(child));
               }
               break;
            case TOKEN:
               if (child.token.type == TokenType.G_SCENARIO) {
                  scenario.setLine(child.token.line + 1);
                  scenario.setSpan(SourceSpan.of(child.token, lastToken(node)));
               }
               break;
            case G_NAME_DESC:
               Pair<String> nd = this.transformNameDesc(child);
               scenario.setName(nd.left);
               scenario.setDescription(nd.right);
            case G_BACKGROUND:
            case G_SCENARIO:
            case G_SCENARIO_OUTLINE:
            case ERROR:
            default:
               break;
            case G_STEP:
               steps.add(this.transformStep(feature, scenario, steps.size(), child));
         }
      }

      scenario.setTags(tags);
      scenario.setSteps(steps);
      return section;
   }

   private FeatureSection transformScenarioOutline(Feature feature, Node node, List<Tag> tags) {
      FeatureSection section = new FeatureSection();
      ScenarioOutline outline = new ScenarioOutline(feature, section);
      section.setScenarioOutline(outline);
      List<Step> steps = new ArrayList<>();
      List<ExamplesTable> examplesTables = new ArrayList<>();
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         switch (child.type) {
            case G_TAGS:
               if (tags == null) {
                  tags = this.transformTags(child);
               } else {
                  tags.addAll(this.transformTags(child));
               }
               break;
            case TOKEN:
               if (child.token.type == TokenType.G_SCENARIO_OUTLINE) {
                  outline.setLine(child.token.line + 1);
                  outline.setSpan(SourceSpan.of(child.token, lastToken(node)));
               }
               break;
            case G_NAME_DESC:
               Pair<String> nd = this.transformNameDesc(child);
               outline.setName(nd.left);
               outline.setDescription(nd.right);
            case G_BACKGROUND:
            case G_SCENARIO:
            case G_SCENARIO_OUTLINE:
            case ERROR:
            default:
               break;
            case G_STEP:
               steps.add(this.transformStep(feature, null, steps.size(), child));
               break;
            case G_EXAMPLES:
               examplesTables.add(this.transformExamples(child));
         }
      }

      outline.setTags(tags);
      outline.setSteps(steps);
      outline.setExamplesTables(examplesTables);
      return section;
   }

   private ExamplesTable transformExamples(Node node) {
      ExamplesTable examples = new ExamplesTable();
      List<Tag> tags = null;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         switch (child.type) {
            case G_TAGS:
               tags = this.transformTags(child);
               break;
            case TOKEN:
               if (child.token.type == TokenType.G_EXAMPLES) {
                  examples.setLine(child.token.line + 1);
                  examples.setSpan(SourceSpan.of(child.token, lastToken(node)));
               }
               break;
            case G_NAME_DESC:
               Pair<String> nd = this.transformNameDesc(child);
               examples.setName(nd.left);
            case G_BACKGROUND:
            case G_SCENARIO:
            case G_SCENARIO_OUTLINE:
            case ERROR:
            case G_STEP:
            case G_EXAMPLES:
            default:
               break;
            case G_TABLE:
               examples.setTable(this.transformTable(child));
         }
      }

      examples.setTags(tags);
      return examples;
   }

   private Step transformStep(Feature feature, Scenario scenario, int index, Node node) {
      Step step = scenario != null ? new Step(scenario, index) : new Step(feature, index);
      Token lastToken = null;
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (!child.isToken()) {
            switch (child.type) {
               case G_TABLE:
                  step.setTable(this.transformTable(child));
                  break;
               case G_STEP_LINE:
                  String text = this.extractStepText(child);
                  step.setText(text);
                  lastToken = child.getLast().token;
                  break;
               case G_DOC_STRING:
                  step.setDocString(this.extractDocString(child));
                  step.setDocStringLine(this.extractDocStringContentLine(child));
            }
         } else {
            lastToken = child.token;
            switch (child.token.type) {
               case G_PREFIX:
                  step.setPrefix(child.token.getText().trim());
                  step.setLine(child.token.line + 1);
                  List<Token> tokenComments = child.token.getComments();
                  if (tokenComments.isEmpty()) {
                     break;
                  }

                  List<String> comments = new ArrayList<>();
                  List<SourceSpan> commentSpans = new ArrayList<>();

                  for (Token t : tokenComments) {
                     comments.add(t.getText().trim());
                     commentSpans.add(SourceSpan.of(t, t));
                  }

                  step.setComments(comments);
                  step.setCommentSpans(commentSpans);
                  break;
               case G_KEYWORD:
                  step.setKeyword(child.token.getText());
            }
         }
      }

      if (lastToken != null) {
         step.setEndLine(lastToken.line + 1);
      } else {
         step.setEndLine(step.getLine());
      }

      step.setSpan(SourceSpan.of(node.getFirstToken(), lastToken(node)));
      return step;
   }

   private static Token lastToken(Node node) {
      if (node.isToken()) {
         return node.token;
      } else {
         for (int i = node.size() - 1; i >= 0; i--) {
            Token token = lastToken(node.get(i));
            if (token != null) {
               return token;
            }
         }

         return null;
      }
   }

   private static List<SourceSpan> commentSpans(List<Token> tokens) {
      List<SourceSpan> spans = new ArrayList<>();

      for (Token token : tokens) {
         for (Token comment : token.getComments()) {
            spans.add(SourceSpan.of(comment, comment));
         }
      }

      return spans;
   }

   private String extractStepText(Node stepLineNode) {
      if (stepLineNode.isEmpty()) {
         return null;
      } else {
         Token first = stepLineNode.getFirst().token;
         Token last = stepLineNode.getLast().token;
         int start = first.pos;
         int end = last.pos + last.length;
         return this.resource.getText().substring(start, end);
      }
   }

   private String extractDocString(Node docStringNode) {
      Token openQuote = null;
      Token closeQuote = null;
      int i = 0;

      for (int n = docStringNode.size(); i < n; i++) {
         Node child = docStringNode.get(i);
         if (child.isToken() && child.token.type == TokenType.G_TRIPLE_QUOTE) {
            if (openQuote == null) {
               openQuote = child.token;
            } else {
               closeQuote = child.token;
            }
         }
      }

      if (openQuote != null && closeQuote != null) {
         i = openQuote.pos + openQuote.length;
         int end = closeQuote.pos;
         String rawContent = this.resource.getText().substring(i, end).replace("\r", "");
         String[] linesArray = rawContent.split("\n", -1);
         List<String> lines = new ArrayList<>();

         for (String line : linesArray) {
            if (!line.isEmpty()) {
               lines.add(line);
            }
         }

         if (lines.isEmpty()) {
            return null;
         } else {
            int marginPos = indexOfFirstText(lines.get(0));
            StringBuilder sb = new StringBuilder();

            for (int ix = 0; ix < lines.size(); ix++) {
               String linex = lines.get(ix);
               int firstTextPos = indexOfFirstText(linex);
               if (marginPos < linex.length() && marginPos <= firstTextPos) {
                  linex = linex.substring(marginPos);
               }

               if (ix < lines.size() - 1) {
                  sb.append(linex).append('\n');
               } else {
                  sb.append(linex);
               }
            }

            return sb.toString().trim();
         }
      } else {
         return null;
      }
   }

   private int extractDocStringContentLine(Node docStringNode) {
      Token openQuote = null;
      int i = 0;

      for (int n = docStringNode.size(); i < n; i++) {
         Node child = docStringNode.get(i);
         if (child.isToken() && child.token.type == TokenType.G_TRIPLE_QUOTE) {
            openQuote = child.token;
            break;
         }
      }

      if (openQuote == null) {
         return -1;
      } else {
         i = openQuote.pos + openQuote.length;
         String afterQuote = this.resource.getText().substring(i);
         String[] rawLines = afterQuote.split("\n", -1);

         for (int ix = 0; ix < rawLines.length; ix++) {
            String rawLine = rawLines[ix].replace("\r", "").trim();
            if (!rawLine.isEmpty()) {
               return openQuote.line + ix;
            }
         }

         return openQuote.line + 1;
      }
   }

   private static int indexOfFirstText(String s) {
      int pos = 0;

      for (char c : s.toCharArray()) {
         if (!Character.isWhitespace(c)) {
            return pos;
         }

         pos++;
      }

      return 0;
   }

   private Table transformTable(Node tableNode) {
      List<List<String>> rows = new ArrayList<>();
      List<Integer> lineNumbers = new ArrayList<>();
      List<SourceSpan> rowSpans = new ArrayList<>();
      int i = 0;

      for (int n = tableNode.size(); i < n; i++) {
         Node rowNode = tableNode.get(i);
         if (rowNode.type == NodeType.G_TABLE_ROW) {
            List<String> cells = new ArrayList<>();
            int line = 0;
            boolean expectingCell = false;
            int j = 0;

            for (int m = rowNode.size(); j < m; j++) {
               Node cellNode = rowNode.get(j);
               if (cellNode.isToken()) {
                  if (cellNode.token.type == TokenType.G_PIPE) {
                     if (line == 0) {
                        line = cellNode.token.line + 1;
                     }

                     if (expectingCell) {
                        cells.add("");
                     }

                     expectingCell = true;
                  } else if (cellNode.token.type == TokenType.G_TABLE_CELL) {
                     cells.add(cellNode.token.getText().trim().replace("\\|", "|"));
                     expectingCell = false;
                  }
               }
            }

            if (!cells.isEmpty()) {
               rows.add(cells);
               lineNumbers.add(line);
               rowSpans.add(SourceSpan.of(rowNode.getFirstToken(), lastToken(rowNode)));
            }
         }
      }

      if (rows.isEmpty()) {
         return null;
      } else {
         Table table = new Table(rows, lineNumbers);
         table.setRowSpans(rowSpans);
         return table;
      }
   }

   public static MatchExpression parseMatchExpression(String expression) {
      String stepText = "* match " + expression;
      Resource resource = Resource.text(stepText);
      String text = resource.getText();
      List<Token> tokens = getTokens(resource);
      boolean each = false;
      String operator = null;
      int actualStart = -1;
      int actualEnd = -1;
      int expectedStart = -1;
      int expectedEnd = -1;

      for (Token token : tokens) {
         if (token.type != TokenType.WS
            && token.type != TokenType.WS_LF
            && token.type != TokenType.G_PREFIX
            && token.type != TokenType.EOF
            && (token.type != TokenType.G_KEYWORD || !"match".equals(token.getText()))) {
            if (token.type == TokenType.G_KEYWORD && "each".equals(token.getText())) {
               each = true;
            } else if (token.type == TokenType.G_KEYWORD && isMatchOperator(token.getText())) {
               operator = token.getText();
            } else {
               int tokenStart = token.pos;
               int tokenEnd = tokenStart + token.length;
               if (operator == null) {
                  if (actualStart < 0) {
                     actualStart = tokenStart;
                  }

                  actualEnd = tokenEnd;
               } else {
                  if (expectedStart < 0) {
                     expectedStart = tokenStart;
                  }

                  expectedEnd = tokenEnd;
               }
            }
         }
      }

      if (operator == null) {
         throw new RuntimeException("Invalid match expression, no operator found: " + expression);
      } else {
         String actualExpr = actualStart >= 0 ? text.substring(actualStart, actualEnd).trim() : "";
         String expectedExpr = expectedStart >= 0 ? text.substring(expectedStart, expectedEnd).trim() : "";
         return new MatchExpression(each, actualExpr, operator, expectedExpr);
      }
   }

   private static boolean isMatchOperator(String text) {
      return text.equals("==")
         || text.equals("!=")
         || text.startsWith("contains")
         || text.equals("!contains")
         || text.equals("within")
         || text.equals("!within");
   }
}
