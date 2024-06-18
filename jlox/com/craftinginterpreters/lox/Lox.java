package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            //invalid number of arguments
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            //run the file specified in args[0]
            runFile(args[0]);
        } else {
            //start an interactive command line prompt
            runPrompt();
        }
    }

    //run a file, with the path specified from the command line
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        //this and runPrompt() are really wrapper methods around this main run() function
        run(new String(bytes, Charset.defaultCharset()));

        //check for errors
        if (hadError) {
            System.exit(65);
        }
    }

    //no arguments => run an interactive command prompt
    private static void runPrompt() throws IOException {
        //get user input from command line
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        //loop forever until user enters CTRL-C (=> line = null)
        while (true) {
            System.out.print("> ");
            //read line from user
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            //call run() like above
            run(line);
            
            //reset hadError flag - if the user types an incorrect line in the interactive session,
            //they should just be able to retry instead of it crashing
            hadError = false;
        }
    }

    //main run() function
    //actually does something!!
    private static void run(String source) {
        /* 
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        //print the tokens
        for (Token token : tokens) {
            System.out.println(token);
        }
        */

        System.out.println("test");
    }

    //error reporting function
    static void error(int line, String message) {
        report(line, "", message);
    }

    //error reporting helper function: print to stderr and set hadError class attribute
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

}