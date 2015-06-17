grammar Cocu;

program: expression*;
expression: expressionReceiver expressionChain*;
expressionReceiver:
    assignment | variableDeclaration | selfMultiKeyMessage |
    messageExchange;
assignment: id assignmentOperator expression;
assignmentOperator: op=(ASSIGN | ASSIGN_PROTO | ASSIGN_QUOTED) behaviorParams;
messageExchange: receiver messageChain* messageEnd?;
messageChain: DOT unaryMessage | indexAccess | slotAccess;
messageEnd: 
    DOT multiKeyMessage | slotAssignment | indexAssignment | binaryMessageChain;
binaryMessageChain: binaryMessage+;
expressionChain: 
    SEMI_COLON 
    (
        multiKeyMessage | unaryMessage | 
        indexAssignment | indexAccess | 
        slotAssignment | slotAccess |
        binaryMessageChain
    );
receiver: atom;
selfMultiKeyMessage: multiKeyMessage;
variableDeclaration: VAR id (ASSIGN expression)?;
access: id;
grouping: PAR_OP (expression)+ PAR_CL;
multiKeyMessage: multiKeyMessageHead multiKeyMessageTail*;
multiKeyMessageHead: ID_UNCAP multiKeyMessageModifier multiKeyMessageArgs;
multiKeyMessageTail: ID_CAP multiKeyMessageModifier multiKeyMessageArgs;
multiKeyMessageModifier: (modifier=COLON|modifier=SINGLE_QUOTE behaviorParams);
multiKeyMessageArgs: (multiKeyMessageArg (COMMA multiKeyMessageArg)*)?;
multiKeyMessageArg: 
    selfSingleKeyMessage |
    multiKeyMessageArgReceiver multiKeyMessageArgChain* multiKeyMessageArgEnd?;
multiKeyMessageArgReceiver: atom;
multiKeyMessageArgChain: DOT unaryMessage | slotAccess | indexAccess;
multiKeyMessageArgEnd:
    DOT singleKeyMessage | slotAssignment | indexAssignment | binaryMessageChain;
atom: access | grouping | literal | pseudoVar | parArg;
selfSingleKeyMessage: singleKeyMessage;
singleKeyMessage: ID_UNCAP multiKeyMessageModifier multiKeyMessageArg;
unaryMessage: ID_UNCAP;
slotAccess: AT selector;
indexAccess: SQ_OP expression SQ_CL;
binaryMessage: BIN_OP binaryMessageArg;
binaryMessageArg: receiver binaryMessageArgChain* binaryMessageArgEnd?;
binaryMessageArgChain: DOT unaryMessage | slotAccess | indexAccess;
binaryMessageArgEnd: slotAssignment | indexAssignment;
indexAssignment: SQ_OP expression SQ_CL ASSIGN expression;
slotAssignment: AT selector assignmentOperator expression;
literal: integer | string | closure | spawn;
integer: INT;
string: STRING;
dictEntry: selector (assignmentOperator expression)?;
closure: BRA_OP behaviorParams (expression*) BRA_CL;
behaviorParams: (PIPE (id)+ PIPE)?;
spawn: HASH explicitPrototype=expression? BRA_OP expression* BRA_CL;
pseudoVar: PSEUDO_VAR;
parArg: BACK_SLASH id;
id: ID_CAP | ID_UNCAP;
selector: id | binaryOperator | indexOperator;
binaryOperator: BIN_OP;
indexOperator: SQ_OP SQ_CL;

VAR: 'var';
PSEUDO_VAR: 'this' | 'null' | 'true' | 'false' | 'frame' /*"here", instead of "frame"?*/;
INT: DIGIT+;
fragment DIGIT: ('0'..'9');
fragment LETTER_LOWER: [a-z];
fragment LETTER_UPPER: [A-Z];
fragment LETTER: (LETTER_LOWER|LETTER_UPPER);
ID_CAP: LETTER_UPPER (LETTER | DIGIT | '_')*;
ID_UNCAP: LETTER_LOWER (LETTER | DIGIT | '_')*;
PIPE: '|';
HASH: '#';
AT: '@';
DOT: '.';
SINGLE_QUOTE: '\'';
COMMA: ',';
COLON: ':';
SEMI_COLON: ';';
BACK_SLASH: '\\';
PAR_OP: '(';
PAR_CL: ')';
SQ_OP: '[';
SQ_CL: ']';
BRA_OP: '{';
BRA_CL: '}';
BIN_OP: '+'|'-'|'*'|'/'|'%'|'=='|'!='|'>'|'>='|'<'|'<='|'&&'|'||';
ASSIGN: '=';
ASSIGN_PROTO: '^=';
ASSIGN_QUOTED: '=>';
WS: [ \t\u000C\r\n]+ -> skip;
SINGLELINE_COMMENT: '//' ~('\r' | '\n')* -> skip;
MULTI_LINE_COMMENT: '/*' .*? '*/' -> skip;
STRING: '"' (EscapeSequence | ~[\\"])* '"';
fragment HexDigit: [0-9a-fA-F];
fragment EscapeSequence: '\\' [btnfr"'\\] | UnicodeEscape | OctalEscape;
fragment OctalEscape: '\\' [0-3] [0-7] [0-7] | '\\' [0-7] [0-7] | '\\' [0-7];
fragment UnicodeEscape: '\\' 'u' HexDigit HexDigit HexDigit HexDigit;