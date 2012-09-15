package jgloss.dictionary.filebased;

import static jgloss.util.StringTools.tokenize;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryEntry;
import jgloss.dictionary.ExpressionSearchModes;
import jgloss.dictionary.MalformedEntryException;
import jgloss.dictionary.MultiWordEntry;
import jgloss.dictionary.SearchException;
import jgloss.dictionary.SearchFieldSelection;
import jgloss.dictionary.SingleWordEntry;
import jgloss.dictionary.attribute.Attribute;
import jgloss.dictionary.attribute.AttributeMapper;
import jgloss.dictionary.attribute.AttributeSet;
import jgloss.dictionary.attribute.Attributes;
import jgloss.dictionary.attribute.DefaultAttributeSet;
import jgloss.dictionary.attribute.InformationAttributeValue;
import jgloss.dictionary.attribute.Priority;
import jgloss.dictionary.attribute.SearchReference;

class EDictEntryParser implements EntryParser {

	private static final Logger LOGGER = Logger.getLogger(EDictEntryParser.class.getPackage().getName());
	
    /**
     * Match an EDICT entry. Group 1 is the word(s), group 2 the (optional) reading(s) and group 3
     * the translations. The optional last group is a reference to the corresponding JMDict entry.
     * This entry is currently not used and skipped
     */
    private static final Pattern ENTRY_PATTERN = Pattern.compile
        ( "(\\S+)(?:\\s\\[(.+?)\\])?\\s/(.+?)/(EntL.+/)?");

    /**
     * Match a string in brackets at the beginning of a string. Group 1 matches all numbers, which is the ROM marker.
     * Group 2 matches a reference to another entry. Group 3 matches an arbitrary string in brackets.
     */
    private static final Pattern BRACKET_PATTERN = Pattern.compile( "\\G(?:\\((\\d+)\\)|\\([Ss]ee (.+?)\\)|\\((.+?)\\))\\s");

    private static final String PRIORITY_MARKER = "(P)";

    static final Priority PRIORITY_VALUE = new Priority() {
            @Override
			public String getPriority() { return "_P_"; }
            @Override
			public int compareTo( Priority p) {
                if (p == PRIORITY_VALUE) { // NOPMD: singleton comparison
	                return 0;
                } else {
	                throw new IllegalArgumentException();
                }
            }
            @Override
			public String toString() { return "_P_"; }
        };


    private static final SearchFieldSelection MATCH_WORD_FIELD =
    				new SearchFieldSelection( true, false, false, true, false);

    static final AttributeMapper MAPPER = initMapper();

    private static AttributeMapper initMapper() {
    	try {
    		Reader r = new InputStreamReader( EDict.class.getResourceAsStream( "/edict.map"));
    		AttributeMapper mapper = new AttributeMapper( new LineNumberReader( r));
    		r.close();
    		return mapper;
    	} catch (IOException ex) {
    		LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
    		return null;
    	}
    }
        
    private Dictionary edict;

	EDictEntryParser() {
	}

	@Override
	public void setDictionary(Dictionary edict) {
		assert edict instanceof EDict;
		this.edict = edict;
	}

	
    /**
     * Parses an EDICT formatted entry. The format is
     * <CODE>word [reading] /translation 1/translation 2/...</CODE> with the reading
     * being optional.
     */
	@Override
    public DictionaryEntry parseEntry( String entry, int startOffset) throws SearchException {
        Matcher entryMatcher = matchEntry(entry);

        String[] words = parseWords(entryMatcher);
        String[] readings = parseReadings(entryMatcher, words);

        List<List<String>> rom = new ArrayList<List<String>>( 10);
        List<String> crm = new ArrayList<String>( 10);
        rom.add( crm);

        DefaultAttributeSet generalA = new DefaultAttributeSet();
        DefaultAttributeSet wordA = new DefaultAttributeSet( generalA);
        DefaultAttributeSet translationA = new DefaultAttributeSet( generalA);
        List<AttributeSet> roma = new ArrayList<AttributeSet>( 10);
        DefaultAttributeSet translationromA = new DefaultAttributeSet( translationA);
        roma.add( translationromA);

        parseTranslations(entryMatcher.group( 3), rom, crm, generalA, wordA, translationA, roma, translationromA);

        DictionaryEntry dictionaryEntry;
        if (words.length == 1 && readings.length == 1) {
        	dictionaryEntry = new SingleWordEntry(startOffset, words[0], readings[0], rom,
        					generalA, wordA, translationA, roma, edict);
        } else {
        	dictionaryEntry = new MultiWordEntry(startOffset, words, readings,
        					rom, generalA, wordA, null, translationA, roma, edict);
        }
        return dictionaryEntry;
    }

	private void parseTranslations(String translations, List<List<String>> rom, List<String> crm, DefaultAttributeSet generalA, DefaultAttributeSet wordA,
                    DefaultAttributeSet translationA, List<AttributeSet> roma, DefaultAttributeSet translationromA) {
	    // ROM markers and entry attributes are written in brackets before the translation text.
        // Attributes which are placed in the first translation before the first ROM marker apply
        // to the whole entry, the other attributes only apply to the ROM.
        boolean seenROM = false;
        
        for (String translation : tokenize(translations, "/")) {
            if (translation.equals( PRIORITY_MARKER)) {
                generalA.addAttribute( Attributes.PRIORITY, PRIORITY_VALUE);
            } else {
                Matcher bracketMatcher = BRACKET_PATTERN.matcher(translation);
                int matchend = 0;

                while (bracketMatcher.find()) {
                    matchend = bracketMatcher.end();

                    // the matcher is constructed such that either group 1, group 2 or group 3 matches (is not null)
                    String romMarker = bracketMatcher.group(1);
                    String refs = bracketMatcher.group(2);
                    String att = bracketMatcher.group(3);

                    if (romMarker != null) {
                        // ROM marker, start new ROM unless this is the first ROM
                        if (!crm.isEmpty()) {
                            crm = new ArrayList<String>( 10);
                            rom.add( crm);
                            translationromA = new DefaultAttributeSet( translationA);
                            roma.add( translationromA);
                        }
                        seenROM = true;
                    } else if (refs != null) {
                    	for (String ref : tokenize(refs, ",")) {
                    	generalA.addAttribute(Attributes.REFERENCE, 
                    					new SearchReference(ref, edict, ExpressionSearchModes.EXACT,
                    									ref, MATCH_WORD_FIELD));
                    	}
                    } else {
                        // attribute list separated by ','
                    	for (String attsub : tokenize(att, ",")) {
                            addAttribute(generalA, wordA, translationA, translationromA, seenROM, attsub);
                        }
                    }
                }

                if (matchend > 0) {
	                translation = translation.substring( matchend, translation.length());
                }

                crm.add( translation);
            }
        }
    }

	private void addAttribute(DefaultAttributeSet generalA, DefaultAttributeSet wordA, DefaultAttributeSet translationA, DefaultAttributeSet translationromA,
                    boolean seenROM, String attsub) {
	    AttributeMapper.Mapping<?> mapping = MAPPER.getMapping( attsub);
	    if (mapping != null) {
	        Attribute<?> a = mapping.getAttribute();
	        if (a.appliesTo( DictionaryEntry.AttributeGroup.GENERAL) &&
	            (!seenROM ||
	             !a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION))) {
	            generalA.addAttribute(mapping);
	        } else if (a.appliesTo( DictionaryEntry.AttributeGroup.WORD)) {
	            wordA.addAttribute(mapping);
	        } else if (a.appliesTo( DictionaryEntry.AttributeGroup.TRANSLATION)) {
	            if (seenROM) {
	                translationromA.addAttribute(mapping);
	            } else {
	                translationA.addAttribute(mapping);
	            }
	        } else {
	            // should not happen, edict does not support READING attributes
	            LOGGER.warning( "EDICT warning: illegal attribute type");
	        }
	    } else {
	        // Not an explicitly defined attribute.
	    	InformationAttributeValue attributeValue = new InformationAttributeValue(attsub);
	    	if (seenROM) {
	    		translationromA.addAttribute(Attributes.EXPLANATION, attributeValue);
	    	} else {
	    		translationA.addAttribute(Attributes.EXPLANATION, attributeValue);
	    	}
	    }
    }

	private String[] parseReadings(Matcher entryMatcher, String[] words) {
	    String[] readings;
        String readingGroup = entryMatcher.group( 2);
        if (readingGroup == null) {
	        readings = words;
        } else {
        	readings = readingGroup.split(";");
        }
        return readings;
    }

	private String[] parseWords(Matcher entryMatcher) {
	    return entryMatcher.group( 1).split(";");
    }

	private Matcher matchEntry(String entry) {
	    Matcher entryMatcher = ENTRY_PATTERN.matcher(entry);
        if (!entryMatcher.matches()) {
	        throw new MalformedEntryException( edict, entry);
        }
	    return entryMatcher;
    }
}
