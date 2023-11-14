/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

export class WikiModel2 {
    
    
    public constructor() {
      this.loadWikiModel();
    }

    public loadWikiModel() {
        try {
            const divEl = document.createElement('script');
            divEl.src = "/src/apps/wikimodel/org.xwiki.rendering.wikimodel.nocache.js";
            divEl.async = false;
            document.head.appendChild(divEl);
            console.log("After import WikiModel")
            // @ts-ignore
            console.log("Wikimodel:", window.WikiModel);
            // @ts-ignore
            if (window.WikiModel) {
                // @ts-ignore
                console.log("Wikimodel parse:", window.WikiModel.parse);
            }
        } catch (e) {
            console.log("Exception loading wikimodel", e);
        }
    }

    public async isWikiModelLoaded() {
        const sleep = (ms : any) => new Promise(r => setTimeout(r, ms));

        for (let i=0;i<10;i++) {
            // @ts-ignore
            if (!window.WikiModel) {
                // this.logger?.debug("Sleeping 1sec mode waiting for WikiModel");
                await sleep(1000);
            } else {
                // this.logger?.info("WikiModel is ready");
                return true;
            }
        }
        // this.logger?.info("WikiModel is not ready");
        return false;
    }

    public parse(source: string) : string {
        try {
            console.log("Source is:", source)
            // @ts-ignore
            const result = window.WikiModel.parse(source);
            console.log("Rendering is:", result)
            // @ts-ignore
            return result;
        } catch (e) {
            console.log("Exception rendering", e);
            return "Exception rendering: " + e;
        }
    }

}