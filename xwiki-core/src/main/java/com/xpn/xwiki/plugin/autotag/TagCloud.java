/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
