JGloss 1.0.1
------------

JGloss homepage: http://jgloss.sourceforge.net/

JGloss is an application for adding reading and translation annotations to
words in a Japanese text document. This can be done automatically and manually.
The document can be exported as plain text with annotations, HTML or LaTeX.

JGloss is written in Java. It should work on any computer with support for
the Java 2 Version 1.3 platform. It is distributed under the terms of the
GNU General Public License.

If this is the binary release, you can find the documentation in the folder
"doc". If is is the source release, you can create the jgloss.jar
executable JAR file by typing "make jgloss", create the documentation by
executing "make doc", or you can read the documentation source
"doc.src/jgloss.docbook".

Known problems:
  - On Windows 98/ME the ChaSen window does not close automatically
    after a document is imported. You will have to close it by hand.
  - When using JGloss with KDE, the word annotation dialog will sometimes
    be resized to a minimum when it is opened. Also, Java locks up if
    the preference window is opened after a document is loaded and when
    the print dialog opens. This seems to be a problem in the interaction 
    between the interaction of Java and the KDE window manager which I don't 
    know how to fix.
  - On Linux force-quitting JGloss by typing Control-C or using kill -HUP
    sometimes doesn't work.
  - The custom user interface font chosen in the style dialog is applied
    thorough the application only after JGloss is restarted.
  - Pasting text in the text field of the input dialog does not work with non-
    Japanese Windows. Use Import Clipboard instead.

(c) 2001,2002 Michael Koch <tensberg@gmx.net>
