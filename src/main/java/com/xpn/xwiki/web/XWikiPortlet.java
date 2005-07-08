/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 19 mai 2004
 * Time: 13:04:58
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiService;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import org.apache.log4j.MDC;
import org.apache.velocity.VelocityContext;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.portlet.*;
import java.io.IOException;

public class XWikiPortlet extends GenericPortlet {
    protected final Log logger = LogFactory.getLog( getClass() );

    private String name = "XWiki Portlet";
	public static final PortletMode CONFIG_PORTLET_MODE = new PortletMode("config");
	public static final String ROOT_SPACE_PARAM_NAME = "rootSpace";

	protected String getTitle(RenderRequest renderRequest) {
        return name;
    }

    protected XWikiContext prepareContext(String action, XWikiRequest request, XWikiResponse response, XWikiEngineContext engine_context) throws XWikiException {
        return Utils.prepareContext(action, request, response, engine_context);
    }

    protected boolean prepareAction(String action, XWikiRequest request, XWikiResponse response,
                                    XWikiEngineContext engine_context, XWikiContext context) throws XWikiException, IOException {
        XWiki xwiki = XWiki.getXWiki(context);
        VelocityContext vcontext = XWikiVelocityRenderer.prepareContext(context);
        return xwiki.prepareDocuments(request, context, vcontext);
    }

    protected void cleanUp(XWikiContext context) {
        try {
            XWiki xwiki = (context!=null) ? context.getWiki() : null;
            // Make sure we cleanup database connections
            // There could be cases where we have some
            if ((context!=null)&&(xwiki!=null)) {
                if (xwiki.getStore()!=null)
                    xwiki.getStore().cleanUp(context);
            }
        } finally {
            MDC.remove("url");
        }
    }

    protected void handleException(XWikiRequest request, XWikiResponse response, Throwable e, XWikiContext context) {

        if (!(e instanceof XWikiException)) {
            e = new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception", e);
        }

        VelocityContext vcontext = ((VelocityContext)context.get("vcontext"));
        if (vcontext==null) {
            vcontext = new VelocityContext();
            context.put("vcontext", vcontext);
        }
        vcontext.put("exp", e);

        try {
            XWikiException xex = (XWikiException) e;
            if (xex.getCode()==XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                String page = Utils.getPage(request, "accessdenied");
                Utils.parseTemplate(page, context);
                return;
            } else if (xex.getCode()==XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                String page = Utils.getPage(request, "userinactive");
                Utils.parseTemplate(page, context);
                return;
            }
            Utils.parseTemplate(Utils.getPage(request, "exception"), context);
        } catch (Exception e2) {
            // I hope this never happens
            e.printStackTrace();
            e2.printStackTrace();
        }
    }

	protected void doDispatch(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
		WindowState windowState = aRenderRequest.getWindowState();
		if(!windowState.equals(WindowState.MINIMIZED) && aRenderRequest.getPortletMode().equals( CONFIG_PORTLET_MODE ) ) {
			doView( aRenderRequest, aRenderResponse );
		} else {
			super.doDispatch(aRenderRequest, aRenderResponse);
		}
	}

    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
		WindowState windowState = actionRequest.getWindowState();
		if(!windowState.equals(WindowState.MINIMIZED) && actionRequest.getPortletMode().equals( CONFIG_PORTLET_MODE ) ) {
			handleConfigForm( actionRequest, actionResponse );
		} else {
			XWikiContext context = null;
			XWikiRequest request = new XWikiPortletRequest(actionRequest);
			XWikiResponse response = new XWikiPortletResponse(actionResponse);
			XWikiEngineContext engine_context = new XWikiPortletContext(actionRequest.getPortletSession().getPortletContext());

			try {
				String action = request.getParameter("action");
				if ((action==null)||(action.equals("")))
					action = "view";

				context = prepareContext(action, request, response, engine_context);
				if (prepareAction(action, request, response, engine_context, context)==false)
					return;

				XWikiService xwikiservice = new XWikiService();
				XWikiForm form = null;

				if (action.equals("save"))
					form = new EditForm();
                else if (action.equals("rollback"))
                    form = new RollbackForm();
				else if (action.equals("objectadd"))
					form = new ObjectAddForm();
				else if (action.equals("commentadd"))
					form = new ObjectAddForm();
				else if (action.equals("objectremove"))
					form = new ObjectRemoveForm();
				else if (action.equals("propadd"))
					form = new PropAddForm();

				if (form!=null) {
					form.reset(null, request);
					context.setForm(form);
				}

				if (action.equals("save")) {
					xwikiservice.actionSave(context);
				}
                else if (action.equals("rollback")) {
                    xwikiservice.actionRollback(context);
                }
                else if (action.equals("cancel")) {
                    xwikiservice.actionCancel(context);
                }
				else if (action.equals("delete")) {
					xwikiservice.actionDelete(context);
				}
				else if (action.equals("propupdate")) {
					xwikiservice.actionPropupdate(context);
				}
				else if (action.equals("propadd")) {
					xwikiservice.actionPropadd(context);
				}
				else if (action.equals("objectadd")) {
					xwikiservice.actionObjectadd(context);
				}
				else if (action.equals("commentadd")) {
					xwikiservice.actionCommentadd(context);
				}
				else if (action.equals("objectremove")) {
					xwikiservice.actionObjectremove(context);
				}
				else if (action.equals("upload")) {
					xwikiservice.actionUpload(context);
				}
				else if (action.equals("delattachment")) {
					xwikiservice.actionDelattachment(context);
				}
				else if (action.equals("skin")) {
					xwikiservice.actionSkin(context);
				}
				else if (action.equals("logout")) {
					xwikiservice.actionLogout(context);
				}
				else if (action.equals("register")) {
					xwikiservice.actionRegister(context);
				}
			} catch (Throwable e) {
				handleException(request, response, e, context);
			} finally {
				cleanUp(context);
			}
		}
    }

	private void handleConfigForm(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
		PortletPreferences preferences = actionRequest.getPreferences();
		String rootSpace = actionRequest.getParameter( ROOT_SPACE_PARAM_NAME );
		preferences.setValue( XWikiPortletRequest.ROOT_SPACE_PREF_NAME, rootSpace );
		actionResponse.setPortletMode( PortletMode.VIEW );
		preferences.store();
		if (logger.isDebugEnabled()) {
			logger.debug("New root space is [" + rootSpace + "]");
		}

	}

	protected void doView(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        XWikiContext context = null;
        XWikiRequest request = new XWikiPortletRequest(renderRequest);
        XWikiResponse response = new XWikiPortletResponse(renderResponse);
        XWikiEngineContext engine_context = new XWikiPortletContext(renderRequest.getPortletSession().getPortletContext());
        String action = null;

        try {
            action = request.getParameter("action");
            if ((action==null)||(action.equals(""))) {
                action = renderRequest.getPortletMode().equals( CONFIG_PORTLET_MODE )
						? "portletConfig"
						: "view";
			}

            context = prepareContext(action, request, response, engine_context);
            if (prepareAction(action, request, response, engine_context, context)==false)
                return;

            XWikiService xwikiservice = new XWikiService();
            XWikiForm form = null;

            if (action.equals("edit")
                    ||action.equals("inline"))
                form = new PrepareEditForm();
            else if (action.equals("preview"))
                form = new EditForm();

            if (form!=null) {
                form.reset(null, request);
                context.setForm(form);
            }

            String renderResult = null;
            // Determine what to do
            if (action.equals("view")) {
                renderResult = xwikiservice.renderView(context);
            }
            else if ( action.equals("inline")) {
                renderResult = xwikiservice.renderInline(context);
            }
            else if ( action.equals("edit") ) {
                renderResult = xwikiservice.renderEdit(context);
            }
            else if ( action.equals("preview")) {
                renderResult = xwikiservice.renderPreview(context);
            }
            else if (action.equals("delete")) {
                renderResult = xwikiservice.renderDelete(context);
            }
            else if (action.equals("download")) {
                renderResult = xwikiservice.renderDownload(context);
            }
            else if (action.equals("dot")) {
                renderResult = xwikiservice.renderDot(context);
            }
            else if (action.equals("svg")) {
                renderResult = xwikiservice.renderSVG(context);
            }
            else if (action.equals("attach")) {
                renderResult = xwikiservice.renderAttach(context);
            }
            else if (action.equals("login")) {
                renderResult = xwikiservice.renderLogin(context);
            }
            else if (action.equals("loginerror")) {
                renderResult = xwikiservice.renderLoginerror(context);
            }
            else if (action.equals("loginerror")) {
                renderResult = xwikiservice.renderRegister(context);
            }
            else if ( action.equals("portletConfig") ) {
				renderResult = "portletConfig";
			}
            if (renderResult!=null) {
                String page = Utils.getPage(request, renderResult);
                Utils.parseTemplate(page, context);
            }
        } catch (Throwable e) {
			if (logger.isWarnEnabled()) {
				logger.warn("oops",e);
			}

            handleException(request, response, e, context);
        } finally {
            // Let's make sure we have flushed content and closed
            try {
                 response.getWriter().flush();
            } catch (Throwable e) {
            }

            // / Let's handle the notification and make sure it never fails
            try {
                context.getWiki().getNotificationManager().verify(context.getDoc(), action, context);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            cleanUp(context);
        }
    }

    protected void doEdit(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        super.doEdit(renderRequest, renderResponse);
    }

}
