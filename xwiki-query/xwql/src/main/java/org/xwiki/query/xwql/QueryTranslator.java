package org.xwiki.query.xwql;

public interface QueryTranslator
{
    String ROLE = QueryTranslator.class.getName();

    String getOutputLanguage();

    String translate(String statement) throws Exception;
}
