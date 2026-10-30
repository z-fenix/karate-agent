package io.karatelabs.ext.jsformat;

import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JsAstFormatter {
   static final Set<NodeType> SUPPORTED_AST_NODE_TYPES = Collections.unmodifiableSet(
      EnumSet.of(
         NodeType.PROGRAM,
         NodeType.STATEMENT,
         NodeType.IF_STMT,
         NodeType.VAR_STMT,
         NodeType.VAR_DECL,
         NodeType.RETURN_STMT,
         NodeType.TRY_STMT,
         NodeType.THROW_STMT,
         NodeType.FOR_STMT,
         NodeType.WHILE_STMT,
         NodeType.DO_WHILE_STMT,
         NodeType.SWITCH_STMT,
         NodeType.CASE_BLOCK,
         NodeType.DEFAULT_BLOCK,
         NodeType.BREAK_STMT,
         NodeType.CONTINUE_STMT,
         NodeType.LABELLED_STMT,
         NodeType.DELETE_EXPR,
         NodeType.BLOCK,
         NodeType.EOS,
         NodeType.EXPR,
         NodeType.EXPR_LIST,
         NodeType.FN_EXPR,
         NodeType.FN_ARROW_EXPR,
         NodeType.FN_DECL_ARGS,
         NodeType.FN_DECL_ARG,
         NodeType.NEW_EXPR,
         NodeType.TYPEOF_EXPR,
         NodeType.INSTANCEOF_EXPR,
         NodeType.IN_EXPR,
         NodeType.FN_CALL_EXPR,
         NodeType.FN_CALL_ARGS,
         NodeType.FN_CALL_ARG,
         NodeType.FN_TAGGED_TEMPLATE_EXPR,
         NodeType.ASSIGN_EXPR,
         NodeType.LOGIC_EXPR,
         NodeType.LOGIC_AND_EXPR,
         NodeType.LOGIC_NULLISH_EXPR,
         NodeType.LOGIC_TERN_EXPR,
         NodeType.LOGIC_BIT_EXPR,
         NodeType.MATH_ADD_EXPR,
         NodeType.MATH_MUL_EXPR,
         NodeType.MATH_EXP_EXPR,
         NodeType.MATH_POST_EXPR,
         NodeType.MATH_PRE_EXPR,
         NodeType.REF_EXPR,
         NodeType.REF_DOT_EXPR,
         NodeType.REF_BRACKET_EXPR,
         NodeType.UNARY_EXPR,
         NodeType.AWAIT_EXPR,
         NodeType.YIELD_EXPR,
         NodeType.LIT_OBJECT,
         NodeType.OBJECT_ELEM,
         NodeType.LIT_ARRAY,
         NodeType.ARRAY_ELEM,
         NodeType.LIT_EXPR,
         NodeType.PAREN_EXPR,
         NodeType.LIT_TEMPLATE,
         NodeType.LIT_REGEX,
         NodeType.CLASS_EXPR,
         NodeType.CLASS_METHOD,
         NodeType.CLASS_STATIC_BLOCK,
         NodeType.SUPER_EXPR,
         NodeType.PRIVATE_NAME_EXPR
      )
   );
   static final Set<NodeType> TRANSPARENT_AST_NODE_TYPES = Collections.unmodifiableSet(EnumSet.of(NodeType.PATH_EXPR, NodeType.PLACEHOLDER, NodeType.CLASS_FIELD, NodeType.VAR_NAMES));
   private final Node rootNode;
   private final String sourceText;
   private final boolean usesSimpleLineBreaks;
   private final io.karatelabs.ext.jsformat.JsLayoutDocument layoutDocument;
   private final Map<Token, List<io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement>> leadingComments = new IdentityHashMap<>();
   private final Map<Token, List<io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement>> trailingComments = new IdentityHashMap<>();
   private final BitSet tokenPositionsWithComments = new BitSet();
   private final Map<String, String> canonicalTokenCache = new HashMap<>();
   private final Set<Token> explicitSemicolonTokens = Collections.newSetFromMap(new IdentityHashMap<>());
   private final Set<Token> directiveStringTokens = Collections.newSetFromMap(new IdentityHashMap<>());
   private int recursionDepth;

   JsAstFormatter(Node var1, io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var2) {
      this.rootNode = var1;
      this.sourceText = var2.sourceText;
      this.usesSimpleLineBreaks = this.sourceText.indexOf(13) < 0 && this.sourceText.indexOf(8232) < 0 && this.sourceText.indexOf(8233) < 0;
      this.layoutDocument = new io.karatelabs.ext.jsformat.JsLayoutDocument(this.sourceText);
      this.indexComments(var2);
   }

   String format() {
      return this.buildLayout().render(100);
   }

   io.karatelabs.ext.jsformat.JsLayoutDocument buildLayout() {
      this.emitProgram(this.rootNode);
      return this.layoutDocument;
   }

   private void indexComments(io.karatelabs.ext.jsformat.JsFormatOracle.SourceCommentStream var1) {
      for (int var2 = 0; var2 < var1.commentCount; var2++) {
         Token var3 = var1.commentTokens[var2];
         Token var4 = var3.getPrev();

         while (var4 != null && !var4.type.primary) {
            var4 = var4.getPrev();
         }

         Token var5 = var3.getNextPrimary();
         boolean var6 = io.karatelabs.ext.jsformat.JsFormatOracle.areTokensAdjacent(this.sourceText, var4, var3);
         boolean var7 = io.karatelabs.ext.jsformat.JsFormatOracle.areAdjacentBeforeNextToken(this.sourceText, var3, var5);
         Token var8 = var6 && !var7 ? var4 : var5;
         (var6 && !var7 ? this.trailingComments : this.leadingComments).computeIfAbsent(var8, var0 -> new ArrayList<>(1)).add(new io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement(var3, var6, var7));
         this.tokenPositionsWithComments.set(var8.pos);
      }

      this.validateTrailingComments(var1.lastPrimaryToken);
   }

   private void validateTrailingComments(Token var1) {
      int var2 = var1 == null ? 0 : var1.pos + var1.length;
      ArrayList<java.lang.Integer> var3 = new ArrayList();
      if (var1 != null && this.trailingComments.containsKey(var1)) {
         this.trailingComments.get(var1).forEach(var1x -> var3.add(var1x.a.pos));
      }

      this.leadingComments.getOrDefault(this.rootNode.getLastToken(), List.of()).forEach(var1x -> var3.add(var1x.a.pos));
      int var4 = this.sourceText.length();

      while (var2 < var4) {
         char var5 = this.sourceText.charAt(var2);
         boolean var6 = var5 == '/' && var2 + 1 < var4 && this.sourceText.charAt(var2 + 1) == '/' || var2 == 0 && var5 == '#' && var4 > 1 && this.sourceText.charAt(1) == '!';
         boolean var7 = var5 == '/' && var2 + 1 < var4 && this.sourceText.charAt(var2 + 1) == '*';
         if (!var6 && !var7) {
            var2++;
         } else {
            if (!var3.contains(var2)) {
               throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.COMMENT_UNPLACEABLE, "a comment at the end of the source is missing from the token stream", 0, 0, var2, TokenType.EOF.name());
            }

            int var8;
            if (var6) {
               var8 = var2;

               while (var8 < var4 && this.sourceText.charAt(var8) != '\n' && this.sourceText.charAt(var8) != '\r') {
                  var8++;
               }
            } else {
               int var9 = this.sourceText.indexOf("*/", var2 + 2);
               var8 = var9 < 0 ? var4 : var9 + 2;
            }

            var2 = var8;
         }
      }
   }

   private void emitToken(Node var1) {
      this.emitTokenWithFlags(var1, 0);
   }

   private void emitTokenWithFlags(Node var1, int var2) {
      Token var3 = var1.token;
      boolean var4 = this.tokenPositionsWithComments.get(var3.pos);
      if (var4) {
         this.emitLeadingComments(var3, true);
      }

      if (var3.type == TokenType.NUMBER) {
         String var5 = this.canonicalizeNumberLiteral(var1);
         if (var5 != null) {
            this.layoutDocument.appendSyntheticToken(var5, var2 | 4, var3.pos);
         } else {
            this.layoutDocument.appendSourceToken(var3.pos, var3.pos + var3.length, var2 | 4, var3.pos);
         }
      } else if (var3.type != TokenType.S_STRING && var3.type != TokenType.D_STRING) {
         this.layoutDocument.appendSourceToken(var3.pos, var3.pos + var3.length, var2, var3.pos);
      } else {
         boolean var8 = this.hasSimpleStringBody(var3);
         String var6 = this.canonicalizeStringLiteral(var1, var8);
         if (var6 != null) {
            this.layoutDocument.appendSyntheticToken(var6, var2, var3.pos);
         } else {
            int var7 = var3.pos + var3.length;
            this.layoutDocument.appendSourceToken(var3.pos, var7, !var8 && this.containsLineBreak(var3.pos, var7) ? var2 | 8 : var2, var3.pos);
         }
      }

      if (var4) {
         this.emitTrailingComments(var3);
      }
   }

   private void emitRawToken(Token var1) {
      this.layoutDocument.appendSourceToken(var1.pos, var1.pos + var1.length, 0, var1.pos);
   }

   private boolean containsLineBreak(int var1, int var2) {
      int var3 = this.sourceText.indexOf(10, var1);
      return var3 >= 0 && var3 < var2;
   }

   private void emitTokenWithComments(Node var1) {
      if (this.tokenPositionsWithComments.get(var1.token.pos)) {
         this.emitLeadingComments(var1.token, true);
         this.emitTrailingComments(var1.token);
      }
   }

   private boolean hasSimpleStringBody(Token var1) {
      int var2 = var1.pos + var1.length - 1;

      for (int var3 = var1.pos + 1; var3 < var2; var3++) {
         char var4 = this.sourceText.charAt(var3);
         if (var4 < ' ' || var4 == '\\' || var4 == '\'' || var4 == 127 || var4 == 8232 || var4 == 8233 || Character.isSurrogate(var4)) {
            return false;
         }
      }

      return true;
   }

   private String canonicalizeStringLiteral(Node var1, boolean var2) {
      Token var3 = var1.token;
      if (this.directiveStringTokens.contains(var3)) {
         return null;
      } else if (var2) {
         return var3.type == TokenType.S_STRING ? null : "'" + this.sourceText.substring(var3.pos + 1, var3.pos + var3.length - 1) + "'";
      } else {
         String var4 = var3.getText();
         boolean var5 = isObjectOrClassMemberName(var1);
         String var6 = this.canonicalTokenCache.computeIfAbsent(var5 ? "k" + var4 : "v" + var4, var2x -> io.karatelabs.ext.jsformat.JsFormatter.canonicalizeStringLiteral(var4, var5));
         if (var6 == null) {
            throw io.karatelabs.ext.jsformat.JsFormatException.atToken(
               io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.UNSUPPORTED_NODE, "no canonical spelling of this key keeps the name the engine reads", var3, var1.getParent().type.name()
            );
         } else {
            return var6.equals(var4) ? null : var6;
         }
      }
   }

   private String canonicalizeNumberLiteral(Node var1) {
      Token var2 = var1.token;
      if (!this.isShortDecimalLiteral(var2.pos, var2.pos + var2.length) && !isObjectOrClassMemberName(var1)) {
         String var3 = var2.getText();
         String var4 = this.canonicalTokenCache.computeIfAbsent("n" + var3, var1x -> io.karatelabs.ext.jsformat.JsFormatter.canonicalizeNumberLiteral(var3));
         return var4.equals(var3) ? null : var4;
      } else {
         return null;
      }
   }

   private boolean isShortDecimalLiteral(int var1, int var2) {
      int var3 = var1;
      if (var1 < var2 && this.sourceText.charAt(var1) == '0') {
         var3 = var1 + 1;
      } else if (var1 < var2 && this.sourceText.charAt(var1) >= '1' && this.sourceText.charAt(var1) <= '9') {
         do {
            var3++;
         } while (var3 < var2 && this.sourceText.charAt(var3) >= '0' && this.sourceText.charAt(var3) <= '9');
      }

      int var4 = var3 - var1;
      if (var4 == 0) {
         return false;
      } else if (var3 == var2) {
         return var4 <= 15;
      } else if (this.sourceText.charAt(var3) != '.') {
         return false;
      } else {
         boolean var5 = var4 == 1 && this.sourceText.charAt(var1) == '0';
         int var6 = ++var3;

         int var7;
         for (var7 = 0; var3 < var2 && this.sourceText.charAt(var3) >= '0' && this.sourceText.charAt(var3) <= '9'; var3++) {
            if (var5 && var7 == var3 - var6 && this.sourceText.charAt(var3) == '0') {
               var7++;
            }
         }

         int var8 = var3 - var6;
         return var3 == var2 && var8 > 0 && this.sourceText.charAt(var2 - 1) != '0' && var7 < 6 && (var5 ? var8 - var7 : var4 + var8) <= 15;
      }
   }

   private static boolean isObjectOrClassMemberName(Node var0) {
      Node var1 = var0.getParent();
      return var1 != null && (var1.type == NodeType.OBJECT_ELEM || var1.type == NodeType.CLASS_METHOD);
   }

   private void emitLeadingComments(Token var1, boolean var2) {
      List var3 = this.tokenPositionsWithComments.get(var1.pos) ? this.leadingComments.get(var1) : null;
      if (var3 != null) {
         for (io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement var5 : (Iterable<io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement>) (Iterable<?>) (var3)) {
            if (var2 || var5.c) {
               this.emitCommentPlacement(var5);
            }
         }
      }
   }

   private boolean hasLeadingCommentRequiringBreak(Token var1) {
      List var2 = this.tokenPositionsWithComments.get(var1.pos) ? this.leadingComments.get(var1) : null;
      if (var2 == null) {
         return false;
      } else {
         for (io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement var4 : (Iterable<io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement>) (Iterable<?>) (var2)) {
            if (!var4.c) {
               return true;
            }
         }

         return false;
      }
   }

   private void emitLeadingCommentsRequiringBreak(Token var1) {
      for (io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement var3 : this.leadingComments.get(var1)) {
         if (!var3.c) {
            this.emitCommentPlacement(var3);
         }
      }
   }

   private void emitCommentPlacement(io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement var1) {
      if (!var1.b) {
         this.layoutDocument.appendControlOpcode((byte)5);
      }

      Token var2 = var1.a;
      int var3 = var2.pos + var2.length;
      this.layoutDocument.appendSourceToken(var2.pos, var3, this.containsLineBreak(var2.pos, var3) ? 10 : 2, var2.pos);
      if (!var1.c) {
         this.layoutDocument.appendControlOpcode((byte)6);
      }
   }

   private void emitTrailingComments(Token var1) {
      List var2 = this.tokenPositionsWithComments.get(var1.pos) ? this.trailingComments.get(var1) : null;
      if (var2 != null) {
         for (io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement var4 : (Iterable<io.karatelabs.ext.jsformat.JsAstFormatter.CommentPlacement>) (Iterable<?>) (var2)) {
            this.layoutDocument.appendDeferredInstruction(var4.a.pos, var4.a.pos + var4.a.length);
         }
      }
   }

   private boolean hasBlankLineBefore(Token var1) {
      for (Token var2 = var1.getPrev(); var2 != null && !var2.type.primary; var2 = var2.getPrev()) {
         if (var2.type == TokenType.WS_LF && this.countLineBreaks(var2) >= 2) {
            return true;
         }
      }

      return false;
   }

   private int countLineBreaks(Token var1) {
      if (this.usesSimpleLineBreaks) {
         return (short)(var1.getNext().line - var1.line);
      } else {
         int var2 = var1.pos + var1.length;
         int var3 = 0;

         for (int var4 = var1.pos; var4 < var2; var4++) {
            char var5 = this.sourceText.charAt(var4);
            if (var5 == '\n' || var5 == 8232 || var5 == 8233 || var5 == '\r' && (var4 + 1 >= var2 || this.sourceText.charAt(var4 + 1) != '\n')) {
               var3++;
            }
         }

         return var3;
      }
   }

   private void emitNode(Node var1) {
      if (var1.isToken()) {
         this.emitToken(var1);
      } else {
         this.emitNonTokenWithDepthGuard(var1);
      }
   }

   private void emitNonTokenWithDepthGuard(Node var1) {
      if (++this.recursionDepth > 200) {
         throw io.karatelabs.ext.jsformat.JsFormatException.atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.NESTING_LIMIT, "nesting deeper than 200 levels", var1);
      } else {
         this.emitByNodeType(var1);
         this.recursionDepth--;
      }
   }

   private void emitByNodeType(Node var1) {
      switch (var1.type) {
         case PROGRAM:
            this.emitProgram(var1);
            break;
         case STATEMENT:
            this.emitStatement(var1);
            break;
         case BLOCK:
            this.emitBlock(var1);
            break;
         case EXPR:
         case REF_EXPR:
         case LIT_EXPR:
         case SUPER_EXPR:
         case PRIVATE_NAME_EXPR:
         case LIT_REGEX:
         case EOS:
            this.emitSingleChild(var1);
            break;
         case EXPR_LIST:
            this.emitExpressionList(var1);
            break;
         case IF_STMT:
            this.emitIfStatement(var1);
            break;
         case VAR_STMT:
            this.emitVariableStatement(var1);
            break;
         case VAR_DECL:
            this.emitVariableDeclarator(var1);
            break;
         case RETURN_STMT:
         case THROW_STMT:
         case BREAK_STMT:
         case CONTINUE_STMT:
         case AWAIT_EXPR:
         case YIELD_EXPR:
            this.emitPrefixControlStatement(var1);
            break;
         case DELETE_EXPR:
         case TYPEOF_EXPR:
         case NEW_EXPR:
            this.emitToken(var1.get(0));
            this.layoutDocument.appendSpace();
            this.emitNode(requireArity(var1, 2).get(1));
            break;
         case UNARY_EXPR:
            this.emitUnaryExpression(var1);
            break;
         case MATH_PRE_EXPR:
         case PAREN_EXPR:
            this.emitWrapperChildren(var1);
            break;
         case MATH_POST_EXPR:
            this.emitNode(requireArity(var1, 2).get(0));
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitToken(var1.get(1));
            break;
         case TRY_STMT:
            this.emitTryStatement(var1);
            break;
         case FOR_STMT:
            this.emitForStatement(var1);
            break;
         case WHILE_STMT:
            this.emitToken(var1.get(0));
            this.layoutDocument.appendSpace();
            this.emitChildrenRange(var1, 1, 4);
            this.emitControlledBody(requireArity(var1, 5).get(4));
            break;
         case DO_WHILE_STMT:
            this.emitDoWhileStatement(var1);
            break;
         case SWITCH_STMT:
            this.emitSwitchStatement(var1);
            break;
         case CASE_BLOCK:
         case DEFAULT_BLOCK:
            this.emitCaseOrDefaultBlock(var1);
            break;
         case LABELLED_STMT:
            this.emitToken(var1.get(0));
            this.emitToken(var1.get(1));
            this.layoutDocument.appendSpace();
            this.emitStatement(requireArity(var1, 3).get(2));
            break;
         case ASSIGN_EXPR:
            this.emitNode(requireArity(var1, 3).get(0));
            this.layoutDocument.appendSpace();
            this.emitToken(var1.get(1));
            this.layoutDocument.appendSpace();
            this.emitNode(var1.get(2));
            break;
         case LOGIC_EXPR:
         case LOGIC_AND_EXPR:
         case LOGIC_NULLISH_EXPR:
         case LOGIC_BIT_EXPR:
         case MATH_ADD_EXPR:
         case MATH_MUL_EXPR:
         case MATH_EXP_EXPR:
         case INSTANCEOF_EXPR:
         case IN_EXPR:
            this.emitBinaryExpression(var1);
            break;
         case LOGIC_TERN_EXPR:
            this.emitConditionalExpression(var1);
            break;
         case FN_CALL_EXPR:
         case REF_DOT_EXPR:
         case REF_BRACKET_EXPR:
         case FN_TAGGED_TEMPLATE_EXPR:
            this.emitMemberAccessChain(var1);
            break;
         case FN_DECL_ARGS:
            this.emitParameterList(var1);
            break;
         case LIT_OBJECT:
         case LIT_ARRAY:
            this.emitCollectionLiteral(var1);
            break;
         case LIT_TEMPLATE:
            this.emitTemplateLiteral(var1);
            break;
         case FN_EXPR:
            this.emitFunctionExpression(var1);
            break;
         case FN_ARROW_EXPR:
            this.emitArrowFunction(var1);
            break;
         case CLASS_EXPR:
            this.emitClassExpression(var1);
            break;
         case CLASS_METHOD:
            this.emitClassMembers(var1, var1.size());
            break;
         case CLASS_STATIC_BLOCK:
            this.emitToken(var1.get(0));
            this.layoutDocument.appendSpace();
            this.emitBlock(requireArity(var1, 2).get(1));
            break;
         case ERROR:
            throw io.karatelabs.ext.jsformat.JsFormatException.atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.PARSE_FAILED, "the tree holds a parse error", var1);
         default:
            throw unsupportedNodeException(var1);
      }
   }

   private static JsFormatException unsupportedNodeException(Node var0) {
      return io.karatelabs.ext.jsformat.JsFormatException.atNode(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.UNSUPPORTED_NODE, "no canonical form for " + var0.type + " in v1", var0);
   }

   private static Node requireArity(Node var0, int var1) {
      if (var0.size() != var1) {
         throw unsupportedNodeException(var0);
      } else {
         return var0;
      }
   }

   private void emitSingleChild(Node var1) {
      this.emitNode(requireArity(var1, 1).get(0));
   }

   private void emitWrapperChildren(Node var1) {
      this.emitChildrenRange(var1, 0, var1.size());
   }

   private void emitChildrenRange(Node var1, int var2, int var3) {
      if (var3 > var1.size()) {
         throw unsupportedNodeException(var1);
      } else {
         for (int var4 = var2; var4 < var3; var4++) {
            this.emitNode(var1.get(var4));
         }
      }
   }

   private void emitProgram(Node var1) {
      int var2 = var1.size() - 1;
      boolean var3 = this.emitStatementsAndTrackDirectives(var1, 0, var2, true);
      Node var4 = var1.get(var2);
      if (this.hasLeadingCommentRequiringBreak(var4.token)) {
         if (var3) {
            this.layoutDocument.appendControlOpcode((byte)3);
            if (this.hasBlankLineBefore(var4.token)) {
               this.layoutDocument.appendControlOpcode((byte)4);
            }
         }

         this.emitLeadingCommentsRequiringBreak(var4.token);
      }

      this.emitLeadingComments(var4.token, false);
   }

   private boolean emitStatementsAndTrackDirectives(Node var1, int var2, int var3, boolean var4) {
      boolean var5 = true;

      for (int var6 = var2; var6 < var3; var6++) {
         Node var7 = var1.get(var6);
         if (var7.type != NodeType.STATEMENT || var7.size() != 1 || !var7.get(0).isToken() || !this.explicitSemicolonTokens.contains(var7.get(0).token)) {
            if (var4) {
               Token var8 = directiveStringToken(var7);
               if (var8 != null) {
                  this.directiveStringTokens.add(var8);
               } else {
                  var4 = false;
               }
            }

            if (!var5) {
               this.layoutDocument.appendControlOpcode((byte)3);
               if (this.hasBlankLineBefore(var7.getFirstToken())) {
                  this.layoutDocument.appendControlOpcode((byte)4);
               }
            }

            this.emitStatement(var7);
            var5 = false;
         }
      }

      return !var5;
   }

   private static Token directiveStringToken(Node var0) {
      if (var0.type == NodeType.STATEMENT && var0.get(0).type == NodeType.EXPR_LIST && var0.get(0).size() == 1) {
         Node var1 = var0.get(0).get(0);
         if (var1.type == NodeType.EXPR && var1.size() == 1 && var1.get(0).type == NodeType.LIT_EXPR && var1.get(0).size() == 1) {
            Node var2 = var1.get(0).get(0);
            return !var2.isToken() || var2.token.type != TokenType.S_STRING && var2.token.type != TokenType.D_STRING ? null : var2.token;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void emitStatement(Node var1) {
      if (var1.type == NodeType.STATEMENT && !var1.isEmpty()) {
         Node var2 = var1.get(0);
         if (var2.isToken()) {
            this.emitToken(var2);
         } else {
            this.emitNonTokenWithDepthGuard(var2);
            switch (var2.type) {
               case EXPR_LIST:
               case VAR_STMT:
               case RETURN_STMT:
               case THROW_STMT:
               case BREAK_STMT:
               case CONTINUE_STMT:
                  if (var1.size() > 1) {
                     Node var4 = var1.get(1);
                     if (var4.type != NodeType.EOS || var1.size() > 2) {
                        throw unsupportedNodeException(var1);
                     }

                     this.emitTokenWithFlags(var4.get(0), 1);
                  } else {
                     this.layoutDocument.appendSemicolon();
                  }
               case IF_STMT:
               case VAR_DECL:
               case AWAIT_EXPR:
               case YIELD_EXPR:
               case DELETE_EXPR:
               case TYPEOF_EXPR:
               case NEW_EXPR:
               case UNARY_EXPR:
               case MATH_PRE_EXPR:
               case PAREN_EXPR:
               case MATH_POST_EXPR:
               case TRY_STMT:
               case FOR_STMT:
               case WHILE_STMT:
               default:
                  break;
               case DO_WHILE_STMT:
                  Token var3 = var2.getLastToken().getNextPrimary();
                  if (var3.type == TokenType.SEMI) {
                     this.explicitSemicolonTokens.add(var3);
                     this.emitLeadingComments(var3, true);
                     this.layoutDocument.appendSourceToken(var3.pos, var3.pos + 1, 1, var3.pos);
                     this.emitTrailingComments(var3);
                  } else if (var3.type != TokenType.ELSE && var1.getParent().type != NodeType.DO_WHILE_STMT) {
                     this.layoutDocument.appendSemicolon();
                  }
            }
         }
      } else {
         throw unsupportedNodeException(var1);
      }
   }

   private static boolean isBlockStatement(Node var0) {
      return var0.type == NodeType.STATEMENT && !var0.isEmpty() && !var0.get(0).isToken() && var0.get(0).type == NodeType.BLOCK;
   }

   private void emitControlledBody(Node var1) {
      if (isBlockStatement(var1)) {
         this.layoutDocument.appendSpace();
         this.emitStatement(var1);
      } else {
         int var2 = this.layoutDocument.saveLayoutState();
         this.layoutDocument.pushIndent(true);
         this.layoutDocument.appendControlOpcode((byte)1);
         this.emitStatement(var1);
         this.layoutDocument.popIndent();
         this.layoutDocument.restoreLayoutState(var2);
      }
   }

   private void emitBlock(Node var1) {
      int var2 = var1.size() - 1;
      if (var2 >= 1 && var1.get(0).isToken() && var1.get(var2).isToken()) {
         Node var3 = var1.getParent();
         boolean var4 = var3 != null && (var3.type == NodeType.FN_EXPR || var3.type == NodeType.FN_ARROW_EXPR);
         this.emitToken(var1.get(0));
         if (var2 == 1) {
            this.emitClosingBrace(var1.get(var2), false);
         } else {
            this.layoutDocument.pushIndent(false);
            this.layoutDocument.appendControlOpcode((byte)3);
            this.emitStatementsAndTrackDirectives(var1, 1, var2, var4);
            this.layoutDocument.popIndent();
            this.emitClosingBrace(var1.get(var2), true);
         }
      } else {
         throw unsupportedNodeException(var1);
      }
   }

   private void emitClosingBrace(Node var1, boolean var2) {
      Token var3 = var1.token;
      boolean var4 = this.hasLeadingCommentRequiringBreak(var3);
      if (var4) {
         this.layoutDocument.pushIndent(false);
         this.layoutDocument.appendControlOpcode((byte)3);
         if (this.hasBlankLineBefore(var3) && var2) {
            this.layoutDocument.appendControlOpcode((byte)4);
         }

         this.emitLeadingCommentsRequiringBreak(var3);
         this.layoutDocument.popIndent();
      }

      if (var2 || var4) {
         this.layoutDocument.appendControlOpcode((byte)3);
      }

      this.emitLeadingComments(var3, false);
      this.emitRawToken(var3);
      this.emitTrailingComments(var3);
   }

   private void emitExpressionList(Node var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         this.emitNode(var3);
         if (var3.isToken() && var3.token.type == TokenType.COMMA) {
            this.layoutDocument.appendSpace();
         }
      }
   }

   private void emitIfStatement(Node var1) {
      if (var1.size() != 5 && var1.size() != 7) {
         throw unsupportedNodeException(var1);
      } else {
         this.emitToken(var1.get(0));
         this.layoutDocument.appendSpace();
         this.emitChildrenRange(var1, 1, 4);
         Node var2 = var1.get(4);
         this.emitControlledBody(var2);
         if (var1.size() == 7) {
            if (isBlockStatement(var2)) {
               this.layoutDocument.appendSpace();
            } else {
               this.layoutDocument.appendControlOpcode((byte)3);
            }

            this.emitToken(var1.get(5));
            Node var3 = var1.get(6);
            boolean var4 = !var3.isEmpty() && var3.get(0).type == NodeType.IF_STMT;
            if (var4) {
               this.layoutDocument.appendSpace();
               this.emitStatement(var3);
            } else {
               this.emitControlledBody(var3);
            }
         }
      }
   }

   private void emitVariableStatement(Node var1) {
      if (var1.size() == 2) {
         this.emitToken(var1.get(0));
         this.layoutDocument.appendSpace();
         this.emitVariableDeclarator(var1.get(1));
      } else {
         int var2 = this.layoutDocument.saveLayoutState();
         this.emitToken(var1.get(0));
         this.layoutDocument.appendSpace();
         this.emitVariableDeclarator(var1.get(1));
         this.layoutDocument.pushIndent(true);

         for (int var3 = 2; var3 < var1.size(); var3++) {
            Node var4 = var1.get(var3);
            if (var4.isToken()) {
               this.emitToken(var4);
               this.layoutDocument.appendControlOpcode((byte)1);
            } else {
               this.emitVariableDeclarator(var4);
            }
         }

         this.layoutDocument.popIndent();
         this.layoutDocument.restoreLayoutState(var2);
      }
   }

   private void emitVariableDeclarator(Node var1) {
      if (var1.type != NodeType.VAR_DECL) {
         throw unsupportedNodeException(var1);
      } else if (var1.size() == 1) {
         this.emitNode(var1.get(0));
      } else {
         this.emitNode(requireArity(var1, 3).get(0));
         this.layoutDocument.appendSpace();
         this.emitToken(var1.get(1));
         this.layoutDocument.appendSpace();
         this.emitNode(var1.get(2));
      }
   }

   private void emitPrefixControlStatement(Node var1) {
      this.emitToken(var1.get(0));

      for (int var2 = 1; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         if (var3.isToken() && var3.token.type == TokenType.STAR) {
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitToken(var3);
         } else {
            this.layoutDocument.appendSpace();
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitNode(var3);
         }
      }
   }

   private void emitUnaryExpression(Node var1) {
      Node var2 = requireArity(var1, 2).get(0);
      if (var2.token.type == TokenType.VOID) {
         this.emitToken(var2);
         this.layoutDocument.appendSpace();
         this.emitNode(var1.get(1));
      } else {
         this.emitWrapperChildren(var1);
      }
   }

   private void emitTryStatement(Node var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         if (var3.isToken()) {
            TokenType var4 = var3.token.type;
            if (var4 == TokenType.CATCH || var4 == TokenType.FINALLY || var4 == TokenType.L_PAREN) {
               this.layoutDocument.appendSpace();
            }

            this.emitToken(var3);
         } else if (var3.type == NodeType.BLOCK) {
            this.layoutDocument.appendSpace();
            this.emitBlock(var3);
         } else {
            this.emitNonTokenWithDepthGuard(var3);
         }
      }
   }

   private void emitForStatement(Node var1) {
      int var2 = var1.size() - 1;

      for (int var3 = 0; var3 < var2; var3++) {
         Node var4 = var1.get(var3);
         if (!var4.isToken()) {
            this.emitNonTokenWithDepthGuard(var4);
         } else {
            TokenType var5 = var4.token.type;
            if (var5 == TokenType.IN || var5 == TokenType.OF) {
               this.layoutDocument.appendSpace();
               this.emitToken(var4);
               this.layoutDocument.appendSpace();
            } else if (var5 == TokenType.SEMI) {
               this.emitToken(var4);
               Node var6 = var1.get(var3 + 1);
               boolean var7 = var6.isToken() && (var6.token.type == TokenType.SEMI || var6.token.type == TokenType.R_PAREN);
               if (!var7) {
                  this.layoutDocument.appendSpace();
               }
            } else {
               this.emitToken(var4);
               if (var5 == TokenType.FOR) {
                  this.layoutDocument.appendSpace();
               }
            }
         }
      }

      this.emitControlledBody(var1.get(var2));
   }

   private void emitDoWhileStatement(Node var1) {
      Node var2 = requireArity(var1, 6).get(1);
      this.emitToken(var1.get(0));
      if (isBlockStatement(var2)) {
         this.layoutDocument.appendSpace();
         this.emitStatement(var2);
         this.layoutDocument.appendSpace();
      } else {
         int var3 = this.layoutDocument.saveLayoutState();
         this.layoutDocument.pushIndent(true);
         this.layoutDocument.appendControlOpcode((byte)1);
         this.emitStatement(var2);
         this.layoutDocument.popIndent();
         this.layoutDocument.restoreLayoutState(var3);
         this.layoutDocument.appendControlOpcode((byte)3);
      }

      this.emitToken(var1.get(2));
      this.layoutDocument.appendSpace();
      this.emitChildrenRange(var1, 3, 6);
   }

   private void emitSwitchStatement(Node var1) {
      int var2 = var1.size() - 1;
      if (var2 < 5) {
         throw unsupportedNodeException(var1);
      } else {
         this.emitToken(var1.get(0));
         this.layoutDocument.appendSpace();
         this.emitChildrenRange(var1, 1, 4);
         this.layoutDocument.appendSpace();
         this.emitToken(var1.get(4));
         if (var2 == 5) {
            this.emitClosingBrace(var1.get(var2), false);
         } else {
            this.layoutDocument.pushIndent(false);
            this.emitNodesWithLineBreaks(var1, 5, var2);
            this.layoutDocument.popIndent();
            this.emitClosingBrace(var1.get(var2), true);
         }
      }
   }

   private void emitCaseOrDefaultBlock(Node var1) {
      int var2 = var1.type == NodeType.CASE_BLOCK ? 2 : 1;
      if (var1.size() > var2 && var1.get(var2).isToken()) {
         if (var1.type == NodeType.CASE_BLOCK) {
            this.emitToken(var1.get(0));
            this.layoutDocument.appendSpace();
            this.emitNode(var1.get(1));
            this.emitToken(var1.get(2));
         } else {
            this.emitChildrenRange(var1, 0, 2);
         }

         int var3 = var1.size() - var2 - 1;
         if (var3 != 0) {
            if (var3 == 1 && isBlockStatement(var1.get(var2 + 1))) {
               this.layoutDocument.appendSpace();
               this.emitStatement(var1.get(var2 + 1));
            } else {
               this.layoutDocument.pushIndent(false);
               this.layoutDocument.appendControlOpcode((byte)3);
               this.emitStatementsAndTrackDirectives(var1, var2 + 1, var1.size(), false);
               this.layoutDocument.popIndent();
            }
         }
      } else {
         throw unsupportedNodeException(var1);
      }
   }

   private void emitNodesWithLineBreaks(Node var1, int var2, int var3) {
      for (int var4 = var2; var4 < var3; var4++) {
         Node var5 = var1.get(var4);
         this.layoutDocument.appendControlOpcode((byte)3);
         if (var4 > var2 && this.hasBlankLineBefore(var5.getFirstToken())) {
            this.layoutDocument.appendControlOpcode((byte)4);
         }

         this.emitNode(var5);
      }
   }

   private void emitBinaryExpression(Node var1) {
      int var2 = 0;

      Node var3;
      for (var3 = var1; !var3.isToken() && var3.type == var1.type; var3 = var3.get(0)) {
         requireArity(var3, 3);
         var2++;
      }

      Node[] var4 = new Node[var2];
      Node var5 = var1;

      for (int var6 = var2 - 1; var6 >= 0; var6--) {
         var4[var6] = var5;
         var5 = var5.get(0);
      }

      int var12 = this.layoutDocument.saveLayoutState();
      this.emitNode(var3);
      this.layoutDocument.pushIndent(true);

      for (Node var10 : var4) {
         Node var11 = var10.get(1);
         this.layoutDocument.appendSpace();
         if (var11.token.type == TokenType.SLASH) {
            this.layoutDocument.appendControlOpcode((byte)7);
         }

         this.emitToken(var11);
         this.layoutDocument.appendControlOpcode((byte)1);
         this.emitNode(var10.get(2));
      }

      this.layoutDocument.popIndent();
      this.layoutDocument.restoreLayoutState(var12);
   }

   private void emitConditionalExpression(Node var1) {
      requireArity(var1, 5);
      int var2 = this.layoutDocument.saveLayoutState();
      this.emitNode(var1.get(0));
      this.layoutDocument.pushIndent(true);
      this.layoutDocument.appendControlOpcode((byte)1);
      this.emitToken(var1.get(1));
      this.layoutDocument.appendSpace();
      this.emitNode(var1.get(2));
      this.layoutDocument.appendControlOpcode((byte)1);
      this.emitToken(var1.get(3));
      this.layoutDocument.appendSpace();
      this.emitNode(var1.get(4));
      this.layoutDocument.popIndent();
      this.layoutDocument.restoreLayoutState(var2);
   }

   private static boolean isMemberAccessNode(Node var0) {
      return !var0.isToken()
         && (
            var0.type == NodeType.REF_DOT_EXPR
               || var0.type == NodeType.FN_CALL_EXPR
               || var0.type == NodeType.REF_BRACKET_EXPR
               || var0.type == NodeType.FN_TAGGED_TEMPLATE_EXPR
         );
   }

   private void emitMemberAccessChain(Node var1) {
      int var2 = 0;

      Node var3;
      for (var3 = var1; isMemberAccessNode(var3) && !var3.isEmpty() && !var3.get(0).isToken(); var3 = var3.get(0)) {
         var2++;
      }

      if (isMemberAccessNode(var3)) {
         throw unsupportedNodeException(var3);
      } else {
         Node[] var4 = new Node[var2];
         Node var5 = var1;

         for (int var6 = var2 - 1; var6 >= 0; var6--) {
            var4[var6] = var5;
            var5 = var5.get(0);
         }

         boolean[] var14 = new boolean[var2];
         int var7 = 0;

         for (int var8 = 0; var8 < var2; var8++) {
            Node var9 = var4[var8];
            switch (var9.type) {
               case FN_CALL_EXPR:
                  var7++;
                  requireArity(var9, 4);
                  break;
               case REF_DOT_EXPR:
                  var14[var8] = true;
                  Node var10 = var9.get(1);
                  if (var10.isToken()) {
                     requireArity(var9, 3);
                  } else if (var10.type == NodeType.FN_CALL_EXPR) {
                     var7++;
                     requireArity(var10, 4);
                  } else {
                     if (var10.type != NodeType.REF_BRACKET_EXPR) {
                        throw unsupportedNodeException(var9);
                     }

                     requireArity(var10, 4);
                  }
                  break;
               case REF_BRACKET_EXPR:
                  requireArity(var9, 4);
                  break;
               default:
                  requireArity(var9, 2);
            }
         }

         int var15 = 0;

         while (var15 < var2 && !var14[var15]) {
            var15++;
         }

         if (isSimpleIdentifierReference(var3) && var15 < var2) {
            var15++;

            while (var15 < var2 && !var14[var15]) {
               var15++;
            }
         }

         int var16 = 0;

         for (int var17 = var15; var17 < var2; var17++) {
            if (var14[var17]) {
               var16++;
            }
         }

         if (var16 >= 2 && var7 >= 2) {
            int var19 = this.layoutDocument.saveLayoutState();
            this.emitNonTokenWithDepthGuard(var3);

            for (int var20 = 0; var20 < var15; var20++) {
               this.emitMemberAccessSegment(var4[var20]);
            }

            this.layoutDocument.pushIndent(true);

            for (int var21 = var15; var21 < var2; var21++) {
               if (var14[var21]) {
                  this.layoutDocument.appendControlOpcode((byte)2);
               }

               this.emitMemberAccessSegment(var4[var21]);
            }

            this.layoutDocument.popIndent();
            this.layoutDocument.restoreLayoutState(var19);
         } else {
            this.emitNonTokenWithDepthGuard(var3);

            for (Node var13 : var4) {
               this.emitMemberAccessSegment(var13);
            }
         }
      }
   }

   private void emitMemberAccessSegment(Node var1) {
      switch (var1.type) {
         case FN_CALL_EXPR:
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitCallExpressionAt(var1, 1);
            break;
         case REF_DOT_EXPR:
            Node var2 = var1.get(1);
            if (var2.isToken()) {
               this.emitToken(var2);
               this.emitToken(var1.get(2));
            } else if (var2.type == NodeType.FN_CALL_EXPR) {
               this.emitToken(var2.get(0));
               this.emitCallExpressionAt(var2, 1);
            } else {
               this.emitToken(var2.get(0));
               this.layoutDocument.appendControlOpcode((byte)7);
               this.emitChildrenRange(var2, 1, 4);
            }
            break;
         case REF_BRACKET_EXPR:
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitChildrenRange(var1, 1, 4);
            break;
         default:
            this.emitTemplateLiteral(var1.get(1));
      }
   }

   private static boolean isSimpleIdentifierReference(Node var0) {
      return var0.type == NodeType.REF_EXPR && var0.size() == 1 && var0.get(0).isToken() && var0.get(0).token.type == TokenType.IDENT;
   }

   private void emitCallExpressionAt(Node var1, int var2) {
      Node var3 = var1.get(var2 + 1);
      if (var3.type != NodeType.FN_CALL_ARGS) {
         throw unsupportedNodeException(var1);
      } else {
         this.emitDelimitedList(var1.get(var2), var3, 0, var3.size(), var1.get(var2 + 2));
      }
   }

   private void emitParameterList(Node var1) {
      if (var1.isEmpty()) {
         throw unsupportedNodeException(var1);
      } else if (!var1.get(0).isToken()) {
         this.emitDelimitedItem(requireArity(var1, 1).get(0), true);
      } else {
         int var2 = var1.size() - 1;
         this.emitDelimitedList(var1.get(0), var1, 1, var2, var1.get(var2));
      }
   }

   private void emitDelimitedList(Node var1, Node var2, int var3, int var4, Node var5) {
      Token var6 = var5.token;
      boolean var7 = this.hasLeadingCommentRequiringBreak(var6);
      if (var3 == var4 && !var7) {
         this.emitToken(var1);
         this.emitClosingBrace(var5, false);
      } else {
         int var8 = this.layoutDocument.saveLayoutState();
         this.emitToken(var1);
         this.layoutDocument.pushIndent(true);
         this.layoutDocument.appendControlOpcode((byte)2);

         for (int var9 = var3; var9 < var4; var9++) {
            if (var9 > var3) {
               this.layoutDocument.appendControlOpcode((byte)1);
            }

            this.emitDelimitedItem(var2.get(var9), var9 == var4 - 1);
         }

         if (var7) {
            this.emitLeadingCommentsRequiringBreak(var6);
         }

         this.layoutDocument.popIndent();
         this.layoutDocument.appendControlOpcode((byte)2);
         this.emitLeadingComments(var6, false);
         this.emitRawToken(var6);
         this.emitTrailingComments(var6);
         this.layoutDocument.restoreLayoutState(var8);
      }
   }

   private void emitDelimitedItem(Node var1, boolean var2) {
      int var3 = var1.size();
      Node var4 = null;
      if (var3 > 0 && var1.get(var3 - 1).isToken() && var1.get(var3 - 1).token.type == TokenType.COMMA) {
         var4 = var1.get(--var3);
      }

      boolean var5 = var3 > 0 && var1.get(0).isToken() && var1.get(0).token.type == TokenType.DOT_DOT_DOT;

      for (int var6 = 0; var6 < var3; var6++) {
         Node var7 = var1.get(var6);
         if (var7.isToken() && var7.token.type == TokenType.EQ) {
            this.layoutDocument.appendSpace();
            this.emitToken(var7);
            this.layoutDocument.appendSpace();
         } else {
            this.emitNode(var7);
         }
      }

      if (!var2) {
         if (var4 == null) {
            throw unsupportedNodeException(var1);
         }

         this.emitToken(var4);
      } else if (var5 && var1.type != NodeType.FN_CALL_ARG
         || var1.getParent() == null
         || var1.getParent().type == NodeType.FN_DECL_ARGS && !var1.getParent().get(0).isToken()) {
         if (var4 != null) {
            this.emitTokenWithComments(var4);
         }
      } else if (var4 != null) {
         int var8 = this.layoutDocument.beginConditionalSkip();
         this.emitTokenWithFlags(var4, 1);
         this.layoutDocument.setConditionalSkipTarget(var8);
         this.emitTokenWithComments(var4);
         this.layoutDocument.closeConditionalSkipWithNoop(var8);
      } else {
         int var9 = this.layoutDocument.beginConditionalSkip();
         this.layoutDocument.appendComma();
         this.layoutDocument.setConditionalSkipTarget(var9);
         this.layoutDocument.closeConditionalSkipWithNoop(var9);
      }
   }

   private void emitCollectionLiteral(Node var1) {
      int var2 = var1.size() - 1;
      if (var2 < 1) {
         throw unsupportedNodeException(var1);
      } else {
         this.emitToken(var1.get(0));
         if (var2 == 1) {
            this.emitClosingBrace(var1.get(var2), false);
         } else {
            this.layoutDocument.pushIndent(false);

            for (int var3 = 1; var3 < var2; var3++) {
               Node var4 = var1.get(var3);
               this.layoutDocument.appendControlOpcode((byte)3);
               if (var3 > 1 && this.hasBlankLineBefore(var4.getFirstToken())) {
                  this.layoutDocument.appendControlOpcode((byte)4);
               }

               this.emitCollectionElement(var4, var3 == var2 - 1);
            }

            this.layoutDocument.popIndent();
            this.emitClosingBrace(var1.get(var2), true);
         }
      }
   }

   private void emitCollectionElement(Node var1, boolean var2) {
      if (var1.type != NodeType.OBJECT_ELEM && var1.type != NodeType.ARRAY_ELEM) {
         throw unsupportedNodeException(var1);
      } else {
         int var3 = var1.size();
         Node var4 = null;
         if (var3 > 0 && var1.get(var3 - 1).isToken() && var1.get(var3 - 1).token.type == TokenType.COMMA) {
            var4 = var1.get(--var3);
         }

         if (var3 == 0) {
            if (var1.type == NodeType.ARRAY_ELEM && var4 != null) {
               this.emitToken(var4);
            } else {
               throw unsupportedNodeException(var1);
            }
         } else if (var4 == null && !var2 || !this.tryEmitInlineMember(var1, var3, var4)) {
            boolean var5 = var1.get(0).isToken() && var1.get(0).token.type == TokenType.DOT_DOT_DOT;
            if (var1.type == NodeType.OBJECT_ELEM) {
               this.emitClassMembers(var1, var3);
            } else {
               this.emitChildrenRange(var1, 0, var3);
            }

            if (!var2) {
               if (var4 == null) {
                  throw unsupportedNodeException(var1);
               }

               this.emitToken(var4);
            } else if (var5 && allowsTrailingComma(var1.getParent())) {
               if (var4 != null) {
                  this.emitTokenWithComments(var4);
               }
            } else if (var4 != null) {
               this.emitTokenWithFlags(var4, 1);
            } else {
               this.layoutDocument.appendComma();
            }
         }
      }
   }

   private static boolean allowsTrailingComma(Node var0) {
      Node var1 = var0;

      for (Node var2 = var0.getParent(); var2 != null; var2 = var2.getParent()) {
         int var3 = 0;

         while (var2.get(var3) != var1) {
            var3++;
         }

         switch (var2.type) {
            case EXPR:
            case LIT_EXPR:
            case LIT_OBJECT:
            case LIT_ARRAY:
            case ARRAY_ELEM:
               break;
            case EXPR_LIST:
               if (var2.size() != 1) {
                  return false;
               }
               break;
            case VAR_DECL:
            case ASSIGN_EXPR:
               return var3 == 0;
            case TRY_STMT:
               return true;
            case FOR_STMT:
               return var3 + 1 < var2.size() && (isTokenOfType(var2.get(var3 + 1), TokenType.OF) || isTokenOfType(var2.get(var3 + 1), TokenType.IN));
            case OBJECT_ELEM:
               if (var3 == 0 || !isTokenOfType(var2.get(var3 - 1), TokenType.COLON)) {
                  return false;
               }
               break;
            case FN_DECL_ARG:
               return var3 == 0 || isTokenOfType(var2.get(0), TokenType.DOT_DOT_DOT);
            default:
               return false;
         }

         var1 = var2;
      }

      return false;
   }

   private boolean tryEmitInlineMember(Node var1, int var2, Node var3) {
      Node var4 = null;
      Node var5;
      if (var1.type == NodeType.OBJECT_ELEM) {
         if (var2 != 3 || !var1.get(0).isToken() || !isTokenOfType(var1.get(1), TokenType.COLON)) {
            return false;
         }

         var4 = var1.get(0);
         var5 = var1.get(2);
      } else {
         if (var2 != 1) {
            return false;
         }

         var5 = var1.get(0);
      }

      while (!var5.isToken() && var5.size() == 1 && (var5.type == NodeType.EXPR || var5.type == NodeType.LIT_EXPR || var5.type == NodeType.REF_EXPR)) {
         var5 = var5.get(0);
      }

      if (!var5.isToken() || var3 != null && this.tokenPositionsWithComments.get(var3.token.pos) || this.tokenPositionsWithComments.get(var5.token.pos)) {
         return false;
      } else if (var4 == null || !this.tokenPositionsWithComments.get(var4.token.pos) && !this.tokenPositionsWithComments.get(var1.get(1).token.pos)) {
         int var6 = this.layoutDocument.instructionCount;
         int var7 = this.layoutDocument.syntheticCharCount;
         int var8 = var1.getFirstToken().pos;
         if (var4 != null) {
            TokenType var9 = var4.token.type;
            if (var9 != TokenType.IDENT && !var9.keyword) {
               if (!this.tryEmitInlineScalar(var4, 128, var8)) {
                  return false;
               }
            } else {
               this.layoutDocument.appendSourceToken(var4.token.pos, var4.token.pos + var4.token.length, 128, var8);
            }
         }

         if (!this.tryEmitInlineScalar(var5, 256, var8)) {
            this.layoutDocument.instructionCount = var6;
            this.layoutDocument.syntheticCharCount = var7;
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean tryEmitInlineScalar(Node var1, int var2, int var3) {
      Token var4 = var1.token;
      switch (var4.type) {
         case NUMBER:
            String var7 = this.canonicalizeNumberLiteral(var1);
            if (var7 != null) {
               this.layoutDocument.appendSyntheticToken(var7, var2, var3);
            } else {
               this.layoutDocument.appendSourceToken(var4.pos, var4.pos + var4.length, var2, var3);
            }

            return true;
         case S_STRING:
         case D_STRING:
            boolean var5 = this.hasSimpleStringBody(var4);
            String var6 = this.canonicalizeStringLiteral(var1, var5);
            if (var6 != null) {
               this.layoutDocument.appendSyntheticToken(var6, var2, var3);
               return true;
            } else {
               if (!var5 && this.containsLineBreak(var4.pos, var4.pos + var4.length)) {
                  return false;
               }

               this.layoutDocument.appendSourceToken(var4.pos, var4.pos + var4.length, var2, var3);
               return true;
            }
         case IDENT:
         case TRUE:
         case FALSE:
         case NULL:
            this.layoutDocument.appendSourceToken(var4.pos, var4.pos + var4.length, var2, var3);
            return true;
         default:
            return false;
      }
   }

   private static boolean isKeywordOrLiteralToken(Node var0) {
      if (!var0.isToken()) {
         return false;
      } else {
         TokenType var1 = var0.token.type;
         return var1.keyword
            || var1 == TokenType.IDENT
            || var1 == TokenType.PRIVATE_NAME
            || var1 == TokenType.S_STRING
            || var1 == TokenType.D_STRING
            || var1 == TokenType.NUMBER
            || var1 == TokenType.BIGINT;
      }
   }

   private static boolean isTokenOfType(Node var0, TokenType var1) {
      return var0.isToken() && var0.token.type == var1;
   }

   private void emitClassMembers(Node var1, int var2) {
      Node var3 = null;

      for (int var4 = 0; var4 < var2; var4++) {
         Node var5 = var1.get(var4);
         boolean var6 = !var5.isToken() && var5.type == NodeType.FN_EXPR;
         if (var3 != null && !var6) {
            if (isTokenOfType(var5, TokenType.EQ) || isTokenOfType(var3, TokenType.EQ) || isTokenOfType(var3, TokenType.COLON)) {
               this.layoutDocument.appendSpace();
            } else if (isKeywordOrLiteralToken(var3) && (isKeywordOrLiteralToken(var5) || isTokenOfType(var5, TokenType.L_BRACKET) || isTokenOfType(var5, TokenType.STAR))) {
               this.layoutDocument.appendSpace();
               this.layoutDocument.appendControlOpcode((byte)7);
            }
         }

         if (var6) {
            this.emitFunctionDeclaration(var5);
         } else {
            this.emitNode(var5);
         }

         var3 = var5;
      }
   }

   private void emitFunctionDeclaration(Node var1) {
      if (requireArity(var1, 2).get(0).type == NodeType.FN_DECL_ARGS && var1.get(1).type == NodeType.BLOCK) {
         this.emitParameterList(var1.get(0));
         this.layoutDocument.appendSpace();
         this.emitBlock(var1.get(1));
      } else {
         throw unsupportedNodeException(var1);
      }
   }

   private void emitFunctionExpression(Node var1) {
      if (!var1.isEmpty() && var1.get(0).type == NodeType.FN_DECL_ARGS) {
         this.emitFunctionDeclaration(var1);
      } else {
         boolean var2 = false;
         boolean var3 = false;

         for (int var4 = 0; var4 < var1.size(); var4++) {
            Node var5 = var1.get(var4);
            if (var5.isToken()) {
               TokenType var6 = var5.token.type;
               if (var6 == TokenType.IDENT && !var2) {
                  this.emitToken(var5);
                  this.layoutDocument.appendSpace();
                  this.layoutDocument.appendControlOpcode((byte)7);
               } else if (var6 == TokenType.IDENT) {
                  this.layoutDocument.appendSpace();
                  this.emitToken(var5);
                  var3 = true;
               } else {
                  this.emitToken(var5);
                  var2 |= var6 == TokenType.FUNCTION;
               }
            } else if (var5.type == NodeType.FN_DECL_ARGS) {
               if (!var3) {
                  this.layoutDocument.appendSpace();
               }

               this.emitParameterList(var5);
            } else {
               if (var5.type != NodeType.BLOCK) {
                  throw unsupportedNodeException(var1);
               }

               this.layoutDocument.appendSpace();
               this.emitBlock(var5);
            }
         }
      }
   }

   private void emitArrowFunction(Node var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         if (var3.isToken() && var3.token.type == TokenType.EQ_GT) {
            this.layoutDocument.appendSpace();
            this.layoutDocument.appendControlOpcode((byte)7);
            this.emitToken(var3);
         } else if (var2 == 0 && var1.async && var1.size() == 4) {
            this.emitToken(var3);
            this.layoutDocument.appendSpace();
            this.layoutDocument.appendControlOpcode((byte)7);
         } else if (var2 == var1.size() - 1) {
            this.layoutDocument.appendSpace();
            this.emitNode(var3);
         } else {
            this.emitNode(var3);
         }
      }
   }

   private void emitClassExpression(Node var1) {
      int var2 = -1;

      for (int var3 = 0; var3 < var1.size(); var3++) {
         if (isTokenOfType(var1.get(var3), TokenType.L_CURLY)) {
            var2 = var3;
            break;
         }
      }

      int var5 = var1.size() - 1;
      if (var2 >= 1 && isTokenOfType(var1.get(var5), TokenType.R_CURLY)) {
         this.emitToken(var1.get(0));

         for (int var4 = 1; var4 < var2; var4++) {
            this.layoutDocument.appendSpace();
            this.emitNode(var1.get(var4));
         }

         this.layoutDocument.appendSpace();
         this.emitToken(var1.get(var2));
         if (var5 == var2 + 1) {
            this.emitClosingBrace(var1.get(var5), false);
         } else {
            this.layoutDocument.pushIndent(false);
            this.emitNodesWithLineBreaks(var1, var2 + 1, var5);
            this.layoutDocument.popIndent();
            this.emitClosingBrace(var1.get(var5), true);
         }
      } else {
         throw unsupportedNodeException(var1);
      }
   }

   private void emitTemplateLiteral(Node var1) {
      if (var1.type != NodeType.LIT_TEMPLATE) {
         throw unsupportedNodeException(var1);
      } else {
         Token var2 = var1.getFirstToken();
         Token var3 = var1.getLastToken();
         int var4 = var3.pos + var3.length;
         this.emitLeadingComments(var2, true);
         this.layoutDocument.appendSourceToken(var2.pos, var4, this.containsLineBreak(var2.pos, var4) ? 8 : 0, var2.pos);
         this.emitTrailingComments(var3);
      }
   }

   private record CommentPlacement(Token a, boolean b, boolean c) {
   }
}
