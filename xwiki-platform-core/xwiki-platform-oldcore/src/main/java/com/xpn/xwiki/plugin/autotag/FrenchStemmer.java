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

/**
 * A stemmer for French words. The algorithm is based on the work of Dr Martin Porter on his snowball project<br>
 * refer to http://snowball.sourceforge.net/french/stemmer.html<br>
 * (French stemming algorithm) for details
 * 
 * @version $Id$
 * @author Patrick Talbot
 */
public class FrenchStemmer
{
    /**
     * Buffer for the terms while stemming them.
     */
    private StringBuilder sb = new StringBuilder();

    /**
     * A temporary buffer, used to reconstruct R2.
     */
    private StringBuilder tb = new StringBuilder();

    /**
     * Region R0 is equal to the whole buffer.
     */
    private String R0;

    /**
     * Region RV "If the word begins with two vowels, RV is the region after the third letter, otherwise the region
     * after the first vowel not at the beginning of the word, or the end of the word if these positions cannot be
     * found.".
     */
    private String RV;

    /**
     * Region R1 "R1 is the region after the first non-vowel following a vowel or is the null region at the end of the
     * word if there is no such non-vowel".
     */
    private String R1;

    /**
     * Region R2 "R2 is the region after the first non-vowel in R1 following a vowel or is the null region at the end of
     * the word if there is no such non-vowel".
     */
    private String R2;

    /**
     * Set to true if we need to perform step 2.
     */
    private boolean suite;

    /**
     * Set to true if the buffer was modified.
     */
    private boolean modified;

    /**
     * Stemms the given term to a unique <tt>discriminator</tt>.
     * 
     * @param term java.langString The term that should be stemmed
     * @return java.lang.String Discriminator for <tt>term</tt>
     */
    public String stem(String term)
    {
        if (!isStemmable(term)) {
            return term;
        }

        // Use lowercase for medium stemming.
        term = term.toLowerCase();

        // Reset the StringBuffer.
        this.sb.delete(0, this.sb.length());
        this.sb.insert(0, term);

        // reset the booleans
        this.modified = false;
        this.suite = false;

        this.sb = treatVowels(this.sb);

        setStrings();

        step1();

        if (!this.modified || this.suite) {
            if (this.RV != null) {
                this.suite = step2a();
                if (!this.suite) {
                    step2b();
                }
            }
        }

        if (this.modified || this.suite) {
            step3();
        } else {
            step4();
        }

        step5();

        step6();

        return this.sb.toString();
    }

    /**
     * Sets the search region Strings. It needs to be done each time the buffer was modified.
     */
    private void setStrings()
    {
        // set the strings
        this.R0 = this.sb.toString();
        this.RV = retrieveRV(this.sb);
        this.R1 = retrieveR(this.sb);
        if (this.R1 != null) {
            this.tb.delete(0, this.tb.length());
            this.tb.insert(0, this.R1);
            this.R2 = retrieveR(this.tb);
        } else {
            this.R2 = null;
        }
    }

    /**
     * First step of the Porter Algorithmn. Refer to http://snowball.sourceforge.net/french/stemmer.html for an
     * explanation.
     */
    private void step1()
    {
        String[] suffix = {"ances", "iqUes", "ismes", "ables", "istes", "ance", "iqUe", "isme", "able", "iste"};
        deleteFrom(this.R2, suffix);

        replaceFrom(this.R2, new String[] {"logies", "logie"}, "log");
        replaceFrom(this.R2, new String[] {"usions", "utions", "usion", "ution"}, "u");
        replaceFrom(this.R2, new String[] {"ences", "ence"}, "ent");

        String[] search = {"atrices", "ateurs", "ations", "atrice", "ateur", "ation"};
        deleteButSuffixFromElseReplace(this.R2, search, "ic", true, this.R0, "iqU");

        deleteButSuffixFromElseReplace(this.R2, new String[] {"ements", "ement"}, "eus", false, this.R0,
            "eux");
        deleteButSuffixFrom(this.R2, new String[] {"ements", "ement"}, "ativ", false);
        deleteButSuffixFrom(this.R2, new String[] {"ements", "ement"}, "iv", false);
        deleteButSuffixFrom(this.R2, new String[] {"ements", "ement"}, "abl", false);
        deleteButSuffixFrom(this.R2, new String[] {"ements", "ement"}, "iqU", false);

        deleteFromIfTestVowelBeforeIn(this.R1, new String[] {"issements", "issement"}, false, this.R0);
        deleteFrom(this.RV, new String[] {"ements", "ement"});

        deleteButSuffixFromElseReplace(this.R2, new String[] {"it\u00e9s", "it\u00e9"}, "abil", false,
            this.R0, "abl");
        deleteButSuffixFromElseReplace(this.R2, new String[] {"it\u00e9s", "it\u00e9"}, "ic", false,
            this.R0, "iqU");
        deleteButSuffixFrom(this.R2, new String[] {"it\u00e9s", "it\u00e9"}, "iv", true);

        String[] autre = {"ifs", "ives", "if", "ive"};
        deleteButSuffixFromElseReplace(this.R2, autre, "icat", false, this.R0, "iqU");
        deleteButSuffixFromElseReplace(this.R2, autre, "at", true, this.R2, "iqU");

        replaceFrom(this.R0, new String[] {"eaux"}, "eau");

        replaceFrom(this.R1, new String[] {"aux"}, "al");

        deleteButSuffixFromElseReplace(this.R2, new String[] {"euses", "euse"}, "", true, this.R1, "eux");

        deleteFrom(this.R2, new String[] {"eux"});

        // if one of the next steps is performed, we will need to perform step2a
        boolean temp = false;
        temp = replaceFrom(this.RV, new String[] {"amment"}, "ant");
        if (temp) {
            this.suite = true;
        }
        temp = replaceFrom(this.RV, new String[] {"emment"}, "ent");
        if (temp) {
            this.suite = true;
        }
        temp = deleteFromIfTestVowelBeforeIn(this.RV, new String[] {"ments", "ment"}, true, this.RV);
        if (temp) {
            this.suite = true;
        }
    }

    /**
     * Second step (A) of the Porter Algorithmn. Will be performed if nothing changed from the first step or changed
     * were done in the amment, emment, ments or ment suffixes. Refer to
     * http://snowball.sourceforge.net/french/stemmer.html for an explanation.
     * 
     * @return boolean - true if something changed in the StringBuffer
     */
    private boolean step2a()
    {
        String[] search =
            {"\u00eemes", "\u00eetes", "iraIent", "irait", "irais", "irai", "iras", "ira",
            "irent", "iriez", "irez", "irions", "irons", "iront", "issaIent", "issais",
            "issantes", "issante", "issants", "issant", "issait", "issais", "issions", "issons",
            "issiez", "issez", "issent", "isses", "isse", "ir", "is", "\u00eet", "it", "ies",
            "ie", "i"};
        return deleteFromIfTestVowelBeforeIn(this.RV, search, false, this.RV);
    }

    /**
     * Second step (B) of the Porter Algorithmn. Will be performed if step 2 A was performed unsuccessfully. Refer to
     * http://snowball.sourceforge.net/french/stemmer.html for an explanation.
     */
    private void step2b()
    {
        String[] suffix =
            {"eraIent", "erais", "erait", "erai", "eras", "erions", "eriez", "erons", "eront",
            "erez", "\u00e8rent", "era", "\u00e9es", "iez", "\u00e9e", "\u00e9s", "er", "ez",
            "\u00e9"};
        deleteFrom(this.RV, suffix);

        String[] search =
            {"assions", "assiez", "assent", "asses", "asse", "aIent", "antes", "aIent", "Aient",
            "ante", "\u00e2mes", "\u00e2tes", "ants", "ant", "ait", "a\u00eet", "ais", "Ait",
            "A\u00eet", "Ais", "\u00e2t", "as", "ai", "Ai", "a"};
        deleteButSuffixFrom(this.RV, search, "e", true);

        deleteFrom(this.R2, new String[] {"ions"});
    }

    /**
     * Third step of the Porter Algorithmn. Refer to http://snowball.sourceforge.net/french/stemmer.html for an
     * explanation.
     */
    private void step3()
    {
        if (this.sb.length() > 0) {
            char ch = this.sb.charAt(this.sb.length() - 1);
            if (ch == 'Y') {
                this.sb.setCharAt(this.sb.length() - 1, 'i');
                setStrings();
            } else if (ch == '\u00E7') {
                this.sb.setCharAt(this.sb.length() - 1, 'c');
                setStrings();
            }
        }
    }

    /**
     * Fourth step of the Porter Algorithmn. Refer to http://snowball.sourceforge.net/french/stemmer.html for an
     * explanation.
     */
    private void step4()
    {
        if (this.sb.length() > 1) {
            char ch = this.sb.charAt(this.sb.length() - 1);
            if (ch == 's') {
                char b = this.sb.charAt(this.sb.length() - 2);
                if (b != 'a' && b != 'i' && b != 'o' && b != 'u' && b != '\u00E8' && b != 's') {
                    this.sb.delete(this.sb.length() - 1, this.sb.length());
                    setStrings();
                }
            }
        }
        boolean found = deleteFromIfPrecededIn(this.R2, new String[] {"ion"}, this.RV, "s");
        if (!found) {
            found = deleteFromIfPrecededIn(this.R2, new String[] {"ion"}, this.RV, "t");
        }

        replaceFrom(this.RV, new String[] {"I\u00e8re", "i\u00e8re", "Ier", "ier"}, "i");
        deleteFrom(this.RV, new String[] {"e"});
        deleteFromIfPrecededIn(this.RV, new String[] {"\u00eb"}, this.R0, "gu");
    }

    /**
     * Fifth step of the Porter Algorithmn. Refer to http://snowball.sourceforge.net/french/stemmer.html for an
     * explanation.
     */
    private void step5()
    {
        if (this.R0 != null) {
            if (this.R0.endsWith("enn") || this.R0.endsWith("onn") || this.R0.endsWith("ett")
                || this.R0.endsWith("ell") || this.R0.endsWith("eill")) {
                this.sb.delete(this.sb.length() - 1, this.sb.length());
                setStrings();
            }
        }
    }

    /**
     * Sixth (and last!) step of the Porter Algorithmn. Refer to http://snowball.sourceforge.net/french/stemmer.html for
     * an explanation.
     */
    private void step6()
    {
        if (this.R0 != null && this.R0.length() > 0) {
            boolean seenVowel = false;
            boolean seenConson = false;
            int pos = -1;
            for (int i = this.R0.length() - 1; i > -1; i--) {
                char ch = this.R0.charAt(i);
                if (isVowel(ch)) {
                    if (!seenVowel) {
                        if (ch == '\u00E9') {
                            pos = i;
                            break;
                        }
                    }
                    seenVowel = true;
                } else {
                    if (seenVowel) {
                        break;
                    } else {
                        seenConson = true;
                    }
                }
            }
            if (pos > -1 && seenConson && !seenVowel) {
                this.sb.setCharAt(pos, 'e');
            }
        }
    }

    /**
     * Delete a suffix searched in zone "source" if zone "from" contains prefix + search string.
     * 
     * @param source java.lang.String - the primary source zone for search
     * @param search java.lang.String[] - the strings to search for suppression
     * @param from java.lang.String - the secondary source zone for search
     * @param prefix java.lang.String - the prefix to add to the search string to test
     * @return boolean - true if modified
     */
    private boolean deleteFromIfPrecededIn(String source, String[] search, String from,
        String prefix)
    {
        boolean found = false;
        if (source != null) {
            for (int i = 0; i < search.length; i++) {
                if (source.endsWith(search[i])) {
                    if (from != null && from.endsWith(prefix + search[i])) {
                        this.sb.delete(this.sb.length() - search[i].length(), this.sb.length());
                        found = true;
                        setStrings();
                        break;
                    }
                }
            }
        }
        return found;
    }

    /**
     * Delete a suffix searched in zone "source" if the preceding letter is (or isn't) a vowel
     * 
     * @param source java.lang.String - the primary source zone for search
     * @param search java.lang.String[] - the strings to search for suppression
     * @param vowel boolean - true if we need a vowel before the search string
     * @param from java.lang.String - the secondary source zone for search (where vowel could be)
     * @return boolean - true if modified
     */
    private boolean deleteFromIfTestVowelBeforeIn(String source, String[] search, boolean vowel,
        String from)
    {
        boolean found = false;
        if (source != null && from != null) {
            for (int i = 0; i < search.length; i++) {
                if (source.endsWith(search[i])) {
                    if ((search[i].length() + 1) <= from.length()) {
                        boolean test = isVowel(this.sb.charAt(this.sb.length() - (search[i].length() + 1)));
                        if (test == vowel) {
                            this.sb.delete(this.sb.length() - search[i].length(), this.sb.length());
                            this.modified = true;
                            found = true;
                            setStrings();
                            break;
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * Delete a suffix searched in zone "source" if preceded by the prefix
     * 
     * @param source java.lang.String - the primary source zone for search
     * @param search java.lang.String[] - the strings to search for suppression
     * @param prefix java.lang.String - the prefix to add to the search string to test
     * @param without boolean - true if it will be deleted even without prefix found
     */
    private void deleteButSuffixFrom(String source, String[] search, String prefix,
        boolean without)
    {
        if (source != null) {
            for (int i = 0; i < search.length; i++) {
                if (source.endsWith(prefix + search[i])) {
                    this.sb.delete(this.sb.length() - (prefix.length() + search[i].length()), this.sb.length());
                    this.modified = true;
                    setStrings();
                    break;
                } else if (without && source.endsWith(search[i])) {
                    this.sb.delete(this.sb.length() - search[i].length(), this.sb.length());
                    this.modified = true;
                    setStrings();
                    break;
                }
            }
        }
    }

    /**
     * Delete a suffix searched in zone "source" if preceded by prefix<br>
     * or replace it with the replace string if preceded by the prefix in the zone "from"<br>
     * or delete the suffix if specified
     * 
     * @param source java.lang.String - the primary source zone for search
     * @param search java.lang.String[] - the strings to search for suppression
     * @param prefix java.lang.String - the prefix to add to the search string to test
     * @param without boolean - true if it will be deleted even without prefix found
     */
    private void deleteButSuffixFromElseReplace(String source, String[] search, String prefix,
        boolean without, String from, String replace)
    {
        if (source != null) {
            for (int i = 0; i < search.length; i++) {
                if (source.endsWith(prefix + search[i])) {
                    this.sb.delete(this.sb.length() - (prefix.length() + search[i].length()), this.sb.length());
                    this.modified = true;
                    setStrings();
                    break;
                } else if (from != null && from.endsWith(prefix + search[i])) {
                    this.sb.replace(this.sb.length() - (prefix.length() + search[i].length()), this.sb.length(),
                        replace);
                    this.modified = true;
                    setStrings();
                    break;
                } else if (without && source.endsWith(search[i])) {
                    this.sb.delete(this.sb.length() - search[i].length(), this.sb.length());
                    this.modified = true;
                    setStrings();
                    break;
                }
            }
        }
    }

    /**
     * Replace a search string with another within the source zone
     * 
     * @param source java.lang.String - the source zone for search
     * @param search java.lang.String[] - the strings to search for replacement
     * @param replace java.lang.String - the replacement string
     */
    private boolean replaceFrom(String source, String[] search, String replace)
    {
        boolean found = false;
        if (source != null) {
            for (int i = 0; i < search.length; i++) {
                if (source.endsWith(search[i])) {
                    this.sb.replace(this.sb.length() - search[i].length(), this.sb.length(), replace);
                    this.modified = true;
                    found = true;
                    setStrings();
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Delete a search string within the source zone
     * 
     * @param source the source zone for search
     * @param suffix the strings to search for suppression
     */
    private void deleteFrom(String source, String[] suffix)
    {
        if (source != null) {
            for (int i = 0; i < suffix.length; i++) {
                if (source.endsWith(suffix[i])) {
                    this.sb.delete(this.sb.length() - suffix[i].length(), this.sb.length());
                    this.modified = true;
                    setStrings();
                    break;
                }
            }
        }
    }

    /**
     * Test if a char is a french vowel, including accentuated ones
     * 
     * @param ch the char to test
     * @return boolean - true if the char is a vowel
     */
    private boolean isVowel(char ch)
    {
        switch (ch) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
            case 'y':
            case '\u00E0': // A WITH GRAVE
            case '\u00E2': // A WITH CIRCUMFLEX
            case '\u00E8': // E WITH GRAVE
            case '\u00E9': // E WITH ACUTE
            case '\u00EA': // E WITH CIRCUMFLEX
            case '\u00EB': // E WITH DIAERESIS
            case '\u00EE': // I WITH CIRCUMFLEX
            case '\u00EF': // I WITH DIAERESIS
            case '\u00F4': // O WITH CIRCUMFLEX
            case '\u00F9': // U WITH GRAVE
            case '\u00FB': // U WITH CIRCUMFLEX
            case '\u00FC': // U WITH DIAERESIS
                return true;
            default:
                return false;
        }
    }

    /**
     * Retrieve the "R zone" (1 or 2 depending on the buffer) and return the corresponding string<br>
     * "R is the region after the first non-vowel following a vowel or is the null region at the end of the word if
     * there is no such non-vowel"<br>
     * 
     * @param buffer java.lang.StringBuffer - the in buffer
     * @return java.lang.String - the resulting string
     */
    private String retrieveR(StringBuilder buffer)
    {
        int len = buffer.length();
        int pos = -1;
        for (int c = 0; c < len; c++) {
            if (isVowel(buffer.charAt(c))) {
                pos = c;
                break;
            }
        }
        if (pos > -1) {
            int consonne = -1;
            for (int c = pos; c < len; c++) {
                if (!isVowel(buffer.charAt(c))) {
                    consonne = c;
                    break;
                }
            }
            if (consonne > -1 && (consonne + 1) < len) {
                return buffer.substring(consonne + 1, len);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieve the "RV zone" from a buffer an return the corresponding string<br>
     * "If the word begins with two vowels, RV is the region after the third letter, otherwise the region after the
     * first vowel not at the beginning of the word, or the end of the word if these positions cannot be found."<br>
     * 
     * @param buffer java.lang.StringBuffer - the in buffer
     * @return java.lang.String - the resulting string
     */
    private String retrieveRV(StringBuilder buffer)
    {
        int len = buffer.length();
        if (buffer.length() > 3) {
            if (isVowel(buffer.charAt(0)) && isVowel(buffer.charAt(1))) {
                return buffer.substring(3, len);
            } else {
                int pos = 0;
                for (int c = 1; c < len; c++) {
                    if (isVowel(buffer.charAt(c))) {
                        pos = c;
                        break;
                    }
                }
                if (pos + 1 < len) {
                    return buffer.substring(pos + 1, len);
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    /**
     * Turns u and i preceded AND followed by a vowel to UpperCase<br>
     * Turns y preceded OR followed by a vowel to UpperCase<br>
     * Turns u preceded by q to UpperCase<br>
     * 
     * @param buffer java.util.StringBuffer - the buffer to treat
     * @return java.util.StringBuffer - the treated buffer
     */
    private StringBuilder treatVowels(StringBuilder buffer)
    {
        for (int c = 0; c < buffer.length(); c++) {
            char ch = buffer.charAt(c);

            if (c == 0) // first char
            {
                if (buffer.length() > 1) {
                    if (ch == 'y' && isVowel(buffer.charAt(c + 1))) {
                        buffer.setCharAt(c, 'Y');
                    }
                }
            } else if (c == buffer.length() - 1) // last char
            {
                if (ch == 'u' && buffer.charAt(c - 1) == 'q') {
                    buffer.setCharAt(c, 'U');
                }
                if (ch == 'y' && isVowel(buffer.charAt(c - 1))) {
                    buffer.setCharAt(c, 'Y');
                }
            } else // other cases
            {
                if (ch == 'u') {
                    if (buffer.charAt(c - 1) == 'q') {
                        buffer.setCharAt(c, 'U');
                    } else if (isVowel(buffer.charAt(c - 1)) && isVowel(buffer.charAt(c + 1))) {
                        buffer.setCharAt(c, 'U');
                    }
                }
                if (ch == 'i') {
                    if (isVowel(buffer.charAt(c - 1)) && isVowel(buffer.charAt(c + 1))) {
                        buffer.setCharAt(c, 'I');
                    }
                }
                if (ch == 'y') {
                    if (isVowel(buffer.charAt(c - 1)) || isVowel(buffer.charAt(c + 1))) {
                        buffer.setCharAt(c, 'Y');
                    }
                }
            }
        }

        return buffer;
    }

    /**
     * Checks a term if it can be processed correctly.
     * 
     * @return boolean - true if, and only if, the given term consists in letters.
     */
    private boolean isStemmable(String term)
    {
        boolean upper = false;
        int first = -1;
        for (int c = 0; c < term.length(); c++) {
            // Discard terms that contain non-letter characters.
            if (!Character.isLetter(term.charAt(c))) {
                return false;
            }
            // Discard terms that contain multiple uppercase letters.
            if (Character.isUpperCase(term.charAt(c))) {
                if (upper) {
                    return false;
                }
                // First encountered uppercase letter, set flag and save
                // position.
                else {
                    first = c;
                    upper = true;
                }
            }
        }
        // Discard the term if it contains a single uppercase letter that
        // is not starting the term.
        if (first > 0) {
            return false;
        }
        return true;
    }
}
