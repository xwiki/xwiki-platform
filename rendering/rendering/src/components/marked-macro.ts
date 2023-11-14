import type { MarkedExtension, TokenizerExtension, RendererExtension, TokenizerThis, RendererThis, Tokens } from 'marked';
type Config = {
  nodeName: string;
  className: string;
}

const startReg = new RegExp(/\{\{(.*?)\s+(.*?)\}\}/);
const parametersReg = new RegExp(/(\w*)\s*=\s*((['"])?((\\\3|[^\3])*?)\3|(\w+))/g);
const debug = false;
let config:Config = { nodeName: 'pre', className: 'wikimodel-macro'};

const macro: TokenizerExtension | RendererExtension = {
  name: 'macro',
  level: 'block',
  start(this: TokenizerThis, src: string) {
    const index = src.match(startReg)?.index;
    debug && console.log('[marked start]', src, index);
    return index;
  },
  tokenizer(src: string, _tokens): Tokens.Generic | undefined { 
    debug && console.log('[marked tokenizer]', src, _tokens);
    const lines = src.split(/\n/);
    let endReg = null;
    if (startReg.test(lines[0])) {
      const section = { x: -1, y: -1, macroName : "", macroParameters : ""};
      const sections = [];
      for (let i = 0, k = lines.length; i < k; i++) {
        let startResult = lines[i].match(startReg)
        if (startResult) {
          section.x = i;
          section.macroName = startResult[1];
          section.macroParameters = startResult[2]
          endReg = new RegExp("\{\{\/(" + section.macroName + ")\}\}");
        } else if (endReg != null && endReg.test(lines[i])) {
          section.y = i;
          if (section.x >= 0) {
            sections.push({ ...section });
            section.x = -1;
            section.y = -1;
          }
        }
      }

      if (sections.length) {
        const section = sections[0];

        let paramsResults = section.macroParameters.matchAll(parametersReg)
        let parameters = new Map<string, string>();
        parameters.set("title", "");
        for (let paramResult of paramsResults) {
          let paramName = paramResult[1]
          let paramValue = paramResult[4]
          parameters.set(paramName, paramValue.replaceAll(/\\/g, ""));
        }
        
        const content = lines.slice(section.x + 1, section.y).join('\n');
        const raw = lines.slice(section.x, section.y + 1).join('\n');
        const title = parameters.get("title") as string;
        const macroName = section.macroName;
        const token = {
          type: 'macro',
          raw,
          macroName,
          title,
          content,
          titleTokens: [],
          tokens: [],
          childTokens: ['title', 'content'],
        };

        this.lexer.inlineTokens(token.title, token.titleTokens);
        this.lexer.blockTokens(token.content, token.tokens);
        return token;
      }
    }
    
  },
  renderer(this: RendererThis, token) {
    debug && console.log('🐉[marked renderer]', this, token);
    const html = `<${config.nodeName} class="${config.className}" macroname="${token.macroName}" title="${token.title}"><!--[CDATA[${token.content}]]--></${config.nodeName}>`;
    console.log("Marked Macro html", html);
    return html;
  },
};

const extensions: (TokenizerExtension | RendererExtension)[] = [macro];

export default <MarkedExtension>{
  extensions,
};