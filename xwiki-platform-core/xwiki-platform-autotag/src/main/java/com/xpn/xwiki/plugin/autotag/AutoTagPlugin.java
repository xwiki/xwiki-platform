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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin which extracts a set of tags from a text.
 *
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class AutoTagPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    /** Identifier for the French language. */
    public static final int LANG_FRENCH = 0;

    /** Identifier for the English language. */
    public static final int LANG_ENGLISH = 1;

    /**
     * The name of the plugin, which is used for retrieving the plugin from the plugin manager (and from the public
     * {@code $xwiki.get()} API).
     */
    private static final String PLUGIN_NAME = "autotag";

    /**
     * Special characters that will cause a token word to be ignored when that token contains one of these characters.
     */
    private static final Pattern SPECIAL_CHARS = Pattern.compile("<|>|=|/|\"|\u0093");

    /** Needed to make chekstyle pass. */
    private static final String A = "a";

    /** Needed to make chekstyle pass. */
    private static final String ON = "on";

    /** French words that should be ignored since they don't add any value, they're very common words. */
    private static final String[] FRENCH_STOP_WORDS = {
        A, "afin", "ai", "ainsi", "apr\u00e8s", "attendu", "au", "aujourd", "auquel", "aussi",
        "autre", "autres", "aux", "auxquelles", "auxquels", "avait", "avant", "avec", "avoir",
        "c", "car", "ce", "ceci", "cela", "celle", "celles", "celui", "cependant", "certain",
        "certaine", "certaines", "certains", "ces", "cet", "cette", "ceux", "chez", "ci",
        "combien", "comme", "comment", "concernant", "contre", "d", "dans", "de", "debout",
        "dedans", "dehors", "del\u00e0", "depuis", "derri\u00e8re", "des", "d\u00e9sormais",
        "desquelles", "desquels", "dessous", "dessus", "devant", "devers", "devra", "divers",
        "diverse", "diverses", "doit", "donc", "dont", "du", "duquel", "durant", "d\u00e8s",
        "elle", "elles", "en", "entre", "environ", "est", "et", "etc", "etre", "eu", "eux",
        "except\u00e9", "hormis", "hors", "h\u00e9las", "hui", "il", "ils", "j", "je", "jusqu",
        "jusque", "l", "la", "laquelle", "le", "lequel", "les", "lesquelles", "lesquels", "leur",
        "leurs", "lorsque", "lui", "l\u00e0", "ma", "mais", "malgr\u00e9", "me", "merci", "mes",
        "mien", "mienne", "miennes", "miens", "moi", "moins", "mon", "moyennant", "m\u00eame",
        "m\u00eames", "n", "ne", "ni", "non", "nos", "notre", "nous", "n\u00e9anmoins",
        "n\u00f4tre", "n\u00f4tres", ON, "ont", "ou", "outre", "o\u00f9", "par", "parmi",
        "partant", "pas", "pass\u00e9", "pendant", "plein", "plus", "plusieurs", "pour",
        "pourquoi", "proche", "pr\u00e8s", "puisque", "qu", "quand", "que", "quel", "quelle",
        "quelles", "quels", "qui", "quoi", "quoique", "revoici", "revoil\u00e0", "s", "sa",
        "sans", "sauf", "se", "selon", "seront", "ses", "si", "sien", "sienne", "siennes",
        "siens", "sinon", "soi", "soit", "son", "sont", "sous", "suivant", "sur", "ta", "te",
        "tes", "tien", "tienne", "tiennes", "tiens", "toi", "ton", "tous", "tout", "toute",
        "toutes", "tu", "un", "une", "va", "vers", "voici", "voil\u00e0", "vos", "votre", "vous",
        "vu", "v\u00f4tre", "v\u00f4tres", "y", "\u00e0", "\u00e7a", "\u00e8s", "\u00e9t\u00e9",
        "\u00eatre", "\u00f4", "avez", "parce", "suis"};

    /** English words that should be ignored since they don't add any value, they're very common words. */
    private static final String[] ENGLISH_STOP_WORDS = {
        "the", "of", "and", A, "to", "in", "is", "you", "that", "it", "he", "was", "for", ON,
        "are", "as", "with", "his", "they", "I", "at", "be", "this", "have", "from", "or", "one",
        "had", "by", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said",
        "there", "use", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up",
        "other", "about", "out", "many", "then", "them", "these", "so", "some", "her", "would",
        "make", "like", "him", "into", "time", "has", "look", "two", "more", "go", "see", "no",
        "way", "could", "my", "than", "first", "been", "call", "who", "its", "now", "find", "long",
        "down", "day", "did", "get", "come", "may"};

    /** The list of words (or, more generally, tokens) that should be ignored. */
    private List<String> ignoreList = Collections.synchronizedList(new ArrayList<String>());

    /** The list of words that should never be ignored, even if they're placed in {@link #ignoreList}. */
    private List<String> dontignoreList = Collections.synchronizedList(new ArrayList<String>());

    /** The maximum number of tags to generate in the tag cloud. */
    private int maximumNumberOfTags = 100;

    /** The maximum size of a tag, corresponding to the most common tag in the document list. */
    private int maxTagSize = 64;

    /** The minimum size of a tag, corresponding to the least common tag that gets included in the tag cloud. */
    private int minTagSize = 12;

    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     *
     * @param name the plugin name, usually ignored, since plugins have a fixed name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public AutoTagPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public AutoTagPluginAPI getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new AutoTagPluginAPI((AutoTagPlugin) plugin, context);
    }

    /**
     * Analyze a piece of text, and extract the most common words into a "tag cloud". In detail, this splits the text
     * into tokens, counts how many times each token appears in the text, removes the "stop-words", joins together words
     * from the same root (stemming), and prepares an HTML tag cloud which can be printed in the response.
     *
     * @param text the text to analyze
     * @param lang the language in which the text is written, {@code 0} for French or {@code 1} for English
     * @return the resulting TagCloud with all the analyzed data, including the HTML tag cloud
     */
    public TagCloud generateTagCloud(String text, int lang)
    {
        TagCloud tagcloud = countWords(text, lang);
        calculateTags(tagcloud);
        return tagcloud;
    }

    /**
     * Analyze a piece of text, splitting it into individual words, along with their frequencies. In detail, this splits
     * the text into tokens, counts how many times each token appears in the text, removes the "stop-words", and joins
     * together words from the same root (stemming). {@link #generateTagCloud(String, int)} also prepares an HTML tag
     * cloud which can be printed in the response.
     *
     * @param text the text to analyze
     * @param lang the language, {@code 0} for French or {@code 1} for English
     * @return the resulting TagCloud with all the analyzed data, except the HTML tag cloud
     */
    public TagCloud countWords(String text, int lang)
    {
        TagCloud tagcloud = new TagCloud();
        tagcloud.setText(text);

        splitWords(tagcloud);
        countWords(tagcloud);
        clearStopWords(tagcloud, lang);
        stemmer(tagcloud, lang);
        return tagcloud;
    }

    // Utility methods

    /**
     * Return a sorted copy of a set.
     *
     * @param <T> the type of the items in the set
     * @param oSet the set containing the values to sort; it is not affected in any way by this method
     * @return a new sorted set containing all the values in the input set
     */
    public static <T extends Comparable<T>> SortedSet<T> sortSet(Set<T> oSet)
    {
        return new TreeSet<T>(oSet);
    }

    /**
     * Return a copy of a map, sorted in ascending order of their values.
     *
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param hmap the map containing the entries to sort; it is not affected in any way by this method
     * @return a new sorted map containing all the entries in the input map
     */
    public static <K, V> Map<K, V> sortMap(Map<K, V> hmap)
    {
        Map<K, V> map = new LinkedHashMap<K, V>();

        List<K> mapKeys = new ArrayList<K>(hmap.keySet());
        List<V> mapValues = new ArrayList<V>(hmap.values());

        Set<V> sortedSet = new TreeSet<V>(mapValues);
        @SuppressWarnings("unchecked")
        V[] sortedArray = (V[]) sortedSet.toArray();

        for (int i = 0; i < sortedArray.length; i++) {
            for (int j = 0; j < mapValues.size(); j++) {
                if (mapValues.get(j).equals(sortedArray[i])) {
                    map.put(mapKeys.get(j), sortedArray[i]);
                }
            }
        }
        return map;
    }

    /**
     * Get the identifier corresponding to the given two-leter country code. Currently the only supported values are
     * "en" and "fr".
     *
     * @param lang the two-letter ISO 3166-1 alpha-2 code of a country
     * @return {@code 0} for French ("fr") or {@code 1} for English ("en") and all other passed values
     */
    public int getLanguageConstant(String lang)
    {
        if (lang.trim().equalsIgnoreCase("fr")) {
            return AutoTagPlugin.LANG_FRENCH;
        }
        // default English
        return AutoTagPlugin.LANG_ENGLISH;
    }

    /**
     * Split the text into tokens. Newlines, spaces, tabs, comma, dot, semi-colon, colon, exclamation, question mark,
     * and apostrophe are considered separators.
     *
     * @param tagCloud the instance to process
     * @return the resulting list of tokens, which is also stored in the instance {@link TagCloud#getWordList()
     *         TagCloud}
     */
    private String[] splitWords(TagCloud tagCloud)
    {
        String text = tagCloud.getText();
        text = text.replaceAll("\n", " ");
        text = text.replaceAll("\r", " ");
        text = text.replaceAll("'", " ");
        text = text.replaceAll("\u0092", " ");
        text = text.toLowerCase();
        String[] words = text.split("[\\s,.;:!\\?]+");
        tagCloud.setWordList(words);
        return words;
    }

    /**
     * Count all the appearances of each token extracted from the text. This method must be called after
     * {@link #splitWords(TagCloud)}.
     *
     * @param tagCloud the instance to process
     * @return the resulting map of {@code token->number of appearances} count for each token present in the text, which
     *         is also stored in the instance {@link TagCloud#getCountedWordMap() TagCloud}
     */
    private Map<String, Integer> countWords(TagCloud tagCloud)
    {
        String[] words = tagCloud.getWordList();
        Map<String, Integer> wordsCount = new HashMap<String, Integer>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!wordsCount.containsKey(word)) {
                wordsCount.put(word, Integer.valueOf(0));
            }
            wordsCount.put(word, Integer.valueOf((wordsCount.get(word)).intValue() + 1));
        }
        tagCloud.setCountedWordMap(wordsCount);
        return wordsCount;
    }

    /**
     * Remove "stop words", words that should be ignored since they don't add any value, they're very common. This
     * method must be called after {@link #countWords(TagCloud)}.
     *
     * @param tagCloud the instance to process
     * @param lang the language in which the text is written, {@code 0} for French or {@code 1} for English
     * @return the resulting map of {@code token->number of appearances} count for each token present in the text, which
     *         is also stored in the instance {@link TagCloud#getCountedWordMap() TagCloud}
     */
    private Map<String, Integer> clearStopWords(TagCloud tagCloud, int lang)
    {
        Map<String, Integer> words = tagCloud.getCountedWordMap();
        String[] stopWordsArray = new String[0];
        switch (lang) {
            case LANG_ENGLISH:
                stopWordsArray = ENGLISH_STOP_WORDS;
                break;
            case LANG_FRENCH:
                stopWordsArray = FRENCH_STOP_WORDS;
                break;
            default:
                // nothing
                break;
        }
        for (String word : stopWordsArray) {
            words.remove(word);
        }

        Set<String> ignored = new HashSet<String>();
        for (String word : words.keySet()) {
            if (SPECIAL_CHARS.matcher(word).find()) {
                ignored.add(word);
            }
        }

        for (String word : this.ignoreList) {
            if (!this.dontignoreList.contains(word)) {
                words.remove(word);
            }
        }
        for (String word : ignored) {
            if (!this.dontignoreList.contains(word)) {
                words.remove(word);
            }
        }

        tagCloud.setCountedWordMap(words);
        return words;
    }

    /**
     * Group tokens based on their common stem. For example, "hand" and "hands" both refer to the same term, "hand",
     * thus they will be grouped together under the "hand" stem. This method must be called after
     * {@link #countWords(TagCloud)} or {@link #clearStopWords(TagCloud, int)}.
     *
     * @param tagCloud the instance to process
     * @param lang the language in which the text is written, {@code 0} for French or {@code 1} for English
     * @return the resulting list of token groups, which is also stored in the instance
     *         {@link TagCloud#getStemmedWordMap() TagCloud}
     */
    private Map<String, Map<String, Integer>> stemmer(TagCloud tagCloud, int lang)
    {
        Map<String, Integer> words = tagCloud.getCountedWordMap();
        // SnowballProgram stemmer;
        // if (lang == LANG_FRENCH) {
        // stemmer = new frenchStemmer();
        // } else {
        // stemmer = new englishStemmer();
        // }
        FrenchStemmer stemmer;
        Map<String, Map<String, Integer>> stemmedWordMap = new HashMap<String, Map<String, Integer>>();

        stemmer = new FrenchStemmer();

        for (String word : words.keySet()) {
            if (word.length() <= 2) {
                continue;
            }
            String stemmedWord = stemmer.stem(word);
            if (!stemmedWordMap.containsKey(stemmedWord)) {
                stemmedWordMap.put(stemmedWord, new HashMap<String, Integer>());
            }
            stemmedWordMap.get(stemmedWord).put(word, words.get(word));
        }
        tagCloud.setStemmedWordMap(stemmedWordMap);
        return stemmedWordMap;
    }

    /**
     * Determine which are the most frequent {@link #maximumNumberOfTags} tokens and build a tag cloud using their
     * relative frequencies. This method must be called after {@link #stemmer(TagCloud, int)}.
     *
     * @param tagCloud the instance to process
     * @return the resulting set of tags, which is also stored in the instance {@link TagCloud#getTags() TagCloud}
     */
    private Set<Tag> calculateTags(TagCloud tagCloud)
    {
        Map<String, Map<String, Integer>> stemmedWords = tagCloud.getStemmedWordMap();
        Map<String, Integer> stemmedWordFreqMap = new HashMap<String, Integer>();

        // Determine the "lead" word for each stem as the most common token by comparing the frequency of each sub-token
        // Calculate the total frequency of each lead word as the sum of the frequencies of all tokens having that stem
        for (Map.Entry<String, Map<String, Integer>> stemmedWord : stemmedWords.entrySet()) {
            Integer totalFreqency = Integer.valueOf(0);
            String leadWord = "";
            Integer leadFrequency = Integer.valueOf(0);

            Map<String, Integer> wordMap = stemmedWord.getValue();

            for (Map.Entry<String, Integer> word : wordMap.entrySet()) {
                Integer frequency = word.getValue();

                totalFreqency = Integer.valueOf(frequency.intValue() + totalFreqency.intValue());

                if (frequency.intValue() > leadFrequency.intValue()) {
                    leadFrequency = word.getValue();
                    leadWord = word.getKey();
                }
            }
            stemmedWordFreqMap.put(leadWord, totalFreqency);
        }
        tagCloud.setStemmedWordFreqMap(stemmedWordFreqMap);
        return calculateTagSizes(tagCloud);
    }

    /**
     * Build a tag cloud using the relative frequencies of the selected tags. This method must be called by
     * {@link #calculateTags(TagCloud)}.
     * 
     * @param tagCloud the instance to process
     * @return the resulting set of tags, which is also stored in the instance {@link TagCloud#getTags() TagCloud}
     */
    private Set<Tag> calculateTagSizes(TagCloud tagCloud)
    {
        Map<String, Integer> stemmedWordFreqMap = tagCloud.getStemmedWordFreqMap();
        // If there's no text, just use an empty set of tags and return
        if (stemmedWordFreqMap == null || stemmedWordFreqMap.size() == 0) {
            tagCloud.setTags(new TreeSet<Tag>());
            return tagCloud.getTags();
        }
        // We order the list by the value to select the most frequent tags
        Map<String, Integer> orderedMap = sortMap(stemmedWordFreqMap);

        Map<String, Integer> tagMap = new LinkedHashMap<String, Integer>();

        int i = 0;
        for (Entry<String, Integer> word : orderedMap.entrySet()) {
            tagMap.put(word.getKey(), word.getValue());
            if (++i > this.maximumNumberOfTags) {
                break;
            }
        }
        Integer[] freqs = tagMap.values().toArray(new Integer[0]);

        Integer minFreq = freqs[0];
        Integer maxFreq = freqs[freqs.length - 1];

        int ftot = 0;

        for (Integer f : freqs) {
            ftot += f.intValue();
        }

        SortedSet<Tag> tagSet = new TreeSet<Tag>();

        for (String tagName : sortSet(tagMap.keySet())) {
            long size = getTagSize(tagMap.get(tagName), maxFreq, minFreq, ftot);
            Tag tag = new Tag(tagName, size);
            tagSet.add(tag);
        }
        tagCloud.setTags(tagSet);
        return tagSet;
    }

    /**
     * Get the size that corresponds to a given tag popularity, relative to all the other tag frequencies.
     *
     * @param tagOccurrences the number of occurrences of the tag
     * @param maxOccurrences the maximum number of occurrences among all tags
     * @param minOccurrences the minimum number of occurrences among all tags
     * @param totalOccurrences the total number of occurrences of all the tags
     * @return a number between {@link #minTagSize} and {@link #maxTagSize} corresponding to the relative popularity of
     *         this tag compared to all the other tags
     */
    private long getTagSize(double tagOccurrences, double maxOccurrences, double minOccurrences,
        double totalOccurrences)
    {
        // The number of available tag sizes
        int fontRange = this.maxTagSize - this.minTagSize;

        // tweak this if all the words seem too similar in size or extremely different
        // rely on the cumulative by x% (0 = 0%, 1 = 100%)
        double cumulativeImportance = 0.7;

        // sizes based on word's frequency vs total/cumulative frequency
        double sumpx = ((fontRange * cumulativeImportance) + 1) * (fontRange * cumulativeImportance) / 2;
        double px = tagOccurrences / totalOccurrences * sumpx;

        // sizes based on word's frequency deviation from max/min frequencies
        px += Math.pow((tagOccurrences - minOccurrences)
            / (1 > maxOccurrences - minOccurrences ? 1 : maxOccurrences - minOccurrences), 0.8)
            * (fontRange * (1 - cumulativeImportance));
        double result = this.maxTagSize < px + this.minTagSize ? this.maxTagSize : px + this.minTagSize;
        return Math.round(result);
    }
}
