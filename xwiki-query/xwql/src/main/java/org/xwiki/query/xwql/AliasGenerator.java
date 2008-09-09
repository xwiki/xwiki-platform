package org.xwiki.query.xwql;

public class AliasGenerator
{
    int count = 0;

    public String generate(String prefix) {
        ++ count;
        return prefix+count;
    }
}
