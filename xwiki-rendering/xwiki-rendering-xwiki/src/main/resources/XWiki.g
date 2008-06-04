grammar XWiki; 


document	: block+ 
		;
		
block		: section 
		| paragraph
		;

paragraph	: sentence (NEWLINE | EOF)
		;
				
section		: SECTIONTITLE WS sentence NEWLINE
		;				
		
sentence	: elementBlock (WS elementBlock)*
		;
		
elementBlock	: macro
		| word
		;		
		
macro		: '{' ID (':' macroParams)? ('/}' | '}' .* 
		;
		
macroParams	: macroParam ('|' macroParam)*
		;
		
macroParam	: WORD '=' WORD
		;
						
word		: boldWord
		| italicWord
		| underlineWord
		| WORD
		;
		
boldWord	: '*' WORD '*'
		;

italicWord	: '~~' WORD '~~'
		;
		
underlineWord	: '__' WORD '__'
		;
								
SECTIONTITLE	: '1.1.1.1.1.1'
		| '1.1.1.1.1'
		| '1.1.1.1'
		| '1.1.1'
		| '1.1'
		| '1'
		;
		 		
WS 		: (' '|'\t')+
		; 

NEWLINE		: '\r'? '\n'
		;
		
WORD		:  ~('\n'|'\r'|' '|'\t'|'{'|'/'|'}'|'*') ~('\n'|'\r'|' '|'\t'|'/'|'}'|'*')*
		;
	
fragment LETTER	: 'a'..'z' | 'A'..'Z';
		
fragment DIGIT	: '0'..'9';
