JGloss 2 Alpha 3
================

JGloss homepage: http://jgloss.sourceforge.net/

JGloss is an application for adding reading and translation annotations to
words in a Japanese text document. This can be done automatically and manually.
The document can be exported as plain text with annotations, HTML or LaTeX.

JGloss is written in Java. It should work on any computer with support for
the Java 2 Version 1.4 platform. It is distributed under the terms of the
GNU General Public License.

To start JGloss, double-click the file "jgloss.jar", or type
"java -jar jgloss.jar" in a shell.

If this is the binary release, you can find the documentation in the folder
"doc". If is is the source release, you can create the jgloss.jar
executable JAR file by typing "make jgloss", create the documentation by
executing "make doc", or you can read the documentation source
"doc.src/jgloss.docbook".

About Alpha 3
-------------
This is a preview of the new JGloss version, mainly to show that I am still
working on the program. It should give you an opportunity to try out the
new user interface and expanded dictionary functions. You are welcome to
send comments about the features and write your ideas, but it is
unneccessary to send bug reports, since the program is still being heavily
worked on (exchanging old bugs with new ones). There are many features
which were in the previous JGloss versions but are not in this alpha
version. They will eventually make their reappearance when I find the time
to reimplement them.

Some of the greater issues of JGloss 2 Alpha 2:
- The file format of the .jgloss files has incompatibly changed, opening
  older files is not (yet) possible
- The handbook is out of date
- The user dictionary is not implemented yet
- LaTeX export is not implemented yet
- The dictionary implementations do not yet support all planned features
  (particularly EDICT).
- Documentation of the source code is incomplete.

Tips for keyboard navigation:
-----------------------------
use cursor up/down to switch between annotations; cursor left/right to
switch between readings/translations, space to select a
reading/translation; use Alt-A to annotate the currently selected text.

Known problems (JGloss 1.0.7):
------------------------------
  - On Linux force-quitting JGloss by typing Control-C or using kill -HUP
    sometimes doesn't work.
  - The custom user interface font chosen in the style dialog is applied
    throughout the application only after JGloss is restarted.
  - Pasting text in the text field of the input dialog does not work with non-
    Japanese Windows. Use Import Clipboard instead.
  - Import Clipboard does not work in MacOS X, unless the system language is
    set to Japanese. Paste the text in the text field of the import dialog
    instead.

(c) 2001-2004 Michael Koch <tensberg@gmx.net>

Parts of JGloss code (C) 2002 Eric Crahen
