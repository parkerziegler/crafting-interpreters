package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
  private final Map<String, Object> values = new HashMap<>();

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

    // Throw a RuntimeError on undefined variables. We want to allow programs
    // to refer to variables without immediately evaluating those variables; this
    // is especially helpful for recursive and mutually recursive functions.
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name)) {
      values.put(name, value);
      return;
    }

    // Throw an error if we attempt to assign to an undefined variable.
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
}
