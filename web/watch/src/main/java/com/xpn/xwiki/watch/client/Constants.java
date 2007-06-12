package com.xpn.xwiki.watch.client;

/**
 * Copyright 2006,XpertNet SARL,and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ldubost
 */

public class Constants {

    public Constants() {};

    public static final int DEFAULT_PARAM_NB_ARTICLES_PER_PAGE = 10;
    public static final int DEFAULT_PARAM_NEWARTICLES_MONITORING_TIMER = 30000;
    public static final String DEFAULT_LOCALE = "en";
    // "remote" means favicon are directly taken from feed site
    // "local" means favicon are taken from XWiki 
    public static final String DEFAULT_FEEDS_FAVICON = "remote";

    public static final String WEBAPPNAME = "xwiki";

    public static final String CSS_PREFIX = "watch";

    public static final String DEFAULT_WATCH_SPACE = "Watch";
    public static final String DEFAULT_CODE_SPACE = "WatchCode";
    public static final String DEFAULT_SHEETS_SPACE = "WatchSheets";
    public static final String DEFAULT_QUERIES_SPACE = "WatchQueries";
    public static final String DEFAULT_TEMPLATE_SPACE = "WatchTemplate";
    public static final String DEFAULT_TRANSLATIONS_PAGE = "WatchCode.Translations";

    // Query pages for custom queries
    public static final String QUERY_PAGE_TAGSLIST = "TagsList";
    public static final String QUERY_PAGE_NEWARTICLES = "NewArticles";
    public static final String QUERY_PAGE_ARTICLENUMBER = "ArticleNumber";

    // Analysis actions
    public static final String PAGE_PRESSREVIEW = "PressReview";
    public static final String PAGE_TAGCLOUD = "TagCloud";
    public static final String PAGE_LOADING_STATUS = "LoadingStatus";
    public static final String PAGE_PREVIEW_FEED = "PreviewFeed";

    public static final String IMAGE_PRESS_REVIEW = "pressreview.png";
    public static final String IMAGE_ANALYSIS = "analysis.png";
    public static final String IMAGE_HIDE_READ = "show-read.png";
    public static final String IMAGE_SHOW_READ = "hide-read.png";
    public static final String IMAGE_CONFIG = "config.png";
    public static final String IMAGE_REFRESH = "refreshData.png";
    public static final String IMAGE_MORE = "more.png";
    public static final String IMAGE_FLAG_ON = "news-flag2.png";
    public static final String IMAGE_FLAG_OFF = "news-noflag2.png";
    public static final String IMAGE_TRASH_ON = "news-trash2.png";
    public static final String IMAGE_TRASH_OFF = "news-untrash2.png";
    public static final String IMAGE_EXT_LINK = "news-ext2.png";

    public static final String CLASS_AGGREGATOR_URL = "XWiki.AggregatorURLClass";
    public static final String PROPERTY_AGGREGATOR_URL_NAME = "name";
    public static final String PROPERTY_AGGREGATOR_URL_URL = "url";
    public static final String PROPERTY_AGGREGATOR_URL_GROUPS = "group";
    public static final String CLASS_AGGREGATOR_KEYWORD = "XWiki.KeywordClass";
    public static final String PROPERTY_KEYWORD_NAME = "name";
    public static final String PROPERTY_KEYWORD_GROUP = "group";
    public static final String CLASS_AGGREGATOR_GROUP = "XWiki.AggregatorGroupClass";
    public static final String PROPERTY_GROUP_NAME = "name";
}
