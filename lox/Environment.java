package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  // no-argument constructor for the global scope.
  Environment() {
    enclosing = null;
  }

  // argument constructor for the local scope.
  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  // Variable definition — bind a variable name to a particular value.
  // Note that variable definition does not check to see if a variable
  // of the current name already exists. This is a semantic choice allowing
  // for variables to be arbitarily overriden.
  void define(String name, Object value) {
    values.put(name, value);
  }

  // Variable lookup — find the value associated with a variable.
  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }

    // Lookup the variable name in enclosing scopes.
    if (enclosing != null) {
      return enclosing.get(name);
    }

    // Throw a RuntimeError on undefined variables. We want to allow programs
    // to refer to variables without immediately evaluating those variables; this
    // is especially helpful for recursive and mutually recursive functions.
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    // If the variable being assigned to isn't in this environment,
    // check enclosing environments.
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    System.out.println("About to throw in assign.");

    // Throw an error if we attempt to assign to an undefined variable.
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
