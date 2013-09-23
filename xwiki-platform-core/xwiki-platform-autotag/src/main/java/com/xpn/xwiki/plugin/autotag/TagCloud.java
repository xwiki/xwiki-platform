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

/**
 * Data structure used by the {@link AutoTagPlugin}, holding information about a collection of tags appearing the most
 * in a collection of documents.
 *
 * @version $Id$
 * @deprecated the entire Autotag plugin is deprecated, along with its data structures
 */
@Deprecated
public class TagCloud
{
    /** The text from which to extract tags. */
    private String text;

    /** The represented text, split into a sequence of words (tokens). */
    private String[] wordList;

    /** Words and their frequency, i.e. how many times they appear in the text. */
    private Map<String, Integer> countedWordMap;

    /** Word frequencies grouped based on their common stem. */
    private Map<String, Map<String, Integer>> stemmedWordMap;

    /** Lead words and their frequencies, i.e. how many times they appear in the text. */
    private Map<String, Integer> stemmedWordFreqMap;

    /** The extracted tags. */
    private Set<Tag> tags;

    /**
     * Get the HTML markup to represent this tag cloud.
     *
     * @return HTML markup
     */
    public String getHtml()
    {
        StringBuilder result = new StringBuilder();
        for (Tag t : this.tags) {
            result.append(t.getHtml());
        }
        return result.toString();
    }

    /**
     * @return the analyzed text
     * @see #text
     */
    public String getText()
    {
        return this.text;
    }

    /**
     * @param text the text to analyze
     * @see #text
     */
    public void setText(String text)
    {
        this.text = text;
    }

    /**
     * @return the tokenized text
     * @see #wordList
     */
    public String[] getWordList()
    {
        return this.wordList;
    }

    /**
     * @param wordList the tokenized text
     * @see #wordList
     */
    public void setWordList(String[] wordList)
    {
        this.wordList = wordList;
    }

    /**
     * @return map of {@code token->number of appearances} count for each token present in the text
     * @see #countedWordMap
     */
    public Map<String, Integer> getCountedWordMap()
    {
        return this.countedWordMap;
    }

    /**
     * @param countedWordMap map of {@code token->number of appearances} count for each token present in the text
     * @see #countedWordMap
     */
    public void setCountedWordMap(Map<String, Integer> countedWordMap)
    {
        this.countedWordMap = countedWordMap;
    }

    /**
     * @return token groups
     * @see #stemmedWordMap
     */
    public Map<String, Map<String, Integer>> getStemmedWordMap()
    {
        return this.stemmedWordMap;
    }

    /**
     * @param stemmedWordMap token groups
     * @see #stemmedWordMap
     */
    public void setStemmedWordMap(Map<String, Map<String, Integer>> stemmedWordMap)
    {
        this.stemmedWordMap = stemmedWordMap;
    }

    /**
     * @return map of lead words and their frequencies
     * @see #stemmedWordFreqMap
     */
    public Map<String, Integer> getStemmedWordFreqMap()
    {
        return this.stemmedWordFreqMap;
    }

    /**
     * @param stemmedWordFreqMap map of lead words and their frequencies
     * @see #stemmedWordFreqMap
     */
    public void setStemmedWordFreqMap(Map<String, Integer> stemmedWordFreqMap)
    {
        this.stemmedWordFreqMap = stemmedWordFreqMap;
    }

    /**
     * @return the set of extracted tags
     * @see #tags
     */
    public Set<Tag> getTags()
    {
        return this.tags;
    }

    /**
     * @param tags the set of extracted tags
     * @see #tags
     */
    public void setTags(Set<Tag> tags)
    {
        this.tags = tags;
    }
}
