package parser;

import error.Reporter;
import error.TranslateBencodeException;
import lexer.Token;
import lexer.TokenType;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private final Reporter reporter;

    private int position;

    private Parser(List<Token> tokens, Reporter reporter) {
        this.tokens = tokens;
        this.reporter = reporter;
    }

    public static List<Expr> parse(List<Token> tokens, Reporter reporter) {
        Parser parser = new Parser(tokens, reporter);
        return parser.parse();
    }

    private List<Expr> parse() {
        List<Expr> expressions = new ArrayList<>();

        while (position < tokens.size()) {
            try {
                expressions.add(parseExpr());
            } catch (TranslateBencodeException e) {
                reporter.report(e.getMessage());
                return null;
            }
        }
        return expressions;
    }

    private Expr parseExpr() throws TranslateBencodeException {
        position++;

        return switch (tokens.get(position - 1).tokenType()) {
            case LIST -> parseList();
            case DICTIONARY -> parseDictionary();
            case STRING -> new Expr.Line((String) tokens.get(position - 1).value());
            case INTEGER -> new Expr.Number((Integer) tokens.get(position - 1).value());
            default -> {
                String message = unexpectedToken("Expected value",
                        tokens.get(position - 1),
                        TokenType.INTEGER, TokenType.STRING, TokenType.LIST, TokenType.DICTIONARY);

                throw new TranslateBencodeException(message);
            }
        };
    }

    private Expr parseList() throws TranslateBencodeException {
        int startType = position - 1;

        List<Expr> list = new ArrayList<>();

        while (position < tokens.size()) {
            if (tokens.get(position).tokenType() == TokenType.TYPE_END) {
                position++;
                return new Expr.Array(list);
            }

            list.add(parseExpr());
        }

        throw new TranslateBencodeException(unexpectedToken("No end complex char, complex type:",
                                                            tokens.get(startType),
                                                            TokenType.TYPE_END));
    }

    private Expr parseDictionary() throws TranslateBencodeException {
        int startType = position - 1;

        LinkedHashMap<String, Expr> map = new LinkedHashMap<>();

        while (position < tokens.size()) {
            if (tokens.get(position).tokenType() == TokenType.TYPE_END) {
                position++;
                return new Expr.Dictionary(map);
            }

            String key = addKey(map);

            if (position >= tokens.size()) {
                String message = "Expected value and end complex type in end of file";
                throw new TranslateBencodeException(message);
            }

            Expr value = parseExpr();
            map.put(key, value);
        }

        throw new TranslateBencodeException(unexpectedToken("No end complex char, complex type:",
                                                            tokens.get(startType),
                                                            TokenType.TYPE_END));
    }

    private String addKey(LinkedHashMap<String, Expr> map) throws TranslateBencodeException {
        if (tokens.get(position).tokenType() != TokenType.STRING) {
            String message = unexpectedToken("Invalid key", tokens.get(position), TokenType.STRING);
            throw new TranslateBencodeException(message);
        }

        String key = (String) tokens.get(position).value();

        if (map.containsKey(key)) {
            String message = unexpectedToken("Repeating key", tokens.get(position), TokenType.STRING);
            throw new TranslateBencodeException(message);
        }

        if (map.size() == 0) {
            position++;
            return key;
        }

        String lastKeyMap = (String) map.keySet().toArray()[map.size() - 1];

        if (lastKeyMap.compareTo(key) >= 0) {
            String message = unexpectedToken("Wrong key order", tokens.get(position), TokenType.STRING);
            throw new TranslateBencodeException(message);
        }

        position++;
        return key;
    }

    private String unexpectedToken(String message, Token token, TokenType... expected) {
        String position = "Line " + token.nLine() + ", position: " + token.pos();
        return """
                %s
                %s
                Expected tokens: %s,
                Actual: %s
                """.formatted(message, position, Arrays.toString(expected), token.tokenType());
    }
}
