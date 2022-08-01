package com.craftinginterpreters.lox;

class AstPrinter implements Expr.Visitor<String> {
  String print(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize("=", new Expr.Variable(expr.name), expr.value);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    Expr[] exprs = new Expr[expr.arguments.size() + 1];
    for (int i = 0; i < expr.arguments.size() + 1; i++) {
      if (i == 0) {
        exprs[i] = expr.callee;
      } else {
        exprs[i] = expr.arguments.get(i - 1);
      }
    }

    return parenthesize("call", exprs);
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return parenthesize("get " + expr.name.lexeme, expr.object);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) {
      return "nil";
    }

    return expr.value.toString();
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize("set " + expr.name.lexeme, expr.object, expr.value);
  }

  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return "(" + expr.keyword.lexeme + " " + expr.method.lexeme + ")";
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return expr.keyword.lexeme;
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      // Here is the recursive step in the printing. We call .accept()
      // on each subexpression, which triggers a call to its visitor.
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  public static void main(String[] args) {
    Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(
            new Expr.Literal(45.67)));

    System.out.println(new AstPrinter().print(expression));
  }
}
