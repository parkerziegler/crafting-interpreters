package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  // Execute lox code from a source file on disk.
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
  }

  // Execute lox code interactively, as you would in a REPL.
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      // readLine will return null when encountering a Ctrl-D EOF condition.
      // Handle this case and break out of the loop.
      if (line == null) break;
      run(line);
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> = tokens = scanner.scanTokens();

    for (Token token : tokens) {
      System.out.println(token);
    }
  }
}
