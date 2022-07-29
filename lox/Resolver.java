package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // Declare the function name even before resolving the function body.
    // This allows functions to refer to themselves (recursion)!
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    // Resolve both branches of a Stmt.If node. We're doing a static analysis
    // here, so we need to be conservative and resolve any branch that _could_ run.
    resolve(stmt.condition);
    resolve(stmt.thenBranch);

    if (stmt.elseBranch != null) {
      resolve(stmt.elseBranch);
    }

    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    // We keep track of whether or not we're currently in a function during
    // resolution. If we aren't in a function, throw an error on a return stmt.
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    // We split variable binding into two steps — declaration and definition.
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    // Resolve expr.value in case it contains references to other variables.
    resolve(expr.value);
    // Resolve the variable that's being assigned to.
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    // The callee is itself an expression, so resolve it.
    resolve(expr.callee);

    // Resolve all variables in the arguments themselves.
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    // Prevent users from being able to define a variable referencing itself, e.g.
    // make an expression like var a = a; illegal. This corresponds to the case
    // where we've declared a variable but we haven't yet defined it.
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) {
      return;
    }

    Map<String, Boolean> scope = scopes.peek();
    // Prevent users from declaring a variable with the same name as an
    // existing variable in a local scope.
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Already a variable with this name in this scope.");
    }

    // Add the declared variable to the innermost scope, but mark it
    // as "not ready yet" by mapping it to false. This indicates we
    // have not yet finished resolving a variable's initializer.
    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) {
      return;
    }

    Map<String, Boolean> scope = scopes.peek();
    // Indicate that the initializer expression has been evaluated and
    // that the variable is now fully initialized and available for use.
    scope.put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    // Start at the innermost scope (top of the stack) and work our way
    // outwards (down the stack). At each scope, check for the variable of
    // interest. If we find it, resolve it and pass the number of scopes
    // between the current innermost scope and the scope where we found it.
    // Current scope = 0, immediately enclosing scope = 1, etc.
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    // Create a new scope for a function's body. Declare and define all
    // parameters of the function within this scope.
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }

    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  private void endScope() {
    scopes.pop();
  }
}