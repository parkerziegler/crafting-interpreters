package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0; // Points to first character in lexeme being scanned.
  private int current = 0; // Points at the character being considered.
  private int line = 1; // Tracks what line current is on.
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap();
    keywords.put("and", AND);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
  }


  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while(!isAtEnd()) {
      // We start at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        // If we find a second slash, we have a comment!
        // Scan all characters through the end of the line.
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n':
        // Increment the line counter while skipping the newline character.
        line++;
        break;
      case '"': string(); break;
      default:
        // Handle number literals in the default block to avoid writing out
        // all digit cases.
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Lox.error(line, "Unsupported character.");
        }
        break;
    }
  }

  private boolean match(char expected) {
    // match acts like a conditional form of advance below.
    // It allows us to check for two character lexemes that match a second character.
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  private char peek() {
    // peek implements lookahead.
    // We look ahead at the current character without consuming it in the scanner.
    // In our case, we implement single character lookahead.
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    // peekNext implements two character lookahead.
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private char advance() {
    // Consume the next character in the source file and return it.
    return source.charAt(current++);
  }

  private void string() {
    // Continue advancing through characters of the string as long as
    // we are not add the closing " character or EOF.
    while (peek() != '"' && !isAtEnd()) {
      // Handle multiline strings with newline characters.
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // The closing " character.
    advance();

    // Trim surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part of the number.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the '.' character.
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER,
      Double.parseDouble(source.substring(start, current)));
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
      (c >= 'A' && c <= 'Z') ||
      c == '_';
  }

  private void identifier() {
    while (isAlpha(peek())) advance();

    String text = source.substring(start, current);
    // Check if the alpha lexeme is a keyword.
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;

    addToken(type);
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    // Create a new token for a given lexeme.
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}