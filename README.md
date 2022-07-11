# Crafting Interpreters

This repo contains my implementation of the interpreters developed in the excellent [Crafting Interpreters](http://craftinginterpreters.com/) by [Bob Nystrom](https://journal.stuffwithstuff.com/).

## Build setup

The build process for the Java interpreter, `jlox`, is pretty lightweight — just a Makefile! The current build commands include:

- `make run` — compiles all Java files in the `lox` directory, which includes the core code for the `jlox` interpreter.
- `make clean` — cleans the output build directory, `com`.
- `make ast` — compiles and invokes [the metaprogramming tool developed in Chapter 5](http://craftinginterpreters.com/representing-code.html#metaprogramming-the-trees) used to generate different AST node types (classes).
- `make ast-printer` — compiles and invokes the AST printer to render a Scheme-like representation of the Lox program's AST.

## Running the interpreter

Currently, the interpreter works by being invoked directly from the command line. To start, make sure you've compiled the Java source by running `make run`. Then, to invoke `jlox`, run the following command:

```sh
java com.craftinginterpreters.lox.Lox
```

This will drop you into an interactive top-level that allows you to execute statements and evaluate expressions in a REPL-like interface. To run a Lox program written in a `.lox` file, pass the file as a second argument, like so:

```sh
java com.craftinginterpreters.lox.Lox Sample.lox
```
