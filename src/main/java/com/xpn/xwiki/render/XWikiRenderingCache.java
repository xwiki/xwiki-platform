package com.xpn.xwiki.render;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 8 sept. 2005
 * Time: 14:59:01
 * To change this template use File | Settings | File Templates.
 */
public class XWikiRenderingCache {
    private String key;
    private String content;
    private int cacheDuration;
    private Date date;

    public XWikiRenderingCache(String key, String content, int cacheDuration, Date date) {
        this.setKey(key);
        this.setContent(content);
        this.setCacheDuration(cacheDuration);
        this.setDate(date);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getCacheDuration() {
        return cacheDuration;
    }

    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isValid() {
        Date cdate = new Date();
        return ((cdate.getTime() - getDate().getTime())<cacheDuration*1000);
    }
}
