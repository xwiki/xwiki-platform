package com.xpn.xwiki.plugin.autotag;

import java.util.Map;
import java.util.Set;

public class TagCloud
{
    private String[] wordList;

    private String text;

    private Set<Tag> tags;

    private Map<String, Integer> countedWordMap;

    private Map<String, Map<String, Integer>> stemmedWordMap;

    private Map<String, Integer> stemmedWordFreqMap;

    public Set<Tag> getTags()
    {
        return this.tags;
    }

    public void setTags(Set<Tag> tags)
    {
        this.tags = tags;
    }

    public String getText()
    {
        return this.text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public String[] getWordList()
    {
        return this.wordList;
    }

    public void setWordList(String[] wordList)
    {
        this.wordList = wordList;
    }

    public Map<String, Integer> getCountedWordMap()
    {
        return this.countedWordMap;
    }

    public void setCountedWordMap(Map<String, Integer> countedWordMap)
    {
        this.countedWordMap = countedWordMap;
    }

    public Map<String, Map<String, Integer>> getStemmedWordMap()
    {
        return this.stemmedWordMap;
    }

    public void setStemmedWordMap(Map<String, Map<String, Integer>> stemmedWordMap)
    {
        this.stemmedWordMap = stemmedWordMap;
    }

    public Map<String, Integer> getStemmedWordFreqMap()
    {
        return this.stemmedWordFreqMap;
    }

    public void setStemmedWordFreqMap(Map<String, Integer> stemmedWordFreqMap)
    {
        this.stemmedWordFreqMap = stemmedWordFreqMap;
    }

    public String getHtml()
    {
        StringBuilder result = new StringBuilder();
        for (Tag t : this.tags) {
            result.append(t.getHtml());
        }
        return result.toString();
    }
}
