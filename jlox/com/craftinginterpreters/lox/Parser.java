package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

class Parser {

    private static class ParseError extends RuntimeException {}
    
    //the list of input tokens
    private final List<Token> tokens;
    //points to the next token to be parsed
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(statement());
        }
        return statements;
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();

        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    //expression → equality
    private Expr expression() {
        return equality();
    }

    //equality → comparison ( ( "!=" | "==" ) comparison )*
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    //term → factor ( ( "-" | "+" ) factor )*
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    //factor → unary ( ( "/" | "*" ) unary )*
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;

    }

    //unary → ( "!" | "-" ) unary | primary
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();

    }

    //primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NIL))   return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    //does the current token match any of the given types?
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            //check type - is the current token of this given type?
            if (check(type)) {
                //consume the token
                advance();
                //current token matches this type - return true
                return true;
            }
        }
        //current token did not match any of the types - return false
        return false;
    }

    //checkss if the current token is of the given type
    //does not consume the token
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    //consumes the current token and returns it
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    //checks if we've run out of tokens to parse
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    //returns the current token we have yet to consume
    private Token peek() {
        return tokens.get(current);
    }

    //return the most recently consumed token
    private Token previous() {
        return tokens.get(current - 1);
    }

    //if the next token is of the expected type, consume it (similar to match())
    //otherwise, we've hit an error
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        } 

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    //try to synchronise the state of the parser
    //by skipping tokens until we reach the end of a statement (;)
    //then, if the next token is a keyword, we're in a safe state
    private void synchronise() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();

        }
    }
}
