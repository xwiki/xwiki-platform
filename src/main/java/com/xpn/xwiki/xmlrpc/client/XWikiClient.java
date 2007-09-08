package com.xpn.xwiki.xmlrpc.client;

import java.util.List;
import java.util.Map;

import com.xpn.xwiki.xmlrpc.model.Attachment;
import com.xpn.xwiki.xmlrpc.model.BlogEntry;
import com.xpn.xwiki.xmlrpc.model.Comment;
import com.xpn.xwiki.xmlrpc.model.Label;
import com.xpn.xwiki.xmlrpc.model.Page;
import com.xpn.xwiki.xmlrpc.model.PageSummary;
import com.xpn.xwiki.xmlrpc.model.ServerInfo;
import com.xpn.xwiki.xmlrpc.model.Space;
import com.xpn.xwiki.xmlrpc.model.User;
import com.xpn.xwiki.xmlrpc.model.UserInformation;

public interface XWikiClient
{

    void login(String username, String password) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * remove this token from the list of logged in tokens. Returns true if the user was logged out,
     * false if they were not logged in in the first place (we don't really need this return, but
     * void seems to kill XML-RPC for me)
     */
    boolean logout() throws XWikiClientException, XWikiClientRemoteException;

    /**
     * exports a Confluence instance and returns a String holding the URL for the download. The
     * boolean argument indicates whether or not attachments ought to be included in the export.
     */
    String exportSite(boolean exportAttachments) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * retrieve some basic information about the server being connected to. Useful for clients that
     * need to turn certain features on or off depending on the version of the server. (Since 1.0.3)
     */
    ServerInfo getServerInfo() throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the {@link SpaceSummary} instances that the current user can see.
     */
    List getSpaces() throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns a single Space.
     */
    Space getSpace(String spaceKey) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * exports a space and returns a String holding the URL for the download. The export type
     * argument indicates whether or not to export in XML, PDF, or HTML format - use "TYPE_XML",
     * "TYPE_PDF", or "TYPE_HTML" respectively. Also, using "all" will select TYPE_XML.
     */
    String exportSpace(String spaceKey, String exportType) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * create a new space, passing in name, key and description.
     */
    Space addSpace(Space space) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * remove a space completely.
     */
    boolean removeSpace(String spaceKey) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the {@link PageSummary} instances in the space. Doesn't include pages which are
     * in the Trash. Equivalent to calling {{Space.getCurrentPages()}}.
     */
    List getPages(String spaceKey) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns a single Page
     */
    Page getPage(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns a single Page
     */
    Page getPage(String spaceKey, String pageTitle) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * returns all the {@link PageHistorySummary} instances - useful for looking up the previous
     * versions of a page, and who changed them.
     */
    List getPageHistory(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the {@link Attachment}s for this page (useful to point users to download them
     * with the full file download URL returned).
     */
    List getAttachments(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the ancestors (as {@link PageSummary} instances) of this page (parent, parent's
     * parent etc).
     */
    List getAncestors(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the direct children (as {@link PageSummary} instances) of this page.
     */
    List getChildren(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the descendents (as {@link PageSummary} instances) of this page (children,
     * children's children etc).
     */
    List getDescendents(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the {@link Comment}s for this page.
     */
    List getComments(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns an individual comment.
     */
    Comment getComment(String commentId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * adds a comment to the page.
     */
    Comment addComment(Comment comment) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * removes a comment from the page.
     */
    boolean removeComment(String commentId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * add or update a page. For adding, the Page given as an argument should have space, title and
     * content fields at a minimum. For updating, the Page given should have id, space, title,
     * content and version fields at a minimum. The parentId field is always optional. All other
     * fields will be ignored.
     */
    Page storePage(Page page) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns the HTML rendered content for this page. If 'content' is provided, then that is
     * rendered as if it were the body of the page (useful for a 'preview page' function). If it's
     * not provided, then the existing content of the page is used instead (ie useful for 'view
     * page' function).
     */
    String renderContent(String spaceKey, String pageId, String content)
        throws XWikiClientException, XWikiClientRemoteException;

    String renderContent(String spaceKey, String pageId) throws XWikiClientException,
        XWikiClientRemoteException;

    String renderContent(PageSummary page) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Like the above renderContent(), but you can supply an optional hash (map, dictionary, etc)
     * containing additional instructions for the renderer. Currently, only one such parameter is
     * supported:
     */
    String renderContent(String spaceKey, String pageId, String content, Map parameters)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * remove a page
     */
    void removePage(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * get information about an attachment.
     */
    Attachment getAttachment(String pageId, String fileName, String versionNumber)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * get the contents of an attachment.
     */
    byte[] getAttachmentData(String pageId, String fileName, String versionNumber)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * add a new attachment to a content entity object. *Note that this uses a lot of memory --
     * about 4 times the size of the attachment.*
     */
    Attachment addAttachment(String pageId, Attachment attachment, byte[] attachmentData)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * remove an attachment from a content entity object.
     */
    boolean removeAttachment(String pageId, String fileName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * move an attachment to a different content entity object and/or give it a new name.
     */
    boolean moveAttachment(String originalPageId, String originalName, String newPageId,
        String newName) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns all the {@link BlogEntrySummary} instances in the space.
     */
    List getBlogEntries(String spaceKey) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * returns a single BlogEntry.
     */
    BlogEntry getBlogEntry(String pageId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * add or update a blog entry. For adding, the BlogEntry given as an argument should have space,
     * title and content fields at a minimum. For updating, the BlogEntry given should have id,
     * space, title, content and version fields at a minimum. All other fields will be ignored.
     */
    BlogEntry storeBlogEntry(BlogEntry entry) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Retrieves a blog post in the Space with the given spaceKey, with the title 'postTitle' and
     * posted on the day 'dayOfMonth'.
     */
    BlogEntry getBlogEntryByDayAndTitle(String spaceKey, int dayOfMonth, String postTitle)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * return a list of {@link SearchResult}s which match a given search query (including pages and
     * other content types). This is the same as a performing a parameterised search (see below)
     * with an empty parameter map.
     */
    List search(String query, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns a list of {@link SearchResult}s like the previous search, but you can optionally
     * limit your search by adding parameters to the parameter map. If you do not include a
     * parameter, the default is used instead.
     */
    List search(String query, Map parameters, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns a List of {@link Permission}s representing the permissions the current user has for
     * this space (a list of "view", "modify", "comment" and / or "admin").
     */
    List getPermissions(String spaceKey) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Returns a List of {@link Permission}s representing the permissions the given user has for
     * this space. (since 2.1.4)
     */
    List getPermissionsForUser(String spaceKey, String userName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns a List of {@link Permission}s representing the permissions set on the given page.
     */
    List getPagePermissions(String pageId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * returns List of the space level {@link Permission}s which may be granted. This is a list of
     * possible permissions to use with {{addPermissionToSpace}}, below, not a list of current
     * permissions on a Space.
     */
    List getSpaceLevelPermissions() throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Give the entity named {{remoteEntityName}} (either a group or a user) the permission
     * {{permission}} on the space with the key {{spaceKey}}.
     */
    boolean addPermissionToSpace(String permission, String remoteEntityName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Give the entity named {{remoteEntityName}} (either a group or a user) the permissions
     * {{permissions}} on the space with the key {{spaceKey}}.
     */
    boolean addPermissionsToSpace(List permissions, String remoteEntityName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Remove the permission {{permission} from the entity named {{remoteEntityName}} (either a
     * group or a user) on the space with the key {{spaceKey}}.
     */
    boolean removePermissionFromSpace(String permission, String remoteEntityName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Give anonymous users the permission {{permission}} on the space with the key {{spaceKey}}.
     * (since 2.0)
     */
    boolean addAnonymousPermissionToSpace(String permission, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Give anonymous users the permissions {{permissions}} on the space with the key {{spaceKey}}.
     * (since 2.0)
     */
    boolean addAnonymousPermissionsToSpace(List permissions, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Remove the permission {{permission} from anonymous users on the space with the key
     * {{spaceKey}}. (since 2.0)
     */
    boolean removeAnonymousPermissionFromSpace(String permission, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Remove all the global and space level permissions for {{groupname}}.
     */
    boolean removeAllPermissionsForGroup(String groupname) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * get a single user
     */
    User getUser(String username) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * add a new user with the given password
     */
    void addUser(User user, String password) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * add a new group
     */
    void addGroup(String group) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * get a user's current groups as a list of {@link String}s
     */
    List getUserGroups(String username) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * add a user to a particular group
     */
    void addUserToGroup(String username, String groupname) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * remove a user from a group.
     */
    boolean removeUserFromGroup(String username, String groupname) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * delete a user.
     */
    boolean removeUser(String username) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * remove a group. If {{defaultGroupName}} is specified, users belonging to {{groupname}} will
     * be added to {{defaultGroupName}}.
     */
    boolean removeGroup(String groupname, String defaultGroupName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * gets all groups as a list of {@link String}s
     */
    List getGroups() throws XWikiClientException, XWikiClientRemoteException;

    /**
     * checks if a user exists
     */
    boolean hasUser(String username) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * checks if a group exists
     */
    boolean hasGroup(String groupname) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * edits the details of a user
     */
    boolean editUser(User remoteUser) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * deactivates the specified user
     */
    boolean deactivateUser(String username) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * reactivates the specified user
     */
    boolean reactivateUser(String username) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * returns all registered users as Strings
     */
    List getActiveUsers(boolean viewAll) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * updates user information
     */
    boolean setUserInformation(UserInformation userInfo) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Retrieves user information
     */
    UserInformation getUserInformation(String username) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * changes the current user's password
     */
    boolean changeMyPassword(String oldPass, String newPass) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * changes the specified user's password
     */
    boolean changeUserPassword(String username, String newPass) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns all {@link Label}s for the given ContentEntityObject ID
     */
    List getLabelsById(long objectId) throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Returns the most popular {@link Label}s for the Confluence instance, with a specified
     * maximum number.
     */
    List getMostPopularLabels(int maxCount) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the most popular {@link Label}s for the given {{spaceKey}}, with a specified maximum
     * number of results.
     */
    List getMostPopularLabelsInSpace(String spaceKey, int maxCount) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the recently used {@link Label}s for the Confluence instance, with a specified
     * maximum number of results.
     */
    List getRecentlyUsedLabels(int maxResults) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the recently used {@link Label}s for the given {{spaceKey}}, with a specified
     * maximum number of results.
     */
    List getRecentlyUsedLabelsInSpace(String spaceKey, int maxResults)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Returns a list of {@link Space}s that have been labeled with {{labelName}}.
     */
    List getSpacesWithLabel(String labelName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the {@link Label}s related to the given label name, with a specified maximum number
     * of results.
     */
    List getRelatedLabels(String labelName, int maxResults) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the {@link Label}s related to the given label name for the given {{spaceKey}}, with
     * a specified maximum number of results.
     */
    List getRelatedLabelsInSpace(String labelName, String spaceKey, int maxResults)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Retrieves the {@link Label}s matching the given {{labelName}}, {{namespace}}, {{spaceKey}}
     * or {{owner}}.
     */
    List getLabelsByDetail(String labelName, String namespace, String spaceKey, String owner)
        throws XWikiClientException, XWikiClientRemoteException;

    /**
     * Returns the content for a given label ID
     */
    List getLabelContentById(long labelId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the content for a given label name.
     */
    List getLabelContentByName(String labelName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns the content for a given Label object.
     */
    List getLabelContentByObject(Label labelObject) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Returns all Spaces that have content labeled with {{labelName}}.
     */
    List getSpacesContainingContentWithLabel(String labelName) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Adds a label to the object with the given ContentEntityObject ID.
     */
    boolean addLabelByName(String labelName, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Adds a label with the given ID to the object with the given ContentEntityObject ID.
     */
    boolean addLabelById(long labelId, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Adds the given label object to the object with the given ContentEntityObject ID.
     */
    boolean addLabelByObject(Label labelObject, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Adds a label to the object with the given ContentEntityObject ID.
     */
    boolean addLabelByNameToSpace(String labelName, String spaceKey) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Removes the given label from the object with the given ContentEntityObject ID.
     */
    boolean removeLabelByName(String labelName, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Removes the label with the given ID from the object with the given ContentEntityObject ID.
     */
    boolean removeLabelById(long labelId, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Removes the given label object from the object with the given ContentEntityObject ID.
     */
    boolean removeLabelByObject(Label labelObject, long objectId) throws XWikiClientException,
        XWikiClientRemoteException;

    /**
     * Removes the given label from the given {{spaceKey}}.
     */
    boolean removeLabelByNameFromSpace(String labelName, String spaceKey)
        throws XWikiClientException, XWikiClientRemoteException;

    // XWiki-only methods

    List getAttachmentVersions(String pageId, String fileName) throws XWikiClientRemoteException,
        XWikiClientException;

    void setNoConversion() throws XWikiClientRemoteException, XWikiClientException;

}
