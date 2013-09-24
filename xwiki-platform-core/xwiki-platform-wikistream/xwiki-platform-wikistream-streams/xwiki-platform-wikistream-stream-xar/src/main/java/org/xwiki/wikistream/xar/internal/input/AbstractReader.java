package org.xwiki.wikistream.xar.internal.input;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;

public abstract class AbstractReader
{
    private SyntaxFactory syntaxFactory;

    public AbstractReader()
    {
    }
    
    public AbstractReader(SyntaxFactory syntaxFactory)
    {
        this.syntaxFactory = syntaxFactory;
    }
    
    protected Object convert(Class< ? > type, String source) throws ParseException
    {
        Object value = source;

        if (type == Locale.class) {
            value = toLocale(source);
        } else if (type == Date.class) {
            value = new Date(Long.parseLong(source));
        } else if (type == Boolean.class) {
            value = Boolean.valueOf(source).booleanValue();
        } else if (type == Syntax.class) {
            value = this.syntaxFactory.createSyntaxFromIdString(source);
        } else if (type == Integer.class) {
            value = Integer.parseInt(source);
        }

        return value;
    }

    protected Locale toLocale(String value)
    {
        Locale locale = null;
        if (value != null) {
            String valueString = value.toString();
            if (valueString.length() == 0) {
                locale = Locale.ROOT;
            } else {
                locale = LocaleUtils.toLocale(valueString);
            }
        }

        return locale;
    }
}
