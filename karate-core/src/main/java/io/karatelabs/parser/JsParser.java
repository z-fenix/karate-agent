package io.karatelabs.parser;

import io.karatelabs.common.Resource;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsParser extends BaseParser {
   private boolean noIn;
   private boolean inAsync = true;
   private boolean inGenerator = false;
   private boolean inAsyncBody = false;
   private final String source;
   private static final EnumSet<TokenType> T_VAR_STMT = EnumSet.of(TokenType.VAR, TokenType.CONST, TokenType.LET);
   private static final EnumSet<TokenType> T_ASSIGN_EXPR = EnumSet.of(
      TokenType.EQ,
      TokenType.PLUS_EQ,
      TokenType.MINUS_EQ,
      TokenType.STAR_EQ,
      TokenType.SLASH_EQ,
      TokenType.PERCENT_EQ,
      TokenType.STAR_STAR_EQ,
      TokenType.GT_GT_EQ,
      TokenType.LT_LT_EQ,
      TokenType.GT_GT_GT_EQ,
      TokenType.AMP_EQ,
      TokenType.PIPE_EQ,
      TokenType.CARET_EQ,
      TokenType.PIPE_PIPE_EQ,
      TokenType.AMP_AMP_EQ,
      TokenType.QUES_QUES_EQ
   );
   private static final EnumSet<TokenType> T_LOGIC_EQ_EXPR = EnumSet.of(TokenType.EQ_EQ_EQ, TokenType.NOT_EQ_EQ, TokenType.EQ_EQ, TokenType.NOT_EQ);
   private static final EnumSet<TokenType> T_LOGIC_CMP_EXPR = EnumSet.of(TokenType.LT, TokenType.GT, TokenType.LT_EQ, TokenType.GT_EQ);
   private static final EnumSet<TokenType> T_LOGIC_SHIFT_EXPR = EnumSet.of(TokenType.GT_GT, TokenType.LT_LT, TokenType.GT_GT_GT);
   private static final EnumSet<TokenType> T_MATH_ADD_EXPR = EnumSet.of(TokenType.PLUS, TokenType.MINUS);
   private static final EnumSet<TokenType> T_MATH_MUL_EXPR = EnumSet.of(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT);
   private static final EnumSet<TokenType> T_REF_DOT_EXPR = EnumSet.of(TokenType.DOT, TokenType.QUES_DOT);
   private static final EnumSet<TokenType> T_MATH_POST_EXPR = EnumSet.of(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS);
   private static final EnumSet<TokenType> T_UNARY_EXPR = EnumSet.of(TokenType.NOT, TokenType.TILDE, TokenType.VOID);
   private static final EnumSet<TokenType> T_MATH_PRE_EXPR = EnumSet.of(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS, TokenType.MINUS, TokenType.PLUS);
   private static final EnumSet<TokenType> T_OBJECT_ELEM = buildObjectElemSet();
   private static final EnumSet<TokenType> T_ACCESSOR_KEY_START = buildAccessorKeySet();
   private static final EnumSet<TokenType> T_LIT_EXPR = EnumSet.of(
      TokenType.S_STRING, TokenType.D_STRING, TokenType.NUMBER, TokenType.BIGINT, TokenType.TRUE, TokenType.FALSE, TokenType.NULL
   );
   private static final EnumSet<TokenType> T_FOR_IN_OF = EnumSet.of(TokenType.IN, TokenType.OF);
   private static final EnumSet<TokenType> T_EXPR_START = EnumSet.of(
      TokenType.IDENT,
      TokenType.S_STRING,
      TokenType.D_STRING,
      TokenType.NUMBER,
      TokenType.BIGINT,
      TokenType.TRUE,
      TokenType.FALSE,
      TokenType.NULL,
      TokenType.L_CURLY,
      TokenType.L_BRACKET,
      TokenType.BACKTICK,
      TokenType.REGEX,
      TokenType.FUNCTION,
      TokenType.CLASS,
      TokenType.SUPER,
      TokenType.L_PAREN,
      TokenType.NEW,
      TokenType.TYPEOF,
      TokenType.VOID,
      TokenType.DELETE,
      TokenType.NOT,
      TokenType.TILDE,
      TokenType.PLUS_PLUS,
      TokenType.MINUS_MINUS,
      TokenType.MINUS,
      TokenType.PLUS,
      TokenType.PRIVATE_NAME
   );
   private static final EnumSet<TokenType> T_LIT_EXPR_START = EnumSet.of(
      TokenType.S_STRING,
      TokenType.D_STRING,
      TokenType.NUMBER,
      TokenType.BIGINT,
      TokenType.TRUE,
      TokenType.FALSE,
      TokenType.NULL,
      TokenType.L_CURLY,
      TokenType.L_BRACKET,
      TokenType.BACKTICK,
      TokenType.REGEX
   );
   private Node ast;
   private boolean sawOptionalChain;
   private boolean sawPrivateName;
   private boolean sawLabel;
   private boolean sawProtoKey;
   private Set<Token> strictReservedIdents;
   private boolean leadingFunctionExpression;
   private static final String PROTO_KEY = "__proto__";
   private static final int SB_FN = 1;
   private static final int SB_LOOP = 2;
   private static final int SB_SWITCH = 4;
   private static final int SB_AWAIT_OK = 8;
   private static final TokenType[] STMT_RECOVERY = new TokenType[]{
      TokenType.IF,
      TokenType.FOR,
      TokenType.WHILE,
      TokenType.DO,
      TokenType.SWITCH,
      TokenType.TRY,
      TokenType.RETURN,
      TokenType.THROW,
      TokenType.BREAK,
      TokenType.CONTINUE,
      TokenType.VAR,
      TokenType.LET,
      TokenType.CONST,
      TokenType.FUNCTION,
      TokenType.L_CURLY,
      TokenType.R_CURLY,
      TokenType.SEMI,
      TokenType.EOF
   };
   private static final String[] STRICT_RESERVED = new String[]{
      "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield"
   };
   private static final EnumSet<TokenType> T_CLASS_KEY_NAME = EnumSet.of(
      TokenType.IDENT, TokenType.S_STRING, TokenType.D_STRING, TokenType.NUMBER, TokenType.PRIVATE_NAME
   );

   private static EnumSet<TokenType> buildObjectElemSet() {
      EnumSet<TokenType> s = EnumSet.of(
         TokenType.IDENT,
         TokenType.S_STRING,
         TokenType.D_STRING,
         TokenType.NUMBER,
         TokenType.BIGINT,
         TokenType.DOT_DOT_DOT,
         TokenType.L_BRACKET,
         TokenType.STAR
      );

      for (TokenType t : TokenType.values()) {
         if (t.keyword) {
            s.add(t);
         }
      }

      return s;
   }

   private static EnumSet<TokenType> buildAccessorKeySet() {
      EnumSet<TokenType> s = EnumSet.of(TokenType.IDENT, TokenType.S_STRING, TokenType.D_STRING, TokenType.NUMBER, TokenType.BIGINT, TokenType.L_BRACKET);

      for (TokenType t : TokenType.values()) {
         if (t.keyword) {
            s.add(t);
         }
      }

      return s;
   }

   public JsParser(Resource resource) {
      this(resource, false);
   }

   public JsParser(Resource resource, boolean errorRecovery) {
      super(resource, JsLexer.getTokens(resource), errorRecovery);
      this.source = resource.getText();
   }

   public JsParser leadingFunctionExpression() {
      this.leadingFunctionExpression = true;
      return this;
   }

   public Node getAst() {
      return this.ast;
   }

   public Node parse() {
      this.enter(NodeType.PROGRAM);
      this.ast = this.markerNode();

      while (true) {
         while (this.statement(false)) {
         }

         if (!this.errorRecoveryEnabled || this.peekIf(TokenType.EOF)) {
            if (!this.consumeIf(TokenType.EOF)) {
               this.error("cannot parse statement");
            }

            this.exit();
            if (!this.errorRecoveryEnabled) {
               this.earlyErrors(this.ast, false, false, null, null);
            }

            return this.ast;
         }

         this.error("cannot parse statement");
         this.recoverToStatement();
      }
   }

   private void earlyErrors(Node node, boolean strict, boolean inPattern, JsParser.PrivateScope privates, JsParser.Label labels) {
      if (node != null) {
         this.earlyErrorNodeChecks(node);
         if (node.type == NodeType.OBJECT_ELEM && hasCoverInitForm(node) && !inPattern) {
            throw new ParserException("invalid shorthand initializer: only allowed in destructuring pattern");
         } else {
            if (inPattern && node.type == NodeType.LIT_ARRAY) {
               validateRestElementRules(node);
            }

            if (node.type == NodeType.CLASS_STATIC_BLOCK) {
               this.checkStaticBlockBody(node, 0);
            }

            if (this.sawProtoKey && !inPattern && node.type == NodeType.LIT_OBJECT) {
               checkNoDuplicateProtoSetter(node);
            }

            JsParser.PrivateScope childPrivates = this.sawPrivateName ? this.privateNodeChecks(node, privates) : privates;
            boolean childStrict = this.strictNodeChecks(node, strict);
            JsParser.Label childLabels = this.sawLabel ? this.labelNodeChecks(node, labels) : null;
            int i = 0;

            for (int n = node.size(); i < n; i++) {
               Node child = node.get(i);
               if (!child.isToken()) {
                  this.earlyErrors(child, childStrict, childInPatternContext(node, i, inPattern), childPrivates, childLabels);
               } else if (childStrict && this.strictReservedIdents != null && this.strictReservedIdents.contains(child.token)) {
                  throw new ParserException("'" + child.getText() + "' is a reserved word in strict mode");
               }
            }
         }
      }
   }

   private JsParser.PrivateScope privateNodeChecks(Node node, JsParser.PrivateScope privates) {
      switch (node.type) {
         case CLASS_EXPR:
            return classPrivateScope(node, privates);
         case REF_DOT_EXPR:
            Node key = node.size() > 2 ? node.get(2) : null;
            if (key != null && key.isToken() && key.token.type == TokenType.PRIVATE_NAME) {
               requirePrivateDeclared(key.getText(), privates);
            }
            break;
         case PRIVATE_NAME_EXPR:
            Node parent = node.getParent();
            if (parent == null || parent.type != NodeType.IN_EXPR || parent.getFirst() != node) {
               throw new ParserException("unexpected private name '" + node.getText() + "': only valid as the left operand of `in`");
            }

            requirePrivateDeclared(node.getText(), privates);
            break;
         case DELETE_EXPR:
            Node target = node.size() > 1 ? node.get(1) : null;

            while (target != null && !target.isToken()) {
               if (target.type == NodeType.REF_DOT_EXPR) {
                  Node keyx = target.size() > 2 ? target.get(2) : null;
                  if (keyx != null && keyx.isToken() && keyx.token.type == TokenType.PRIVATE_NAME) {
                     throw new ParserException("cannot delete private member " + keyx.getText());
                  }
                  break;
               }

               if (target.type == NodeType.PAREN_EXPR) {
                  target = target.size() > 1 ? target.get(1) : null;
               } else {
                  if (target.type != NodeType.EXPR && target.type != NodeType.EXPR_LIST) {
                     break;
                  }

                  target = target.size() == 1 ? target.getFirst() : null;
               }
            }
      }

      return privates;
   }

   private static void requirePrivateDeclared(String name, JsParser.PrivateScope privates) {
      if (privates == null || !privates.declares(name)) {
         throw new ParserException("private name " + name + " is not declared in an enclosing class");
      }
   }

   private static JsParser.PrivateScope classPrivateScope(Node classNode, JsParser.PrivateScope parent) {
      Set<String> names = null;
      Map<String, Integer> kinds = null;
      int i = 0;

      for (int n = classNode.size(); i < n; i++) {
         Node member = classNode.get(i);
         if (member.type == NodeType.CLASS_METHOD) {
            Node key = null;
            int kind = 4;
            int j = 0;

            for (int m = member.size(); j < m; j++) {
               Node c = member.get(j);
               if (c.isToken()) {
                  if (c.token.type == TokenType.PRIVATE_NAME) {
                     key = c;
                     break;
                  }

                  String t = c.getText();
                  if ("get".equals(t)) {
                     kind = 1;
                  } else if ("set".equals(t)) {
                     kind = 2;
                  } else if ("static".equals(t)) {
                     kind |= 8;
                  }
               }
            }

            if (key != null) {
               String name = key.getText();
               if ("#constructor".equals(name)) {
                  throw new ParserException("#constructor is a reserved private name");
               }

               if (names == null) {
                  names = new HashSet<>(4);
                  kinds = new HashMap<>(4);
               }

               Integer prior = kinds.put(name, kind);
               boolean accessorPair = prior != null && (prior & 8) == (kind & 8) && (prior & 4) == 0 && (kind & 4) == 0 && (prior & 3 | kind & 3) == 3;
               if (prior != null && !accessorPair) {
                  throw new ParserException("duplicate private name " + name + " in class body");
               }

               names.add(name);
            }
         }
      }

      return names == null ? parent : new JsParser.PrivateScope(parent, names);
   }

   private JsParser.Label labelNodeChecks(Node node, JsParser.Label labels) {
      switch (node.type) {
         case LABELLED_STMT:
            String name = node.getFirst().getText();
            if (JsParser.Label.contains(labels, name, false)) {
               throw new ParserException("label already in scope: " + name);
            }

            return new JsParser.Label(name, labelsIterationStatement(node), labels);
         case FN_EXPR:
         case FN_ARROW_EXPR:
         case CLASS_STATIC_BLOCK:
            return null;
         case BREAK_STMT:
            String targetx = labelReference(node);
            if (targetx != null && !JsParser.Label.contains(labels, targetx, false)) {
               throw new ParserException("break references an undefined label: " + targetx);
            }
            break;
         case CONTINUE_STMT:
            String target = labelReference(node);
            if (target != null) {
               if (!JsParser.Label.contains(labels, target, false)) {
                  throw new ParserException("continue references an undefined label: " + target);
               }

               if (!JsParser.Label.contains(labels, target, true)) {
                  throw new ParserException("continue label does not name a loop: " + target);
               }
            }
      }

      return labels;
   }

   private static String labelReference(Node node) {
      return node.size() > 1 ? node.get(1).getText() : null;
   }

   private static boolean labelsIterationStatement(Node labelled) {
      Node item = labelledItem(labelled);

      while (item != null && item.type == NodeType.LABELLED_STMT) {
         item = labelledItem(item);
      }

      return item != null && (item.type == NodeType.FOR_STMT || item.type == NodeType.WHILE_STMT || item.type == NodeType.DO_WHILE_STMT);
   }

   private static Node labelledItem(Node labelled) {
      if (labelled.size() < 3) {
         return null;
      } else {
         Node statement = labelled.get(2);
         int i = 0;

         for (int n = statement.size(); i < n; i++) {
            Node inner = statement.get(i);
            if (!inner.isToken()) {
               return inner;
            }
         }

         return null;
      }
   }

   private void earlyErrorNodeChecks(Node node) {
      switch (node.type) {
         case LABELLED_STMT:
            checkNoFunctionDeclarationBody(node, "a labelled statement");
            checkNoLexicalOrClassDeclarationBody(node, "a labelled statement");
         case FN_EXPR:
         case FN_ARROW_EXPR:
         case CLASS_STATIC_BLOCK:
         case BREAK_STMT:
         case CONTINUE_STMT:
         case TYPEOF_EXPR:
         case UNARY_EXPR:
         case AWAIT_EXPR:
         default:
            break;
         case MATH_PRE_EXPR:
            if (node.size() > 1) {
               Node op = node.getFirst();
               if (op.isToken() && (op.token.type == TokenType.PLUS_PLUS || op.token.type == TokenType.MINUS_MINUS)) {
                  Node operand = node.get(1);
                  if (this.sawOptionalChain && subtreeContainsOptionalChain(operand)) {
                     throw new ParserException("optional chain cannot be the operand of prefix ++/--");
                  }

                  checkSimpleAssignmentTarget(operand, "operand of prefix update", false);
               }
            }
            break;
         case ASSIGN_EXPR:
            if (node.size() > 0) {
               Node lhs = node.getFirst();
               if (this.sawOptionalChain && subtreeContainsOptionalChain(lhs)) {
                  throw new ParserException("optional chain is not a valid assignment target");
               }

               TokenType op = node.size() > 1 && node.get(1).isToken() ? node.get(1).token.type : null;
               boolean noCallCarveOut = op == TokenType.PIPE_PIPE_EQ || op == TokenType.AMP_AMP_EQ || op == TokenType.QUES_QUES_EQ;
               checkSimpleAssignmentTarget(lhs, "assignment target", noCallCarveOut);
            }
            break;
         case MATH_EXP_EXPR:
            Node base = stripExprWrappers(node.size() > 0 ? node.getFirst() : null);
            if (base != null) {
               boolean var10000;
               label137: {
                  switch (base.type) {
                     case DELETE_EXPR:
                     case TYPEOF_EXPR:
                     case UNARY_EXPR:
                     case AWAIT_EXPR:
                        var10000 = true;
                        break label137;
                     case LABELLED_STMT:
                     case FN_EXPR:
                     case FN_ARROW_EXPR:
                     case CLASS_STATIC_BLOCK:
                     case BREAK_STMT:
                     case CONTINUE_STMT:
                     default:
                        var10000 = false;
                        break label137;
                     case MATH_PRE_EXPR:
                  }

                  var10000 = base.size() > 0
                     && base.getFirst().isToken()
                     && (base.getFirst().token.type == TokenType.PLUS || base.getFirst().token.type == TokenType.MINUS);
               }

               boolean unaryBase = var10000;
               if (unaryBase) {
                  throw new ParserException("unary expression cannot be the base of '**'; wrap it in parentheses");
               }
            }
            break;
         case MATH_POST_EXPR:
            if (node.size() > 0) {
               Node operand = node.getFirst();
               if (this.sawOptionalChain && subtreeContainsOptionalChain(operand)) {
                  throw new ParserException("optional chain cannot be the operand of postfix ++/--");
               }

               checkSimpleAssignmentTarget(operand, "operand of postfix update", false);
            }
            break;
         case FN_TAGGED_TEMPLATE_EXPR:
            if (this.sawOptionalChain && node.size() > 0 && subtreeContainsOptionalChain(node.getFirst())) {
               throw new ParserException("tagged template literal cannot follow an optional chain");
            }
            break;
         case FOR_STMT:
         case WHILE_STMT:
         case DO_WHILE_STMT:
            checkNoFunctionDeclarationBody(node, "a loop");
            checkNoLexicalOrClassDeclarationBody(node, "a loop");
            break;
         case IF_STMT:
            checkNoLexicalOrClassDeclarationBody(node, "an `if`/`else` clause");
            break;
         case SWITCH_STMT:
            boolean seen = false;
            int i = 0;

            for (int n = node.size(); i < n; i++) {
               Node child = node.get(i);
               if (!child.isToken() && child.type == NodeType.DEFAULT_BLOCK) {
                  if (seen) {
                     throw new ParserException("more than one `default` clause in a switch");
                  }

                  seen = true;
               }
            }
      }
   }

   public static boolean isProtoSetter(Node elem) {
      if (elem.size() >= 3 && elem.get(1).isToken() && elem.get(1).token.type == TokenType.COLON) {
         Node key = elem.getFirst();
         return !key.isToken() ? false : isProtoKeyText(key.token.type, key.getText());
      } else {
         return false;
      }
   }

   private static boolean isProtoKeyText(TokenType type, String text) {
      if (text == null) {
         return false;
      } else if (type == TokenType.IDENT) {
         return "__proto__".equals(text);
      } else {
         return (type == TokenType.S_STRING || type == TokenType.D_STRING) && text.length() >= 2
            ? "__proto__".equals(JsLexer.unescapeStringLiteral(text.substring(1, text.length() - 1)))
            : false;
      }
   }

   private static void checkNoDuplicateProtoSetter(Node litObject) {
      boolean seen = false;
      int i = 0;

      for (int n = litObject.size(); i < n; i++) {
         Node child = litObject.get(i);
         if (!child.isToken() && child.type == NodeType.OBJECT_ELEM && isProtoSetter(child)) {
            if (seen) {
               throw new ParserException("duplicate __proto__ in an object literal");
            }

            seen = true;
         }
      }
   }

   private void checkStaticBlockBody(Node node, int flags) {
      boolean keepAwait = (flags & 8) == 0 && !isPropertyNameHolder(node);
      int last = node.size() - 1;
      TokenType prev = null;

      for (int i = 0; i <= last; i++) {
         Node child = node.get(i);
         if (child.isToken()) {
            TokenType type = child.token.type;
            if (keepAwait && type == TokenType.IDENT && prev != TokenType.DOT && prev != TokenType.QUES_DOT && this.isIdentText(child.token, "await")) {
               throw new ParserException("'await' is a reserved word in a class static initialization block");
            }

            prev = type;
         } else {
            prev = null;
            int childFlags = flags;
            switch (child.type) {
               case FN_EXPR:
               case FN_ARROW_EXPR:
                  childFlags = flags | 1;
               case CLASS_STATIC_BLOCK:
               case TYPEOF_EXPR:
               case UNARY_EXPR:
               case AWAIT_EXPR:
               case MATH_PRE_EXPR:
               case ASSIGN_EXPR:
               case MATH_EXP_EXPR:
               case MATH_POST_EXPR:
               case FN_TAGGED_TEMPLATE_EXPR:
               case IF_STMT:
               default:
                  break;
               case BREAK_STMT:
                  if ((flags & 7) == 0 && child.size() == 1) {
                     throw new ParserException("'break' may not cross a class static initialization block");
                  }
                  break;
               case CONTINUE_STMT:
                  if ((flags & 3) == 0 && child.size() == 1) {
                     throw new ParserException("'continue' may not cross a class static initialization block");
                  }
                  break;
               case FOR_STMT:
               case WHILE_STMT:
               case DO_WHILE_STMT:
                  childFlags = flags | 2;
                  break;
               case SWITCH_STMT:
                  childFlags = flags | 4;
                  break;
               case RETURN_STMT:
                  if ((flags & 1) == 0) {
                     throw new ParserException("'return' is not allowed in a class static initialization block");
                  }
            }

            if (node.type == NodeType.FN_EXPR || node.type == NodeType.FN_ARROW_EXPR && i == last) {
               childFlags |= 8;
            }

            this.checkStaticBlockBody(child, childFlags);
         }
      }
   }

   private static boolean isPropertyNameHolder(Node node) {
      if (node.type == NodeType.CLASS_METHOD) {
         return true;
      } else if (node.type != NodeType.OBJECT_ELEM) {
         return false;
      } else {
         int i = 0;

         for (int n = node.size(); i < n; i++) {
            Node child = node.get(i);
            if (!child.isToken() || child.token.type == TokenType.COLON) {
               return true;
            }
         }

         return false;
      }
   }

   private static void checkNoFunctionDeclarationBody(Node node, String where) {
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (!child.isToken() && child.type == NodeType.STATEMENT) {
            int j = 0;

            for (int m = child.size(); j < m; j++) {
               Node inner = child.get(j);
               if (!inner.isToken()) {
                  if (inner.type == NodeType.FN_EXPR) {
                     throw new ParserException("a function declaration may not be the body of " + where);
                  }
                  break;
               }
            }
         }
      }
   }

   private static void checkNoLexicalOrClassDeclarationBody(Node node, String where) {
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node child = node.get(i);
         if (!child.isToken() && child.type == NodeType.STATEMENT) {
            int j = 0;

            for (int m = child.size(); j < m; j++) {
               Node inner = child.get(j);
               if (!inner.isToken()) {
                  if (inner.type == NodeType.CLASS_EXPR) {
                     throw new ParserException("a class declaration may not be the body of " + where);
                  }

                  if (inner.type == NodeType.VAR_STMT && isLexicalVarStmt(inner)) {
                     throw new ParserException("a lexical declaration may not be the body of " + where);
                  }
                  break;
               }
            }
         }
      }
   }

   private static boolean isLexicalVarStmt(Node varStmt) {
      int i = 0;

      for (int n = varStmt.size(); i < n; i++) {
         Node child = varStmt.get(i);
         if (child.isToken()) {
            TokenType tt = child.token.type;
            if (tt == TokenType.CONST) {
               return true;
            }

            return tt == TokenType.LET && !lineTerminatorFollows(child.token);
         }
      }

      return false;
   }

   private boolean strictNodeChecks(Node node, boolean strict) {
      boolean childStrict = strict;
      switch (node.type) {
         case CLASS_EXPR:
            childStrict = true;
            break;
         case FN_EXPR:
         case FN_ARROW_EXPR:
            childStrict = strict || fnBodyHasUseStrict(node);
            if (childStrict) {
               checkFunctionName(node);
            }

            checkFormalParameters(node, childStrict, node.type == NodeType.FN_ARROW_EXPR);
            break;
         case SWITCH_STMT:
            this.checkSwitchCaseBlockRedeclaration(node, strict);
            break;
         case PROGRAM:
            childStrict = strict || hasUseStrictPrologue(node);
            this.checkScopeRedeclaration(directStatements(node), true, childStrict);
            break;
         case BLOCK:
            Node p = node.getParent();
            boolean fnBody = p != null && (p.type == NodeType.FN_EXPR || p.type == NodeType.FN_ARROW_EXPR);
            this.checkScopeRedeclaration(directStatements(node), fnBody, strict);
            break;
         case TRY_STMT:
            checkCatchParameter(node, strict);
      }

      if (childStrict) {
         switch (node.type) {
            case MATH_PRE_EXPR:
               if (node.size() > 1
                  && node.getFirst().isToken()
                  && (node.getFirst().token.type == TokenType.PLUS_PLUS || node.getFirst().token.type == TokenType.MINUS_MINUS)) {
                  this.checkAssignTargetBinding(node.get(1));
               }
               break;
            case ASSIGN_EXPR:
            case MATH_POST_EXPR:
               this.checkAssignTargetBinding(node.size() > 0 ? node.getFirst() : null);
            case MATH_EXP_EXPR:
            case FN_TAGGED_TEMPLATE_EXPR:
            case FOR_STMT:
            case WHILE_STMT:
            case DO_WHILE_STMT:
            case SWITCH_STMT:
            case RETURN_STMT:
            case PROGRAM:
            case BLOCK:
            case TRY_STMT:
            default:
               break;
            case IF_STMT:
               checkNoFunctionDeclarationBody(node, "an `if` statement in strict mode");
               break;
            case LIT_EXPR:
               checkLegacyOctalLiteral(node);
               break;
            case VAR_DECL:
               checkVarDeclName(node);
         }
      }

      return childStrict;
   }

   private static boolean isEvalOrArguments(String s) {
      return "eval".equals(s) || "arguments".equals(s);
   }

   private static boolean hasUseStrictPrologue(Node body) {
      int i = 0;

      for (int n = body.size(); i < n; i++) {
         Node child = body.get(i);
         if (!child.isToken()) {
            if (child.type != NodeType.STATEMENT) {
               return false;
            }

            String dir = directiveString(child);
            if (dir == null) {
               return false;
            }

            if ("\"use strict\"".equals(dir) || "'use strict'".equals(dir)) {
               return true;
            }
         }
      }

      return false;
   }

   private static String directiveString(Node stmt) {
      if (stmt.size() == 0) {
         return null;
      } else {
         Node exprList = stmt.getFirst();
         if (exprList.type == NodeType.EXPR_LIST && exprList.size() == 1) {
            Node expr = exprList.getFirst();
            if (expr.type == NodeType.EXPR && expr.size() == 1) {
               Node lit = expr.getFirst();
               if (lit.type == NodeType.LIT_EXPR && lit.size() == 1) {
                  Node tok = lit.getFirst();
                  if (!tok.isToken()) {
                     return null;
                  } else {
                     TokenType tt = tok.token.type;
                     return tt != TokenType.S_STRING && tt != TokenType.D_STRING ? null : tok.getText();
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private static boolean fnBodyHasUseStrict(Node fn) {
      int i = 0;

      for (int n = fn.size(); i < n; i++) {
         Node child = fn.get(i);
         if (!child.isToken() && child.type == NodeType.BLOCK) {
            return hasUseStrictPrologue(child);
         }
      }

      return false;
   }

   private static void checkFunctionName(Node fn) {
      int i = fn.async ? 1 : 0;

      for (int n = fn.size(); i < n; i++) {
         Node child = fn.get(i);
         if (child.isToken()) {
            if (child.token.type == TokenType.IDENT) {
               if (isEvalOrArguments(child.getText())) {
                  throw new ParserException("'" + child.getText() + "' is not a valid function name in strict mode");
               }

               return;
            }
         } else if (child.type == NodeType.FN_DECL_ARGS) {
            return;
         }
      }
   }

   private static void checkFormalParameters(Node fn, boolean strict, boolean isArrow) {
      Node args = null;
      int i = 0;

      for (int n = fn.size(); i < n; i++) {
         Node child = fn.get(i);
         if (!child.isToken() && child.type == NodeType.FN_DECL_ARGS) {
            args = child;
            break;
         }
      }

      if (args != null) {
         boolean nonSimple = false;
         int ix = 0;

         for (int nx = args.size(); ix < nx; ix++) {
            Node arg = args.get(ix);
            if (!arg.isToken() && arg.type == NodeType.FN_DECL_ARG && isNonSimpleParam(arg)) {
               nonSimple = true;
               break;
            }
         }

         if (strict || isArrow || nonSimple) {
            List<String> names = new ArrayList<>();
            int ixx = 0;

            for (int nxx = args.size(); ixx < nxx; ixx++) {
               Node arg = args.get(ixx);
               if (!arg.isToken() && arg.type == NodeType.FN_DECL_ARG) {
                  collectBindingBoundNames(arg, names);
               }
            }

            if (strict) {
               for (String name : names) {
                  if (isEvalOrArguments(name)) {
                     throw new ParserException("'" + name + "' is not a valid parameter name in strict mode");
                  }
               }
            }

            if (strict || isArrow || nonSimple) {
               String dup = firstDuplicate(names);
               if (dup != null) {
                  throw new ParserException("duplicate parameter name '" + dup + "' is not allowed here");
               }
            }
         }
      }
   }

   private static boolean isNonSimpleParam(Node arg) {
      int i = 0;

      for (int n = arg.size(); i < n; i++) {
         Node ch = arg.get(i);
         if (ch.isToken()) {
            if (ch.token.type == TokenType.EQ || ch.token.type == TokenType.DOT_DOT_DOT) {
               return true;
            }
         } else if (ch.type == NodeType.LIT_ARRAY || ch.type == NodeType.LIT_OBJECT) {
            return true;
         }
      }

      return false;
   }

   private static void checkCatchParameter(Node tryStmt, boolean strict) {
      int catchIdx = -1;
      int i = 0;

      for (int n = tryStmt.size(); i < n; i++) {
         Node ch = tryStmt.get(i);
         if (ch.isToken() && ch.token.type == TokenType.CATCH) {
            catchIdx = i;
            break;
         }
      }

      if (catchIdx >= 0) {
         Node binding = null;
         int ix = catchIdx + 1;

         for (int nx = tryStmt.size(); ix < nx; ix++) {
            Node ch = tryStmt.get(ix);
            if (!ch.isToken()) {
               binding = ch;
               break;
            }

            if (ch.token.type == TokenType.R_PAREN) {
               break;
            }

            if (ch.token.type == TokenType.IDENT) {
               binding = ch;
               break;
            }
         }

         if (binding != null) {
            List<String> names = new ArrayList<>();
            collectBoundNames(binding, names);
            String dup = firstDuplicate(names);
            if (dup != null) {
               throw new ParserException("duplicate binding name '" + dup + "' is not allowed in a catch parameter");
            } else {
               if (strict) {
                  for (String name : names) {
                     if (isEvalOrArguments(name)) {
                        throw new ParserException("'" + name + "' is not a valid binding name in strict mode");
                     }
                  }
               }
            }
         }
      }
   }

   private static void checkVarDeclName(Node varDecl) {
      List<String> names = new ArrayList<>();
      collectBindingBoundNames(varDecl, names);

      for (String name : names) {
         if (isEvalOrArguments(name)) {
            throw new ParserException("'" + name + "' is not a valid binding name in strict mode");
         }
      }
   }

   private static List<Node> directStatements(Node scope) {
      List<Node> out = new ArrayList<>();
      int i = 0;

      for (int n = scope.size(); i < n; i++) {
         Node ch = scope.get(i);
         if (!ch.isToken() && ch.type == NodeType.STATEMENT) {
            out.add(ch);
         }
      }

      return out;
   }

   private static Node firstNonToken(Node node) {
      int i = 0;

      for (int n = node.size(); i < n; i++) {
         Node ch = node.get(i);
         if (!ch.isToken()) {
            return ch;
         }
      }

      return null;
   }

   private static String declarationName(Node decl) {
      int i = decl.async ? 1 : 0;

      for (int n = decl.size(); i < n; i++) {
         Node ch = decl.get(i);
         if (!ch.isToken()) {
            return null;
         }

         TokenType tt = ch.token.type;
         if (tt == TokenType.IDENT) {
            return ch.getText();
         }

         if (tt == TokenType.EXTENDS || tt == TokenType.L_CURLY || tt == TokenType.L_PAREN) {
            return null;
         }
      }

      return null;
   }

   public static void lexicalDeclarations(Node scope, List<String> names, List<Boolean> consts) {
      int i = 0;

      for (int n = scope.size(); i < n; i++) {
         Node stmt = scope.get(i);
         if (!stmt.isToken() && stmt.type == NodeType.STATEMENT) {
            Node d = firstNonToken(stmt);
            if (d != null) {
               if (d.type == NodeType.VAR_STMT && isLexicalVarStmt(d)) {
                  int before = names.size();
                  int j = 0;

                  for (int m = d.size(); j < m; j++) {
                     Node ch = d.get(j);
                     if (!ch.isToken() && ch.type == NodeType.VAR_DECL) {
                        collectBindingBoundNames(ch, names);
                     }
                  }

                  boolean isConst = d.getFirstToken().type == TokenType.CONST;

                  for (int jx = before; jx < names.size(); jx++) {
                     consts.add(isConst);
                  }
               } else if (d.type == NodeType.CLASS_EXPR) {
                  String name = declarationName(d);
                  if (name != null) {
                     names.add(name);
                     consts.add(false);
                  }
               }
            }
         }
      }
   }

   public static void boundNames(Node declarator, List<String> out) {
      collectBindingBoundNames(declarator, out);
   }

   private void checkSwitchCaseBlockRedeclaration(Node switchStmt, boolean strict) {
      List<Node> statements = new ArrayList<>();
      int i = 0;

      for (int n = switchStmt.size(); i < n; i++) {
         Node ch = switchStmt.get(i);
         if (!ch.isToken() && (ch.type == NodeType.CASE_BLOCK || ch.type == NodeType.DEFAULT_BLOCK)) {
            statements.addAll(directStatements(ch));
         }
      }

      this.checkScopeRedeclaration(statements, false, strict);
   }

   private void checkScopeRedeclaration(List<Node> statements, boolean topLevel, boolean strict) {
      List<String> lexNames = new ArrayList<>();
      Set<String> lexNonFn = new HashSet<>();

      for (Node stmt : statements) {
         Node d = firstNonToken(stmt);
         if (d != null) {
            if (d.type == NodeType.VAR_STMT && isLexicalVarStmt(d)) {
               List<String> tmp = new ArrayList<>();
               int i = 0;

               for (int n = d.size(); i < n; i++) {
                  Node ch = d.get(i);
                  if (!ch.isToken() && ch.type == NodeType.VAR_DECL) {
                     collectBindingBoundNames(ch, tmp);
                  }
               }

               lexNames.addAll(tmp);
               lexNonFn.addAll(tmp);
            } else if (d.type == NodeType.CLASS_EXPR) {
               String name = declarationName(d);
               if (name != null) {
                  lexNames.add(name);
                  lexNonFn.add(name);
               }
            } else if (d.type == NodeType.FN_EXPR && !topLevel) {
               String name = declarationName(d);
               if (name != null) {
                  lexNames.add(name);
               }
            }
         }
      }

      if (!lexNames.isEmpty()) {
         Set<String> seen = new HashSet<>();

         for (String name : lexNames) {
            if (!seen.add(name)) {
               boolean onlyFunctions = !lexNonFn.contains(name);
               if (strict || !onlyFunctions) {
                  throw new ParserException("identifier '" + name + "' has already been declared");
               }
            }
         }

         List<String> varNames = new ArrayList<>();

         for (Node stmtx : statements) {
            collectVarNames(stmtx, varNames);
            if (topLevel) {
               Node d = firstNonToken(stmtx);
               if (d != null && d.type == NodeType.FN_EXPR) {
                  String namex = declarationName(d);
                  if (namex != null) {
                     varNames.add(namex);
                  }
               }
            }
         }

         if (!varNames.isEmpty()) {
            Set<String> varSet = new HashSet<>(varNames);

            for (String namex : lexNames) {
               if (varSet.contains(namex)) {
                  throw new ParserException("identifier '" + namex + "' has already been declared");
               }
            }
         }
      }
   }

   private static void collectVarNames(Node node, List<String> out) {
      if (node != null && !node.isToken()) {
         switch (node.type) {
            case CLASS_EXPR:
            case FN_EXPR:
            case FN_ARROW_EXPR:
               return;
            case VAR_STMT:
               if (!isLexicalVarStmt(node)) {
                  int i = 0;

                  for (int n = node.size(); i < n; i++) {
                     Node ch = node.get(i);
                     if (!ch.isToken() && ch.type == NodeType.VAR_DECL) {
                        collectBindingBoundNames(ch, out);
                     }
                  }
               }

               return;
            default:
               int i = 0;

               for (int n = node.size(); i < n; i++) {
                  collectVarNames(node.get(i), out);
               }
         }
      }
   }

   private static void collectBindingBoundNames(Node binding, List<String> out) {
      int i = 0;

      for (int n = binding.size(); i < n; i++) {
         Node ch = binding.get(i);
         if (ch.isToken()) {
            if (ch.token.type == TokenType.EQ) {
               return;
            }

            if (ch.token.type == TokenType.IDENT) {
               out.add(ch.getText());
            }
         } else {
            collectBoundNames(ch, out);
         }
      }
   }

   private static void collectBoundNames(Node n, List<String> out) {
      if (n != null) {
         if (n.isToken()) {
            if (n.token.type == TokenType.IDENT) {
               out.add(n.getText());
            }
         } else {
            switch (n.type) {
               case ASSIGN_EXPR:
                  if (n.size() > 0) {
                     collectBoundNames(n.getFirst(), out);
                  }
               case MATH_EXP_EXPR:
               case MATH_POST_EXPR:
               case FN_TAGGED_TEMPLATE_EXPR:
               case FOR_STMT:
               case WHILE_STMT:
               case DO_WHILE_STMT:
               case IF_STMT:
               case SWITCH_STMT:
               case RETURN_STMT:
               case PROGRAM:
               case BLOCK:
               case TRY_STMT:
               case VAR_DECL:
               case VAR_STMT:
               default:
                  break;
               case LIT_EXPR:
               case EXPR:
               case EXPR_LIST:
               case PAREN_EXPR: {
                  int i = 0;

                  for (int cxx = n.size(); i < cxx; i++) {
                     collectBoundNames(n.get(i), out);
                  }
                  break;
               }
               case REF_EXPR:
                  Node f = n.getFirst();
                  if (f != null && f.isToken() && f.token.type == TokenType.IDENT) {
                     out.add(f.getText());
                  }
                  break;
               case LIT_OBJECT: {
                  int i = 0;

                  for (int cx = n.size(); i < cx; i++) {
                     Node ch = n.get(i);
                     if (!ch.isToken() && ch.type == NodeType.OBJECT_ELEM) {
                        collectObjectElemBoundNames(ch, out);
                     }
                  }
                  break;
               }
               case LIT_ARRAY: {
                  int i = 0;

                  for (int c = n.size(); i < c; i++) {
                     Node ch = n.get(i);
                     if (!ch.isToken() && ch.type == NodeType.ARRAY_ELEM) {
                        int j = 0;

                        for (int e = ch.size(); j < e; j++) {
                           Node sub = ch.get(j);
                           if (!sub.isToken()) {
                              collectBoundNames(sub, out);
                           }
                        }
                     }
                  }
                  break;
               }
            }
         }
      }
   }

   private static void collectObjectElemBoundNames(Node elem, List<String> out) {
      int colonIdx = -1;
      int restIdx = -1;
      int i = 0;

      for (int n = elem.size(); i < n; i++) {
         Node ch = elem.get(i);
         if (ch.isToken()) {
            if (ch.token.type == TokenType.COLON) {
               colonIdx = i;
            } else if (ch.token.type == TokenType.DOT_DOT_DOT) {
               restIdx = i;
            }
         } else if (ch.type == NodeType.FN_EXPR) {
            return;
         }
      }

      if (colonIdx < 0 && restIdx < 0) {
         i = 0;

         for (int nx = elem.size(); i < nx; i++) {
            Node ch = elem.get(i);
            if (ch.isToken() && ch.token.type == TokenType.IDENT) {
               out.add(ch.getText());
               return;
            }
         }
      } else {
         i = (colonIdx >= 0 ? colonIdx : restIdx) + 1;

         for (int nxx = elem.size(); i < nxx; i++) {
            Node ch = elem.get(i);
            if (!ch.isToken()) {
               collectBoundNames(ch, out);
               return;
            }
         }
      }
   }

   private static String firstDuplicate(List<String> names) {
      Set<String> seen = new HashSet<>(Math.max(4, names.size() * 2));

      for (String name : names) {
         if (!seen.add(name)) {
            return name;
         }
      }

      return null;
   }

   private void checkAssignTargetBinding(Node lhs) {
      if (lhs != null) {
         Node n = stripExprWrappers(lhs);
         if (n != null
            && n.type == NodeType.REF_EXPR
            && n.size() == 1
            && n.getFirst().isToken()
            && n.getFirst().token.type == TokenType.IDENT
            && isEvalOrArguments(n.getFirst().getText())) {
            throw new ParserException("invalid assignment to '" + n.getFirst().getText() + "' in strict mode");
         }
      }
   }

   private static void checkLegacyOctalLiteral(Node litExpr) {
      if (litExpr.size() == 1) {
         Node tok = litExpr.getFirst();
         if (tok.isToken() && tok.token.type == TokenType.NUMBER) {
            String text = tok.getText();
            if (text.length() >= 2 && text.charAt(0) == '0') {
               char c = text.charAt(1);
               if (c >= '0' && c <= '9') {
                  throw new ParserException("octal literals are not allowed in strict mode");
               }
            }
         }
      }
   }

   private static void validateRestElementRules(Node litArray) {
      int lastArrayElemIdx = -1;
      int i = 0;

      for (int n = litArray.size(); i < n; i++) {
         Node ch = litArray.get(i);
         if (!ch.isToken() && ch.type == NodeType.ARRAY_ELEM) {
            lastArrayElemIdx = i;
         }
      }

      i = 0;

      for (int nx = litArray.size(); i < nx; i++) {
         Node ch = litArray.get(i);
         if (!ch.isToken() && ch.type == NodeType.ARRAY_ELEM && hasRestPrefix(ch)) {
            if (i != lastArrayElemIdx) {
               throw new ParserException("invalid destructuring: rest element must be the last binding element");
            }

            if (restHasInitializer(ch)) {
               throw new ParserException("invalid destructuring: rest element cannot have an initializer");
            }
         }
      }
   }

   private static boolean hasRestPrefix(Node arrayElem) {
      if (arrayElem.size() == 0) {
         return false;
      } else {
         Node first = arrayElem.getFirst();
         return first.isToken() && first.token.type == TokenType.DOT_DOT_DOT;
      }
   }

   private static boolean restHasInitializer(Node arrayElem) {
      boolean sawDot = false;
      int i = 0;

      for (int n = arrayElem.size(); i < n; i++) {
         Node ch = arrayElem.get(i);
         if (ch.isToken()) {
            if (ch.token.type == TokenType.DOT_DOT_DOT) {
               sawDot = true;
            }
         } else if (sawDot) {
            Node inner = ch;

            while (inner != null && inner.size() == 1 && (inner.type == NodeType.EXPR || inner.type == NodeType.EXPR_LIST || inner.type == NodeType.LIT_EXPR)) {
               Node next = inner.getFirst();
               if (next.isToken()) {
                  break;
               }

               inner = next;
            }

            if (inner != null && inner.type == NodeType.ASSIGN_EXPR) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean childInPatternContext(Node parent, int i, boolean inPattern) {
      switch (parent.type) {
         case ASSIGN_EXPR:
         case VAR_DECL:
            return i == 0;
         case MATH_EXP_EXPR:
         case MATH_POST_EXPR:
         case FN_TAGGED_TEMPLATE_EXPR:
         case WHILE_STMT:
         case DO_WHILE_STMT:
         case IF_STMT:
         case SWITCH_STMT:
         case RETURN_STMT:
         case PROGRAM:
         case BLOCK:
         case VAR_STMT:
         case PAREN_EXPR:
         case REF_EXPR:
         default:
            return false;
         case FOR_STMT:
            for (int j = 0; j < parent.size(); j++) {
               Node sib = parent.get(j);
               if (sib.isToken() && (sib.token.type == TokenType.OF || sib.token.type == TokenType.IN)) {
                  return i == j - 1;
               }
            }

            return false;
         case TRY_STMT:
            for (int jx = 1; jx < parent.size(); jx++) {
               Node sib = parent.get(jx);
               if (sib.isToken() && sib.token.type == TokenType.L_PAREN && parent.get(jx - 1).isToken() && parent.get(jx - 1).token.type == TokenType.CATCH) {
                  return i == jx + 1;
               }
            }

            return false;
         case LIT_EXPR:
         case EXPR:
         case EXPR_LIST:
         case LIT_OBJECT:
         case LIT_ARRAY:
         case ARRAY_ELEM:
            return inPattern;
         case FN_DECL_ARG:
            Node first = parent.getFirst();
            return i == (first.isToken() && first.token.type == TokenType.DOT_DOT_DOT ? 1 : 0);
         case OBJECT_ELEM:
            return !inPattern ? false : !hasCoverInitForm(parent);
      }
   }

   private static boolean hasCoverInitForm(Node objElem) {
      int i = 0;

      for (int n = objElem.size(); i < n; i++) {
         Node ch = objElem.get(i);
         if (ch.isToken()) {
            TokenType t = ch.token.type;
            if (t == TokenType.EQ) {
               return true;
            }

            if (t == TokenType.COLON) {
               return false;
            }
         }
      }

      return false;
   }

   private static void checkSimpleAssignmentTarget(Node lhs, String siteName, boolean noCallCarveOut) {
      Node n = stripExprWrappers(lhs);
      if (n != null) {
         if (n.type == NodeType.LIT_EXPR && n.size() >= 1) {
            NodeType lit = n.getFirst().type;
            if (lit == NodeType.LIT_ARRAY || lit == NodeType.LIT_OBJECT) {
               return;
            }
         }

         while (n.type == NodeType.PAREN_EXPR) {
            Node body = n.size() >= 2 ? n.get(1) : null;
            if (body == null) {
               throw new ParserException("invalid " + siteName);
            }

            if (body.type == NodeType.EXPR_LIST && body.size() != 1) {
               throw new ParserException("invalid " + siteName + ": comma expression");
            }

            Node inner = stripExprWrappers(body);
            if (inner == null) {
               throw new ParserException("invalid " + siteName);
            }

            if (inner.type == NodeType.LIT_EXPR && inner.size() >= 1) {
               NodeType lit = inner.getFirst().type;
               if (lit == NodeType.LIT_ARRAY || lit == NodeType.LIT_OBJECT) {
                  throw new ParserException("invalid " + siteName + ": parenthesized destructuring pattern");
               }
            }

            n = inner;
         }

         switch (n.type) {
            case REF_DOT_EXPR:
            case REF_BRACKET_EXPR:
               if (n.type == NodeType.REF_DOT_EXPR && isImportMeta(n)) {
                  throw new ParserException("invalid " + siteName + ": import.meta");
               }
               break;
            case REF_EXPR:
               if (n.size() >= 1 && !n.getFirst().isToken() && n.getFirst().type == NodeType.FN_ARROW_EXPR) {
                  throw new ParserException("invalid " + siteName + ": arrow function");
               }

               if (n.size() >= 1 && n.getFirst().isToken() && n.getFirst().token.type == TokenType.IDENT && "this".equals(n.getFirst().getText())) {
                  throw new ParserException("invalid " + siteName + ": this");
               }
               break;
            case FN_CALL_EXPR:
               if (noCallCarveOut) {
                  throw new ParserException("invalid " + siteName + ": call expression");
               }
               break;
            default:
               throw new ParserException("invalid " + siteName + ": " + n.type.name() + " is not a valid assignment target");
         }
      }
   }

   private static Node stripExprWrappers(Node n) {
      while (n != null && n.size() == 1 && (n.type == NodeType.EXPR || n.type == NodeType.EXPR_LIST)) {
         n = n.get(0);
      }

      return n;
   }

   private static boolean isImportMeta(Node node) {
      if (node.type == NodeType.REF_DOT_EXPR && node.size() >= 3) {
         Node head = node.getFirst();
         if (head.type == NodeType.REF_EXPR && head.size() == 1) {
            Node headIdent = head.getFirst();
            if (headIdent.isToken() && headIdent.token.type == TokenType.IDENT && "import".equals(headIdent.getText())) {
               Node prop = node.get(2);
               return prop.isToken() && prop.token.type == TokenType.IDENT && "meta".equals(prop.getText());
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean subtreeContainsOptionalChain(Node node) {
      Node cur;
      for (cur = node;
         cur != null && (cur.type == NodeType.EXPR || cur.type == NodeType.EXPR_LIST || cur.type == NodeType.PAREN_EXPR);
         cur = cur.type == NodeType.PAREN_EXPR ? cur.get(1) : cur.getFirst()
      ) {
         if (cur.size() == 0) {
            return false;
         }
      }

      while (cur != null) {
         if (cur.type == NodeType.REF_DOT_EXPR) {
            Node second = cur.size() > 1 ? cur.get(1) : null;
            if (second != null) {
               if (second.isToken() && second.token.type == TokenType.QUES_DOT) {
                  return true;
               }

               if (!second.isToken() && second.size() > 0) {
                  Node firstOfSecond = second.getFirst();
                  if (firstOfSecond.isToken() && firstOfSecond.token.type == TokenType.QUES_DOT) {
                     return true;
                  }
               }
            }

            cur = cur.size() > 0 ? cur.getFirst() : null;
         } else {
            if (cur.type != NodeType.REF_BRACKET_EXPR && cur.type != NodeType.FN_CALL_EXPR && cur.type != NodeType.FN_TAGGED_TEMPLATE_EXPR) {
               return false;
            }

            cur = cur.size() > 0 ? cur.getFirst() : null;
         }
      }

      return false;
   }

   private void recoverToStatement() {
      this.recoverTo(STMT_RECOVERY);
   }

   private boolean statement(boolean mandatory) {
      this.enter(NodeType.STATEMENT);
      boolean result = this.if_stmt()
         || this.var_stmt(false) && this.eos()
         || this.return_stmt() && this.eos()
         || this.throw_stmt() && this.eos()
         || this.try_stmt()
         || this.for_stmt()
         || this.while_stmt()
         || this.do_while_stmt()
         || this.switch_stmt()
         || this.break_stmt() && this.eos()
         || this.continue_stmt() && this.eos()
         || this.labelled_stmt()
         || this.fn_decl()
         || this.class_expr()
         || this.block(false)
         || this.expr_list(false) && this.eos()
         || this.consumeIf(TokenType.SEMI);
      return !result && this.errorRecoveryEnabled && !this.markerNode().isEmpty() ? this.exitSoft() : this.exit(result, mandatory);
   }

   private boolean eos() {
      if (this.enter(NodeType.EOS, TokenType.SEMI)) {
         return this.exit();
      } else {
         TokenType next = this.peek();
         if (next != TokenType.R_CURLY && next != TokenType.EOF && this.noLineTerminatorBefore()) {
            this.error(new TokenType[]{TokenType.SEMI});
            return true;
         } else {
            return true;
         }
      }
   }

   private boolean expr_list(boolean mandatory) {
      if (!mandatory && !this.peekAnyOf(T_EXPR_START)) {
         return false;
      } else {
         this.enter(NodeType.EXPR_LIST);
         boolean atLeastOne = false;

         while (this.expr(-1, false)) {
            atLeastOne = true;
            if (!this.consumeIf(TokenType.COMMA)) {
               break;
            }
         }

         return this.exit(atLeastOne, mandatory);
      }
   }

   private boolean if_stmt() {
      if (!this.enter(NodeType.IF_STMT, TokenType.IF)) {
         return false;
      } else {
         this.consumeSoft(TokenType.L_PAREN);
         this.expr_list(true);
         this.consumeSoft(TokenType.R_PAREN);
         this.statement(true);
         if (this.consumeIf(TokenType.ELSE)) {
            this.statement(true);
         }

         return this.exit();
      }
   }

   private boolean var_stmt(boolean forLoop) {
      if (!this.enter(NodeType.VAR_STMT, T_VAR_STMT)) {
         return false;
      } else {
         boolean requireInit = this.lastConsumed() == TokenType.CONST && !forLoop;
         if (!this.var_decl(requireInit)) {
            this.error(new NodeType[]{NodeType.VAR_DECL});
         }

         while (this.consumeIf(TokenType.COMMA)) {
            if (!this.var_decl(requireInit)) {
               this.error(new NodeType[]{NodeType.VAR_DECL});
            }
         }

         return this.exit();
      }
   }

   private boolean var_decl(boolean requireInit) {
      this.enter(NodeType.VAR_DECL);
      boolean hasBinding = this.lit_array() || this.lit_object() || this.consumeIdentifier();
      if (!hasBinding) {
         return this.exit(false, false);
      } else {
         if (this.consumeIf(TokenType.EQ)) {
            this.expr(-1, true);
         } else if (requireInit) {
            this.error(new TokenType[]{TokenType.EQ});
         }

         return this.exit();
      }
   }

   private boolean return_stmt() {
      if (!this.enter(NodeType.RETURN_STMT, TokenType.RETURN)) {
         return false;
      } else if (!this.noLineTerminatorBefore()) {
         return this.exit();
      } else {
         this.expr_list(false);
         return this.exit();
      }
   }

   private boolean throw_stmt() {
      if (!this.enter(NodeType.THROW_STMT, TokenType.THROW)) {
         return false;
      } else {
         if (!this.noLineTerminatorBefore()) {
            this.error("a line terminator after 'throw' is not allowed — put the expression on the same line");
         }

         this.expr_list(true);
         return this.exit();
      }
   }

   private boolean try_stmt() {
      if (!this.enter(NodeType.TRY_STMT, TokenType.TRY)) {
         return false;
      } else {
         this.block(true);
         boolean hasCatch = this.consumeIf(TokenType.CATCH);
         if (hasCatch) {
            if (this.consumeIf(TokenType.L_PAREN)) {
               if (!this.lit_array() && !this.lit_object() && !this.consumeIdentifier()) {
                  this.error(new TokenType[]{TokenType.IDENT, TokenType.L_BRACKET, TokenType.L_CURLY});
               }

               this.consumeSoft(TokenType.R_PAREN);
            }

            this.block(true);
         }

         if (this.consumeIf(TokenType.FINALLY)) {
            this.block(true);
         } else if (!hasCatch) {
            this.error("expected " + TokenType.CATCH + " or " + TokenType.FINALLY);
         }

         return this.exit();
      }
   }

   private boolean for_stmt() {
      if (!this.enter(NodeType.FOR_STMT, TokenType.FOR)) {
         return false;
      } else {
         Token token = this.peekToken();
         if (token.type == TokenType.IDENT && token.length == 5 && this.isIdentText(token, "await")) {
            this.error("`for await` (async iteration) is not supported yet");
            this.consumeNext();
         }

         this.consumeSoft(TokenType.L_PAREN);
         boolean prevNoIn = this.noIn;
         this.noIn = true;

         try {
            if (!this.peekIf(TokenType.SEMI) && !this.var_stmt(true) && !this.expr_list(false)) {
               this.error(new NodeType[]{NodeType.VAR_STMT, NodeType.EXPR});
            }
         } finally {
            this.noIn = prevNoIn;
         }

         if (this.consumeIf(TokenType.SEMI)) {
            if (!this.peekIf(TokenType.SEMI) && !this.expr_list(false)) {
               this.error(new NodeType[]{NodeType.EXPR});
            } else if (this.consumeIf(TokenType.SEMI)) {
               if (!this.peekIf(TokenType.R_PAREN) && !this.expr_list(false)) {
                  this.error(new NodeType[]{NodeType.EXPR});
               }
            } else {
               this.error(new TokenType[]{TokenType.SEMI});
            }
         } else if (this.anyOf(T_FOR_IN_OF)) {
            this.expr(-1, true);
         } else {
            this.error(new TokenType[]{TokenType.SEMI, TokenType.IN, TokenType.OF});
         }

         this.consumeSoft(TokenType.R_PAREN);
         if (!this.peekIf(TokenType.EOF)) {
            this.statement(true);
         } else {
            this.error(new NodeType[]{NodeType.STATEMENT});
         }

         return this.exit();
      }
   }

   private boolean while_stmt() {
      if (!this.enter(NodeType.WHILE_STMT, TokenType.WHILE)) {
         return false;
      } else {
         this.consumeSoft(TokenType.L_PAREN);
         this.expr_list(true);
         this.consumeSoft(TokenType.R_PAREN);
         this.statement(true);
         return this.exit();
      }
   }

   private boolean do_while_stmt() {
      if (!this.enter(NodeType.DO_WHILE_STMT, TokenType.DO)) {
         return false;
      } else {
         this.statement(true);
         this.consumeSoft(TokenType.WHILE);
         this.consumeSoft(TokenType.L_PAREN);
         this.expr_list(true);
         this.consumeSoft(TokenType.R_PAREN);
         return this.exit();
      }
   }

   private boolean switch_stmt() {
      if (!this.enter(NodeType.SWITCH_STMT, TokenType.SWITCH)) {
         return false;
      } else {
         this.consumeSoft(TokenType.L_PAREN);
         this.expr_list(true);
         this.consumeSoft(TokenType.R_PAREN);
         this.consumeSoft(TokenType.L_CURLY);

         while (!this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
            if (!this.case_block() && !this.default_block()) {
               if (!this.errorRecoveryEnabled) {
                  break;
               }

               this.error("invalid case block");
               this.recoverTo(new TokenType[]{TokenType.R_CURLY, TokenType.CASE, TokenType.DEFAULT, TokenType.EOF});
            }
         }

         this.consumeSoft(TokenType.R_CURLY);
         return this.exit();
      }
   }

   private boolean case_block() {
      if (!this.enter(NodeType.CASE_BLOCK, TokenType.CASE)) {
         return false;
      } else {
         this.expr(-1, true);
         this.consumeSoft(TokenType.COLON);

         while (!this.peekIf(TokenType.CASE) && !this.peekIf(TokenType.DEFAULT) && !this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
            if (!this.statement(false)) {
               if (!this.errorRecoveryEnabled) {
                  break;
               }

               this.error("cannot parse statement in case block");
               this.recoverTo(new TokenType[]{TokenType.CASE, TokenType.DEFAULT, TokenType.R_CURLY, TokenType.SEMI, TokenType.EOF});
            }
         }

         return this.exit();
      }
   }

   private boolean default_block() {
      if (!this.enter(NodeType.DEFAULT_BLOCK, TokenType.DEFAULT)) {
         return false;
      } else {
         this.consumeSoft(TokenType.COLON);

         while (!this.peekIf(TokenType.CASE) && !this.peekIf(TokenType.DEFAULT) && !this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
            if (!this.statement(false)) {
               if (!this.errorRecoveryEnabled) {
                  break;
               }

               this.error("cannot parse statement in default block");
               this.recoverTo(new TokenType[]{TokenType.CASE, TokenType.DEFAULT, TokenType.R_CURLY, TokenType.SEMI, TokenType.EOF});
            }
         }

         return this.exit();
      }
   }

   private boolean break_stmt() {
      if (!this.enter(NodeType.BREAK_STMT, TokenType.BREAK)) {
         return false;
      } else {
         this.label_ref();
         return this.exit();
      }
   }

   private boolean continue_stmt() {
      if (!this.enter(NodeType.CONTINUE_STMT, TokenType.CONTINUE)) {
         return false;
      } else {
         this.label_ref();
         return this.exit();
      }
   }

   private void label_ref() {
      if (this.peekIf(TokenType.IDENT) && this.noLineTerminatorBefore()) {
         this.consumeNext();
         this.checkIdentifier(this.lastConsumedToken());
         this.sawLabel = true;
      }
   }

   private boolean labelled_stmt() {
      if (this.peek() == TokenType.IDENT && this.peekAhead(1).type == TokenType.COLON) {
         this.enter(NodeType.LABELLED_STMT, TokenType.IDENT);
         this.checkIdentifier(this.lastConsumedToken());
         this.sawLabel = true;
         this.consumeSoft(TokenType.COLON);
         this.statement(true);
         return this.exit();
      } else {
         return false;
      }
   }

   private boolean delete_expr() {
      if (!this.enter(NodeType.DELETE_EXPR, TokenType.DELETE)) {
         return false;
      } else {
         this.expr(13, true);
         return this.exit();
      }
   }

   private boolean block(boolean mandatory) {
      if (!this.enter(NodeType.BLOCK, TokenType.L_CURLY)) {
         if (mandatory) {
            if (this.errorRecoveryEnabled) {
               this.enter(NodeType.BLOCK);
               this.error(new TokenType[]{TokenType.L_CURLY});
               return this.exitSoft();
            }

            this.error(new NodeType[]{NodeType.BLOCK});
         }

         return false;
      } else {
         while (!this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
            if (!this.statement(false)) {
               if (!this.errorRecoveryEnabled) {
                  break;
               }

               this.error("cannot parse statement in block");
               this.recoverTo(new TokenType[]{TokenType.R_CURLY, TokenType.SEMI, TokenType.EOF});
               if (this.peekIf(TokenType.SEMI)) {
                  this.consumeNext();
               }
            }
         }

         this.consumeSoft(TokenType.R_CURLY);
         return this.exit();
      }
   }

   private boolean expr(int priority, boolean mandatory) {
      if (!mandatory && !this.peekAnyOf(T_EXPR_START)) {
         return false;
      } else {
         this.enter(NodeType.EXPR);
         boolean result = this.contextual_expr()
            || this.ref_expr()
            || this.lit_expr()
            || this.fn_expr()
            || this.class_expr()
            || this.super_expr()
            || this.fn_arrow_expr()
            || this.paren_expr()
            || this.unary_expr()
            || this.math_pre_expr()
            || this.new_expr()
            || this.typeof_expr()
            || this.delete_expr()
            || this.private_name_expr();
         if (result && this.markerNode().getLast().type != NodeType.YIELD_EXPR) {
            this.expr_rhs(priority);
         }

         if (!result && mandatory && this.errorRecoveryEnabled) {
            this.enter(NodeType.ERROR);
            this.error(new NodeType[]{NodeType.EXPR});
            this.exit();
            return this.exit(true, false);
         } else {
            return this.exit(result, mandatory);
         }
      }
   }

   private void expr_rhs(int priority) {
      while (true) {
         if (priority < 0 && this.enter(NodeType.ASSIGN_EXPR, T_ASSIGN_EXPR)) {
            this.expr(-1, true);
            this.exit(BaseParser.Shift.RIGHT);
         } else if (priority < 1 && this.enter(NodeType.LOGIC_TERN_EXPR, TokenType.QUES)) {
            this.expr(-1, true);
            this.consumeSoft(TokenType.COLON);
            this.expr(-1, true);
            this.exit(BaseParser.Shift.RIGHT);
         } else if (priority < 2 && this.enter(NodeType.LOGIC_AND_EXPR, TokenType.PIPE_PIPE)) {
            this.expr(2, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 2 && this.enter(NodeType.LOGIC_NULLISH_EXPR, TokenType.QUES_QUES)) {
            this.expr(2, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 3 && this.enter(NodeType.LOGIC_AND_EXPR, TokenType.AMP_AMP)) {
            this.expr(3, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 4 && this.enter(NodeType.LOGIC_BIT_EXPR, TokenType.PIPE)) {
            this.expr(4, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 5 && this.enter(NodeType.LOGIC_BIT_EXPR, TokenType.CARET)) {
            this.expr(5, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 6 && this.enter(NodeType.LOGIC_BIT_EXPR, TokenType.AMP)) {
            this.expr(6, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 7 && this.enter(NodeType.LOGIC_EXPR, T_LOGIC_EQ_EXPR)) {
            this.expr(7, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 8 && this.enter(NodeType.LOGIC_EXPR, T_LOGIC_CMP_EXPR)) {
            this.expr(8, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 9 && this.enter(NodeType.LOGIC_BIT_EXPR, T_LOGIC_SHIFT_EXPR)) {
            this.expr(9, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 10 && this.enter(NodeType.MATH_ADD_EXPR, T_MATH_ADD_EXPR)) {
            this.expr(10, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 11 && this.enter(NodeType.MATH_MUL_EXPR, T_MATH_MUL_EXPR)) {
            this.expr(11, true);
            this.exit(BaseParser.Shift.LEFT);
         } else if (priority < 12 && this.peekIf(TokenType.STAR_STAR)) {
            while (true) {
               this.enter(NodeType.MATH_EXP_EXPR);
               this.consumeNext();
               this.expr(12, true);
               this.exit(BaseParser.Shift.RIGHT);
               if (!this.peekIf(TokenType.STAR_STAR)) {
                  break;
               }
            }
         } else {
            if (this.enter(NodeType.FN_CALL_EXPR, TokenType.L_PAREN)) {
               this.fn_call_args();
               this.consumeSoft(TokenType.R_PAREN);
               this.exit(BaseParser.Shift.LEFT);
               if (!this.isCallerType(NodeType.NEW_EXPR)) {
                  continue;
               }
            } else {
               if (this.peekIf(TokenType.BACKTICK)) {
                  this.enter(NodeType.FN_TAGGED_TEMPLATE_EXPR);
                  this.lit_template();
                  this.exit(BaseParser.Shift.LEFT);
                  continue;
               }

               if (this.enter(NodeType.REF_DOT_EXPR, T_REF_DOT_EXPR)) {
                  TokenType dotType = this.lastConsumed();
                  if (dotType == TokenType.QUES_DOT) {
                     this.sawOptionalChain = true;
                  }

                  TokenType dotNext = this.peek();
                  if (dotNext == TokenType.IDENT || dotNext.keyword) {
                     this.consumeNext();
                  } else if (dotNext == TokenType.PRIVATE_NAME) {
                     this.sawPrivateName = true;
                     this.consumeNext();
                  } else if (dotType == TokenType.QUES_DOT) {
                     if (dotNext == TokenType.L_BRACKET) {
                        this.enter(NodeType.REF_BRACKET_EXPR, TokenType.L_BRACKET);
                        this.expr(-1, true);
                        this.consumeSoft(TokenType.R_BRACKET);
                        this.exit(BaseParser.Shift.LEFT);
                     } else if (dotNext == TokenType.L_PAREN) {
                        this.enter(NodeType.FN_CALL_EXPR, TokenType.L_PAREN);
                        this.fn_call_args();
                        this.consumeSoft(TokenType.R_PAREN);
                        this.exit(BaseParser.Shift.LEFT);
                     } else {
                        this.error(new TokenType[]{TokenType.L_BRACKET, TokenType.L_PAREN});
                     }
                  } else {
                     this.error(new TokenType[]{TokenType.IDENT});
                  }

                  this.exit(BaseParser.Shift.LEFT);
                  continue;
               }

               if (this.enter(NodeType.REF_BRACKET_EXPR, TokenType.L_BRACKET)) {
                  this.expr(-1, true);
                  this.consumeSoft(TokenType.R_BRACKET);
                  this.exit(BaseParser.Shift.LEFT);
                  continue;
               }

               if (this.peekAnyOf(T_MATH_POST_EXPR)) {
                  if (this.noLineTerminatorBefore()) {
                     this.enter(NodeType.MATH_POST_EXPR, T_MATH_POST_EXPR);
                     this.exit(BaseParser.Shift.LEFT);
                     continue;
                  }
               } else {
                  if (priority < 8 && this.enter(NodeType.INSTANCEOF_EXPR, TokenType.INSTANCEOF)) {
                     this.expr(8, true);
                     this.exit(BaseParser.Shift.LEFT);
                     continue;
                  }

                  if (priority < 8 && !this.noIn && this.enter(NodeType.IN_EXPR, TokenType.IN)) {
                     this.expr(8, true);
                     this.exit(BaseParser.Shift.LEFT);
                     continue;
                  }
               }
            }

            return;
         }
      }
   }

   private boolean contextual_expr() {
      Token token = this.peekToken();
      if (token.type == TokenType.IDENT && token.length == 5) {
         char c0 = this.source.charAt(token.pos);
         if (c0 == 'y') {
            return this.inGenerator && this.isIdentText(token, "yield") && this.yield_expr();
         } else if (c0 != 'a') {
            return false;
         } else {
            return this.inAsync && this.isIdentText(token, "await")
               ? this.await_expr()
               : this.isIdentText(token, "async") && (this.async_arrow_expr() || this.fn_expr());
         }
      } else {
         return false;
      }
   }

   private boolean yield_expr() {
      this.enter(NodeType.YIELD_EXPR);
      Token yieldToken = this.peekToken();
      this.consumeNext();
      if (!lineTerminatorFollows(yieldToken)) {
         if (this.consumeIf(TokenType.STAR)) {
            this.expr(-1, true);
         } else if (this.peekAnyOf(T_EXPR_START)) {
            this.expr(-1, true);
         }
      }

      return this.exit();
   }

   private boolean await_expr() {
      if (!T_EXPR_START.contains(this.peekAhead(1).type)) {
         return false;
      } else {
         this.enter(NodeType.AWAIT_EXPR);
         this.consumeNext();
         this.expr(13, true);
         return this.exit();
      }
   }

   private boolean async_arrow_expr() {
      Token asyncToken = this.peekToken();
      Token next = this.peekAhead(1);
      boolean parens = next.type == TokenType.L_PAREN && this.looksLikeArrowFunction(this.getPosition() + 1);
      boolean single = next.type == TokenType.IDENT && this.peekAhead(2).type == TokenType.EQ_GT;
      if ((parens || single) && !lineTerminatorFollows(asyncToken)) {
         this.enter(NodeType.FN_ARROW_EXPR);
         this.markerNode().async = true;
         this.consumeNext();
         if (parens) {
            if (!this.fn_decl_args()) {
               this.error(new NodeType[]{NodeType.FN_DECL_ARGS});
               return this.exit(false, false);
            }
         } else {
            this.enter(NodeType.FN_DECL_ARGS);
            this.enter(NodeType.FN_DECL_ARG);
            this.consumeNext();
            this.exit();
            this.exit();
         }

         this.lineTerminatorBeforeArrowIsAnError();
         this.consumeSoft(TokenType.EQ_GT);
         if (!this.fn_body_or_expr(true)) {
            this.error(new NodeType[]{NodeType.BLOCK, NodeType.EXPR});
         }

         return this.exit();
      } else {
         return false;
      }
   }

   private boolean consumeIdentifier() {
      if (!this.consumeIf(TokenType.IDENT)) {
         return false;
      } else {
         this.checkIdentifier(this.lastConsumedToken());
         return true;
      }
   }

   private void checkIdentifier(Token token) {
      String name = token.buffer.escapedIdentifiers && this.hasEscape(token) ? token.getText() : null;
      if (name != null && JsLexer.RESERVED_WORDS.contains(name)) {
         this.error("keyword '" + name + "' must not contain escape sequences");
      }

      if (this.inGenerator && this.spells(token, name, "yield") || this.inAsyncBody && this.spells(token, name, "await")) {
         this.error("'" + token.getText() + "' is a reserved word here");
      }

      char first = name == null ? this.source.charAt(token.pos) : name.charAt(0);
      if (first == 'i' || first == 'l' || first == 'p' || first == 's' || first == 'y') {
         for (String word : STRICT_RESERVED) {
            if (this.spells(token, name, word)) {
               if (this.strictReservedIdents == null) {
                  this.strictReservedIdents = new HashSet<>();
               }

               this.strictReservedIdents.add(token);
               return;
            }
         }
      }
   }

   private boolean hasEscape(Token token) {
      int i = token.pos;

      for (int end = token.pos + token.length; i < end; i++) {
         if (this.source.charAt(i) == '\\') {
            return true;
         }
      }

      return false;
   }

   private boolean spells(Token token, String name, String word) {
      return name == null ? this.isIdentText(token, word) : name.equals(word);
   }

   private Token lastConsumedToken() {
      return this.markerNode().getLast().token;
   }

   private boolean lastConsumedIs(String text) {
      Token token = this.lastConsumedToken();
      return token.type == TokenType.IDENT && this.isIdentText(token, text);
   }

   private boolean isIdentText(Token token, String text) {
      if (token.length != text.length()) {
         return false;
      } else {
         int i = 0;

         for (int n = text.length(); i < n; i++) {
            if (this.source.charAt(token.pos + i) != text.charAt(i)) {
               return false;
            }
         }

         return true;
      }
   }

   private Token peekAhead(int offset) {
      int index = this.getPosition() + offset;
      return index < this.tokens.size() ? this.tokens.get(index) : Token.EMPTY;
   }

   private boolean fn_body(boolean async) {
      return this.fn_body(async, false);
   }

   private boolean fn_body(boolean async, boolean generator) {
      boolean prevAsync = this.inAsync;
      boolean prevGenerator = this.inGenerator;
      boolean prevAsyncBody = this.inAsyncBody;
      this.inAsync = this.inAsyncBody = async;
      this.inGenerator = generator;

      boolean var6;
      try {
         var6 = this.block(true);
      } finally {
         this.inAsync = prevAsync;
         this.inGenerator = prevGenerator;
         this.inAsyncBody = prevAsyncBody;
      }

      return var6;
   }

   private boolean fn_body_or_expr(boolean async) {
      boolean prevAsync = this.inAsync;
      boolean prevGenerator = this.inGenerator;
      boolean prevAsyncBody = this.inAsyncBody;
      this.inAsync = this.inAsyncBody = async;
      this.inGenerator = false;

      boolean var5;
      try {
         var5 = this.block(false) || this.expr(-1, false);
      } finally {
         this.inAsync = prevAsync;
         this.inGenerator = prevGenerator;
         this.inAsyncBody = prevAsyncBody;
      }

      return var5;
   }

   private boolean fn_arrow_expr() {
      if (!this.looksLikeArrowFunction(this.getPosition())) {
         return false;
      } else {
         this.enter(NodeType.FN_ARROW_EXPR);
         if (this.fn_decl_args()) {
            this.lineTerminatorBeforeArrowIsAnError();
            if (this.consumeIf(TokenType.EQ_GT)) {
               if (this.fn_body_or_expr(false)) {
                  return this.exit();
               }

               this.error(new NodeType[]{NodeType.BLOCK, NodeType.EXPR});
            }
         }

         return this.exit(false, false);
      }
   }

   private void lineTerminatorBeforeArrowIsAnError() {
      if (this.peekIf(TokenType.EQ_GT) && !this.noLineTerminatorBefore()) {
         this.error("a line terminator before '=>' is not allowed — put '=>' on the same line as the parameters");
      }
   }

   private boolean looksLikeArrowFunction(int start) {
      if (start < this.tokens.size() && this.tokens.get(start).type == TokenType.L_PAREN) {
         int parenDepth = 0;
         int curlyDepth = 0;
         int size = this.tokens.size();

         for (int i = start; i < size; i++) {
            TokenType t = this.tokens.get(i).type;
            if (t == TokenType.L_PAREN) {
               parenDepth++;
            } else if (t == TokenType.R_PAREN) {
               if (--parenDepth == 0 && curlyDepth == 0) {
                  return i + 1 < size && this.tokens.get(i + 1).type == TokenType.EQ_GT;
               }
            } else if (t == TokenType.L_CURLY) {
               curlyDepth++;
            } else if (t == TokenType.R_CURLY) {
               curlyDepth--;
            } else {
               if (t == TokenType.EOF) {
                  return false;
               }

               if (t == TokenType.SEMI && curlyDepth == 0) {
                  return false;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean fn_decl() {
      TokenType t = this.peek();
      int i = t == TokenType.FUNCTION ? 0 : (t == TokenType.IDENT && this.isIdentText(this.peekToken(), "async") ? 1 : -1);
      if (i >= 0 && this.peekAhead(i).type == TokenType.FUNCTION) {
         if (this.peekAhead(i + 1).type == TokenType.STAR) {
            i++;
         }

         if (this.peekAhead(i + 1).type == TokenType.L_PAREN) {
            if (this.leadingFunctionExpression && this.getPosition() == 0) {
               return false;
            }

            this.error("a function declaration requires a name");
         }

         return this.fn_expr(true);
      } else {
         return false;
      }
   }

   private boolean fn_expr() {
      return this.fn_expr(false);
   }

   private boolean fn_expr(boolean declaration) {
      if (this.enter(NodeType.FN_EXPR, TokenType.FUNCTION)) {
         return this.fn_expr_tail(false, declaration);
      } else {
         Token token = this.peekToken();
         if (token.type == TokenType.IDENT
            && token.length == 5
            && this.peekAhead(1).type == TokenType.FUNCTION
            && this.isIdentText(token, "async")
            && !lineTerminatorFollows(token)) {
            this.enter(NodeType.FN_EXPR);
            this.markerNode().async = true;
            this.consumeNext();
            this.consumeNext();
            return this.fn_expr_tail(true, declaration);
         } else {
            return false;
         }
      }
   }

   private boolean fn_expr_tail(boolean async, boolean declaration) {
      boolean generator = this.consumeIf(TokenType.STAR);
      if (generator) {
         if (async) {
            this.error("async generators are not supported");
         }

         this.markerNode().generator = true;
      }

      if (declaration) {
         this.consumeIdentifier();
      } else {
         boolean prevGenerator = this.inGenerator;
         boolean prevAsyncBody = this.inAsyncBody;
         this.inGenerator = generator;
         this.inAsyncBody = async;
         this.consumeIdentifier();
         this.inGenerator = prevGenerator;
         this.inAsyncBody = prevAsyncBody;
      }

      this.fn_decl_args();
      this.fn_body(async, generator);
      return this.exit();
   }

   private boolean fn_decl_args() {
      if (!this.enter(NodeType.FN_DECL_ARGS, TokenType.L_PAREN)) {
         return false;
      } else {
         boolean hasParams = false;

         while (!this.peekIf(TokenType.R_PAREN) && !this.peekIf(TokenType.EOF)) {
            if (!this.fn_decl_arg()) {
               if (!this.errorRecoveryEnabled || !hasParams) {
                  break;
               }

               this.error("invalid function parameter");
               this.recoverTo(new TokenType[]{TokenType.R_PAREN, TokenType.COMMA, TokenType.EOF});
            } else {
               hasParams = true;
            }
         }

         return this.exit(this.consumeIf(TokenType.R_PAREN), false);
      }
   }

   private boolean fn_decl_arg() {
      this.enter(NodeType.FN_DECL_ARG);
      if (this.consumeIf(TokenType.DOT_DOT_DOT)) {
         if (!this.consumeIdentifier() && !this.lit_array() && !this.lit_object()) {
            this.error(new TokenType[]{TokenType.IDENT, TokenType.L_BRACKET, TokenType.L_CURLY});
         }

         if (!this.peekIf(TokenType.R_PAREN)) {
            this.error(new TokenType[]{TokenType.R_PAREN});
         }

         return this.exit();
      } else {
         boolean result = this.consumeIdentifier() || this.lit_array() || this.lit_object();
         if (result && this.consumeIf(TokenType.EQ)) {
            this.expr(-1, true);
         }

         result = result && (this.consumeIf(TokenType.COMMA) || this.peekIf(TokenType.R_PAREN));
         return this.exit(result, false);
      }
   }

   private void fn_call_args() {
      this.enter(NodeType.FN_CALL_ARGS);

      while (!this.peekIf(TokenType.R_PAREN) && !this.peekIf(TokenType.EOF)) {
         if (!this.fn_call_arg()) {
            if (!this.errorRecoveryEnabled) {
               break;
            }

            this.error("invalid function argument");
            this.recoverTo(new TokenType[]{TokenType.R_PAREN, TokenType.COMMA, TokenType.EOF});
         }
      }

      this.exit();
   }

   private boolean fn_call_arg() {
      this.enter(NodeType.FN_CALL_ARG);
      this.consumeIf(TokenType.DOT_DOT_DOT);
      boolean result = this.expr(-1, false);
      result = result && (this.consumeIf(TokenType.COMMA) || this.peekIf(TokenType.R_PAREN));
      return this.exit(result, false);
   }

   private boolean new_expr() {
      if (!this.enter(NodeType.NEW_EXPR, TokenType.NEW)) {
         return false;
      } else {
         this.expr(13, true);
         return this.exit();
      }
   }

   private boolean super_expr() {
      return !this.enter(NodeType.SUPER_EXPR, TokenType.SUPER) ? false : this.exit();
   }

   private boolean class_expr() {
      if (!this.enter(NodeType.CLASS_EXPR, TokenType.CLASS)) {
         return false;
      } else {
         this.consumeIdentifier();
         if (this.consumeIf(TokenType.EXTENDS)) {
            this.expr(13, true);
         }

         if (!this.consumeIf(TokenType.L_CURLY)) {
            this.error(new TokenType[]{TokenType.L_CURLY});
            return this.exit(false, false);
         } else {
            while (!this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
               if (!this.consumeIf(TokenType.SEMI) && !this.class_element()) {
                  if (!this.errorRecoveryEnabled) {
                     return this.exit(false, false);
                  }

                  this.error("invalid class element");
                  this.recoverTo(new TokenType[]{TokenType.R_CURLY, TokenType.SEMI, TokenType.EOF});
               }
            }

            return this.exit(this.consumeIf(TokenType.R_CURLY), true);
         }
      }
   }

   private boolean class_element() {
      if (this.peekIf(TokenType.IDENT) && this.isIdentText(this.peekToken(), "static") && this.peekAhead(1).type == TokenType.L_CURLY) {
         return this.class_static_block();
      } else {
         this.enter(NodeType.CLASS_METHOD);
         boolean async = false;
         boolean generator = false;

         while (true) {
            while (!generator && this.consumeIf(TokenType.STAR)) {
               generator = true;
            }

            if (this.consumeIf(TokenType.L_BRACKET)) {
               this.expr(-1, true);
               if (!this.consumeIf(TokenType.R_BRACKET)) {
                  this.error(new TokenType[]{TokenType.R_BRACKET});
                  return this.exit(false, false);
               }
            } else {
               if (!this.peekAnyOf(T_CLASS_KEY_NAME)) {
                  this.error("class member name");
                  return this.exit(false, false);
               }

               this.consumeNext();
               if (this.lastConsumed() == TokenType.PRIVATE_NAME) {
                  this.sawPrivateName = true;
               }

               if (!this.peekIf(TokenType.L_PAREN)) {
                  if ((this.lastConsumedIs("static") || this.lastConsumedIs("get") || this.lastConsumedIs("set"))
                     && (this.peekAnyOf(T_CLASS_KEY_NAME) || this.peekIf(TokenType.L_BRACKET) || this.peekIf(TokenType.STAR))) {
                     continue;
                  }

                  if (this.lastConsumedIs("async")
                     && (this.peekAnyOf(T_CLASS_KEY_NAME) || this.peekIf(TokenType.L_BRACKET) || this.peekIf(TokenType.STAR))
                     && !lineTerminatorFollows(this.markerNode().getLast().token)) {
                     async = true;
                     continue;
                  }

                  return this.class_field_tail();
               }
            }

            if (!this.peekIf(TokenType.L_PAREN)) {
               return this.class_field_tail();
            }

            this.enter(NodeType.FN_EXPR);
            this.markerNode().async = async;
            if (generator) {
               if (async) {
                  this.error("async generators are not supported");
               }

               this.markerNode().generator = true;
            }

            this.fn_decl_args();
            this.fn_body(async, generator);
            this.exit();
            return this.exit();
         }
      }
   }

   private boolean class_static_block() {
      this.enter(NodeType.CLASS_STATIC_BLOCK);
      this.consumeNext();
      return this.exit(this.block(true), false);
   }

   private boolean class_field_tail() {
      if (this.consumeIf(TokenType.EQ)) {
         this.expr(-1, true);
      }

      this.consumeIf(TokenType.SEMI);
      return this.exit();
   }

   private boolean typeof_expr() {
      if (!this.enter(NodeType.TYPEOF_EXPR, TokenType.TYPEOF)) {
         return false;
      } else {
         this.expr(13, true);
         return this.exit();
      }
   }

   private boolean private_name_expr() {
      if (!this.enter(NodeType.PRIVATE_NAME_EXPR, TokenType.PRIVATE_NAME)) {
         return false;
      } else {
         this.sawPrivateName = true;
         return this.exit();
      }
   }

   private boolean ref_expr() {
      if (!this.enter(NodeType.REF_EXPR, TokenType.IDENT)) {
         return false;
      } else {
         this.checkIdentifier(this.lastConsumedToken());
         this.lineTerminatorBeforeArrowIsAnError();
         if (this.enter(NodeType.FN_ARROW_EXPR, TokenType.EQ_GT)) {
            if (this.fn_body_or_expr(false)) {
               this.exit(BaseParser.Shift.LEFT);
            } else {
               this.error(new NodeType[]{NodeType.BLOCK, NodeType.EXPR});
            }
         }

         return this.exit();
      }
   }

   private boolean lit_expr() {
      if (!this.peekAnyOf(T_LIT_EXPR_START)) {
         return false;
      } else {
         this.enter(NodeType.LIT_EXPR);
         boolean result = this.anyOf(T_LIT_EXPR) || this.lit_object() || this.lit_array() || this.lit_template() || this.lit_regex();
         return this.exit(result, false);
      }
   }

   private boolean lit_template() {
      if (!this.enter(NodeType.LIT_TEMPLATE, TokenType.BACKTICK)) {
         return false;
      } else {
         while (true) {
            if (this.peek() == TokenType.EOF) {
               this.error(new TokenType[]{TokenType.BACKTICK});
               break;
            }

            if (this.consumeIf(TokenType.BACKTICK)) {
               break;
            }

            if (!this.consumeIf(TokenType.T_STRING) && this.consumeIf(TokenType.DOLLAR_L_CURLY)) {
               this.expr(-1, false);
               this.consumeSoft(TokenType.R_CURLY);
            }
         }

         return this.exit();
      }
   }

   private boolean unary_expr() {
      if (!this.enter(NodeType.UNARY_EXPR, T_UNARY_EXPR)) {
         return false;
      } else {
         this.expr(13, true);
         return this.exit();
      }
   }

   private boolean math_pre_expr() {
      if (!this.enter(NodeType.MATH_PRE_EXPR, T_MATH_PRE_EXPR)) {
         return false;
      } else {
         if (!this.expr(13, false) && !this.consumeIf(TokenType.NUMBER) && !this.consumeIf(TokenType.BIGINT)) {
            this.error(new NodeType[]{NodeType.EXPR});
         }

         return this.exit();
      }
   }

   private boolean lit_object() {
      if (!this.enter(NodeType.LIT_OBJECT, TokenType.L_CURLY)) {
         return false;
      } else {
         boolean prevNoIn = this.noIn;
         this.noIn = false;

         boolean var4;
         try {
            boolean hasElements = false;

            while (!this.peekIf(TokenType.R_CURLY) && !this.peekIf(TokenType.EOF)) {
               if (!this.object_elem()) {
                  if (!this.errorRecoveryEnabled || !hasElements) {
                     break;
                  }

                  this.error("invalid object element");
                  this.recoverTo(new TokenType[]{TokenType.R_CURLY, TokenType.COMMA, TokenType.EOF});
               } else {
                  hasElements = true;
               }
            }

            boolean result = this.consumeIf(TokenType.R_CURLY);
            var4 = this.exit(result, false);
         } finally {
            this.noIn = prevNoIn;
         }

         return var4;
      }
   }

   private boolean object_elem() {
      if (!this.enter(NodeType.OBJECT_ELEM, T_OBJECT_ELEM)) {
         return false;
      } else {
         if (!this.sawProtoKey && isProtoKeyText(this.lastConsumed(), this.lastConsumedText())) {
            this.sawProtoKey = true;
         }

         if (this.lastConsumed() == TokenType.STAR) {
            return this.object_method_elem(false, true);
         } else if ((this.lastConsumedIs("get") || this.lastConsumedIs("set")) && this.peekAnyOf(T_ACCESSOR_KEY_START)) {
            return this.object_method_elem(false);
         } else if (this.lastConsumedIs("async") && this.peekAnyOf(T_ACCESSOR_KEY_START) && !lineTerminatorFollows(this.markerNode().getLast().token)) {
            return this.object_method_elem(true);
         } else {
            if (this.lastConsumed() == TokenType.L_BRACKET) {
               this.expr(-1, true);
               if (!this.consumeIf(TokenType.R_BRACKET)) {
                  this.error(new TokenType[]{TokenType.R_BRACKET});
                  return this.exit(false, false);
               }
            }

            if (this.consumeIf(TokenType.EQ)) {
               this.expr(-1, true);
            }

            if (this.peekIf(TokenType.L_PAREN)) {
               this.enter(NodeType.FN_EXPR);
               this.fn_decl_args();
               this.fn_body(false);
               this.exit();
               if (!this.consumeIf(TokenType.COMMA) && !this.peekIf(TokenType.R_CURLY)) {
                  this.error(new TokenType[]{TokenType.COMMA, TokenType.R_CURLY});
               }

               return this.exit();
            } else if (!this.consumeIf(TokenType.COMMA) && !this.peekIf(TokenType.R_CURLY)) {
               boolean spread = false;
               if (!this.consumeIf(TokenType.COLON)) {
                  if (this.lastConsumed() != TokenType.DOT_DOT_DOT) {
                     return this.exit(false, false);
                  }

                  spread = true;
                  this.expr(-1, true);
               }

               if (!spread) {
                  this.expr(-1, true);
               }

               if (!this.consumeIf(TokenType.COMMA) && !this.peekIf(TokenType.R_CURLY)) {
                  this.error(new TokenType[]{TokenType.COMMA, TokenType.R_CURLY});
               }

               return this.exit();
            } else {
               Token key = this.markerNode().getFirst().token;
               if (key != null && key.type == TokenType.IDENT) {
                  this.checkIdentifier(key);
               }

               return this.exit();
            }
         }
      }
   }

   private boolean object_method_elem(boolean async) {
      return this.object_method_elem(async, false);
   }

   private boolean object_method_elem(boolean async, boolean generator) {
      if (this.consumeIf(TokenType.L_BRACKET)) {
         this.expr(-1, true);
         if (!this.consumeIf(TokenType.R_BRACKET)) {
            this.error(new TokenType[]{TokenType.R_BRACKET});
            return this.exit(false, false);
         }
      } else if (!this.anyOf(T_ACCESSOR_KEY_START)) {
         this.error(new TokenType[]{TokenType.IDENT, TokenType.S_STRING});
         return this.exit(false, false);
      }

      this.enter(NodeType.FN_EXPR);
      this.markerNode().async = async;
      this.markerNode().generator = generator;
      this.fn_decl_args();
      this.fn_body(async, generator);
      this.exit();
      if (!this.consumeIf(TokenType.COMMA) && !this.peekIf(TokenType.R_CURLY)) {
         this.error(new TokenType[]{TokenType.COMMA, TokenType.R_CURLY});
      }

      return this.exit();
   }

   private boolean lit_array() {
      if (!this.enter(NodeType.LIT_ARRAY, TokenType.L_BRACKET)) {
         return false;
      } else {
         boolean prevNoIn = this.noIn;
         this.noIn = false;

         boolean var2;
         try {
            while (!this.peekIf(TokenType.R_BRACKET) && !this.peekIf(TokenType.EOF)) {
               if (!this.array_elem()) {
                  if (!this.errorRecoveryEnabled) {
                     break;
                  }

                  this.error("invalid array element");
                  this.recoverTo(new TokenType[]{TokenType.R_BRACKET, TokenType.COMMA, TokenType.EOF});
               }
            }

            this.consumeSoft(TokenType.R_BRACKET);
            var2 = this.exit();
         } finally {
            this.noIn = prevNoIn;
         }

         return var2;
      }
   }

   private boolean array_elem() {
      this.enter(NodeType.ARRAY_ELEM);
      this.consumeIf(TokenType.DOT_DOT_DOT);
      this.expr(-1, false);
      if (!this.consumeIf(TokenType.COMMA) && !this.peekIf(TokenType.R_BRACKET) && !this.peekIf(TokenType.EOF)) {
         this.error(new TokenType[]{TokenType.COMMA, TokenType.R_BRACKET});
      }

      return this.exit();
   }

   private boolean lit_regex() {
      return !this.enter(NodeType.LIT_REGEX, TokenType.REGEX) ? false : this.exit();
   }

   private boolean paren_expr() {
      if (!this.enter(NodeType.PAREN_EXPR, TokenType.L_PAREN)) {
         return false;
      } else {
         boolean prevNoIn = this.noIn;
         this.noIn = false;

         try {
            this.expr_list(true);
         } finally {
            this.noIn = prevNoIn;
         }

         this.consumeSoft(TokenType.R_PAREN);
         return this.exit();
      }
   }

   private record Label(String name, boolean iteration, JsParser.Label next) {
      static boolean contains(JsParser.Label chain, String name, boolean iterationOnly) {
         for (JsParser.Label l = chain; l != null; l = l.next) {
            if (l.name.equals(name) && (!iterationOnly || l.iteration)) {
               return true;
            }
         }

         return false;
      }
   }

   private record PrivateScope(JsParser.PrivateScope parent, Set<String> names) {
      boolean declares(String name) {
         for (JsParser.PrivateScope s = this; s != null; s = s.parent) {
            if (s.names.contains(name)) {
               return true;
            }
         }

         return false;
      }
   }
}
