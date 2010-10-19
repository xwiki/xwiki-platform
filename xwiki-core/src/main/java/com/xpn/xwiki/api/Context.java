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
package com.xpn.xwiki.api;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.Programming;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * The <code>Context</code> class represents a secure proxy for the XWiki context, which in turn represents the
 * execution environment for all the wiki pages. An instance of the <code>Context</code> class is available as a
 * predefined variable for scripting inside any wiki page. You can access it using <code>$xcontext</code> in Velocity
 * scripts or simply <code>xcontext</code> in Groovy ones. The <code>Context</code> class provides a means of getting
 * contextual information about the current request or configuring XWiki on the fly.
 * 
 * @version $Id$
 */
public class Context extends Api
{
    /**
     * The Constructor.
     * 
     * @param context The {@link com.xpn.xwiki.XWikiContext} to wrap.
     */
    public Context(XWikiContext context)
    {
        super(context);
    }

    /**
     * Returns the current request object. If the request has been made to a servlet container using the HTTP protocol
     * then the returned object wraps a <code>HttpServletRequest</code> instance.
     * 
     * @return an object wrapping the current request object
     */
    public XWikiRequest getRequest()
    {
        return getXWikiContext().getRequest();
    }

    /**
     * Returns the current response object. If the request has been made to a servlet container using the HTTP protocol
     * then the returned object wraps a <code>HttpServletResponse</code> instance.
     * 
     * @return an object wrapping the current response object
     */
    public XWikiResponse getResponse()
    {
        return getXWikiContext().getResponse();
    }

    /**
     * Specifies the container or environment in which XWiki is currently running. See the following table for possible
     * values it can return:
     * <table>
     * <thead>
     * <tr>
     * <th>Return</th>
     * <th>Meaning</th>
     * </tr>
     * </thead> <tbody>
     * <tr>
     * <td>0</td>
     * <td>Servlet Container</td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>Portlet Container</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>XML RPC</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>Atom</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>PDF</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>GWT</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>GWT Debugging</td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @return an integer constant identifying the container or environment in which XWiki is currently running
     */
    public int getMode()
    {
        return getXWikiContext().getMode();
    }

    /**
     * Returns the current database name. If {@link #isMainWiki()} returns <code>true</code> then the current database
     * name is the same as {@link #getMainWikiName()}. Otherwise, in virtual mode, each virtual wiki has it's own
     * database. In this case the returned string identifies the current virtual wiki we operate on and prefixes
     * document names like in <i>databaseName:spaceName.pageName</i>.
     * 
     * @return The current database name.
     * @see #isMainWiki()
     * @see #getMainWikiName()
     */
    public String getDatabase()
    {
        return getXWikiContext().getDatabase();
    }

    /**
     * Returns the name of the main wiki. In non-virtual mode there is only one wiki called <i>xwiki</i>. In virtual
     * mode, the main wiki stores information about all the other virtual wikis.
     * 
     * @return The name of the main wiki.
     */
    public String getMainWikiName()
    {
        return getXWikiContext().getMainXWiki();
    }

    /**
     * Returns the name of the original database. Here, original means the database corresponding to the requested URL,
     * which can be changed when including a document from a different database, by using, for example,
     * <code>#includeTopic("virtualwiki:Some.Document")</code>.
     * 
     * @return The original database name
     */
    public String getOriginalDatabase()
    {
        return getXWikiContext().getOriginalDatabase();
    }

    /**
     * Sets the current database. You need programming rights to be able to call this method.
     * 
     * @param database a database name
     * @see #getDatabase()
     */
    @Programming
    public void setDatabase(String database)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().setDatabase(database);
        }
    }

    /**
     * Returns the URL factory matching both the protocol used to make the current request and the container or
     * environment in which XWiki is running. The most used concrete implementations of the <code>XWikiURLFactory</code>
     * are <code>XWikiServletURLFactory</code> and <code>XWikiPortletURLFactory</code>. <code>XWikiURLFactory</code>
     * offers a generic way of creating XWiki specific URLs that should be chosen instead of the basic string
     * concatenation. Hard-coding the protocol used, like HTTP, inside wiki pages should be avoided.
     * 
     * @return The URL factory.
     */
    public XWikiURLFactory getURLFactory()
    {
        return getXWikiContext().getURLFactory();
    }

    /**
     * <p>
     * Specifies if the current request was made to a virtual (non-main) wiki, or to the main wiki.
     * </p>
     * <p>
     * In virtual mode the server can host more than one wiki, each having it's own database and its own URL. We refer
     * to them as <i>virtual wikis</i>. One of them stores information about the others and it is called <i>the main
     * wiki</i>. You can switch to virtual mode by changing the <code>xwiki.virtual</code> parameter from <code>0</code>
     * to <code>1</code> in the configuration file.
     * </p>
     * 
     * @return <code>true</code> if XWiki is in the main wiki, or if virtual mode is disabled.
     * @see #getDatabase()
     * @see #getMainWikiName()
     */
    public boolean isMainWiki()
    {
        return getXWikiContext().isMainWiki();
    }

    /**
     * Returns the current action. XWiki is built on top of the Struts framework, so here "action" means a StrutsAction.
     * Some predefined actions are: <i>view</i>, <i>edit</i>, <i>delete</i>, <i>download</i> and <i>export</i>. Custom
     * actions can be defined. The action and it's target are extracted from the request URL. For instance, in
     * http://platform.xwiki.org/xwiki/bin/view/DevGuide/API the action is <i>view</i> and it's target is the
     * <i>DevGuide.API</i> document.
     * 
     * @return The action corresponding to the request URL.
     */
    public String getAction()
    {
        return getXWikiContext().getAction();
    }

    /**
     * Returns the language of the current request. If <code>multilingual</code> is turned off then the language used is
     * given by the <code>default_language</code> preference. Otherwise, the language is take from either the request
     * object, the cookie, user preferences or from the navigator language settings, the last having the lower priority.
     * 
     * @return The language of the current request.
     * @see #getInterfaceLanguage()
     */
    public String getLanguage()
    {
        return getXWikiContext().getLanguage();
    }

    /**
     * Returns the interface language preference of the current user. If <code>multilingual</code> is turned off then
     * the language used is given by the <code>default_language</code> preference. Otherwise, the language is take from
     * either the request object, the context, the cookie, user preferences or from the navigator language settings, the
     * last having the lower priority.
     * 
     * @return The interface language preference of the current user.
     * @see #getLanguage()
     */
    public String getInterfaceLanguage()
    {
        return getXWikiContext().getInterfaceLanguage();
    }

    /**
     * Returns the XWiki object. Programming rights are needed in order to call this method. If programming rights are
     * not available in the current document, the XWiki object can still be accessed through a secure API available as a
     * predefined variable for scripting inside wiki pages; use <code>$xwiki</code> in Velocity.
     * 
     * @return The internal XWiki object, if the document has programming rights, or <code>null</code> otherwise.
     */
    @Programming
    public com.xpn.xwiki.XWiki getXWiki()
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().getWiki();
        } else {
            return null;
        }
    }

    /**
     * Returns the current requested document. Programming rights are needed in order to call this method. If
     * programming rights are not available in the current document, the current can can still be accessed document
     * through a secure API available as a predefined variable for scripting inside wiki pages; use <code>$doc</code> in
     * Velocity.
     * 
     * @return The current requested document, if the document has programming rights, or <code>null</code> otherwise.
     */
    @Programming
    public XWikiDocument getDoc()
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().getDoc();
        } else {
            return null;
        }
    }

    /**
     * Returns the current user which made the request. If there's no currently logged in user in XWiki then the
     * returned string is <i>XWiki.XWikiGuest</i> which represents any anonymous user. Otherwise the returned string has
     * the format <i>databaseName:XWiki.UserLoginName</i> when XWiki runs in virtual mode, or simply
     * <i>XWiki.UserLoginName</i> in non-virtual mode. At the same time this method returns the name of the document
     * containing the current user's profile so in Velocity you can do, for instance,
     * <code>$xwiki.getDocument($context.user)</code> to find out more about the current user, like his/hers real name
     * or e-mail address.
     * 
     * @return The current user which made the request.
     * @see #getLocalUser()
     * @see #getDatabase()
     */
    public String getUser()
    {
        return getXWikiContext().getUser();
    }

    /**
     * Returns the current user which made the request. The difference from {@link #getUser()} is that the returned
     * string is never prefixed with the database name, not even in virtual mode.
     * 
     * @return The current user which made the request.
     * @see #getUser()
     * @see #getDatabase()
     */
    public String getLocalUser()
    {
        return getXWikiContext().getLocalUser();
    }

    /**
     * Sets the current document. Programming rights are needed in order to call this method.
     * 
     * @param doc XWiki document to set as the context document.
     */
    @Programming
    public void setDoc(XWikiDocument doc)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().setDoc(doc);
        }
    }

    /**
     * Returns the XWiki context. Programming rights are needed in order to call this method. The XWiki context
     * represents the execution environment for all the wiki pages. Accessing it directly in wiki pages may lead to
     * security issues.
     * 
     * @return The unwrapped version of the context if you have programming rights, or <code>null</code> otherwise.
     */
    @Programming
    public XWikiContext getContext()
    {
        if (hasProgrammingRights()) {
            return super.getXWikiContext();
        } else {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key in the XWiki context. Programming rights are needed in order to
     * call this method. The context can be seen as a map of (paramName, paramValue) pairs. This mechanism is useful for
     * passing parameters between pages or from Java to Velocity. For instance an exception caught in Java code can be
     * put on the context and handled in a user-friendly way in Velocity. This method is protected because sensitive
     * information may be placed in the internal context, which shouldn't be publicly accessible.
     * 
     * @param key The key to look for in the context.
     * @return The value associated with the given key in the XWiki context, if you have programming rights, or
     *         <code>null</code> otherwise.
     * @see #put(String, Object)
     */
    @Programming
    public java.lang.Object get(String key)
    {
        if (hasProgrammingRights()) {
            return getXWikiContext().get(key);
        } else {
            return null;
        }
    }

    /**
     * Returns the list of textarea fields that use the WYSIWYG editor. This list is automatically built when displaying
     * textarea properties.
     * 
     * @return A string containing a comma-separated list of textarea field names for which the WYSIWYG editor should be
     *         enabled.
     */
    public String getEditorWysiwyg()
    {
        return getXWikiContext().getEditorWysiwyg();
    }

    /**
     * Puts an object on the context using the given key. The context can be seen as a map of (paramName, paramValue)
     * pairs. Requires programming rights.
     * 
     * @param key The parameter name.
     * @param value The parameter value.
     * @see #get(String)
     */
    @Programming
    public void put(String key, java.lang.Object value)
    {
        if (hasProgrammingRights()) {
            getXWikiContext().put(key, value);
        }
    }

    /**
     * Specifies if the current page should be sent to the client or not. When the context is finished, the client
     * response contains only the (HTTP) headers and no body (as in the case of a response to a HTTP HEAD request). This
     * is useful for instance when exporting the entire wiki as a <code>.xar</code> archive.
     * 
     * @param finished <code>true</code> to avoid rendering of the current page
     */
    public void setFinished(boolean finished)
    {
        getXWikiContext().setFinished(finished);
    }

    /**
     * Returns the amount of time this document should be cached.
     * 
     * @return The cache duration, in seconds.
     * @see #setCacheDuration(int)
     */
    public int getCacheDuration()
    {
        return getXWikiContext().getCacheDuration();
    }

    /**
     * Sets the cache duration in seconds. Setting this to a non-zero, pozitive value will cause the rendered document
     * to be stored in a cache, so next time a client requests this document, if it is still in the cache, and the
     * document content did not change, then it will be taken from the cache and will not be parsed/rendered again.
     * While it is a good idea to cache pages containing only static content (no scripting), it should be used with care
     * for documents that gather information from the wiki using queries.
     * 
     * @param duration The cache duration specified in seconds.
     * @see #getCacheDuration()
     */
    public void setCacheDuration(int duration)
    {
        getXWikiContext().setCacheDuration(duration);
    }

    /**
     * Sets the action to be used instead of the <i>view</i> action inside URLs. The XWiki URL factories will replace
     * the <i>view</i> action with the given action when creating URLs.
     * 
     * @param action <i>view</i> action replacement
     * @see #unsetLinksAction()
     * @see #getLinksAction()
     * @see #getURLFactory()
     */
    public void setLinksAction(String action)
    {
        getXWikiContext().setLinksAction(action);
    }

    /**
     * Stops the <i>view</i> action from being replaced with another action inside URLs.
     * 
     * @see #setLinksAction(String)
     * @see #getLinksAction()
     */
    public void unsetLinksAction()
    {
        getXWikiContext().unsetLinksAction();
    }

    /**
     * Returns the action used by XWiki URL factories to replace the <i>view</i> action when creating URLs. If no action
     * replacement has been specified, it returns <code>null</code>.
     * 
     * @return The <i>view</i> action replacement, or <code>null</code>.
     * @see #setLinksAction(String)
     * @see #unsetLinksAction()
     * @see #getURLFactory()
     */
    public String getLinksAction()
    {
        return getXWikiContext().getLinksAction();
    }

    /**
     * Sets an extra query string to be added to all the URLs created by XWiki URL factories.
     * 
     * @param value The additional query string to be added to all the URLs created by XWiki URL factories.
     * @see #unsetLinksQueryString()
     * @see #getLinksQueryString()
     * @see #getURLFactory()
     */
    public void setLinksQueryString(String value)
    {
        getXWikiContext().setLinksQueryString(value);
    }

    /**
     * Specifies that no additional query string should be added to XWiki URLs.
     * 
     * @see #setLinksQueryString(String)
     * @see #getLinksQueryString()
     */
    public void unsetLinksQueryString()
    {
        getXWikiContext().unsetLinksQueryString();
    }

    /**
     * Returns the extra query string that is added to all the URLs created by XWiki URL factories. If no such string
     * has been specified it returns <code>null</code>.
     * 
     * @return The additional query string that is added to all XWiki URLs, or <code>null</code>.
     * @see #setLinksQueryString(String)
     * @see #unsetLinksQueryString()
     * @see #getURLFactory()
     */
    public String getLinksQueryString()
    {
        return getXWikiContext().getLinksQueryString();
    }

    /**
     * Returns the form field validation status, which contains the exceptions or errors that may have occured during
     * the validation process performed during a <i>save</i>.
     * 
     * @return The validation status.
     */
    public XWikiValidationStatus getValidationStatus()
    {
        return getXWikiContext().getValidationStatus();
    }

    /**
     * Returns the list with the currently displayed fields. Each time we call <code>display</code> on a document for a
     * specific field that field is added to the list returned by this method.
     * 
     * @return The list with the currently displayed fields.
     * @see Document#display(String)
     */
    public List<String> getDisplayedFields()
    {
        return getXWikiContext().getDisplayedFields();
    }

    /**
     * Returns an instance of the {@link com.xpn.xwiki.util.Util} class.
     * 
     * @return an instance of the {@link com.xpn.xwiki.util.Util} class
     * @see Util
     * @deprecated since 2.6M1 the functions provided by Util are internal, please do not use them.
     */
    @Deprecated
    public Util getUtil()
    {
        return this.context.getUtil();
    }

    /**
     * Sets the default field display mode, when using {@link Document#display(String)} or
     * {@link Document#display(String, Object)}. It is automatically set to "edit" when the action is "inline", and to
     * "view" in all other cases.
     * 
     * @param mode the display mode, one of "view", "edit", "hidden", "search", "rendered".
     */
    public void setDisplayMode(String mode)
    {
        getXWikiContext().put("display", mode);
    }

    /**
     * Retrieves the information about the currently executing macro. This method is only useful inside wiki macros.
     * 
     * @return macro information, normally a {@link java.util.Map} containing the macro {@code content}, the {@code
     *         params}, and the macro execution {@code context}
     */
    public java.lang.Object getMacro()
    {
        return getXWikiContext().get("macro");
    }

    /**
     * Drop all author permissions by switching author to guest.
     * Call this when all code requiring permission has been executed and the code following may be untrusted.
     * Once dropped, permissions cannot be regained for the duration of the request.
     * 
     * @since 2.5M2
     */
    public void dropPermissions()
    {
        getXWikiContext().put("hasDroppedPermissions", "true");
    }
}
