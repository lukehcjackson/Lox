package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/*
 * Automatically generates our abstract syntax tree(s)
 * so we don't have to do it by hand
 */
public class GenerateAST {
    
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
        System.out.println("Usage: generate_ast <output-directory>");
        System.exit(64);
        }

        //get output directory from command line
        String outputDir = args[0];

        //define the abstract syntax tree for expressions:
        //they can be of one of four types
        //binary expressions take two expressions, one on either side of an operator
        //grouping - i.e brackets () - just surround one expression
        //literal - i.e. x - has a value
        //unary - i.e. -10 - has an operator and a right operand
        defineAst(outputDir, "Expr", Arrays.asList (
            "Binary     : Expr left, Token operator, Expr right",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Unary      : Token operator, Expr right"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
            "Expression : Expr expression",
            "Print      : Expr expression"
        ));

    }


    //actual defineAst method
    //writes the abstract syntax tree to a file ('baseName'.java)
    private static void defineAst(
            String outputDir, String baseName, List<String> types)
            throws IOException {

        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {" );

        defineVisitor(writer, baseName, types);

        //The AST classes
        for (String type : types) {
            //get everything before the : and remove whitespace => className (i.e. "Binary")
            String className = type.split(":")[0].trim();
            //everything after => fields (i.e. "Expr left, Token operator, Expr right")
            String fields = type.split(":")[1].trim();
            //call the actual method to write the class definition for this type
            defineType(writer, baseName, className, fields);
        }

        //the base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();

    }

    private static void defineType(
            PrintWriter writer, String baseName, String className, String fieldList) {

        writer.println("  static class " + className + " extends " + baseName + "{");

        //constructor
        writer.println("    " + className + "(" + fieldList + ") {");
        //store parameters in fields
        String[] fields = fieldList.split(", ");
        //fields is ["Expr left", "Token operator", "Expr right"] for instance
        for (String field : fields) {
            //name would be 'left', 'operator', 'right', ...
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }

        writer.println("    }");

        //visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("        return visitor.visit" + 
            className + baseName + "(this);");
        writer.println("    }");

        //fields
        for (String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("  }");
        
    }

    private static void defineVisitor(
            PrintWriter writer, String baseName, List<String> types) {

        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" +
                typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");

    }

}

