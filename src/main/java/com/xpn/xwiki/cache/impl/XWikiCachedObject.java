package com.xpn.xwiki.cache.impl;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 24 juil. 2006
 * Time: 03:09:34
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiCachedObject {
    public void finalize() throws Throwable;
}
