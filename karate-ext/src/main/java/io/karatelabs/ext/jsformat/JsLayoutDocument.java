package io.karatelabs.ext.jsformat;

import java.util.Arrays;

final class JsLayoutDocument {
   static final byte OP_TOKEN = 0;
   static final byte OP_SOFT_SPACE_OR_BREAK = 1;
   static final byte OP_SOFT_GAP_OR_BREAK = 2;
   static final byte OP_LINE_BREAK = 3;
   static final byte OP_EXTRA_LINE_BREAK = 4;
   static final byte OP_FORCE_COMMENT_LINE_START = 5;
   static final byte OP_FORCE_BREAK_AFTER_COMMENT = 6;
   static final byte OP_PROTECT_TOKEN_ADJACENCY = 7;
   static final byte OP_DEFER_INSTRUCTION = 8;
   static final byte OP_SAVE_LAYOUT_STATE = 9;
   static final byte OP_RESTORE_LAYOUT_STATE = 10;
   static final byte OP_PUSH_INDENT = 11;
   static final byte OP_POP_INDENT = 12;
   static final byte OP_SKIP_IF_INLINE = 13;
   static final byte OP_JUMP = 14;
   static final byte OP_NOOP_JUMP_TARGET = 15;
   static final int FLAG_NO_AUTOMATIC_BREAK_BEFORE = 1;
   static final int FLAG_NO_SPACE_AFTER = 2;
   static final int FLAG_NUMBER_TOKEN = 4;
   static final int FLAG_CONTAINS_LINE_BREAK = 8;
   static final int FLAG_NON_SIGNIFICANT_TOKEN = 16;
   static final int FLAG_SYNTHETIC_TOKEN = 32;
   static final int FLAG_INDENT_WHEN_EXPANDED = 64;
   static final int FLAG_APPEND_COLON_SPACE = 128;
   static final int FLAG_APPEND_COMMA = 256;
   final String sourceText;
   byte[] opcodes;
   int[] flags;
   int[] startOrJumpTarget;
   int[] endOffset;
   int[] sourceLine;
   int instructionCount;
   char[] syntheticChars;
   int syntheticCharCount;
   private static final String[] INDENT_CACHE = new String[32];

   JsLayoutDocument(String var1) {
      this.sourceText = var1;
      int var2 = var1.length() / 3 + 64;
      this.opcodes = new byte[var2];
      this.flags = new int[var2];
      this.startOrJumpTarget = new int[var2];
      this.endOffset = new int[var2];
      this.sourceLine = new int[var2];
      this.syntheticChars = new char[var1.length() / 4 + 64];
      this.appendSyntheticChars(" ;,");
   }

   private int appendInstruction(byte var1, int var2, int var3, int var4, int var5) {
      if (this.instructionCount == this.opcodes.length) {
         int var6 = this.instructionCount * 2;
         this.opcodes = Arrays.copyOf(this.opcodes, var6);
         this.flags = Arrays.copyOf(this.flags, var6);
         this.startOrJumpTarget = Arrays.copyOf(this.startOrJumpTarget, var6);
         this.endOffset = Arrays.copyOf(this.endOffset, var6);
         this.sourceLine = Arrays.copyOf(this.sourceLine, var6);
      }

      this.opcodes[this.instructionCount] = var1;
      this.flags[this.instructionCount] = var2;
      this.startOrJumpTarget[this.instructionCount] = var3;
      this.endOffset[this.instructionCount] = var4;
      this.sourceLine[this.instructionCount] = var5;
      return this.instructionCount++;
   }

   void appendSourceToken(int var1, int var2, int var3, int var4) {
      this.appendInstruction((byte)0, var3, var1, var2, var4);
   }

   void appendSyntheticToken(String var1, int var2, int var3) {
      int var4 = this.syntheticCharCount;
      this.appendSyntheticChars(var1);
      this.appendInstruction((byte)0, var2 | 32, var4, this.syntheticCharCount, var3);
   }

   void appendSpace() {
      this.appendInstruction((byte)0, 48, 0, 1, -1);
   }

   void appendSemicolon() {
      this.appendInstruction((byte)0, 33, 1, 2, -1);
   }

   void appendComma() {
      this.appendInstruction((byte)0, 33, 2, 3, -1);
   }

   private void appendSyntheticChars(String var1) {
      int var2 = var1.length();
      if (this.syntheticCharCount + var2 > this.syntheticChars.length) {
         this.syntheticChars = Arrays.copyOf(this.syntheticChars, Math.max(this.syntheticChars.length * 2, this.syntheticCharCount + var2));
      }

      var1.getChars(0, var2, this.syntheticChars, this.syntheticCharCount);
      this.syntheticCharCount += var2;
   }

   void appendControlOpcode(byte var1) {
      this.appendInstruction(var1, 0, 0, 0, -1);
   }

   void appendDeferredInstruction(int var1, int var2) {
      this.appendInstruction((byte)8, 0, var1, var2, -1);
   }

   int saveLayoutState() {
      return this.appendInstruction((byte)9, 0, 0, 0, -1);
   }

   void restoreLayoutState(int var1) {
      int var2 = this.appendInstruction((byte)10, 0, 0, 0, -1);
      this.startOrJumpTarget[var1] = var2;
   }

   void pushIndent(boolean var1) {
      this.appendInstruction((byte)11, var1 ? 64 : 0, 0, 0, -1);
   }

   void popIndent() {
      this.appendControlOpcode((byte)12);
   }

   int beginConditionalSkip() {
      return this.appendInstruction((byte)13, 0, 0, 0, -1);
   }

   void setConditionalSkipTarget(int var1) {
      int var2 = this.appendInstruction((byte)14, 0, 0, 0, -1);
      this.startOrJumpTarget[var1] = var2;
   }

   void closeConditionalSkipWithNoop(int var1) {
      int var2 = this.appendInstruction((byte)15, 0, 0, 0, -1);
      this.startOrJumpTarget[this.startOrJumpTarget[var1]] = var2;
   }

   char charAtInstructionOffset(int var1, int var2) {
      return (this.flags[var1] & 32) != 0 ? this.syntheticChars[var2] : this.sourceText.charAt(var2);
   }

   String instructionText(int var1) {
      return (this.flags[var1] & 32) != 0 ? new String(this.syntheticChars, this.startOrJumpTarget[var1], this.endOffset[var1] - this.startOrJumpTarget[var1]) : this.sourceText.substring(this.startOrJumpTarget[var1], this.endOffset[var1]);
   }

   String render(int var1) {
      return new io.karatelabs.ext.jsformat.JsLayoutDocument.LayoutRenderer(this, var1).render();
   }

   static {
      for (int var0 = 0; var0 < INDENT_CACHE.length; var0++) {
         INDENT_CACHE[var0] = "  ".repeat(var0);
      }
   }

   private static final class LayoutRenderer {
      final io.karatelabs.ext.jsformat.JsLayoutDocument document;
      final int lineWidth;
      char[] output;
      int outputLength;
      int[] layoutStateStack = new int[32];
      int layoutStateDepth;
      int[] indentStack = new int[32];
      int indentDepth;
      int[] deferredInstructionStack = new int[8];
      int deferredInstructionCount;
      int column;
      boolean atLineStart = true;
      int pendingIndent;
      int deferredSpaceCount;
      char previousChar = '\n';
      boolean forceBreakBeforeNextToken;
      boolean breakForbiddenAfterPreviousToken;
      boolean previousTokenNoSpaceAfter;
      int lastSignificantInstruction = -1;
      int lineNumber;
      boolean[] indentGrowthStack = new boolean[16];

      LayoutRenderer(io.karatelabs.ext.jsformat.JsLayoutDocument var1, int var2) {
         this.document = var1;
         this.lineWidth = var2;
         int var3 = var1.sourceText.length();
         this.output = new char[var3 + (var3 >> 2) + 16];
      }

      void appendChar(char var1) {
         if (this.outputLength == this.output.length) {
            this.output = Arrays.copyOf(this.output, this.outputLength * 2);
         }

         this.output[this.outputLength++] = var1;
      }

      void appendInstruction(int var1) {
         int var2 = this.document.startOrJumpTarget[var1];
         int var3 = this.document.endOffset[var1] - var2;
         if (this.outputLength + var3 > this.output.length) {
            this.output = Arrays.copyOf(this.output, Math.max(this.output.length * 2, this.outputLength + var3));
         }

         if ((this.document.flags[var1] & 32) != 0) {
            System.arraycopy(this.document.syntheticChars, var2, this.output, this.outputLength, var3);
         } else {
            this.document.sourceText.getChars(var2, var2 + var3, this.output, this.outputLength);
         }

         this.outputLength += var3;
      }

      void appendString(String var1) {
         int var2 = var1.length();
         if (this.outputLength + var2 > this.output.length) {
            this.output = Arrays.copyOf(this.output, Math.max(this.output.length * 2, this.outputLength + var2));
         }

         var1.getChars(0, var2, this.output, this.outputLength);
         this.outputLength += var2;
      }

      String render() {
         byte[] var1 = this.document.opcodes;
         int[] var2 = this.document.startOrJumpTarget;
         boolean var3 = false;
         int var4 = -1;
         int var5 = 0;
         int var6 = 0;

         for (int var7 = this.document.instructionCount; var6 < var7; var6++) {
            switch (var1[var6]) {
               case 0:
                  this.appendTokenInstruction(var6, var5);
                  break;
               case 1:
               case 2:
                  if (var3 && !this.forceBreakBeforeNextToken && this.deferredInstructionCount == 0) {
                     if (var1[var6] == 1 && !this.atLineStart) {
                        this.appendChar(' ');
                        this.previousChar = ' ';
                        this.column++;
                        this.deferredSpaceCount++;
                     }
                  } else {
                     this.appendLineBreak(var5, -1);
                  }
                  break;
               case 3:
                  this.appendLineBreak(var5, -1);
                  break;
               case 4:
                  if (!this.atLineStart) {
                     this.appendLineBreak(var5, -1);
                  }

                  this.appendChar('\n');
                  this.lineNumber++;
                  break;
               case 5:
                  if (!this.atLineStart) {
                     this.appendLineBreak(var5, -1);
                  }
                  break;
               case 6:
                  this.forceBreakBeforeNextToken = true;
                  break;
               case 7:
                  this.breakForbiddenAfterPreviousToken = true;
                  break;
               case 8:
                  if (this.deferredInstructionCount == this.deferredInstructionStack.length) {
                     this.deferredInstructionStack = Arrays.copyOf(this.deferredInstructionStack, this.deferredInstructionCount * 2);
                  }

                  this.deferredInstructionStack[this.deferredInstructionCount++] = var6;
                  break;
               case 9:
                  if (this.layoutStateDepth == this.layoutStateStack.length) {
                     this.layoutStateStack = Arrays.copyOf(this.layoutStateStack, this.layoutStateDepth * 2);
                  }

                  this.layoutStateStack[this.layoutStateDepth++] = var4 << 1 | (var3 ? 1 : 0);
                  if (!var3 || var4 != this.lineNumber) {
                     var3 = this.fitsOnLine(var6, var5);
                     var4 = this.lineNumber;
                  }
                  break;
               case 10:
                  int var8 = this.layoutStateStack[--this.layoutStateDepth];
                  var3 = (var8 & 1) != 0;
                  var4 = var8 >> 1;
                  break;
               case 11:
                  if (this.indentDepth == this.indentStack.length) {
                     this.indentStack = Arrays.copyOf(this.indentStack, this.indentDepth * 2);
                  }

                  this.indentStack[this.indentDepth++] = var5;
                  if ((this.document.flags[var6] & 64) == 0 || !var3) {
                     var5++;
                  }
                  break;
               case 12:
                  var5 = this.indentStack[--this.indentDepth];
                  break;
               case 13:
                  if (var3) {
                     var6 = var2[var6];
                  }
                  break;
               case 14:
                  var6 = var2[var6];
               case 15:
                  break;
               default:
                  throw new IllegalStateException();
            }
         }

         this.flushDeferredInstructions();
         var6 = this.outputLength;

         while (var6 > 0 && this.output[var6 - 1] == '\n') {
            var6--;
         }

         if (var6 == this.outputLength) {
            this.appendChar('\n');
         }

         this.output[var6] = '\n';
         return new String(this.output, 0, var6 + 1);
      }

      void appendTokenInstruction(int var1, int var2) {
         int var3 = this.document.startOrJumpTarget[var1];
         int var4 = this.document.endOffset[var1];
         int var5 = this.document.flags[var1];
         if (var3 != var4) {
            if (!this.atLineStart && (this.forceBreakBeforeNextToken || this.deferredInstructionCount > 0 && (var5 & 1) == 0)) {
               this.appendLineBreak(var2, var1);
            }

            this.forceBreakBeforeNextToken = false;
            if (this.atLineStart) {
               this.appendString(this.pendingIndent < io.karatelabs.ext.jsformat.JsLayoutDocument.INDENT_CACHE.length ? io.karatelabs.ext.jsformat.JsLayoutDocument.INDENT_CACHE[this.pendingIndent] : "  ".repeat(this.pendingIndent));
               this.column = this.pendingIndent * 2;
               this.atLineStart = false;
            } else if (this.deferredSpaceCount == 0 && this.needsSpaceBeforeToken(var1, var5)) {
               this.appendChar(' ');
               this.column++;
            }

            this.previousTokenNoSpaceAfter = (var5 & 2) != 0;
            this.appendInstruction(var1);
            if ((var5 & 128) != 0) {
               this.appendChar(':');
               this.appendChar(' ');
               this.column += 2;
            } else if ((var5 & 256) != 0) {
               this.appendChar(',');
               this.column++;
            }

            this.previousChar = this.output[this.outputLength - 1];
            this.deferredSpaceCount = 0;
            if ((var5 & 8) != 0) {
               this.lineNumber++;
               this.column = var4 - this.document.sourceText.lastIndexOf(10, var4 - 1) - 1;
            } else {
               this.column += var4 - var3;
            }

            if ((var5 & 16) == 0) {
               this.lastSignificantInstruction = var1;
               this.breakForbiddenAfterPreviousToken = false;
            }
         }
      }

      boolean needsSpaceBeforeToken(int var1, int var2) {
         char var3 = this.previousChar;
         char var4 = this.document.charAtInstructionOffset(var1, this.document.startOrJumpTarget[var1]);
         if (var3 == ' ' || var4 == ' ') {
            return false;
         } else if ((var2 & 2) != 0) {
            return var3 != '(' && var3 != '[';
         } else if (this.previousTokenNoSpaceAfter) {
            return var4 != ')' && var4 != ']' && var4 != ',' && var4 != ';';
         } else if (isIdentifierPart(var4)) {
            return isIdentifierPart(var3);
         } else if ((var3 == '+' || var3 == '-') && var4 == var3) {
            return true;
         } else if (var3 != '/' || var4 != '/' && var4 != '*') {
            if (var3 == '<' && var4 == '!') {
               return true;
            } else if (var4 == '.' && this.lastSignificantInstruction >= 0 && (this.document.flags[this.lastSignificantInstruction] & 4) != 0) {
               int var5 = this.document.startOrJumpTarget[this.lastSignificantInstruction];

               for (int var6 = this.document.endOffset[this.lastSignificantInstruction]; var5 < var6; var5++) {
                  char var7 = this.document.charAtInstructionOffset(this.lastSignificantInstruction, var5);
                  if (var7 == '.' || var7 == 'e' || var7 == 'E' || var7 == 'x' || var7 == 'X' || var7 == 'o' || var7 == 'O' || var7 == 'b' || var7 == 'B') {
                     return false;
                  }
               }

               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      }

      static boolean isIdentifierPart(char var0) {
         return var0 >= 'a' && var0 <= 'z'
            || var0 >= 'A' && var0 <= 'Z'
            || var0 >= '0' && var0 <= '9'
            || var0 == '_'
            || var0 == '$'
            || var0 == '#'
            || var0 == '\\'
            || var0 > 127;
      }

      void appendLineBreak(int var1, int var2) {
         if (this.breakForbiddenAfterPreviousToken) {
            String var3 = this.lastSignificantInstruction < 0 ? "" : this.document.instructionText(this.lastSignificantInstruction);
            int var4 = var2 >= 0 ? this.document.sourceLine[var2] : (this.lastSignificantInstruction < 0 ? -1 : this.document.sourceLine[this.lastSignificantInstruction]);
            throw new JsFormatException(io.karatelabs.ext.jsformat.JsFormatException.FormatErrorCode.COMMENT_UNPLACEABLE, "a comment would put a line break after '" + var3 + "', which changes the meaning", 0, 0, var4, null);
         } else {
            if (this.deferredSpaceCount > 0) {
               this.outputLength = this.outputLength - this.deferredSpaceCount;
               this.deferredSpaceCount = 0;
            }

            this.flushDeferredInstructions();
            this.appendChar('\n');
            this.previousChar = '\n';
            this.lineNumber++;
            this.atLineStart = true;
            this.pendingIndent = var1;
            this.column = var1 * 2;
            this.forceBreakBeforeNextToken = false;
            this.previousTokenNoSpaceAfter = false;
         }
      }

      void flushDeferredInstructions() {
         for (int var1 = 0; var1 < this.deferredInstructionCount; var1++) {
            this.appendChar(' ');
            this.appendInstruction(this.deferredInstructionStack[var1]);
         }

         this.deferredInstructionCount = 0;
      }

      boolean fitsOnLine(int var1, int var2) {
         if (!this.forceBreakBeforeNextToken && this.deferredInstructionCount <= 0) {
            int var3 = this.lineWidth - this.column;
            byte[] var4 = this.document.opcodes;
            int[] var5 = this.document.flags;
            int[] var6 = this.document.startOrJumpTarget;
            int[] var7 = this.document.endOffset;
            int var8 = var6[var1];
            int var9 = this.document.instructionCount;
            boolean var10 = true;
            boolean var11 = false;
            boolean var12 = false;
            boolean var13 = false;
            int var14 = 0;
            int var15 = this.layoutStateDepth - 1;
            int var16 = var2;
            int var17 = 0;

            for (int var18 = var1 + 1; var3 >= 0; var18++) {
               if (var18 == var9) {
                  return true;
               }

               if (var18 == var8) {
                  var11 = true;
                  var13 = false;
                  var10 = (this.layoutStateStack[var15] & 1) != 0;
               } else {
                  switch (var4[var18]) {
                     case 0:
                        if (!var13) {
                           if (var12 && (var5[var18] & 1) == 0) {
                              return var11;
                           }

                           if ((var5[var18] & 8) != 0) {
                              int var20 = this.document.sourceText.indexOf(10, var6[var18]);
                              if (var11 || var3 - (var20 - var6[var18]) < 0) {
                                 return var3 - (var20 - var6[var18]) >= 0;
                              }

                              var3 = this.lineWidth - (var7[var18] - this.document.sourceText.lastIndexOf(10, var7[var18] - 1) - 1);
                           } else {
                              var3 -= var7[var18] - var6[var18] + ((var5[var18] & 128) != 0 ? 2 : ((var5[var18] & 256) != 0 ? 1 : 0));
                           }
                        }
                        break;
                     case 1:
                     case 2:
                        if (!var13) {
                           if (!var10) {
                              return true;
                           }

                           if (var12) {
                              return var11;
                           }

                           if (var4[var18] == 1) {
                              var3--;
                           }
                        }
                        break;
                     case 3:
                     case 4:
                        if (var11) {
                           return true;
                        }

                        var13 = var16 > var2;
                        var12 = false;
                        var3 = this.lineWidth - var16 * 2;
                        break;
                     case 5:
                     case 6:
                        if (!var13) {
                           return var11;
                        }
                     case 7:
                     default:
                        break;
                     case 8:
                        var12 |= !var13;
                        break;
                     case 9:
                        var14++;
                        break;
                     case 10:
                        if (var14 > 0) {
                           var14--;
                        } else {
                           var15--;
                           var10 = (this.layoutStateStack[var15] & 1) != 0;
                        }
                        break;
                     case 11:
                        if (!var11) {
                           boolean var19 = (var5[var18] & 64) == 0 || !var10;
                           if (var17 == this.indentGrowthStack.length) {
                              this.indentGrowthStack = Arrays.copyOf(this.indentGrowthStack, var17 * 2);
                           }

                           this.indentGrowthStack[var17++] = var19;
                           var16 += var19 ? 1 : 0;
                        }
                        break;
                     case 12:
                        if (!var11 && var17 > 0) {
                           var17--;
                           var16 -= this.indentGrowthStack[var17] ? 1 : 0;
                        }
                        break;
                     case 13:
                        if (var10) {
                           var18 = var6[var18];
                        }
                        break;
                     case 14:
                        var18 = var6[var18];
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }
}
