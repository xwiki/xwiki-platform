/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author jeremi
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.feed;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class FeedPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
        private XWikiCache feedCache;
        private int refreshPeriod;

      public static class SyndEntryComparator implements Comparator {

          public int compare(Object element1, Object element2) {
            SyndEntry entry1 = (SyndEntry) element1;
            SyndEntry entry2 = (SyndEntry) element2;

            if ((entry1.getPublishedDate() == null) &&  (entry2.getPublishedDate() == null))
                return 0;
            if (entry1.getPublishedDate() == null)
                return 1;
            if (entry2.getPublishedDate() == null)
                return -1;
            return (-entry1.getPublishedDate().compareTo(entry2.getPublishedDate()));
        }
    }


      public static class EntriesComparator implements Comparator {

          public int compare(Object element1, Object element2) {
            BaseObject entry1 = ((com.xpn.xwiki.api.Object) element1).getXWikiObject();
            BaseObject entry2 = ((com.xpn.xwiki.api.Object) element2).getXWikiObject();

            if ((entry1.getDateValue("date") == null) &&  (entry2.getDateValue("date") == null))
                return 0;
            if (entry1.getDateValue("date") == null)
                return 1;
            if (entry2.getDateValue("date") == null)
                return -1;
            return (-entry1.getDateValue("date").compareTo(entry2.getDateValue("date")));
        }
    }

    public FeedPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    public String getName() {
        return "feed";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new FeedPluginApi((FeedPlugin) plugin, context);
    }

    public void flushCache() {
        if (feedCache!=null)
            feedCache.flushAll();
        feedCache = null;
    }

    public void init(XWikiContext context) {
        super.init(context);
        prepareCache(context);
        refreshPeriod = (int) context.getWiki().ParamAsLong("xwiki.plugins.feed.cacherefresh", 3600);
        // Make sure we have this class
        try {
            getAggregatorURLClass(context);
        } catch (XWikiException e) {
        }
        // Make sure we have this class
        try {
            getFeedEntryClass(context);
        } catch (XWikiException e) {
        }
    }


    public void initCache(XWikiContext context) throws XWikiException {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.plugins.feed.cache.capacity");
            if (capacity != null)
                iCapacity = Integer.parseInt(capacity);
        } catch (Exception e) {}

        initCache(iCapacity, context);
    }

    public void initCache(int iCapacity, XWikiContext context) throws XWikiException {
            feedCache = context.getWiki().getCacheService().newLocalCache(iCapacity);
    }

    protected void prepareCache(XWikiContext context) {
        try {
            if (feedCache==null)
                initCache(context);
        } catch (XWikiException e) {
        }
    }

    public SyndFeed getFeeds(String sfeeds, XWikiContext context) throws IOException {
            return getFeeds(sfeeds, false, true, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean force, XWikiContext context) throws IOException {
            return getFeeds(sfeeds, false, force, context);
    }

    public SyndFeed getFeeds(String sfeeds, boolean ignoreInvalidFeeds, boolean force, XWikiContext context) throws IOException {
        String[] feeds;
        if (sfeeds.indexOf("\n") != -1)
            feeds = sfeeds.split("\n");
        else
            feeds = sfeeds.split("\\|");
        List entries = new ArrayList();
        SyndFeed outputFeed = new SyndFeedImpl();
        if (context.getDoc() != null)
        {
            outputFeed.setTitle(context.getDoc().getFullName());
            try {
                outputFeed.setUri(context.getWiki().getURL(context.getDoc().getFullName(), "view", context));
            } catch (XWikiException e) {
                e.printStackTrace();
            }
            outputFeed.setAuthor(context.getDoc().getAuthor());
        }
        else
        {
            outputFeed.setTitle("XWiki Feeds");
            outputFeed.setAuthor("XWiki Team");
        }
        outputFeed.setEntries(entries);
        for (int i = 0; i < feeds.length; i++)
        {
            SyndFeed feed = getFeed(feeds[i], ignoreInvalidFeeds, force, context);
            if (feed != null)
                entries.addAll(feed.getEntries());
        }
        SyndEntryComparator comp = new SyndEntryComparator();
        Collections.sort(entries, comp);
        return outputFeed;
    }


    public SyndFeed getFeed(String sfeed, XWikiContext context) throws IOException {
        return getFeed(sfeed, true, false, context);
    }

    public SyndFeed getFeed(String sfeed, boolean force, XWikiContext context) throws IOException {
        return getFeed(sfeed, true, force, context);
    }

    public SyndFeed getFeed(String sfeed, boolean ignoreInvalidFeeds, boolean force, XWikiContext context) throws IOException {
        SyndFeed feed = null;
        prepareCache(context);
        if (!force) {
            try {
                feed = (SyndFeed) feedCache.getFromCache(sfeed, refreshPeriod);

            } catch (XWikiCacheNeedsRefreshException e) {
                feedCache.cancelUpdate(sfeed);
            } catch (Exception e) {
            }
        }

        if (feed==null)
         feed = getFeedForce(sfeed, ignoreInvalidFeeds, context);

        if (feed!=null)
         feedCache.putInCache(sfeed, feed);

        return feed;
    }

    public SyndFeed getFeedForce(String sfeed, boolean ignoreInvalidFeeds, XWikiContext context) throws IOException {
            try {
                URL feedURL = new URL(sfeed);
                XWikiFeedFetcher feedFetcher = new XWikiFeedFetcher();
                feedFetcher.setUserAgent(context.getWiki().Param("xwiki.plugins.feed.useragent", context.getWiki().getHttpUserAgent(context)));
                SyndFeed feed = feedFetcher.retrieveFeed(feedURL, (int) context.getWiki().ParamAsLong("xwiki.plugins.feed.timeout", context.getWiki().getHttpTimeout(context)) );
                return feed;
            }
            catch (Exception ex) {
                if (ignoreInvalidFeeds) {
                    Map map = (Map) context.get("invalidFeeds");
                    if (map==null) {
                        map = new HashMap();
                        context.put("invalidFeeds", map);
                    }
                    map.put(sfeed, ex);
                    return null;
                }

                throw new java.io.IOException("Error processing " + sfeed + ": " + ex.getMessage());
            }
        }

    public int updateFeeds(XWikiContext context) throws XWikiException {
        return updateFeeds("XWiki.FeedList", context);
    }
    
    public int updateFeeds(String feedDoc, XWikiContext context) throws XWikiException {
        return updateFeeds(feedDoc, false, context);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, XWikiContext context) throws XWikiException {
        return updateFeeds(feedDoc, fullContent, true, context);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, XWikiContext context) throws XWikiException {
        return updateFeeds(feedDoc, fullContent, oneDocPerEntry, false, context);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force, XWikiContext context) throws XWikiException {
        return updateFeeds(feedDoc, fullContent, oneDocPerEntry, force, "Feeds", context);
    }

    public int updateFeeds(String feedDoc, boolean fullContent, boolean oneDocPerEntry, boolean force, String space, XWikiContext context) throws XWikiException {
        // Make sure we have this class
        getAggregatorURLClass(context);

        XWikiDocument doc = context.getWiki().getDocument(feedDoc, context);
        Vector objs = doc.getObjects("XWiki.AggregatorURLClass");
        if (objs==null)
         return 0;

        Iterator it = objs.iterator();
        int total = 0;
        int nbfeeds  = 0;
        int nbfeedsError = 0;
        while(it.hasNext())
        {
            BaseObject obj = (BaseObject) it.next();
            if (obj!=null) {
                String feedurl = obj.getStringValue("url");
                String feedname = obj.getStringValue("name");
                nbfeeds++;
                int nb = updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force, space, context);
                if (nb!=-1)
                    total += nb;
                else
                    nbfeedsError++;
                if (context.getEngineContext()!=null) {
                    context.getEngineContext().setAttribute("feedNbFeeds", new Integer(nbfeeds));
                    context.getEngineContext().setAttribute("feedNbFeedsError", new Integer(nbfeedsError));
                    context.getEngineContext().setAttribute("feedNbArticles", new Integer(total));
                }
                if (context.get("feedimgurl")!=null) {
                    obj.set("imgurl", context.get("feedimgurl"),context);
                    context.remove("feedimgurl");
                }
                obj.set("nb", new Integer(nb), context);
                obj.set("date", new Date(), context);

                // Update original document
                context.getWiki().saveDocument(doc, context);
            }
        }
        return total;
    }

    public int updateFeedsInSpace(boolean fullContent, boolean oneDocPerEntry, boolean force, String space, XWikiContext context) throws XWikiException {
        // Make sure we have this class
        getAggregatorURLClass(context);

        String sql = ", BaseObject as obj where doc.fullName=obj.name and obj.className='XWiki.AggregatorURLClass' and doc.web='" + space + "'";
        int total = 0;
        List feedDocList = context.getWiki().getStore().searchDocumentsNames(sql, context);
        if (feedDocList!=null) {
            for (int i=0;i<feedDocList.size();i++) {
                String feedDocName = (String) feedDocList.get(i);
                total += updateFeeds(feedDocName, fullContent, oneDocPerEntry, force, space, context);
            }
        }
        return total;
    }

    public int updateFeed(String feedname, String feedurl, boolean oneDocPerEntry, XWikiContext context) {
        return updateFeed(feedname, feedurl, false, oneDocPerEntry, context);
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent, boolean oneDocPerEntry, XWikiContext context) {
        return updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, false, context);
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent,  boolean oneDocPerEntry, boolean force, XWikiContext context) {
        return updateFeed(feedname, feedurl, fullContent, oneDocPerEntry, force, "Feeds", context);
    }

    public int updateFeed(String feedname, String feedurl, boolean fullContent,  boolean oneDocPerEntry, boolean force, String space, XWikiContext context) {
        try {
            // Make sure we have this class
            getFeedEntryClass(context);

            SyndFeed feed = getFeedForce(feedurl, true, context);
            if (feed != null) {
                if (feed.getImage()!=null)
                 context.put("feedimgurl", feed.getImage().getUrl());
                return saveFeed(feedname, feedurl, feed, fullContent, oneDocPerEntry, force, space, context);
            } else
             return 0;
        }
        catch(Exception e)
        {
            Map map = (Map) context.get("updateFeedError");
            if (map==null) {
                map = new HashMap();
                context.put("updateFeedError", map);
            }
            map.put(feedurl, e);
        }
        return -1;
    }

    private int saveFeed(String feedname, String feedurl, SyndFeed feed, boolean fullContent, boolean oneDocPerEntry, boolean force, XWikiContext context) throws XWikiException {
        return saveFeed(feedname, feedurl, feed, fullContent, oneDocPerEntry, force, "Feeds", context);
    }

    private int saveFeed(String feedname, String feedurl, SyndFeed feed, boolean fullContent, boolean oneDocPerEntry, boolean force, String space, XWikiContext context) throws XWikiException {
        XWikiDocument doc = null;
        Vector objs = null;
        int nbtotal = 0;

        String prefix = space + ".Feed";
        if (!oneDocPerEntry) {
          doc = context.getWiki().getDocument(prefix + "_" + context.getWiki().clearName(feedname, true, true, context), context);
          objs = doc.getObjects("XWiki.FeedEntryClass");
            if ((doc.getContent()==null)||doc.getContent().trim().equals(""))
              doc.setContent("#includeForm(\"XWiki.FeedEntryClassSheet\")");
        }

        List entries = feed.getEntries();
        int nb = entries.size();
        for (int i=nb-1;i>=0;i--) {
            SyndEntry entry = (SyndEntry) entries.get(i);
            if (oneDocPerEntry) {
                String hashCode = "" + entry.getLink().hashCode();
                String pagename = feedname  + "_" + hashCode.replaceAll("-","") + "_" + entry.getTitle();
                doc = context.getWiki().getDocument(prefix + "_" + context.getWiki().clearName(pagename, true, true, context), context);
                if (doc.isNew() || force) {
                    // Set the document date to the current date
                    doc.setDate(new Date());
                    // Set the creation date to the feed date if it exists, otherwise the current date
                    Date adate = (entry.getPublishedDate()==null) ? new Date() : entry.getPublishedDate();
                    doc.setCreationDate(adate);
                    if ((doc.getContent()==null)||doc.getContent().trim().equals(""))
                        doc.setContent("#includeForm(\"XWiki.FeedEntryClassSheet\")");
                    if (force) {
                        BaseObject obj = doc.getObject("XWiki.FeedEntryClass");
                        if (obj==null)
                         saveEntry(feedname, feedurl, entry, doc, fullContent, context);
                        else
                         saveEntry(feedname, feedurl, entry, doc, obj, fullContent, context);
                    } else
                     saveEntry(feedname, feedurl, entry, doc, fullContent, context);
                    nbtotal++;
                    context.getWiki().saveDocument(doc, context);
                }
            } else {
                BaseObject obj = postExist(objs, entry , context);
                if (obj==null) {
                 saveEntry(feedname, feedurl, entry, doc, fullContent, context);
                 nbtotal++;
                } else if (force) {
                    saveEntry(feedname, feedurl, entry, doc, obj, fullContent, context);
                    nbtotal++;
                }
            }
        }

        if (!oneDocPerEntry)
         context.getWiki().saveDocument(doc, context);

        return nbtotal;
    }

    public BaseClass getAggregatorURLClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = context.getWiki().getDocument("XWiki.AggregatorURLClass", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.AggregatorURLClass");
        if (!"internal".equals(bclass.getCustomMapping())) {
            needsUpdate = true;
            bclass.setCustomMapping("internal");
        }

        needsUpdate |= bclass.addTextField("name", "Name", 80);
        needsUpdate |= bclass.addTextField("url", "url", 80);
        needsUpdate |= bclass.addTextField("imgurl", "Image url", 80);
        needsUpdate |= bclass.addDateField("date", "date", "dd/MM/yyyy HH:mm:ss");
        needsUpdate |= bclass.addNumberField("nb","nb",5,"integer");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("#includeForm(\"XWiki.ClassSheet\")");
        }

        if (needsUpdate)
            context.getWiki().saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getFeedEntryClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = context.getWiki().getDocument("XWiki.FeedEntryClass", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.FeedEntryClass");
        if (!"internal".equals(bclass.getCustomMapping())) {
            needsUpdate = true;
            bclass.setCustomMapping("internal");
        }

        needsUpdate |= bclass.addTextField("title", "Title", 80);
        needsUpdate |= bclass.addTextField("author", "Author", 40);
        needsUpdate |= bclass.addTextField("feedurl", "Feed URL", 80);
        needsUpdate |= bclass.addTextField("feedname", "Feed Name", 40);
        needsUpdate |= bclass.addTextField("url", "URL", 80);
        needsUpdate |= bclass.addTextField("category", "Category", 255);
        needsUpdate |= bclass.addTextAreaField("content", "Content", 80, 10);
        needsUpdate |= bclass.addTextAreaField("fullContent", "Full Content", 80, 10);
        needsUpdate |= bclass.addTextAreaField("xml", "XML", 80, 10);
        needsUpdate |= bclass.addDateField("date", "date", "dd/MM/yyyy HH:mm:ss");
        needsUpdate |= bclass.addNumberField("flag", "Flag", 5, "integer");
        needsUpdate |= bclass.addNumberField("read", "Read", 5, "integer");
        needsUpdate |= bclass.addStaticListField("tags", "Tags", 1, true, "");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("#includeForm(\"XWiki.ClassSheet\")");
        }

        if (needsUpdate)
            context.getWiki().saveDocument(doc, context);
        return bclass;

    }

    private void saveEntry(String feedname, String feedurl, SyndEntry entry, XWikiDocument doc, boolean fullContent, XWikiContext context) throws XWikiException {
        int id = doc.createNewObject("XWiki.FeedEntryClass", context);
        BaseObject obj = doc.getObject("XWiki.FeedEntryClass", id);
        saveEntry(feedname, feedurl, entry, doc, obj, fullContent, context);
    }

    private void saveEntry(String feedname, String feedurl, SyndEntry entry, XWikiDocument doc, BaseObject obj, boolean fullContent, XWikiContext context) throws XWikiException {
        obj.setStringValue("feedname", feedname);
        obj.setStringValue("title", entry.getTitle());
        obj.setIntValue("flag", 0);
        List categList = entry.getCategories();
        StringBuffer categs = new StringBuffer("");
        if (categList != null)
        {
            Iterator it = categList.iterator();
            while(it.hasNext())
            {
                SyndCategory categ = (SyndCategory) it.next();
                if (categs.length() != 0)
                    categs.append(", ");
                categs.append(categ.getName());
            }
        }
        obj.setStringValue("category", categs.toString());

        StringBuffer contents = new StringBuffer("");
        String description = (entry.getDescription()==null) ? null : entry.getDescription().getValue();

        List contentList = entry.getContents();
        if (contentList != null && contentList.size() > 0)
        {
            Iterator it = contentList.iterator();
            while(it.hasNext())
            {
                SyndContent content =  (SyndContent) it.next();
                if (contents.length() != 0)
                    contents.append("\n");
                contents.append(content.getValue());
            }
        }

        // If we find more data in the description we will use that one instead of the content field
        if ((description!=null)&&(description.length()>contents.length()))
         obj.setLargeStringValue("content", description);
        else
         obj.setLargeStringValue("content", contents.toString());

        Date edate = entry.getPublishedDate();
        if (edate==null)
         edate = new Date();

        obj.setDateValue("date", edate);
        obj.setStringValue("url", entry.getLink());
        obj.setStringValue("author", entry.getAuthor());
        obj.setStringValue("feedurl", feedurl);

        // TODO: need to get entry xml or serialization
        // obj.setLargeStringValue("xml", entry.toString());
        obj.setLargeStringValue("xml", "");

        if (fullContent) {
            String url = entry.getLink();
            if ((url!=null)&&(!url.trim().equals(""))) {
                try {
                    String sfullContent = context.getWiki().getURLContent(url, context);
                    obj.setLargeStringValue("fullContent", (sfullContent.length()>65000) ? sfullContent.substring(0,65000) : sfullContent);
                } catch (Exception e) {
                    obj.setLargeStringValue("fullContent", "Exception while reading fullContent: " + e.getMessage());
                }
            } else {
                obj.setLargeStringValue("fullContent", "No url");                
            }
        }
    }

    private BaseObject postExist(Vector objs, SyndEntry entry, XWikiContext context)
    {
        if (objs == null)
            return null;
        Iterator it = objs.iterator();
        String title = context.getWiki().clearName(entry.getTitle(), true, true, context);
        while (it.hasNext())
        {
            BaseObject obj = (BaseObject) it.next();
            if (obj != null)
            {
                String title2 = obj.getStringValue("title");
                if (title2==null)
                 title2 = "";
                else
                 title2 = context.getWiki().clearName(title2, true, true, context);

                if (title2.compareTo(title) == 0) {
                    return obj;
                }
            }
        }
        return null;
    }

    public List search(String query, XWikiContext context) throws XWikiException {
        String[] queryTab = query.split(" ");

        if (queryTab.length > 0)
        {
            String sql = "select distinct obj.number, obj.name from BaseObject as obj, StringProperty as prop , LargeStringProperty as lprop " +
                    "where obj.className='XWiki.FeedEntryClass' and obj.id=prop.id.id and obj.id=lprop.id.id ";

            for (int i = 0; i < queryTab.length; i++)
                sql += " and (prop.value LIKE '%" + queryTab[i] + "%' or lprop.value LIKE '%" + queryTab[i] + "%')";
            List res = context.getWiki().search(sql, context);

            if (res == null)
                return null;

            Iterator it = res.iterator();
            List apiObjs = new ArrayList();
            while (it.hasNext())
            {
                try {
                    Object obj[] = (Object[]) it.next();
                    XWikiDocument doc = context.getWiki().getDocument((String) obj[1], context);
                    if (context.getWiki().getRightService().checkAccess("view", doc, context))
                    {
                        BaseObject bObj = doc.getObject("XWiki.FeedEntryClass", ((Integer)obj[0]).intValue());
                        com.xpn.xwiki.api.Object apiObj = new com.xpn.xwiki.api.Object(bObj, context);
                        apiObjs.add(apiObj);
                    }
                }
                catch(Exception e)
                {
                    Map map = (Map) context.get("searchFeedError");
                    if (map==null) {
                        map = new HashMap();
                        context.put("searchFeedError", map);
                    }
                    map.put(query, e);
                }
            }

            Collections.sort(apiObjs, new EntriesComparator());
            return apiObjs;
        }
        return null;
    }

    public com.xpn.xwiki.api.Object getFeedInfosbyGuid(String guid, XWikiContext context) throws XWikiException {
        return getFeedInfosbyGuid("XWiki.FeedList", guid, context);      
    }

    public com.xpn.xwiki.api.Object getFeedInfosbyGuid(String feedDoc, String guid, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(feedDoc, context);
        Vector objs = doc.getObjects("XWiki.AggregatorURLClass");
        Iterator it = objs.iterator();
        while(it.hasNext())
        {
            BaseObject obj = (BaseObject) it.next();
            if (guid.compareTo(obj.getStringValue("guid")) == 0)
                return new com.xpn.xwiki.api.Object(obj, context);
        }
        return null;
    }
}
