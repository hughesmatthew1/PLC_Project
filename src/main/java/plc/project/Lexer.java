package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the invalid character.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while(chars.has(0)){
            // If not whitespace, add token
            if (!match("[ \b\r\n\t]")){
                chars.skip();
                tokens.add(lexToken());
            }
        }
        return tokens;

    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[A-Za-z_]")){ // identified
            return lexIdentifier();
        }
        else if (peek("'0'") || peek("[1-9]") || peek("[+-]?", "[1-9]")){ // number
            return lexNumber();
        }
        else if (peek("'")){ // character
            return lexCharacter();
        }
        else if (peek("\"")){ // string
            return lexString();
        }
        else if (peek("[<>!=]'='?") || peek("&&") || peek("||") || peek(".")){ // operator
            return lexOperator();
        }
        throw new UnsupportedOperationException(); // In the case none of the above are met
    }

    public Token lexIdentifier() {

        match("[A-Za-z_]"); // matches first character of an identifier
        while (peek("[A-Za-z0-9_-]")){ // matches ensuing characters of an identifier
            match("[A-Za-z0-9_-]");
        }

        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek("[+-]")){ // Account for optional sign
            match("[+-]");
        }
        while (peek("[0-9]")){ // Integer / Whole number Integer, loop until non-number
            match("[0-9]");
        }
        if (!peek("\\.")){
            return chars.emit(Token.Type.INTEGER);
        }
        else{
            match("\\.");
            if(!peek("[0-9]")){
                throw new ParseException("Invalid Decimal", chars.index);
            }
            else{
                while(peek("[0-9]")){
                    match("[0-9]");
                }
            }
            return chars.emit(Token.Type.DECIMAL);
        }
    }

    public Token lexCharacter() {
        match("'"); // Matches first character of a character
        if (peek("\\\\") | peek("[^'\\n\\r]")){ // Account for valid char
            if (peek("\\\\")){ // Account for escape sequence
                lexEscape();
            }
            else{ // Match non-escape (or single quote) characters
                match("[^'\\n\\r]");
            }
            if (!match("'")){ // Ensure proper char termination
                throw new ParseException("Unterminated Character", chars.index);
            }
            return chars.emit(Token.Type.CHARACTER);
        }
        throw new ParseException("Invalid Character", chars.index);
    }

    public Token lexString() {
        match("\""); // Matches first character of a string
        while (!peek("\"")){ // Loop until closing quotations found
            if (peek("\\\\")){ // Account for escape sequences
                lexEscape();
            }
            else if (peek("[^\"\\n\\r\\\\]")){ // Account for normal characters
                match("[^\"\\n\\r\\\\]");
            }
            else{ // Account for invalid characters (ie new line)
                throw new ParseException("Invalid String Literal", chars.index);
            }
        }
        if (!peek("\"")){
            throw new ParseException("Unterminated String", chars.index);
        }
        match("\"");

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (!match("\\\\")){ // Ensure \ begins escape sequence
            throw new ParseException("Invalid Escape Sequence", chars.index);
        }
        else if (!match("[\\\\bnrt'\"]")){ // Ensure a valid escape sequence is issued

            throw new ParseException("Invalid Escape Character", chars.index);
        }
    }

    public Token lexOperator() {
        if (peek("[<>!=]")){
            match("[<>!=]");
            if(peek("=")){
                match("=");
            }
        }
        else if (peek("&&")){
            match("&&");
        }
        else if (peek("||")){
            match("||");
        }
        else if (peek(".")){
            match(".");
        }
        else{
            throw new ParseException("Invalid Operator", chars.index);
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) ||
                !String.valueOf(chars.get(i)).matches(patterns[i]) ){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
