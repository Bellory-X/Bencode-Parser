package lexer;
import error.ConsoleReporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final BufferedReader br;
    private final ConsoleReporter reporter;
    private final List<Token> tokens = new ArrayList<>();

    private String line;
    private int nLine;
    private int position;

    private Lexer(BufferedReader br, ConsoleReporter reporter) {
        this.br = br;
        this.reporter = reporter;
    }

    public static List<Token> scan(BufferedReader br, ConsoleReporter reporter) {
        Lexer lexer = new Lexer(br, reporter);
        return lexer.scan();
    }

    private List<Token> scan() {
        while ((line = getLine()) != null) {
            nLine++;
            position = 0;
            while (position < line.length()) {
                char c = line.charAt(position);
                if (Character.isWhitespace(c)) {
                    position++;
                    continue;
                }
                if (isDigit(c)) {
                    if (!addString()) return null;
                    continue;
                }
                if (!valueType(c)) {
                    return null;
                }
            }
        }
        tokens.add(new Token(TokenType.EOF, nLine, position, null));
        return tokens;
    }

    private boolean textProcessing() {
        do {
            while (position < line.length()) {
                char c = line.charAt(position);
                if (c == 'e') {
                    position++;
                    tokens.add(new Token(TokenType.TYPE_END, nLine, position, null));
                    return true;
                }
                if (Character.isWhitespace(c)) {
                    position++;
                    continue;
                }
                if (isDigit(c)) {
                    if (!addString()) {
                        return false;
                    }
                    continue;
                }
                if (!valueType(c)) {
                    return false;
                }
            }
            nLine++;
            position = 0;
        } while ((line = getLine()) != null);
        return false;
    }

    private boolean valueType(char c) {
        position++;
        switch (c) {
            case 'i' -> {
                return addNumber();
            }
            case 'l' -> {
                return addList();
            }
            case 'd' -> {
                return addDictionary();
            }
            default -> {
                position--;
                reporter.report(unknownChar());
                return false;
            }
        }
    }

    private boolean addDictionary() {
        tokens.add(new Token(TokenType.DICTIONARY, nLine, position, null));
        return textProcessing();
    }

    private boolean addList() {
        tokens.add(new Token(TokenType.LIST, nLine, position, null));
        return textProcessing();
    }

    private boolean addNumber() {
        String number = getNumber('e');
        if (number != null) {
            tokens.add(new Token(TokenType.INTEGER, nLine, position, number));
            return true;
        }
        reporter.report(unknownChar());
        return false;
    }

    private boolean addString() {
        String number = getNumber(':');
        if (number != null) {
            int size = Integer.parseInt(number);
            int start = position;
            do {
                position++;
                size--;
            } while (size > 0 && position < line.length());
            String value = line.substring(start, position);
            if (size == 0) {
                tokens.add(new Token(TokenType.STRING, nLine, position, value));
                return true;
            }
        }
        reporter.report(unknownChar());
        return true;
    }

    private String unknownChar() {
        return """
                Unknown char '%c' at line %d:
                %s
                %s^--- here
                """.formatted(line.charAt(position), nLine, line, " ".repeat(position));
    }

    private String getNumber(char end) {
        int start = position;
        while (line.charAt(position) != end) {
            if (!isDigit(line.charAt(position))) {
                return null;
            }
            position++;
        }
        String result = line.substring(start, position);
        position++;
        return result;
    }

    private String getLine() {
        try {
            return br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}