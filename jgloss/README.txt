JGloss 1.0.6
------------

JGloss homepage: http://jgloss.sourceforge.net/

JGloss is an application for adding reading and translation annotations to
words in a Japanese text document. This can be done automatically and manually.
The document can be exported as plain text with annotations, HTML or LaTeX.

JGloss is written in Java. It should work on any computer with support for
the Java 2 Version 1.3/1.4 platform. It is distributed under the terms of the
GNU General Public License.

To start JGloss, double-click the file "jgloss.jar", or type
"java -jar jgloss.jar" in a shell.

If this is the binary release, you can find the documentation in the folder
"doc". If is is the source release, you can create the jgloss.jar
executable JAR file by typing "make jgloss", create the documentation by
executing "make doc", or you can read the documentation source
"doc.src/jgloss.docbook".

JGloss 1.0.3 fixed some bugs in the EDICT implementation. To improve dictionary
lookup accuracy, you should delete any old .jjdx index files created by
versions of JGloss before 1.0.3.

Known problems:
  - When the ChaSen parser is used with JRE 1.3 on Windows 98/ME/XP, a window
    pops up while ChaSen runs. This does not happen with JRE 1.4.
  - On Windows 98/ME when using JRE 1.3 the ChaSen window does not close 
    automatically after a document is imported. You will have to close it by
    hand. Use JRE 1.4 instead.
  - When using JGloss with JRE 1.3 and KDE, the word annotation dialog will
    sometimes be resized to a minimum when it is opened. Also, Java locks up if
    the preference window is opened after a document is loaded and when
    the print dialog opens. This seems to be a problem in the interaction 
    between Java and the KDE window manager which I don't know how to fix. Use 
    JRE 1.4 instead.
  - On Linux force-quitting JGloss by typing Control-C or using kill -HUP
    sometimes doesn't work.
  - The custom user interface font chosen in the style dialog is applied
    throughout the application only after JGloss is restarted.
  - Pasting text in the text field of the input dialog does not work with non-
    Japanese Windows. Use Import Clipboard instead.
  - Import Clipboard does not work in MacOS X, unless the system language is
    set to Japanese. Paste the text in the text field of the import dialog
    instead.

(c) 2001,2002 Michael Koch <tensberg@gmx.net>
