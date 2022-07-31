package com.craftinginterpreters.lox;

import java.util.List;

import com.craftinginterpreters.lox.Environment;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  // The closure Environment "closes over" scope at the function's
  // declaration site. This allows us to use local functions (functions)
  // defined in the body of other functions.
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.isInitializer = isInitializer;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      // If we're in an initializer and execute a return statment with no value,
      // return "this".
      if (isInitializer) {
        return closure.getAt(0, "this");
      }

      return returnValue.value;
    }

    // If the LoxFunction instance is an initializer, return "this".
    if (isInitializer) {
      return closure.getAt(0, "this");
    }
    return null;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    // Define the "this" keyword so that the method invoked on the instance
    // can find the instance in its environment.
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer);
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }

}
