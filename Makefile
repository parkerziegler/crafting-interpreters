objects = lox/*.java

run : $(objects)
	javac -d . $(objects)

clean :
	rm -rf com

ast : tool/GenerateAst.java
	javac -d . tool/GenerateAst.java
	java com.craftinginterpreters.tool.GenerateAst lox

ast-printer : lox/AstPrinter.java $(objects)
	javac -d . lox/AstPrinter.java $(objects)
	java com.craftinginterpreters.lox.AstPrinter