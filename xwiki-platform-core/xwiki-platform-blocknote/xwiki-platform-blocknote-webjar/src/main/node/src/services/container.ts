/**
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
 */
import { DefaultAuthenticationManagerProvider } from "./authentication/DefaultAuthenticationManagerProvider";
import { XWikiAuthenticationManager } from "./authentication/XWikiAuthenticationManager";
import { DefaultBlockNoteIterator } from "./blocknote/DefaultBlockNoteIterator";
import { XWikiBlockNoteProcessor } from "./blocknote/XWikiBlockNoteProcessor";
import { MinimalApp } from "./cristal/MinimalApp";
import { DefaultDocumentService } from "./document/DefaultDocumentService";
import { DefaultImageWizard } from "./image/DefaultImageWizard";
import { DefaultLinkSuggestServiceProvider } from "./link/DefaultLinkSuggestServiceProvider";
import { DefaultMacroWizard } from "./macros/DefaultMacroWizard";
import { DefaultBlockNoteMacroWizard } from "./macros/MacroWizard";
import { DefaultModelReferenceHandlerProvider } from "./model/reference/DefaultModelReferenceHandlerProvider";
import { DefaultModelReferenceParserProvider } from "./model/reference/DefaultModelReferenceParserProvider";
import { DefaultModelReferenceSerializerProvider } from "./model/reference/DefaultModelReferenceSerializerProvider";
import { XWikiModelReferenceHandler } from "./model/reference/XWikiModelReferenceHandler";
import { XWikiModelReferenceParser } from "./model/reference/XWikiModelReferenceParser";
import { XWikiModelReferenceSerializer } from "./model/reference/XWikiModelReferenceSerializer";
import { DefaultRemoteURLParserProvider } from "./model/url/DefaultRemoteURLParserProvider";
import { DefaultRemoteURLSerializerProvider } from "./model/url/DefaultRemoteURLSerializerProvider";
import { XWikiRemoteURLParser } from "./model/url/XWikiRemoteURLParser";
import { XWikiRemoteURLSerializer } from "./model/url/XWikiRemoteURLSerializer";
import { DefaultSkinManager } from "./skin/DefaultSkinManager";
import { DefaultStorageProvider } from "./storage/DefaultStorageProvider";
import { XWikiStorage } from "./storage/XWikiStorage";
import { DefaultLogger } from "@xwiki/platform-api";
import { ComponentInit as DefaultAttachmentsComponentInit } from "@xwiki/platform-attachments-default";
import { ComponentInit as CollaborationComponentList } from "@xwiki/platform-collaboration-api";
import { ComponentInit as XWikiCollaborationComponentList } from "@xwiki/platform-collaboration-xwiki";
import { ComponentInit as LinkSuggestXWikiComponentList } from "@xwiki/platform-link-suggest-xwiki";
import { ComponentInit as MarkdownSyntaxConfig } from "@xwiki/platform-markdown-syntax-config";
import { ComponentInit as MinimalSyntaxConfig } from "@xwiki/platform-minimal-syntax-config";
import { DefaultResourceReferenceParser } from "@xwiki/platform-rendering-api";
import { ComponentInit as XWikiSyntaxConfig } from "@xwiki/platform-xwiki-syntax-config";
import { Container } from "inversify";

const container: Container = new Container();
container.bind("Container").toConstantValue(container);
container.bind("Logger").to(DefaultLogger).inSingletonScope();

DefaultDocumentService.bind(container);

DefaultModelReferenceParserProvider.bind(container);
XWikiModelReferenceParser.bind(container);

DefaultModelReferenceSerializerProvider.bind(container);
XWikiModelReferenceSerializer.bind(container);

DefaultModelReferenceHandlerProvider.bind(container);
XWikiModelReferenceHandler.bindComponents(container);

DefaultResourceReferenceParser.bind(container);

DefaultRemoteURLParserProvider.bind(container);
XWikiRemoteURLParser.bind(container);
DefaultRemoteURLSerializerProvider.bind(container);
XWikiRemoteURLSerializer.bind(container);

DefaultAuthenticationManagerProvider.bind(container);
XWikiAuthenticationManager.bind(container);

DefaultLinkSuggestServiceProvider.bind(container);

DefaultSkinManager.bind(container);

DefaultStorageProvider.bind(container);
XWikiStorage.bind(container);
new DefaultAttachmentsComponentInit(container);

new CollaborationComponentList(container);
new XWikiCollaborationComponentList(container);
new LinkSuggestXWikiComponentList(container);

DefaultBlockNoteIterator.bind(container);
XWikiBlockNoteProcessor.bind(container);

DefaultImageWizard.bind(container);
DefaultMacroWizard.bind(container);
DefaultBlockNoteMacroWizard.bind(container);

MinimalApp.bind(container);

new MarkdownSyntaxConfig(container);
new XWikiSyntaxConfig(container);
new MinimalSyntaxConfig(container);

export { container };
