package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();

    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  // statement → exprStmt | ifStmt | printStmt | whileStmt | block;
  private Stmt statement() {
    if (match(IF)) {
      return ifStatement();
    }

    if (match(PRINT)) {
      return printStatement();
    }

    if (match(WHILE)) {
      return whileStatement();
    }

    if (match(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    return expressionStatement();
  }

  // declaration → varDecl | statement;
  private Stmt declaration() {
    try {
      // Check if we're declaring a variable.
      // If not, fall through to another statement type.
      if (match(VAR)) {
        return varDeclaration();
      }

      return statement();
    } catch (ParseError error) {
      synchronize();

      return null;
    }
  }

  // exprStmt → expression ";";
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");

    return new Stmt.Expression(expr);
  }

  // ifStmt → "if" "(" expression ")" statement ( "else" statement )?;
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    // Since we call statement here, we may recurse into another ifStatement() call.
    // For example, in a statement like:
    // if (first) if (second) whenTrue(); else whenFalse();
    // we'll call ifStatement twice. The innermost if claims the optional else to
    // avoid the dangling else problem.
    Stmt thenBranch = statement();
    Stmt elseBranch = null;

    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  // printStmt → "print" expression ";";
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value.");

    return new Stmt.Print(value);
  }

  // whileStmt → "while" "(" expression ")" statement;
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after while condition.");

    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  // block → "{" declaration* "}";
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  // varDecl → "var" IDENTIFIER ( "=" expression )? ";";
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  // expression → assignment;
  private Expr expression() {
    return assignment();
  }

  // assignment → IDENTIFIER "=" assignment | logic_or;
  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      // Assignment is right-associative, so we recursively call assignment
      // to ensure we've executed rightmost assignment expressions first.
      Expr value = assignment();

      // Look at the LHS of the assignment expression and ensure we're working with an
      // assignment target, Expr.Variable. Every valid assignment target is itself
      // just an expression (e.g. consider newPoint(x + 2, 0).y = 3, where the LHS is
      // itself an expression).
      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  // logic_or → logic_and ( "or" logic_and )* ;
  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  // logic_and → equality ( "and" equality )* ;
  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  // equality → comparison ( ( "!=" | "==" ) comparison )*;
  private Expr equality() {
    // Left comparison non-terminal call.
    Expr expr = comparison();

    // Sequence of ( ( "!="" and "==") comparison )* corresponds to a while loop.
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // term → factor ( ( "-" | "+" ) factor )* ;
  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // factor → unary ( ( "/" | "*" ) unary )* ;
  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  // unary → ( "!" | "-" ) unary
  // | primary ;
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  // primary → NUMBER | STRING | "true" | "false" | "nil"
  // | "(" expression ")" | IDENTIFIER;
  private Expr primary() {
    if (match(FALSE)) {
      return new Expr.Literal(false);
    }
    if (match(TRUE)) {
      return new Expr.Literal(true);
    }
    if (match(NIL)) {
      return new Expr.Literal(null);
    }
    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    // For the case of parentheses, we _must_ find a RIGHT_PAREN token
    // after an opening LEFT_PAREN token.
    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  // If we don't match one of a list of token types, then we know the current
  // production has completed.
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  // Check if a token matches the next token. We use peek to look at the token
  // without consuming it.
  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }

    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }

    return previous();
  }

  // Check if we've run out of tokens to parse.
  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  // Returns the current token we have yet to consume.
  private Token peek() {
    return tokens.get(current);
  }

  // Returns the token we previously consumed.
  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) {
      return advance();
    }

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token.line, message);
    return new ParseError();
  }

  // Synchronize discards tokens until we're at the beginning of the next
  // statement. We make some inferences that the next statement will either
  // be _after_ a SEMICOLON token _or_ if the next token is a class, function,
  // variable, for loop, etc.
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}