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
 */
module.exports = {
    console.log("The plugin was installed.");
    this.process = function(str, options) {
        // If the current processed file is the main one
        if (options.fileInfo.filename === options.fileInfo.rootFilename) {
            // We append the content to add to the 'str' variable holding the LESS code to render.
            let finalStr = str + '\n' + this.contentToAdd;
            // We append the CSS variable definitions.
            console.log(options);
            return finalStr;
        }
        // Otherwise, we just return the less file as it is
        // (the code to add is not supposed to be appended to imported files).
        return str;
    };/*
        functions.add('getVariables', function () {
            console.log("Got variables....");
            const root = less.evaldRoot;
            const varNames = Object.keys(root._variables);
            return varNames.reduce(
                function (varMap, varName) {
                    varMap[varName] = root._variables[varName].value;
                },
                {});
        });
    }*/
}