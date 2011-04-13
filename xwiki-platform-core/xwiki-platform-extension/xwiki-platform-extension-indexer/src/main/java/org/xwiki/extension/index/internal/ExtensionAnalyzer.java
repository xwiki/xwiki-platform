package org.xwiki.extension.index.internal;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

public class ExtensionAnalyzer extends Analyzer
{
    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        return getTokenizer(fieldName, reader);
    }

    protected Tokenizer getTokenizer(String fieldName, Reader reader)
    {
        return new LetterOrDigitTokenizer(reader);
    }

    //

    public static class LetterOrDigitTokenizer extends CharTokenizer
    {
        public LetterOrDigitTokenizer(Reader in)
        {
            super(in);
        }

        @Override
        protected boolean isTokenChar(char c)
        {
            return Character.isLetterOrDigit(c);
        }

        @Override
        protected char normalize(char c)
        {
            return Character.toLowerCase(c);
        }
    }
}
