package com.xpn.xwiki.plugin.autotag;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;

public class TagCloud
{
    private String[] wordList;

    private String text;

    private Map countedWordMap;

    private Map stemmedWordMap;

    private Map stemmedWordFreqMap;

    public Set getTags()
    {
        return tags;
    }

    public void setTags(Set tags)
    {
        this.tags = tags;
    }

    private Set tags;

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String[] getWordList()
    {
        return wordList;
    }

    public void setWordList(String[] wordList)
    {
        this.wordList = wordList;
    }

    public Map getCountedWordMap()
    {
        return countedWordMap;
    }

    public void setCountedWordMap(Map countedWordMap)
    {
        this.countedWordMap = countedWordMap;
    }

    public Map getStemmedWordMap()
    {
        return stemmedWordMap;
    }

    public void setStemmedWordMap(Map stemmedWordMap)
    {
        this.stemmedWordMap = stemmedWordMap;
    }

    public Map getStemmedWordFreqMap()
    {
        return stemmedWordFreqMap;
    }

    public void setStemmedWordFreqMap(Map stemmedWordFreqMap)
    {
        this.stemmedWordFreqMap = stemmedWordFreqMap;
    }

    public String getHtml()
    {
        StringBuffer strb = new StringBuffer();
        Iterator it = tags.iterator();
        while (it.hasNext()) {
            strb.append(((Tag) it.next()).getHtml());
        }
        return strb.toString();
    }
}
