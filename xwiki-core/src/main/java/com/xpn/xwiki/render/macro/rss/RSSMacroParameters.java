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
 *
 */


package com.xpn.xwiki.render.macro.rss;

/**
 * A simple bean which encapsulates macro parameters and does some logic,
 * like making sure the <code>feed</code> property, a <code>String</code>
 * is a well-formed URL, and evaluating the <code>count</code> property
 * against a proposed number of items, in light of the possibility that
 * <code>count</code> may not have been defined.
 * @author Joe Germuska
 * @version 0.2d
 */
public class RSSMacroParameters
{

    public static final String DEFAULT_ALIGNMENT = "left";
    public static final int UNDEFINED = -1;
    private String feed = null;
    private String align = DEFAULT_ALIGNMENT;
    private boolean img = false;
    private boolean css = false;
    private int count = UNDEFINED;

    private java.net.URL feedURL = null;
    private boolean full;
    private boolean search;

    public String getFeed() {
        return feed;
    }
    public void setFeed(String feed) throws java.net.MalformedURLException
    {
        this.feed = feed;
        this.feedURL = new java.net.URL(feed);
    }
    public void setAlign(String align) {
        this.align = align;
    }
    public String getAlign() {
        return align;
    }
    public void setImg(boolean img) {
        this.img = img;
    }
    public boolean isImg() {
        return img;
    }
    public void setCss(boolean css) {
        this.css = css;
    }
    public boolean isCss() {
        return true;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public int getCount() {
        return count;
    }
    public java.net.URL getFeedURL() {
        return feedURL;
    }

    /**
     * Given <code>proposed</code> items and the current value of our
     * <code>count</code> property, how many items should be processed?
     * If <code>count</code> is undefined or greater than <code>proposed</code>,
     * return <code>proposed</code>, otherwise return <code>count</code>.
     * @param proposed
     * @return
     */
    public int evalCount(int proposed)
    {
        if (this.count == UNDEFINED) return proposed;
        if (this.count < proposed) return this.count;
        return proposed;
    }
    public void setFull(boolean full) {
        this.full = full;
    }
    public boolean isFull() {
        return full;
    }
    public void setSearch(boolean search) {
        this.search = search;
    }
    public boolean isSearch() {
        return search;
    }

}