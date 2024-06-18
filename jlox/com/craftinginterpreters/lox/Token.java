package com.craftinginterpreters.lox;

class Token {
    final TokenType type; //from TokenType enum
    final String lexeme; // 'var', 'a', '=' '"abc"', ';', ...
    final Object literal; //'12', ... , or a variable name
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
