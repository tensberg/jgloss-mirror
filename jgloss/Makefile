#
# Copyright (C) 2001-2003 Michael Koch (tensberg@gmx.net)
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

RELEASE := 2.0a0
JGLOSS_DISTNAME := jgloss-$(RELEASE)
JDICTIONARY_DISTNAME := jdictionary-$(RELEASE)

DATA_WWW := HTMLAnnotator vconj

ICONS := jgloss.png html.png tex.png txt.png chasen.png template.png

RESOURCES := $(basename $(notdir $(wildcard src/resources/*.in)))

DICTIONARY_MAPS := edict.map wadokujt.map wadokujt-gairaigo.map

RESOURCES_WWW := messages-www.properties \
		 messages-dictionary.properties \
                 messages-dictionary_de.properties \
                 messages-dictionary_fr.properties \
                 messages-parser.properties \
                 messages-parser_de.properties

HTML_WWW := index.html converturl.js

CLASSPATH := ./src:$(CLASSPATH)

# List main class source file and all other class source files not referenced from main class
# for compilation
JGLOSS_SOURCES := src/jgloss/JGlossApp.java \
                  src/jgloss/ui/export/XSLTExporter.java \
                  src/jgloss/ui/export/LaTeXExporter.java
JDICTIONARY_SOURCES := src/jgloss/JDictionaryApp.java
IM_SOURCES := src/jgloss/ui/im/KanaInputMethodDescriptor.java
WWW_SOURCES := src/jgloss/www/JGlossServlet.java

# files and directories that go into the source distribution
SOURCE_DIST := README.txt COPYING ChangeLog Makefile \
               MANIFEST.MF MANIFEST.MF.jdictionary MANIFEST.MF.kanaim \
               src doc.src latex

# files that go into the binary distribution jar file
JGLOSS_BINARY_JAR := jgloss data resources
# files that go into the binary distribution zip file
JGLOSS_BINARY_ZIP := jgloss.jar README.txt COPYING ChangeLog doc latex

# files that go into the binary distribution jar file
JDICTIONARY_BINARY_JAR := jgloss data resources
# files that go into the binary distribution zip file
JDICTIONARY_BINARY_ZIP := jdictionary.jar README.txt COPYING ChangeLog doc

#
# end configuration
#

vpath %.properties resources
vpath %.properties.in src/resources
vpath %.pdf doc
vpath %.docbook doc.src
vpath %.html doc/html

.phony: dist dist-binary-jgloss dist-binary-jdictionary dist-source setup setup_www clean\
        compile-jgloss compile-jdictionary doc gen-javadoc\
        jgloss jdictionary jgloss-www resources

jgloss: compile-jgloss $(RESOURCES) setup
	$(JAR) cfm jgloss.jar MANIFEST.MF $(JGLOSS_BINARY_JAR)

jdictionary: compile-jdictionary $(RESOURCES) setup
	$(JAR) cfm jdictionary.jar MANIFEST.MF.jdictionary $(JDICTIONARY_BINARY_JAR)

im: compile-im
	mkdir -p META-INF/services
	cp src/jgloss/ui/im/InputMethodDescriptor META-INF/services/java.awt.im.spi.InputMethodDescriptor
	$(JAR) cfm kanaim.jar MANIFEST.MF.kanaim jgloss META-INF

jgloss-www: compile-www $(RESOURCES_WWW) setup_www
	mkdir -p jgloss-www/WEB-INF/classes
	cp $(addprefix src/www/,$(HTML_WWW)) jgloss-www
	cp src/www/web.xml jgloss-www/WEB-INF
	cp -r jgloss resources data jgloss-www/WEB-INF/classes

dist: dist-source dist-binary-jdictionary dist-binary-jgloss

dist-binary-jgloss: jgloss doc
	mkdir -p dist/$(JGLOSS_DISTNAME)
	cp -r $(JGLOSS_BINARY_ZIP) dist/$(JGLOSS_DISTNAME)
	-cd dist/$(JGLOSS_DISTNAME) && find -depth -name "CVS" -exec rm -r \{\} \;
	cd dist && zip -q -r $(JGLOSS_DISTNAME).zip $(JGLOSS_DISTNAME)
	cd dist && tar -czf $(JGLOSS_DISTNAME).tgz $(JGLOSS_DISTNAME)
	rm -r dist/$(JGLOSS_DISTNAME)

dist-binary-jdictionary: jdictionary doc
	mkdir -p dist/$(JDICTIONARY_DISTNAME)
	cp -r $(JDICTIONARY_BINARY_ZIP) dist/$(JDICTIONARY_DISTNAME)
	-cd dist/$(JDICTIONARY_DISTNAME) && find -depth -name "CVS" -exec rm -r \{\} \;
	cd dist && zip -q -r $(JDICTIONARY_DISTNAME).zip $(JDICTIONARY_DISTNAME)
	cd dist && tar -czf $(JDICTIONARY_DISTNAME).tgz $(JDICTIONARY_DISTNAME)
	rm -r dist/$(JDICTIONARY_DISTNAME)

dist-source: clean
	mkdir -p dist/$(JGLOSS_DISTNAME)
	cp -r $(SOURCE_DIST) dist/$(JGLOSS_DISTNAME)
	-cd dist/$(JGLOSS_DISTNAME) && find -depth \( -name "CVS" -or -name "*.class" \) -exec rm -r \{\} \;
	-cd dist/$(JGLOSS_DISTNAME)/src/resources/ && rm $(RESOURCES)
	cd dist && zip -q -r $(JGLOSS_DISTNAME)-src.zip $(JGLOSS_DISTNAME)
	cd dist && tar -czf $(JGLOSS_DISTNAME)-src.tgz $(JGLOSS_DISTNAME)
	rm -r dist/$(JGLOSS_DISTNAME)

all: jdictionary jgloss jgloss-www

doc: jgloss.pdf index.html

gen-javadoc:
	mkdir -p javadoc
	javadoc -quiet -d javadoc -private \
                -classpath "src:$(CLASSPATH)" \
                -sourcepath src \
                -windowtitle "JGloss documentation" -doctitle "JGloss documentation" \
                -use -author -version \
		-breakiterator \
                -subpackages jgloss -exclude jgloss.www
# generate for all packages, except jgloss.www because it is currently broken

setup:
	cp $(addprefix src/resources/, $(DICTIONARY_MAPS)) resources
	mkdir -p resources/icons
	cp $(addprefix src/resources/icons/, $(ICONS)) resources/icons
	mkdir -p data
	cp -r src/data/* data
	-cd data && find -depth -name "CVS" -exec rm -r \{\} \;

setup_www:
	mkdir -p data
	cp $(addprefix src/data/, $(DATA_WWW)) data

compile-jgloss:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(JGLOSS_SOURCES)

compile-jdictionary:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(JDICTIONARY_SOURCES)

compile-im:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(IM_SOURCES)

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
	-rm doc/jgloss.out

index.html: jgloss.docbook doc.src/stylesheet.dsl
	mkdir -p doc/html
	cp -r doc.src/img doc/html
	docbook2html -o doc/html -d $(shell pwd)/doc.src/stylesheet.dsl $<

resources: $(RESOURCES)

clean:
	-rm -r jgloss resources data dist doc jgloss.jar jdictionary.jar jgloss-www META-INF
