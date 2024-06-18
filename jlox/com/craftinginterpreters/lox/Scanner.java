package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
    private final String source; //raw source code
    private final List<Token> tokens = new ArrayList<>(); //list to fill with tokens we will generate from source

    //fields to keep track of where the scanner is in the source code
    //start and current are offsets that index into the string ('source')
    //line tracks the current line of the source code
    private int start = 0;
    private int current = 0;
    private int line = 1;

    //define a map of reserved keywords
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

    //main scan function
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            //We are at the beginning of the next lexeme
            start = current;
            //Each iteration, scan one token
            scanToken();

        }

        //append an EOF token to the end of the token list
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        
        //do we ever scan the first token since we immediately look at current++ ??
        char c = advance();

        switch (c) {
            //Single character lexemes
            case '(' : addToken(LEFT_PAREN) ; break;
            case ')' : addToken(RIGHT_PAREN); break;
            case '{' : addToken(LEFT_BRACE) ; break;
            case '}' : addToken(RIGHT_BRACE); break;
            case ',' : addToken(COMMA)      ; break;
            case '.' : addToken(DOT)        ; break;
            case '-' : addToken(MINUS)      ; break;
            case '+' : addToken(PLUS)       ; break;
            case ';' : addToken(SEMICOLON)  ; break;
            case '*' : addToken(STAR)       ; break;

            //Two-character lexemes
            //in the case of !, this character could be just an ! or part of !=, so we need to check two characters and add the right token
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
                if (match('/')) {
                    // two slashes => comment goes from here until the end of the line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance(); //keep consuming characters until we reach the end of the line
                        //do not add a token for comments
                    }
                } else {
                    addToken(SLASH);
                }
                break;
            
            //handle newlines, whitespace, ...
            case ' ' :
            case '\r':
            case '\t':
                break;
            
            case '\n':
                line++;
                break;

            //handle string literals
            case '"': string(); break;

            default:
            if (isDigit(c)) {
                //numeric literal
                number();
            } else if (isAlpha(c)) {
                //assume this 'word' is an identifier
                identifier();
            } else {
                //We get an input token we don't recognise, like '@'
                //We will still continue scanning! But calling error() means the code isn't executed (as hasError is set)
                Lox.error(line, "Unexpected character");
            }
            break;
            
        }
    }

    //have we consumed all of the characters of the source code?
    private boolean isAtEnd() {
        return current >= source.length();
    }

    //get next character in source code string
    private char advance() {
        return source.charAt(current++);
    }

    //does the next character in the lexeme match our expectation?
    private boolean match(char expected) {
        //on the last character of the source code => return false, as there's nothing left to scan
        if (isAtEnd()) return false;
        //if the next character != expected => return false (why is this right? is this not looking at the current character still?)
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    //similar to advance() but doesn't consume the character
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    //handles a string literal
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            //the next character is not a "
            if (peek() == '\n') {
                //if we hit a newline character increment line
                //(needed because we support multiline strings (but this isn't WHY we support them, just a consequence))
                line++;
            }

            advance();
        }

        //end of the source code with no closing "
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
            return;
        }

        //we get the closing "
        advance();

        //Trim the surrounding quotes from the string
        //if we supported escape characters we would have to unescape them here as well
        String trimmedString = source.substring(start+1, current-1);
        addToken(STRING, trimmedString);
    }

    //classifies character as 0-9
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    //handles a numeric literal
    private void number() {
        //consume digits
        while (isDigit(peek())) {
            advance();
        }

        //look for a fractional part i.e. '.'
        //peeknext => lookahead by 2 characters as we have to look after the decimal point
        if (peek() == '.' && isDigit(peekNext())) {
            //consume the '.'
            advance();
            //keep consuming digits
            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start,current)));

    }

    //lookahead by 2 characters
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    //classifies character as a-z or A-Z or _
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c == '_');
    }

    //classifies character as alphabetical or numeric
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    //handles identifiers
    private void identifier() {
        //read alphanumeric characters
        while (isAlphaNumeric(peek())) {
            advance();
        }

        //check to see if the identifier is a reserved keyword
        String text = source.substring(start, current);
        TokenType type = keywords.get(text); //if this lookup fails it will return 'null'
        if (type == null) {
            type = IDENTIFIER;
        }
        addToken(type);

    }

    //helper function for addToken(type, literal)
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    //add token to token list
    private void addToken(TokenType type, Object literal) {
        //get current lexeme
        String text = source.substring(start, current);
        //add to token list
        tokens.add(new Token(type, text, literal, line));
    }



}
