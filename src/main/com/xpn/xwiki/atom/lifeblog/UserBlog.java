/**
 * 
 */
package com.xpn.xwiki.atom.lifeblog;

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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("<link type=\"application/atom+xml\" rel=\"service.post\" href=\"");
    sb.append(getPostHref());
    sb.append("\"/>\n");
    return sb.toString();
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
