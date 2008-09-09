package org.xwiki.query.jpql;

import java.io.PushbackReader;
import java.io.StringReader;

import org.xwiki.query.jpql.lexer.Lexer;
import org.xwiki.query.jpql.node.Start;
import org.xwiki.query.jpql.parser.Parser;

public class JPQLParser
{
    public Start parse(String stmt) throws Exception {
        Parser p =
            new Parser(
                new Lexer(
                    new PushbackReader(
                        new StringReader(stmt))));
        Start start = p.parse();
        return start;
    }
}
