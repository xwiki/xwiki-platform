/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */

package com.xpn.xwiki.gwt.api.client.widgets;

import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * SuggestOracle to suggest a list of words with respect to a given list of separators.
 *
 * The <tt>SuggestOracle</tt> used for a single word suggestion is set through
 * {@link com.xpn.xwiki.gwt.api.client.widgets.WordListSuggestOracle#setWordOracle(com.google.gwt.user.client.ui.SuggestOracle)}
 * and the separators through {@link com.xpn.xwiki.gwt.api.client.widgets.WordListSuggestOracle#setSeparators(String)}
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
     * Default constructor: builds an oracle with an empty {@link com.google.gwt.user.client.ui.SuggestOracle}
     * and the space as a separator.
     */
    public WordListSuggestOracle() {
        this(new MultiWordSuggestOracle(), " ");
    }

    /**
     * Builds an oracle with the specified {@link com.google.gwt.user.client.ui.SuggestOracle}
     * and the space as a default separator.
     * @param wordOracle the oracle to be used for predicting a single word.
     */
    public WordListSuggestOracle(SuggestOracle wordOracle) {
        this(wordOracle, " ");
    }

    /**
     * Builds an wordOracle with the specified {@link com.google.gwt.user.client.ui.SuggestOracle} and separators.
     * @param wordOracle the wordOracle to be used for predicting a single word
     * @param separators the separators between the words in the word list
     */
    public WordListSuggestOracle(SuggestOracle wordOracle, String separators) {
        this.setWordOracle(wordOracle);
        this.setSeparators(separators);
    }

    /**
     * Returns a list of suggestions for the last word in the list.
     *
     * @param request
     * @param callback
     */
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
        //get the response
        request.setQuery(oneWordQuery);
        Callback innerCallback = new Callback() {
            public void onSuggestionsReady(Request defaultRequest, Response defaultResponse) {
                Response wordListResponse = new Response();
                ArrayList suggestions = new ArrayList();
                //generate the word list response from the single response one
                for (Iterator sIt = defaultResponse.getSuggestions().iterator(); sIt.hasNext();) {
                    Suggestion oldSuggestion = (Suggestion)sIt.next();
                    //display one, replace all
                    MultiWordSuggestOracle.MultiWordSuggestion newSuggestion =
                            new MultiWordSuggestOracle.MultiWordSuggestion(prefix + oldSuggestion.getReplacementString(),
                            oldSuggestion.getDisplayString());
                    suggestions.add(newSuggestion);
                }
                wordListResponse.setSuggestions(suggestions);
                callback.onSuggestionsReady(defaultRequest, wordListResponse);
            }
        };
        this.wordOracle.requestSuggestions(request, innerCallback);
    }

    public String getSeparators() {
        return separators;
    }

    public void setSeparators(String separators) {
        this.separators = separators;
    }

    public boolean isDisplayStringHTML() {
        return this.wordOracle.isDisplayStringHTML();
    }

    public SuggestOracle getWordOracle() {
        return wordOracle;
    }

    public void setWordOracle(SuggestOracle wordOracle) {
        this.wordOracle = wordOracle;
    }
}
