package com.xpn.xwiki.plugin.autotag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class AutoTagPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    public static final int LANG_FRENCH = 0;

    public static final int LANG_ENGLISH = 1;

    public static final String NAME = "autotag";

    public final static String[] FRENCH_STOP_WORDS =
        {"a", "afin", "ai", "ainsi", "apr\u00e8s", "attendu", "au", "aujourd", "auquel", "aussi",
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
        "n\u00f4tre", "n\u00f4tres", "on", "ont", "ou", "outre", "o\u00f9", "par", "parmi",
        "partant", "pas", "pass\u00e9", "pendant", "plein", "plus", "plusieurs", "pour",
        "pourquoi", "proche", "pr\u00e8s", "puisque", "qu", "quand", "que", "quel", "quelle",
        "quelles", "quels", "qui", "quoi", "quoique", "revoici", "revoil\u00e0", "s", "sa",
        "sans", "sauf", "se", "selon", "seront", "ses", "si", "sien", "sienne", "siennes",
        "siens", "sinon", "soi", "soit", "son", "sont", "sous", "suivant", "sur", "ta", "te",
        "tes", "tien", "tienne", "tiennes", "tiens", "toi", "ton", "tous", "tout", "toute",
        "toutes", "tu", "un", "une", "va", "vers", "voici", "voil\u00e0", "vos", "votre", "vous",
        "vu", "v\u00f4tre", "v\u00f4tres", "y", "\u00e0", "\u00e7a", "\u00e8s", "\u00e9t\u00e9",
        "\u00eatre", "\u00f4", "avez", "parce", "suis"};

    public final static String[] ENGLISH_STOP_WORDS =
        {"the", "of", "and", "a", "to", "in", "is", "you", "that", "it", "he", "was", "for", "on",
        "are", "as", "with", "his", "they", "I", "at", "be", "this", "have", "from", "or", "one",
        "had", "by", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said",
        "there", "use", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up",
        "other", "about", "out", "many", "then", "them", "these", "so", "some", "her", "would",
        "make", "like", "him", "into", "time", "has", "look", "two", "more", "go", "see", "no",
        "way", "could", "my", "than", "first", "been", "call", "who", "its", "now", "find", "long",
        "down", "day", "did", "get", "come", "may"};

    List<String> ignoreList = new ArrayList<String>();

    List<String> dontignoreList = null;

    int maxTag = 100;

    int maxTagSize = 64;

    int minTagSize = 12;

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

    /**
     * Allow to get the plugin name
     * 
     * @return plugin name
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new AutoTagPluginAPI((AutoTagPlugin) plugin, context);
    }

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

    public TagCloud generateTagCloud(String text, int lang)
    {
        TagCloud tagcloud = countWords(text, lang);
        calculateTags(tagcloud);
        return tagcloud;
    }

    private Set<Tag> calculateTags(TagCloud tagcloud)
    {
        Map<String, Map<String, Integer>> stemmedWords = tagcloud.getStemmedWordMap();
        Map<String, Integer> stemmedWordFreqMap = new HashMap<String, Integer>();

        // we calculate the frequency of each word
        for (Map.Entry<String, Map<String, Integer>> stemmedWord : stemmedWords.entrySet()) {
            Integer totalFreq = Integer.valueOf(0);
            String leadWord = "";
            Integer leadFreq = Integer.valueOf(0);

            Map<String, Integer> wordMap = stemmedWord.getValue();

            for (Map.Entry<String, Integer> word : wordMap.entrySet()) {
                Integer freq = word.getValue();

                totalFreq = Integer.valueOf(freq.intValue() + totalFreq.intValue());

                if (freq.intValue() > leadFreq.intValue()) {
                    leadFreq = word.getValue();
                    leadWord = word.getKey();
                }
            }
            stemmedWordFreqMap.put(leadWord, totalFreq);
        }
        tagcloud.setStemmedWordFreqMap(stemmedWordFreqMap);

        // we order the list by the value to select the most frequent tags
        LinkedHashMap<String, Integer> orderedMap = sortMap(stemmedWordFreqMap);

        String[] keyset = (String[]) orderedMap.keySet().toArray();

        LinkedHashMap<String, Integer> tagMap = new LinkedHashMap<String, Integer>();

        for (int i = 0, pos = keyset.length - 1; i < keyset.length && i < this.maxTag; i++, pos--) {
            tagMap.put(keyset[pos], orderedMap.get(keyset[pos]));
        }

        Integer[] freqs = (Integer[]) tagMap.values().toArray();

        Integer maxFreq = freqs[0];
        Integer minFreq = freqs[freqs.length - 1];

        Integer ftot = Integer.valueOf(0);

        for (int i = 0; i < freqs.length; i++) {
            ftot = Integer.valueOf(freqs[i].intValue() + ftot.intValue());
        }

        SortedSet<Tag> tagSet = new TreeSet<Tag>();

        for (String tagName : sortSet(tagMap.keySet())) {
            long size = getTagSize(tagMap.get(tagName), maxFreq, minFreq, ftot);
            Tag tag = new Tag(tagName, size);
            tagSet.add(tag);
        }
        tagcloud.setTags(tagSet);
        return tagSet;
    }

    private long getTagSize(double freq, double fmax, double fmin, double ftot)
    {
        int fontrange = this.maxTagSize - this.minTagSize;

        // tweak this if all the words seem too similar in size or extremely different
        // rely on the cumulative by x% (0 = 0%, 1 = 100%)
        double cuml_importance = 0.7;

        // sizes based on word's frequency vs total/cumulative frequency
        double sumpx = ((fontrange * cuml_importance) + 1) * (fontrange * cuml_importance) / 2;
        double px = freq / ftot * sumpx;

        // sizes based on word's frequency deviation from max/min frequencies
        px += Math.pow((freq - fmin) / (1 > fmax - fmin ? 1 : fmax - fmin), 0.8) * (fontrange * (1 - cuml_importance));
        double res = this.maxTagSize < px + this.minTagSize ? this.maxTagSize : px + this.minTagSize;
        return Math.round(res);
    }

    public static <T extends Comparable<T>> SortedSet<T> sortSet(Set<T> oSet)
    {
        return new TreeSet<T>(oSet);
    }

    public static <K, V> LinkedHashMap<K, V> sortMap(Map<K, V> hmap)
    {
        LinkedHashMap<K, V> map = new LinkedHashMap<K, V>();

        List<K> mapKeys = new ArrayList<K>(hmap.keySet());
        List<V> mapValues = new ArrayList<V>(hmap.values());

        TreeSet<V> sortedSet = new TreeSet<V>(mapValues);
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

    private Map<String, Integer> clearStopWords(TagCloud tagcloud, int lang)
    {
        Map<String, Integer> words = tagcloud.getCountedWordMap();
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
        for (int i = 0; i < stopWordsArray.length; i++) {
            words.remove(stopWordsArray[i]);
        }

        for (String word : words.keySet()) {
            if (word.indexOf("<") >= 0 || word.indexOf(">") >= 0 || word.indexOf("=") >= 0
                || word.indexOf("\"") >= 0 || word.indexOf("/") >= 0
                || word.indexOf("\u0093") >= 0) {
                if (!this.ignoreList.contains(word)) {
                    this.ignoreList.add(word);
                }
            }
        }

        if (this.ignoreList != null) {
            for (String word : this.ignoreList) {
                if (this.dontignoreList == null || !this.dontignoreList.contains(word)) {
                    words.remove(word);
                }
            }
        }

        tagcloud.setCountedWordMap(words);
        return words;
    }

    private Map<String, Integer> countWords(TagCloud tagcloud)
    {
        String[] words = tagcloud.getWordList();
        Map<String, Integer> wordsCnt = new HashMap<String, Integer>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!wordsCnt.containsKey(word)) {
                wordsCnt.put(word, Integer.valueOf(0));
            }
            wordsCnt.put(word, Integer.valueOf((wordsCnt.get(word)).intValue() + 1));
        }
        tagcloud.setCountedWordMap(wordsCnt);
        return wordsCnt;
    }

    private Map<String, Map<String, Integer>> stemmer(TagCloud tagcloud, int lang)
    {
        Map<String, Integer> words = tagcloud.getCountedWordMap();
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
        tagcloud.setStemmedWordMap(stemmedWordMap);
        return stemmedWordMap;
    }

    /*
    private Map stemmer(TagCloud tagcloud, int lang){
        Map words = tagcloud.getCountedWordMap();
        SnowballProgram stemmer;
        Map stemmedWordMap = new HashMap();

        if (lang == LANG_FRENCH){
            stemmer = new frenchStemmer();
        }
        else {
            stemmer = new englishStemmer();
        }


        Iterator it = words.keySet().iterator();
        while(it.hasNext()) {
            String word = (String) it.next();
            if (word.length() <= 2)
                continue;
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();
            if (!stemmedWordMap.containsKey(stemmedWord))
                stemmedWordMap.put(stemmedWord, new HashMap());
            ((Map) stemmedWordMap.get(stemmedWord)).put(word, words.get(word));
        }
        tagcloud.setStemmedWordMap(stemmedWordMap);
        return stemmedWordMap;
    }
    */

    private String[] splitWords(TagCloud tagcloud)
    {
        String text = tagcloud.getText();
        text = text.replaceAll("\n", " ");
        text = text.replaceAll("\r", " ");
        text = text.replaceAll("'", " ");
        text = text.replaceAll("\u0092", " ");
        text = text.toLowerCase();
        String[] words = text.split("[\\s,.;:!\\?]+");
        tagcloud.setWordList(words);
        return words;
    }

    public int getLanguageConstant(String lang)
    {
        if (lang.trim().toLowerCase().equals("fr")) {
            return AutoTagPlugin.LANG_FRENCH;
        }
        // default english
        return AutoTagPlugin.LANG_ENGLISH;
    }
}
