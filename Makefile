objects = lox/Lox.java lox/Scanner.java lox/Token.java lox/TokenType.java

run : $(objects)
	javac -d . $(objects)

clean :
	rm -rf com