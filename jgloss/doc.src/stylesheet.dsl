<!DOCTYPE style-sheet PUBLIC "-//James Clark//DTD DSSSL Style Sheet//EN" [
<!ENTITY % html "IGNORE">
<![%html;[
<!ENTITY % print "IGNORE">
<!ENTITY docbook.dsl PUBLIC "-//Norman Walsh//DOCUMENT DocBook HTML Stylesheet//EN" CDATA dsssl>
]]>
<!ENTITY % print "INCLUDE">
<![%print;[
<!ENTITY docbook.dsl PUBLIC "-//Norman Walsh//DOCUMENT DocBook Print Stylesheet//EN" CDATA dsssl>
]]>
]>

<!-- (c) 2001 Michael Koch
 This file contains some customizations for the documentation output.

 This file is part of JGloss.

 JGloss is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 JGloss is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with JGloss; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 $Id$ 
-->

<style-sheet>
<style-specification use="docbook">
<style-specification-body> 

(define (book-titlepage-recto-elements)
  (list 
   (normalize "title")
   (normalize "author")
   (normalize "copyright")
   (normalize "publisher")))

<![%print;[

(define %footnote-ulinks%
  ;; Generate footnotes for ULinks?
  #t)

]]>

<![%html;[

(define %html-ext% ".html")
(define %root-filename% "index")
;;(define nochunks #t)
;;(define rootchunk #t)

(define (toc-depth nd) 2)

(define (chunk-element-list)
  (list (normalize "preface")
	(normalize "chapter")
	(normalize "appendix") 
	(normalize "article")
	(normalize "glossary")
	(normalize "bibliography")
	(normalize "index")
	(normalize "setindex")
	(normalize "reference")
	(normalize "refentry")
	(normalize "part")
	(normalize "book")
	(normalize "set")
	))

(define (chunk-skip-first-element-list)
  '())

;;(define %html-header-tags% 
;;  '(("META" ("http-equiv" "content-type") ("content" "text/html; charset=EUC-JP"))))

]]>

</style-specification-body>
</style-specification>
<external-specification id="docbook" document="docbook.dsl">
</style-sheet>