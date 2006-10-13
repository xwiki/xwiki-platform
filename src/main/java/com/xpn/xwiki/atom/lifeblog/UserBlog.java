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
 * @author sdumitriu
 */
/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

import com.xpn.xwiki.XWikiException;

/**
 * @author Luis
 * 
 * UserBlog represents a UserBlog entry as per the Lifeblog Posting Protocol Specification.
 */

public class UserBlog {

  /**
   * The user recognizable title of the blog.
   */
  private String title;
  
  /**
   * The url for posting a new entry to this blog.  Something like
   * 
   * http://mywiki.xwiki.com/bin/lifeblog/MyWeb/Blog
   */
  private String postHref;
  
  /**
   * The url for retrieving a feed for this blog.  Something like
   * 
   * http://mywiki.xwiki.com/bin/view/MyWeb/BlogRss?xpage=rdf
   */
  private String feedHref;
  
  /**
   * The url for retrieving an alternate (normal) representation of this blog.  Something like
   * 
   * http://mywiki.xwiki.com/bin/view/MyWeb/Blog
   */
  private String alternateHref;

  /**
   * @return Returns the feedHref.
   */
  public String getFeedHref() {
    return feedHref;
  }

  /**
   * @throws XWikiException 
   * 
   */
  public UserBlog() {
    super();
  }

  /**
   * @return Returns the postHref.
   */
  public String getPostHref() {
    return postHref;
  }

  /**
   * @return Returns the alternateHref.
   */
  public String getAlternateHref() {
    return alternateHref;
  }

  /**
   * @return Returns the title.
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param alternateHref The alternateHref to set.
   */
  public void setAlternateHref(String alternateHref) {
    this.alternateHref = alternateHref;
  }

  /**
   * @param feedHref The feedHref to set.
   */
  public void setFeedHref(String feedHref) {
    this.feedHref = feedHref;
  }

  /**
   * @param postHref The postHref to set.
   */
  public void setPostHref(String postHref) {
    this.postHref = postHref;
  }

  /**
   * @param title The title to set.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  public boolean equals(Object arg0) {
    // TODO Auto-generated method stub
    if (arg0 instanceof UserBlog) {
      UserBlog userBlog = (UserBlog) arg0;
      return this.postHref.equals(userBlog.postHref)
        && this.feedHref.equals(userBlog.feedHref)
        && this.alternateHref.equals(userBlog.alternateHref);
    }
    return false;
  }
}
