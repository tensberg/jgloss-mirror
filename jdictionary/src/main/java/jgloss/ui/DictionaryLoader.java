package jgloss.ui;

import static java.util.logging.Level.SEVERE;
import static jgloss.JGloss.MESSAGES;
import static jgloss.ui.util.SwingWorkerProgressFeedback.DEFAULT_MESSAGE_PROPERTY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import jgloss.dictionary.Dictionary;
import jgloss.dictionary.DictionaryFactory;
import jgloss.dictionary.DictionaryFactory.InstantiationException;
import jgloss.dictionary.DictionaryFactory.NotSupportedException;
import jgloss.dictionary.IndexedDictionary;

/**
 * Thread used to load dictionaries asynchronously when the user has selected "add dictionaries".
 */
class DictionaryLoader extends SwingWorker<List<LoadingFailure>, DictionaryWrapper> {
    private static final Logger LOGGER = Logger.getLogger(DictionaryLoader.class.getPackage().getName());
    
    private final List<String> dictionaryDescriptors;
    
    private final Dictionaries dictionaries;

    private final DefaultListModel model;
    
    /**
     * Load the dictionaries from the list of files and add them to the current list of
     * dictionaries. The dictionaries are loaded in their own thread. If the thread does
     * not terminate after one second, this method will pop up a model information dialog and
     * return. The thread will dispose the dialog after it has loaded all dictionaries and display
     * any error messages for errors in dictionary loading.
     *
     * @param dictionaryDescriptors List of dictionary descriptors to load. If a dictionary descriptor is already
     *        loaded, it will be ignored.
     */
    DictionaryLoader(Dictionaries dictionaries, DefaultListModel model, List<String> dictionaryDescriptors) {
        this.dictionaries = dictionaries;
        this.model = model;
        this.dictionaryDescriptors = dictionaryDescriptors;
    }
    
    @Override
    protected List<LoadingFailure> doInBackground() {
        List<LoadingFailure> failures = new ArrayList<LoadingFailure>(dictionaryDescriptors.size());       
        int loadedDictionaryCount = 0;
        for (String descriptor : dictionaryDescriptors) {
            try {
                loadDictionary(descriptor);
            } catch (Exception ex) {
                failures.add(new LoadingFailure(descriptor, ex));
            }
            loadedDictionaryCount++;
            setProgress(loadedDictionaryCount*100/dictionaryDescriptors.size());
        }
        
        return failures;
    }
    
    @Override
    protected void process(List<DictionaryWrapper> chunks) {
        for (DictionaryWrapper wrapper : chunks) {
            model.addElement(wrapper);
        }
    }
    
    @Override
    protected void done() {
        try {
            for (LoadingFailure failure : get()) {
                dictionaries.showDictionaryError(failure.getCause(), failure.getDescriptor());
            }
        } catch (Exception ex) {
            LOGGER.log(SEVERE, "failed to load dictionaries", ex);
        }
    }
    
    private DictionaryWrapper loadDictionary(String descriptor) throws NotSupportedException, InstantiationException {
        firePropertyChange(DEFAULT_MESSAGE_PROPERTY, null, MESSAGES.getString( "dictionaries.loading",
                        new File( descriptor).getName()));
        
        Dictionary dictionary = DictionaryFactory.createDictionary(descriptor);
        
        if (dictionary instanceof IndexedDictionary) {
            IndexedDictionary indexedDictionary = (IndexedDictionary) dictionary;
            boolean indexLoaded = indexedDictionary.loadIndex();
            if (!indexLoaded) {
                indexedDictionary.buildIndex();
            }
        }
        
        return new DictionaryWrapper(descriptor, dictionary);
    }
}