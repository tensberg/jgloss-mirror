/*
 * Copyright (C) 2002-2004 Michael Koch (tensberg@gmx.net)
 *
 * This file is part of JGloss.
 *
 * JGloss is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JGloss is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGloss; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * $Id$
 *
 */

package jgloss.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgloss.dictionary.attribute.Abbreviation;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeMapper;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.DefaultAttributeFormatter;
import jgloss.dictionary.attribute.DefaultAttributeSet;
import jgloss.dictionary.attribute.Gairaigo;
import jgloss.dictionary.attribute.InformationAttributeValue;
import jgloss.dictionary.attribute.ReferenceAttributeValue;
import jgloss.dictionary.attribute.SearchReference;
import jgloss.util.DefaultListFormatter;

/**
 * Implementation for dictionaries in WadokuJT.txt format. 
 * WadokuJT is a Japanese-German dictionary directed by Ulrich Apel 
 * (see <a href="http://www.wadoku.org">http://www.wadoku.org</a>).
 * The WadokuJT.txt file form of the dictionary is maintained by Hans-Joerg Bibiko and available
 * from <a href="http://www.bibiko.com/dlde.htm">http://www.bibiko.com/dlde.htm</a>.
 *
 * @author Michael Koch
 */
public class WadokuJT extends FileBasedDictionary {
    public static void main( String[] args) throws Exception {
        System.err.println( "Creating WadokuJT");
        IndexedDictionary d = new WadokuJT( new java.io.File( args[0]));
        System.err.println( "Loading index");
        if (!d.loadIndex()) {
            System.err.println( "Building index");
            d.buildIndex();
        }
        System.err.println( "Successfully loaded index");
        SearchFieldSelection f = new SearchFieldSelection();
        f.select( DictionaryEntryField.WORD, true);
        f.select( DictionaryEntryField.READING, true);
        f.select( DictionaryEntryField.TRANSLATION, true);
        f.select( MatchMode.WORD, true);

        DictionaryEntryFormatter formatter = new DictionaryEntryFormatter
            ( new DefaultListFormatter( "", "; ", ""),
              new DefaultListFormatter( " [", ": ", "]"),
              new DefaultListFormatter( "", " ", ".", " (n) ", ". (n) ", "."),
              new DefaultListFormatter( "", "; ", ""),
              new DefaultListFormatter( "", "/", ""));
        formatter.addAttributeFormat( Attributes.PART_OF_SPEECH,
                                      new DefaultAttributeFormatter
                                      ( " (", ")", "", false, new DefaultListFormatter( ",")),
                                      DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        formatter.addAttributeFormat( Attributes.EXAMPLE,
                                      new DefaultAttributeFormatter( " {", "}", "", true, null),
                                      DictionaryEntryFormatter.Position.BEFORE_FIELD3);
        formatter.addAttributeFormat( Attributes.EXPLANATION, new DefaultAttributeFormatter
                                      ( " (", ")", "", false, new DefaultListFormatter( ",")), false);

        ResultIterator r = d.search( ExpressionSearchModes.ANY,
                                     new Object[] { args[1], f });
        System.err.println( "Matches:");
        StringBuffer out = new StringBuffer( 128);
        while (r.hasNext()) {
            out.setLength( 0);
            DictionaryEntry de = (DictionaryEntry) r.next();
            System.err.println( de);
            System.err.println( formatter.format( de, out).toString());
        }
        //r.next();
        d.dispose();
    }

    protected static final ResourceBundle NAMES = ResourceBundle.getBundle
        ( "resources/messages-dictionary");

    public static final Attribute MAIN_ENTRY = new Attributes
        ( NAMES.getString( "wadoku.att.main_entry.name"),
          NAMES.getString( "wadoku.att.main_entry.desc"),
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute MAIN_ENTRY_REF = new Attributes
        ( NAMES.getString( "wadoku.att.main_entry_ref.name"),
          NAMES.getString( "wadoku.att.main_entry_ref.desc"),
          true, ReferenceAttributeValue.class, Attributes.EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    public static final Attribute ALT_READING = new Attributes
        ( NAMES.getString( "wadoku.att.alt_reading.name"),
          NAMES.getString( "wadoku.att.alt_reading.desc"),
          true, ReferenceAttributeValue.class, Attributes.EXAMPLE_REFERENCE_VALUE,
          new DictionaryEntry.AttributeGroup[] 
            { DictionaryEntry.AttributeGroup.GENERAL });

    protected static final int EXPLANATION_MIN_LENGTH = 10;

    /**
     * Name of the dictionary format.
     */
    public static final String FORMAT_NAME = "WadokuJT";

    protected static final AttributeMapper mapper = initMapper();

    private static AttributeMapper initMapper() {
        try {
            Reader r = new InputStreamReader( WadokuJT.class.getResourceAsStream
                                              ( "/resources/wadokujt.map"),
                                              "UTF-8");
            AttributeMapper mapper = new AttributeMapper( new LineNumberReader( r));
            r.close();
            return mapper;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected static final Map gairaigoMap = initGairaigoMap();

    private static Map initGairaigoMap() {
        try {
            Map map = new HashMap( 19);
            
            Matcher matcher = Pattern.compile( "\\A(\\S+)\\s+(\\S+)\\Z").matcher( "");
            LineNumberReader r = new LineNumberReader
                ( new InputStreamReader
                  ( WadokuJT.class.getResourceAsStream
                    ( "/resources/wadokujt-gairaigo.map"), "UTF-8"));

            String line;
            while ((line=r.readLine()) != null) {
                line = line.trim();
                if (line.length()==0 || line.charAt( 0)=='#')
                    continue;

                matcher.reset( line);
                if (matcher.find()) {
                    map.put( matcher.group( 1), matcher.group( 2));
                }
                else {
                    throw new IOException( "malformed line " + r.getLineNumber() +
                                           " in wadokujt-gairaigo.map");
                }
            }
            r.close();
            return map;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected static final SearchFieldSelection MATCH_WORD_FIELD =
        new SearchFieldSelection( true, false, false, true, false);

    /**
     * Object describing this implementation of the <CODE>Dictionary</CODE> interface. The
     * Object can be used to register this class with the <CODE>DictionaryFactory</CODE>, or
     * test if a descriptor matches this class.
     *
     * @see DictionaryFactory
     */
    public static final DictionaryFactory.Implementation implementation = 
        initImplementation();

    /**
     * Returns a {@link FileBasedDictionary.Implementation FileBasedDictionary.Implementation}
     * which recognizes UTF-8 encoded Wadoku dictionaries. Used to initialize the
     * {@link #implementation implementation} final member because the constructor has to
     * be wrapped in a try/catch block.
     * 
     */
    private static DictionaryFactory.Implementation initImplementation() {
        try {
            // Dictionary entries are of the form
            // japanese|reading|part of speech|translation|comment|reference
            // reading,part of speech, comment and reference may be empty.
            // At least four of the fields must be present in the first line of the file for
            // the match to be successful.
            return new FileBasedDictionary.Implementation
                ( FORMAT_NAME, "UTF-8", true, Pattern.compile
                  ( "\\A(.*?\\|){3}.*$", Pattern.MULTILINE),
                  1.0f, 4096, WadokuJT.class.getConstructor( new Class[] { File.class }));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Matches each word entry with alternatives. The word match is stored in group 1, the
     * alternatives are stores as single string in group 2.
     */
    protected final static Pattern WORD_PATTERN = Pattern.compile
        ( "(\\S+)" + // word text
          "(?:(?:\\s\\[\\w+\\])|(?:\\s\\{.+?\\}))*" + // remarks, cross references
          "(?:\\s\\((.+?)\\))?" + // alternative spellings
          "(?:(?:\\s\\[\\w+\\])|(?:\\s\\{.+?\\}))*" + // remarks, cross references
          "(?:;\\s|$)"); // end of word
    protected Matcher WORD_MATCHER = WORD_PATTERN.matcher( "");
    /**
     * Matches semicolon-separated alternatives. The separator is a semicolon followed by a single
     * whitespace. The matched alternative is stored in group 1. Semicolons in brackets are ignored.
     * If an opening bracket is not matched by a closing bracket, everything to the end of the
     * pattern is matched.
     */
    protected final static Pattern ALTERNATIVES_PATTERN = Pattern.compile
        ( "((?:[^(\\{]|" + // normal text
          "(?:\\(.*?[)|$])|" + // text in (), ignore "; "
          "(?:\\{.*?[}|$]))+?)" + // text in {}, ignore "; "
          "(?:\\s\\[\\w+\\])?" + // optional comment (ignored)
          "(?:\\s\\{.+?\\})?" + // optional comment (ignored)
          "(?:;\\s|$)"); // separation marker
    protected Matcher ALTERNATIVES_MATCHER = ALTERNATIVES_PATTERN.matcher( "");
    /**
     * Matches translation ranges of meaning. Group 1 contains the number of the range written in 
     * brackets at the beginning of the entry (or <code>null</code> if there is no such number), 
     * group 2 contains a string of all the meanings in the range.
     */
    protected final static Pattern TRANSLATIONS_PATTERN = Pattern.compile
        ( "(?:\\[(\\d+)\\]\\s|//\\s)?(.+?)\\.?\\s?(?=\\[\\d+\\]|//|$)");
    protected Matcher TRANSLATIONS_MATCHER = TRANSLATIONS_PATTERN.matcher( "");

    protected final static Pattern CATEGORY_PATTERN = Pattern.compile
        ( "(.+?)(?:[,;]\\s?|\\z)");
    protected final Matcher CATEGORY_MATCHER = CATEGORY_PATTERN.matcher( "");

    protected final static Pattern REFERENCE_PATTERN = Pattern.compile
        ( "(\u21d2|\u2192|\u21d4)\\s(\\S+)(?:\\s\\(.*?\\))(?:;\\s|\\z)");
    protected final Matcher REFERENCE_MATCHER = REFERENCE_PATTERN.matcher( "");

    protected final static Pattern GAIRAIGO_PATTERN = Pattern.compile
        ( "(?:\\A|; )(?:(?:von (\\S+?)\\.? \"([^\"]+)\")|(?:aus d(?:em|\\.) (\\S+?)\\.?))(?:; |\\Z)");
    protected final static Matcher GAIRAIGO_MATCHER = GAIRAIGO_PATTERN.matcher( "");

    protected final static Pattern ABBR_PATTERN = Pattern.compile
        ( "(?:\\A|; )Abk\\.?(?: (?:f\u00fcr |v(?:on|\\.) )?(?:(\\S+?)\\.?)? ?\"([^\"]+)\")?(?:; |\\Z)");
    protected final static Matcher ABBR_MATCHER = ABBR_PATTERN.matcher( "");

    public WadokuJT( File dicfile) throws IOException {
        super( dicfile);
    }

    protected void initSupportedAttributes() {
        super.initSupportedAttributes();
        
        supportedAttributes.putAll( mapper.getAttributes());
        supportedAttributes.put( Attributes.ABBREVIATION, null);
        supportedAttributes.put( Attributes.GAIRAIGO, null);
        supportedAttributes.put( Attributes.EXPLANATION, null);
        supportedAttributes.put( Attributes.REFERENCE, null);
        supportedAttributes.put( Attributes.SYNONYM, null);
        supportedAttributes.put( Attributes.ANTONYM, null);
        supportedAttributes.put( ALT_READING, null);
        supportedAttributes.put( MAIN_ENTRY, null);
        supportedAttributes.put( MAIN_ENTRY_REF, null);
    }

    protected EncodedCharacterHandler createCharacterHandler() {
        return new UTF8CharacterHandler();
    }

    protected boolean isFieldStart( ByteBuffer entry, int location, DictionaryEntryField field) {
        if (location == 0)
            return true;

        try {
            byte b = entry.get( location-1);
            if (b==';' || b=='|' || b==10 || b==13)
                return true;
            if (b == ' ') {
                byte b2 = entry.get( location-2);
                return (b2 == ';' || b2 == ']');
            }
            if (b=='(' && field==DictionaryEntryField.WORD)
                // ( followed by a 3-byte encoded character is assumed to be an alternative
                // spelling in the word field
                return true;

            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    protected boolean isFieldEnd( ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            byte b = entry.get( location);
            if (b==';' || b=='|' || b==10 || b==13)
                return true;
            if (b == '.') {
                // end of translation if followed by field end marker '|' or new range of meaning " [..."
                byte b2 = entry.get( location+1);
                if (b2 == '|')
                    return true;
                else if (b2 == ' ') 
                    return (entry.get( location+2) == '[');
            }
            else if ((b==' ' || b==')') && 
                     field==DictionaryEntryField.WORD)
                return true;
            return false;
        } catch (IndexOutOfBoundsException ex) {
            return true;
        }
    }

    protected DictionaryEntryField moveToNextField( ByteBuffer buf, int character,
                                                    DictionaryEntryField field) {
        if (field == null) {
            // first call to moveToNextField
            // skip first (comment) line
            while (!isEntrySeparator( buf.get()))
                ; // buf.get() advances the loop
            return DictionaryEntryField.WORD;
        }

        if (character == '|') {
            if (field==DictionaryEntryField.WORD) {
                field = DictionaryEntryField.READING;
            } 
            else if (field==DictionaryEntryField.READING) {
                // skip to translation field
                field = DictionaryEntryField.TRANSLATION;
                byte c;
                do {
                    c = buf.get();
                    if (isEntrySeparator( c)) { // fallback for error in dictionary
                        field = DictionaryEntryField.WORD;
                        break;
                    }
                } while (c != '|');
            } 
            else if (field==DictionaryEntryField.TRANSLATION) {
                // skip fields to next entry
                while (!isEntrySeparator( buf.get()))
                    ; // buf.get() advances the loop
                field = DictionaryEntryField.WORD;
            }
            else
                throw new IllegalArgumentException();
        } else if (character==10 || character==13) {
            // broken dictionary entry; reset for error recovery
            field = DictionaryEntryField.WORD;
        }

        return field;
    }

    protected DictionaryEntryField getFieldType( ByteBuffer buf, int entryStart, int entryEnd,
                                                 int position) {
        // count field delimiters from location to entry start or end (whatever is closer)
        // note: entryEnd is the first position not to be read
        int fields = 0;
        byte c;
        if (position-entryStart <= entryEnd-position-1) {
            // read from start to location
            buf.position( entryStart);
            while (buf.position() <= position) {
                if (buf.get() == '|')
                    fields++;
            }
            switch (fields) {
            case 0:
                return DictionaryEntryField.WORD;
            case 1:
                return DictionaryEntryField.READING;
            case 3:
                return DictionaryEntryField.TRANSLATION;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
        else {
            // read from location to end
            buf.position( position);
            while (buf.position() < entryEnd) {
                if (buf.get() == '|')
                    fields++;
            }
            switch (fields) {
            case 2:
                return DictionaryEntryField.TRANSLATION;
            case 4:
                return DictionaryEntryField.READING;
            case 5:
                return DictionaryEntryField.WORD;
            default:
                return DictionaryEntryField.OTHER;
            }
        }
    }

    protected DictionaryEntry parseEntry( String entry, int startOffset) throws SearchException {
        try {
            DictionaryEntry out = null; 
            List wordlist = new ArrayList( 10);
            String reading;
            List rom = new ArrayList( 10);
            DefaultAttributeSet generalA = new DefaultAttributeSet( null);
            DefaultAttributeSet wordA = new DefaultAttributeSet( generalA);
            List wordsA = new ArrayList( 10);
            DefaultAttributeSet translationA = new DefaultAttributeSet( generalA);
            List romA = new ArrayList( 10);

            int start = 0;
            int end = entry.indexOf( '|');

            // parse word field
            String words = entry.substring( start, end);
            // split words
            WORD_MATCHER.reset( words);
            while (WORD_MATCHER.find()) {
                wordlist.add( unescape( WORD_MATCHER.group( 1)));
                wordsA.add( null);
                if (WORD_MATCHER.group( 2) != null) {
                    // word with alternatives
                    ALTERNATIVES_MATCHER.reset( WORD_MATCHER.group( 2));
                    while (ALTERNATIVES_MATCHER.find()) {
                        wordlist.add( unescape( ALTERNATIVES_MATCHER.group( 1)));
                        wordsA.add( null);
                    }
                }
            }   

            start = end+1;
            end = entry.indexOf( '|', start);
            reading = entry.substring( start, end);
            // cut off [n] marker
            int bracket = reading.lastIndexOf( '[');
            if (bracket != -1)
                // the [ must always be preceeded by a single space, therefore bracket-1
                reading = unescape( reading.substring( 0, bracket-1));
            if (reading.length() == 0)
                reading = null;

            // parse part of speech
            start = end+1;
            end = entry.indexOf( '|', start);
            String pos = entry.substring( start, end);
            // POS field may encode several POS attributes. Try to find matching
            // POS attributes for longes possible prefix of pos, and repeat with the remainder.
            nextpos: while (pos.length() > 0) {
                for ( int i=pos.length(); i>0; i--) {
                    AttributeMapper.Mapping mapping = mapper.getMapping( pos.substring( 0, i));
                    if (mapping != null) {
                        generalA.addAttribute( mapping.getAttribute(), mapping.getValue());
                        // continue outer loop with remainder
                        pos = pos.substring( i);
                        continue nextpos;
                    }
                }
                // no match found, cut off first char and try again
                pos = pos.substring( 1);
            }

            // parse translation field
            start = end+1;
            end = entry.indexOf( '|', start);
            String translations = entry.substring( start, end);

            // split translations; translationsMatcher matches each ROM
            Matcher translationsMatcher = TRANSLATIONS_PATTERN.matcher( translations);
            while (translationsMatcher.find()) {
                // Test if the attributes apply to all translations. This is the case if
                // they are written before the first ROM marker, or if there are no ROM
                // markers. In both cases, group(1) is null. If allTranslations is
                // false, attributes apply only to the current ROM.
                boolean allTranslations = translationsMatcher.group( 1) == null;

                String crm = translationsMatcher.group( 2); // one ROM of CRMs
                // attributes of this rom
                DefaultAttributeSet thisRomA = new DefaultAttributeSet( translationA);

                // handle abbreviation/gairaigo/explanation (always in () brackets at end of rom)
                if (crm.charAt( crm.length()-1) == ')') try {
                    int openb = crm.lastIndexOf( '(');
                    String ex = crm.substring( openb+1, crm.length()-1);
                    // cut off comment (last char before ( is a space)
                    crm = crm.substring( 0, openb-1);

                    ABBR_MATCHER.reset( ex);
                    if (ABBR_MATCHER.find()) {
                        String lang = ABBR_MATCHER.group( 1);
                        String code = null;
                        if (lang != null) {
                            code = (String) gairaigoMap.get( lang.toLowerCase());
                            if (code == null) {
                                System.err.println( "WadokuJT warning: unrecognized language " +
                                                    lang + " (" + ex + ")");
                                code = lang;
                            } 
                        }
                        String word = ABBR_MATCHER.group( 2);
                        Abbreviation abbr = null;
                        if (word != null)
                            abbr = new Abbreviation( word, code);
                        if (allTranslations)
                            generalA.addAttribute( Attributes.ABBREVIATION, abbr);
                        else
                            thisRomA.addAttribute( Attributes.ABBREVIATION, abbr);

                        // strip abbr comment from crm
                        if (ABBR_MATCHER.start() > 0) {
                            if (ABBR_MATCHER.end() < ex.length()) {
                                ex = ex.substring( 0, ABBR_MATCHER.start()) + "; " +
                                    ex.substring( ABBR_MATCHER.end());
                            }
                            else {
                                ex = ex.substring( 0, ABBR_MATCHER.start());
                            }
                        }
                        else
                            ex = ex.substring( ABBR_MATCHER.end());
                    }

                    GAIRAIGO_MATCHER.reset( ex);
                    if (GAIRAIGO_MATCHER.find()) {
                        String lang;
                        String word = null;
                        if (GAIRAIGO_MATCHER.group( 1) != null) {
                            // gairaigo with original word
                            lang = GAIRAIGO_MATCHER.group( 1);
                            word = GAIRAIGO_MATCHER.group( 2);
                        }
                        else {
                            // gairaigo without original word
                            lang = GAIRAIGO_MATCHER.group( 3);
                        }
			    
                        String code = (String) gairaigoMap.get( lang.toLowerCase());
                        if (code != null) {
                            Gairaigo gairaigo = new Gairaigo( word, code);
                            if (allTranslations)
                                generalA.addAttribute( Attributes.GAIRAIGO, gairaigo);
                            else
                                thisRomA.addAttribute( Attributes.GAIRAIGO, gairaigo);

                            // strip gairaigo comment from crm
                            if (GAIRAIGO_MATCHER.start() > 0) {
                                if (GAIRAIGO_MATCHER.end() < ex.length()) {
                                    ex = ex.substring( 0, GAIRAIGO_MATCHER.start()) + "; " +
                                        ex.substring( GAIRAIGO_MATCHER.end());
                                }
                                else {
                                    ex = ex.substring( 0, GAIRAIGO_MATCHER.start());
                                }
                            }
                            else
                                ex = ex.substring( GAIRAIGO_MATCHER.end());
                        }
                        else {
                            System.err.println( "WadokuJT warning: unrecognized language " +
                                                lang + " (" + ex + ")");
                        }
                    }

                    if (ex.length() > EXPLANATION_MIN_LENGTH) {
                        thisRomA.addAttribute( Attributes.EXPLANATION,
                                               new InformationAttributeValue( ex));
                        ex = "";
                    }

                    // append remainder of ex to crm
                    if (ex.length() > 0)
                        crm += " (" + ex + ")";
                } catch (StringIndexOutOfBoundsException ex) {
                    System.err.println
                        ( "WadokuJT warning: missing opening bracket in translation " + crm);
                    // can be safely ignored
                }

                // handle categories (marked at beginning of translation enclosed in {})
                if (crm.charAt( 0) == '{') try {
                    int endb = crm.indexOf( '}');
                    // attribute strings unrecognized by mapping
                    StringBuffer unrecognized = null;

                    CATEGORY_MATCHER.reset( crm.substring( 1, endb));
                    while (CATEGORY_MATCHER.find()) {
                        String cat = CATEGORY_MATCHER.group( 1);
                        AttributeMapper.Mapping mapping = mapper.getMapping( cat);
                        if (mapping != null) {
                            Attribute att = mapping.getAttribute();
                            if (allTranslations) {
                                if (att.appliesTo
                                    ( DictionaryEntry.AttributeGroup.GENERAL))
                                    generalA.addAttribute( att, mapping.getValue());
                                else if (att.appliesTo
                                         ( DictionaryEntry.AttributeGroup.TRANSLATION))
                                    translationA.addAttribute( att, mapping.getValue());
                                else // program error, should not happen
                                    throw new SearchException
                                        ( "wrong attribute type for " + cat);
                            }
                            else {
                                if (att.appliesTo
                                    ( DictionaryEntry.AttributeGroup.TRANSLATION)) {
                                    thisRomA.addAttribute( att, mapping.getValue());
                                }
                                else if (att.appliesTo
                                         ( DictionaryEntry.AttributeGroup.GENERAL))
                                    generalA.addAttribute( att, mapping.getValue());
                                else // program error, should not happen
                                    throw new SearchException
                                        ( "wrong attribute type for " + cat);
                            }
                        }
                        else {
                            // unrecognized category
                            if (unrecognized == null) {
                                unrecognized = new StringBuffer( cat.length() + 128);
                                unrecognized.append( '{');
                                unrecognized.append( cat);
                            }
                            else {
                                unrecognized.append( "; ");
                                unrecognized.append( cat);
                            }
                        }
                    }

                    // cut off categories
                    if (crm.length()>endb+1 && crm.charAt( endb + 1) == ' ')
                        endb++;
                    crm = crm.substring( endb + 1);
                    if (crm.length() == 0) {
                        // just attributes, no translations
                        continue;
                    }
                    if (unrecognized != null) {
                        // restore unrecognized categories
                        unrecognized.append( "} ");
                        unrecognized.append( crm);
                        crm = unrecognized.toString();
                    }
                } catch (StringIndexOutOfBoundsException ex) {
                    System.err.println
                        ( "WadokuJT warning: missing closing bracket in translation " + crm);
                    // can be safely ignored
                }

                List crml = new ArrayList( 10);
                rom.add( crml);
                ALTERNATIVES_MATCHER.reset( crm);
                while (ALTERNATIVES_MATCHER.find()) {
                    crml.add( unescape( ALTERNATIVES_MATCHER.group( 1)));
                }

                if (thisRomA.isEmpty())
                    romA.add( null);
                else
                    romA.add( thisRomA);
            }

            // parse comment/reference field
            start = end+1;
            end = entry.indexOf( '|', start);
            if (end > start+1) {
                String comment = entry.substring( start, end);
                REFERENCE_MATCHER.reset( comment);
                while (REFERENCE_MATCHER.find()) {
                    char tc = REFERENCE_MATCHER.group( 1).charAt( 0);
                    Attribute type;
                    if (tc == '\u2192')
                        type = ALT_READING;
                    else if (tc == '\u21d2')
                        type = Attributes.REFERENCE;
                    else
                        type = Attributes.ANTONYM;
                    generalA.addAttribute( type, new SearchReference
                                           ( REFERENCE_MATCHER.group( 2), this,
                                             ExpressionSearchModes.EXACT,
                                             new Object[] { REFERENCE_MATCHER.group( 2),
                                                            MATCH_WORD_FIELD }));
                }
            }

            // parse main entry field
            String mainEntry = entry.substring( end+1);
            if (mainEntry.startsWith( "HE")) {
                generalA.addAttribute( MAIN_ENTRY, null);
                if (mainEntry.length() <= 4)
                    mainEntry = "";
                else // cut off "HE; " part of mainEntry
                    mainEntry = mainEntry.substring( 4);
            }
            if (mainEntry.length() > 0) {
                // reference to main entry
                generalA.addAttribute( MAIN_ENTRY_REF, new SearchReference
                                       ( mainEntry, this, ExpressionSearchModes.EXACT, new Object[] 
                                           { mainEntry, MATCH_WORD_FIELD }));
            }

            // create entry
            if (wordlist.size() == 1) {
                out = new SingleWordEntry( startOffset, (String) wordlist.get( 0), reading, rom,
                                           generalA, wordA, translationA, romA, this);
            }
            else {
                out = new MultiWordEntry( startOffset, wordlist, reading, rom, generalA,
                                          wordA, wordsA, translationA, romA, this);
            }

            return out;
        } catch (Exception ex) {
            throw new MalformedEntryException( this, entry, ex);
        }
    }

    public String toString() {
        return FORMAT_NAME + " " + getName();
    }

    /**
     * Escape all dictionary special characters.
     */
    protected boolean escapeChar( char c) {
        switch (c) {
        case 10:
        case 13:
        case '|':
        case ';':
            return true;
        }

        return false;
    }
} // class WadokuJT
