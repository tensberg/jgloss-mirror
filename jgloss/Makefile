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
  JAVAC_FLAGS := -deprecation -O
endif

ifndef JAR
  JAR := jar
endif

ifndef NATIVE2ASCII
  NATIVE2ASCII := native2ascii
endif

RELEASE := 0.9
DISTNAME := jgloss-$(RELEASE)

PACKAGES := jgloss jgloss.dictionary jgloss.ui jgloss.ui.doc jgloss.ui.annotation

CLASSPATH := $(CLASSPATH):./src
ROOT_SOURCE := src/jgloss/JGloss.java
RESOURCES := messages.properties messages_de.properties preferences.properties
# files and directories that go into the source distribution
SOURCE_DIST := COPYING Makefile MANIFEST.MF src doc.src javadoc latex-ruby
# files and directories that go into the binary distribution, either in the JGloss jar file
# or in the zip file.
BINARY_DIST := jgloss data resources COPYING doc latex-ruby
# files that go into the binary distribution jar file
BINARY_JAR := jgloss data resources
# files that go into the binary distribution zip file
BINARY_ZIP := COPYING doc jgloss.jar latex-ruby

#
# end configuration
#

vpath %.properties resources
vpath %.properties.in src/resources
vpath %.pdf doc
vpath %.docbook doc.src
vpath %.html doc/html

.phony: dist dist-binary dist-source setup clean compile doc gen-javadoc jar

jar: compile $(RESOURCES) setup
	$(JAR) cfm jgloss.jar MANIFEST.MF $(BINARY_JAR)

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

dist-source: clean gen-javadoc
	mkdir -p dist/$(DISTNAME)
	cp -r $(SOURCE_DIST) dist/$(DISTNAME)
	-cd dist/$(DISTNAME) && find -depth \( -name "CVS" -or -name "*.class" \) -exec rm -r \{\} \;
	-cd dist/$(DISTNAME)/src/resources/ && rm $(RESOURCES)
	cd dist && zip -q -r $(DISTNAME)-src.zip $(DISTNAME)
	cd dist && tar -czf $(DISTNAME)-src.tgz $(DISTNAME)
	rm -r dist/$(DISTNAME)

all: compile $(RESOURCES) setup doc gen-javadoc

doc: jgloss.pdf index.html

gen-javadoc:
	-mkdir javadoc
	javadoc -d javadoc -private \
                -classpath "src:$(CLASSPATH)" \
                -sourcepath src \
                -windowtitle "JGloss documentation" -doctitle "JGloss documentation" \
                -author -version \
                $(PACKAGES)

setup:
	cp -r src/data .

compile:
	$(JAVAC) $(JAVAC_FLAGS) -d . -classpath "$(CLASSPATH)" $(ROOT_SOURCE)

# Convert properties files in native charset to ascii.
# The charset is given in the input file by a line of the form "# Encoding: SOME-ENCODING"
%.properties: %.properties.in
	-mkdir resources
	$(NATIVE2ASCII) -encoding $(shell sed -n -e "/^# *Encoding:/s/.*Encoding:\(.*\)/\1/p" $<) $< resources/$@
	cp resources/$@ src/resources/$@

%.pdf: %.docbook doc.src/stylesheet.dsl
	-mkdir doc
	cp -r doc.src/img doc
	docbook2pdf -o doc -d $(shell pwd)/doc.src/stylesheet.dsl $<
	rm -r doc/img
	rm doc/jgloss.out

index.html: jgloss.docbook doc.src/stylesheet.dsl
	-mkdir -p doc/html
	cp -r doc.src/img doc/html
	docbook2html -o doc/html -d $(shell pwd)/doc.src/stylesheet.dsl $<

clean:
	-rm -r jgloss resources data dist doc javadoc jgloss.jar
