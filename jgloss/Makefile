#
# Copyright (C) 2001 Michael Koch (tensberg@gmx.net)
#
# This file is part of JGloss.
#
# JGloss is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# JGloss is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with JGloss; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
# $Id$
#

#
# Configuration
#

ifndef JAVAC
  JAVAC := javac
endif

ifndef JAVAC_FLAGS
  JAVAC_FLAGS := -deprecation
endif

ifndef JAR
  JAR := jar
endif

ifndef NATIVE2ASCII
  NATIVE2ASCII := native2ascii
endif

RELEASE := 1.0.4
DISTNAME := jgloss-$(RELEASE)

PACKAGES := jgloss jgloss.dictionary jgloss.ui jgloss.ui.doc jgloss.ui.annotation jgloss.www

DATA := vconj HTMLExporter HTMLAnnotator COPYING
DATA_WWW := HTMLAnnotator vconj

ICONS := jgloss.png html.png tex.png txt.png chasen.png

RESOURCES := messages.properties messages-dictionary.properties \
             messages_de.properties messages-dictionary_de.properties \
             messages_fr.properties messages-dictionary_fr.properties \
             preferences.properties

RESOURCES_WWW := messages-www.properties messages-dictionary.properties \
                 messages-dictionary_de.properties \
                 messages-dictionary_fr.properties

HTML_WWW := index.html converturl.js

CLASSPATH := $(CLASSPATH):./src

# List main class source file and all other class source files not referenced from main class
# for compilation
ROOT_SOURCES := src/jgloss/JGloss.java src/jgloss/JavaPreferences.java src/jgloss/dictionary/GDict.java \
                src/jgloss/dictionary/EDictNIO.java
WWW_SOURCES := src/jgloss/www/JGlossServlet.java

# files and directories that go into the source distribution
SOURCE_DIST := README.txt COPYING ChangeLog Makefile MANIFEST.MF src doc.src latex-ruby
# files and directories that go into the binary distribution, either in the JGloss jar file
# or in the zip file.
BINARY_DIST := README.txt COPYING ChangeLog jgloss data resources doc latex-ruby
# files that go into the binary distribution jar file
BINARY_JAR := jgloss data resources
# files that go into the binary distribution zip file
BINARY_ZIP := jgloss.jar README.txt COPYING ChangeLog doc latex-ruby

#
# end configuration
#

vpath %.properties resources
vpath %.properties.in src/resources
vpath %.pdf doc
vpath %.docbook doc.src
vpath %.html doc/html

.phony: dist dist-binary dist-source setup setup_www clean compile doc gen-javadoc jgloss jgloss-www

jgloss: compile $(RESOURCES) setup
	$(JAR) cfm jgloss.jar MANIFEST.MF $(BINARY_JAR)

jgloss-www: compile-www $(RESOURCES_WWW) setup_www
	mkdir -p jgloss-www/WEB-INF/classes
	cp $(addprefix src/www/,$(HTML_WWW)) jgloss-www
	cp src/www/web.xml jgloss-www/WEB-INF
	cp -r jgloss resources data jgloss-www/WEB-INF/classes

dist: dist-source dist-binary

dist-binary: compile $(RESOURCES) setup doc
	mkdir -p dist/$(DISTNAME)
	cp -r $(BINARY_DIST) dist/$(DISTNAME)
	-cd dist/$(DISTNAME) && find -depth -name "CVS" -exec rm -r \{\} \;
	cd dist/$(DISTNAME) && $(JAR) cfm jgloss.jar ../../MANIFEST.MF $(BINARY_JAR)
	cd dist/$(DISTNAME) && rm -r $(filter-out $(BINARY_ZIP), $(BINARY_DIST))
	cd dist && zip -q -r $(DISTNAME).zip $(DISTNAME)
	cd dist && tar -czf $(DISTNAME).tgz $(DISTNAME)
	rm -r dist/$(DISTNAME)

dist-source: clean
	mkdir -p dist/$(DISTNAME)
	cp -r $(SOURCE_DIST) dist/$(DISTNAME)
	-cd dist/$(DISTNAME) && find -depth \( -name "CVS" -or -name "*.class" \) -exec rm -r \{\} \;
	-cd dist/$(DISTNAME)/src/resources/ && rm $(RESOURCES)
	cd dist && zip -q -r $(DISTNAME)-src.zip $(DISTNAME)
	cd dist && tar -czf $(DISTNAME)-src.tgz $(DISTNAME)
	rm -r dist/$(DISTNAME)

all: jgloss jgloss-www

doc: jgloss.pdf index.html

gen-javadoc:
	mkdir -p javadoc
	javadoc -d javadoc -package \
                -classpath "src:$(CLASSPATH)" \
                -sourcepath src \
                -windowtitle "JGloss documentation" -doctitle "JGloss documentation" \
                -use -author -version \
                $(PACKAGES)

setup:
	mkdir -p resources/icons
	cp $(addprefix src/resources/icons/, $(ICONS)) resources/icons
	mkdir -p data
	cp $(addprefix src/data/, $(DATA)) data

setup_www:
	mkdir -p data
	cp $(addprefix src/data/, $(DATA_WWW)) data

compile:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(ROOT_SOURCES)

compile-www:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(WWW_SOURCES)

# Convert properties files in native charset to ascii.
# The charset is given in the input file by a line of the form "# Encoding: SOME-ENCODING"
%.properties: %.properties.in
	mkdir -p resources
	$(NATIVE2ASCII) -encoding $(shell sed -n -e "/^# *Encoding:/s/.*Encoding:\(.*\)/\1/p" $<) $< resources/$@

%.pdf: %.docbook doc.src/stylesheet.dsl
	mkdir -p doc
	cp -r doc.src/img doc
	docbook2pdf -o doc -d $(shell pwd)/doc.src/stylesheet.dsl $<
	rm -r doc/img
	rm doc/jgloss.out

index.html: jgloss.docbook doc.src/stylesheet.dsl
	mkdir -p doc/html
	cp -r doc.src/img doc/html
	docbook2html -o doc/html -d $(shell pwd)/doc.src/stylesheet.dsl $<

clean:
	-rm -r jgloss resources data dist doc javadoc jgloss.jar jgloss-www
