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
package com.xpn.xwiki.gwt.api.client.widgets;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Set;

/**
 * SuggestOracle to suggest a list of words with respect to a given list of separators.
 *
 * The <tt>SuggestOracle</tt> used for a single word suggestion is set through
 * {@link com.xpn.xwiki.gwt.api.client.widgets.WordListSuggestOracle#setWordOracle(com.google.gwt.user.client.ui.SuggestOracle)}
 * and the separators through {@link com.xpn.xwiki.gwt.api.client.widgets.WordListSuggestOracle#setSeparators(String)}.
 * Whether this oracle generates unique suggestions with respect to the already typed list, can 
 * be configured through {@link WordListSuggestOracle#setSuggestUnique(boolean)}.
 */
public class WordListSuggestOracle extends SuggestOracle {
    /**
     * oracle to use for suggesting a single word
     */
    protected SuggestOracle wordOracle;
    /**
     * multiple words separator
     */
    protected String separators;
    /**
     * whether this oracle omits words that already are in the typed list or not
     */
    protected boolean suggestUnique;

    /**
     * Default constructor: builds an oracle with an empty {@link com.google.gwt.user.client.ui.SuggestOracle} 
     * and the space as a separator. 
     * By default, this oracle generates suggestions that duplicate existing words in the list.
     */
    public WordListSuggestOracle() {
        this(new MultiWordSuggestOracle(), " ", false);
    }

    /**
     * Builds an oracle with the specified {@link com.google.gwt.user.client.ui.SuggestOracle} and
     * the space as a default separator. By default, this oracle generates suggestions that
     * duplicate existing words in the list.
     * 
     * @param wordOracle the oracle to be used for predicting a single word.
     */
    public WordListSuggestOracle(SuggestOracle wordOracle) {
        this(wordOracle, " ", false);
    }

    /**
     * Builds an wordOracle with the specified {@link com.google.gwt.user.client.ui.SuggestOracle}
     * and separators. By default, this oracle generates suggestions that duplicate existing words
     * in the list.
     * 
     * @param wordOracle the wordOracle to be used for predicting a single word
     * @param separators the separators between the words in the word list
     */
    public WordListSuggestOracle(SuggestOracle wordOracle, String separators) {
        this(wordOracle, separators, false);
    }

    /**
     * Builds an wordOracle with the specified {@link com.google.gwt.user.client.ui.SuggestOracle},
     * separators and specifying weather it suggests words that are already in the list or not.
     * 
     * @param wordOracle the wordOracle to be used for predicting a single word
     * @param separators the separators between the words in the word list
     */
    public WordListSuggestOracle(SuggestOracle wordOracle, String separators,
        boolean suggestUnique) {
        this.setWordOracle(wordOracle);
        this.setSeparators(separators);
        this.setSuggestUnique(suggestUnique);
    }

    /**
     * Returns a list of suggestions for the last word in the list.
     *
     * @param request
     * @param callback
     */
    @Override
    public void requestSuggestions(Request request, final Callback callback) {
        //get query
        String query = request.getQuery();
        //extract the last part of it
        int lastSeparator = -1;
        for (int i = 0; i < this.separators.length(); i++) {
            int currentSeparator = query.lastIndexOf(this.separators.charAt(i));
            if (currentSeparator > lastSeparator) {
                lastSeparator = currentSeparator;
            }
        }
        String oneWordQuery = query.substring(lastSeparator + 1);
        final String prefix = query.substring(0, lastSeparator + 1);    
        // If this oracle is set to suggest unique, parse the rest of the list and get the
        // existing words
        final Set existingWords;
        // Do the parsing only if needed. It might be too time consuming 
        if (this.suggestUnique) {
            existingWords = this.parseQueryString(prefix);
        } else {
            existingWords = new HashSet();
        }
        // Get the response from the embedded wordOracle
        request.setQuery(oneWordQuery);
        Callback innerCallback = new Callback() {
            @Override
            public void onSuggestionsReady(Request defaultRequest, Response defaultResponse) {
                Response wordListResponse = new Response();
                ArrayList suggestions = new ArrayList();
                // Generate the word list response from the single response one
                for (Iterator sIt = defaultResponse.getSuggestions().iterator(); sIt.hasNext();) {
                    Suggestion oldSuggestion = (Suggestion) sIt.next();
                    String oneWordSuggestion = oldSuggestion.getReplacementString();
                    // If we suggest unique and suggestion already exists, skip it
                    if (suggestUnique && existingWords.contains(oneWordSuggestion)) {
                        continue;
                    }
                    // Display one, replace all
                    MultiWordSuggestOracle.MultiWordSuggestion newSuggestion =
                        new MultiWordSuggestOracle.MultiWordSuggestion(
                            prefix + oneWordSuggestion, oldSuggestion.getDisplayString());
                    suggestions.add(newSuggestion);
                }
                wordListResponse.setSuggestions(suggestions);
                callback.onSuggestionsReady(defaultRequest, wordListResponse);
            }
        };
        this.wordOracle.requestSuggestions(request, innerCallback);
    }
    
    /**
     * Parses the passed query string into a list of tokens, based on the instance 
     * <tt>separators</tt>.
     * 
     * @param queryString the string to parse
     * @return the list of words, excluding empty words and duplicates 
     */
    protected Set parseQueryString(String queryString) {
        Set existingWords = new HashSet();
        int lastSeparator = queryString.length();
        if (this.suggestUnique) {
            while (lastSeparator > 0) {
                int currentLastSeparator = -1;
                // Find the last occurrence of any separator 
                for (int i = 0; i < this.separators.length(); i++) {
                    int currentSeparator =
                        queryString.substring(0, lastSeparator).lastIndexOf(
                            this.separators.charAt(i));
                    if (currentSeparator > currentLastSeparator) {
                        currentLastSeparator = currentSeparator;
                    }
                }
                // Get the current word determined by the last separator
                String currentWord =
                    queryString.substring(currentLastSeparator + 1, lastSeparator);
                if (currentWord.length() > 0) {
                    existingWords.add(currentWord);
                }
                lastSeparator = currentLastSeparator;
            }
        }
        return existingWords;
    }

    public String getSeparators() {
        return separators;
    }

    public void setSeparators(String separators) {
        this.separators = separators;
    }

    @Override
    public boolean isDisplayStringHTML() {
        return this.wordOracle.isDisplayStringHTML();
    }

    public SuggestOracle getWordOracle() {
        return wordOracle;
    }

    public void setWordOracle(SuggestOracle wordOracle) {
        this.wordOracle = wordOracle;
    }

    public boolean getSuggestUnique() {
        return suggestUnique;
    }

    public void setSuggestUnique(boolean suggestUnique) {
        this.suggestUnique = suggestUnique;
    }
}
