  /*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


package tufts.vue;

import tufts.Util;

import java.util.*;
import java.lang.ref.*;

import com.google.common.collect.*;


/**
 * A general HashMap for storing property values: e.g., meta-data.
 *
 * @version $Revision: 1.1 $ / $Date: 2008-09-16 12:14:04 $ / $Author: sfraize $
 */

//public class PropertyMap //extends java.util.HashMap<String,Object>
// TODO: need to be able to specify a prefix, and maybe some special handling for nesting, tho what would that even be?
// having a list of maps w/different prefixes should be enough for most impls -- oh, yeah, tho we'd like a single
// FLAT property-map outer view of all the contained property maps.

// Holy crap: what if had an introspecting PropertyMap impl?  (or Map impl, or Multimap impl, or whatever)
// could specify which fields you want bound to what to use just a sub-set (e.g., LWComponent properties)
// Or in meantime, could just make use of our existing LWComponent.Key class to make it even easier.
// Might NOT want that to just be called the "node." sub-set: maybe something more semantic like "ui." or "display."

// TODO: this type of map should not be used for internal runtime properties that want
// to be set and re-set, for possibly including SoftReference'd properties, etc -- to
// switch to this impl, would need to add a class similar to old PropertyMap (w/out the
// addProperty / unique key stuff, e.g., a DataMap), but with the special Reference
// handling, Class property handling, etc.  Ideally have it impl a ClientData interface
// that any object can impl to use to pass through to the DataMap (class Resouce and
// class LWComponent could even subclass this, as their currently top level objects)

// TODO: may want to create this as a generic with Object as key (and maybe even value
// as Object v.s. a Collection, or some wrapper class that handles
// collection/singleton/meta-map), and handle in that any hierarchy encapsulation we end
// up wanting: e.g., a value could actually be another meta-map, and the flatting
// iterator could build up the key each time it descends.  Would make for very noisy
// persistance (fat keys) when flattened then serialized, tho would ideally create a
// specialized castor mapping that would beautifully handle the nesting, (hopefully
// castor's limitiations wouldn't get in our way).

// Don't forget that the main reason to using hashing collections for all of this is not
// for presenting a sorted order (that could be produced once), but to provide fast
// hashed lookups / searches / filters - tho currently all our searches are value based,
// we'll be adding keyword based searches.  (e.g. styling)  E.g., a TreeMap is not
// only a map that maintains sorted order, but uses the red-black tree as a hashing
// looked mechanism based on the searched for key.  Still, technically, I suppose
// if you know a "done/loaded" time for your collection, at which you could perform
// your sort once, you could use a LinkedHashMap all the way through, just
// performing the sort on it once (or a TreeMap to start, switching over to LinkedHashMap
// later)   Has anyone created a crazy run-time-polymorphic collection set such as this?

// *IDEALLY*, the end-all-be-all version, would actually maintain insertion order over
// time, as for repository meta-data this could be semantically relevant, but then be
// SWAPPABLE betwee that and sorted at any given time (e.g., a LinkedHashTreeMap).  Tho
// this may finally be an argument for just going ahead and use a LinkedHashMultiMap,
// which preserves everything, and then do any desired sorting at the UI level.  <*>
// This could be achieved simply by a linkedhashmap/set that provides a sorted view
// fetcher / sorted iterator fetcher, which caches the result (soft-reference even),
// and flushes cache should the collection be modified.

// Note: HashMap double-hashes: so must use IdentityHashMap if want to truly
// take advantage of identity hashing.  Could use String.intern() for
// creating all keys, tho apparenly this is intern() is quite slow --
// better off doing on your own.  This is a place a Schema could come
// in handy, providing unique Key objects.


public class MetaMap implements TableBag
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(MetaMap.class);

    //private final Multimap<String,Object> mData = Multimaps.newHashMultimap();
    //private final Multimap<String,Object> mData = Multimaps.newLinkedListMultimap();
    //private final Multimap<String,Object> mData = new TreeMultimap(); // attempting "dumb" comparators just made worse
    
    private final Multimap<String,Object> mData = Multimaps.newLinkedHashMultimap();

    //private final Map<String,Collection> mData = new LinkedHashMap();

    // Ideally we want a linked hash multi map (preserve key insertion order), with sorted-set values
    // -- looks like we'll need to create our own version for this.
    // Could do thus by subclassing LinkedHashMultimap, and overriding createCollection() to return
    // a new TreeSet()

    // Crap: LinkedHashMultimap preserves the order of all "KEY+VALUE" additions, not just the KEY...
    // So a StandardSortedSetMultimap impl should help us, but then create the key-container as a LinkedHashSet
    // -- could hack TreeMultimap for this, or perhaps LinkedHashMultimap to use a set for it's values
    
    // Also: do we really need the maps to be key-hashable?  Only rarely to we query by key, and that's
    // mostly internal Resource class usage, which should be using it's own set of (hashable) properties.
    // Oh -- YOU WANT THE KEYS TO BE HASHABLE: otherwise, building the property set will be very
    // slow in cases such as Resources that contain 500+ entries for the key "Subject", or "Keywords"

    // Google collections issues: creates lots of extra objects, provides no way to iterate
    // just through the keys.
    
//     public interface Listener {
//         void metaMapChanged(MetaMap p);
//     }

    private volatile SortedMapModel mTableModel;
    private Object mTableModel_LOCK = new Object();
    private boolean mHoldingChanges = false;
    private int mTotalSize = 0;
    private int mChanges;
    private List listeners;
    
    private static final String NULL_MASK = "(empty)";
    
    public MetaMap() {}

//     /** erase any other values for given key */
    public synchronized Object put(final String key, final Object value) {
        // backward compat for now
        return add(key, value);
    }
    
    
    //@Override
    public synchronized Object add(final String key, final Object value) {
        
        // TODO: we want to *preserve* case for display, but not differentiate based on it...
        //if (k instanceof String) k = ((String)k).toLowerCase();
        
        //            if (v instanceof String)
        //v = org.apache.commons.lang.StringEscapeUtils.unescapeHtml((String)v);
        
        //final Object prior = super.put(k, v == null ? NULL_MASK : v);
        //final Object prior = mData.put(k, v == null ? NULL_MASK : v);
        
        final Collection prior = mData.get(key);
        final boolean added;
        if (prior == null) {
            Collection bag = createCollection();
            bag.add(value);
            mData.put(key, bag);
            mTotalSize++;
        } else {
            if (prior.add(value))
                mTotalSize++;
        }
        
        // todo: this a bit overkill: could have a higher level
        // trigger for this, instead of triggering any table listeners
        // every time.  Our hold/release deals with specific batch
        // loads, which is good enough for now.
        
        if (mHoldingChanges)
            mChanges++;
        else if (mTableModel != null)
            mTableModel.reload();
        
        return prior;
    }

    private Collection createCollection() {
        // todo: okay, don't sort -- actually keep in order, but provide
        // a sorted caching collection-value fetcher
        return new TreeSet();
    }

    private static boolean hasContent(Object v) {

        if (v == null)
            return false;
        else if (v.getClass() == String.class && v.toString().length() == 0)
            return false;
        else if (v instanceof Collection && ((Collection)v).size() == 0)
            return false;
        else
            return true;

    }

    public Object addIfContent(String k, Object v) {
        return hasContent(v) ? add(k, v) : null;
    }
    

    //@Override
    public synchronized Object remove(Object key) {

        //final Object prior = super.remove(key);
        final Object prior = mData.removeAll(key);
        //final Object prior = mData.remove(key);

        if (prior != null) {

            mTotalSize--;
            //Util.printStackTrace("remove used; key=" + Util.tags(key) + "; prior=" + Util.tags(prior));
            
            if (mHoldingChanges)
                mChanges++;
            else if (mTableModel != null)
                mTableModel.reload();
        }

        return prior;
    }
    
    public synchronized Object get(String key) {
        Object o = mData.get(key);
        if (o instanceof Collection) {
            Collection bag = (Collection) o;
            if (bag.size() == 0)
                return null;
            else if (bag instanceof List)
                return ((List)bag).get(0);
            else
                return Iterators.get(bag.iterator(), 0);
        }
        else
            return o;
        //return super.get(k);
    }
    
    /** @return the property value for the given key, dereferened if an instanceof java.lang.ref.Reference is found */
    public Object getValue(String key) {

        Object o = get(key);
            
        if (o instanceof Reference) {
            o = ((Reference)o).get();
            if (DEBUG.Enabled && o == null)
                Log.debug("value was GC'd for key: " + key);
        }
        
        return o;
    }


    public String getProperty(String key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }
    
//     /** Set the value for the given key, overwriting any existing value. */
//     public void setProperty(String key, String value) {
//         put(key, value);
//     }

//     /**
//      * Add a property with the given key.  If a key already exists
//      * with this name, the key will be modified with an index.
//      * @return - the key actually created
//      */
//     public synchronized String addProperty(final String desiredKey, Object value) {
//         String key = desiredKey;
//         int index = 1;
//         while (containsKey(key))
//             key = String.format("%s.%03d", desiredKey, index++);
//         put(key, value);
//         return key;
//     }

    public int size() {
        //return mTotalSize;
        return mData.size();
    }
    public boolean containsKey(String key) {
        return mData.containsKey(key);
    }
    public Set keySet() {
        return mData.keySet();
    }
    // consider just having this impl map -- tho semantics may be very different
    public Map asMap() {
        //return mData;
        return mData.asMap();
    }

//     /** @return a set of entries that presents a flattened view of all key-value pairs */
//     public Collection<Map.Entry<String,Object>> entrySet() {
//         //return mData.entries();

//         // here we need the a flattening Collection forwarder, or
//         // replace entrySet with a special flattening iterator
//         //return mData.entrySet();
//     }
    
    public Collection<Map.Entry<String,Collection<Object>>> entrySet() {
        //return mData.entrySet();
        return mData.asMap().entrySet();
    }

    public Iterable<Map.Entry<String,Object>> entries() {
        return mData.entries();
    }
    
//     public Util.Itering<Map.Entry<String,?>> entries() {
//         return new FlatteningIterator(mData);
//     }
    
    public static Util.Itering<Map.Entry<String,?>> entries(Map map) {
        return new FlatteningIterator(map);
    }

    /** Flatten's a Map who's values are collections: returns a key/value for each value in each collection */
    private static final class FlatteningIterator<K,V> extends Util.AbstractItering<Map.Entry<K,V>> {

        final Iterator<Map.Entry<Object,Collection>> keyIterator;
        
        /** this object returned as the result every time */
        final KVEntry entry = new KVEntry();
        
        Iterator valueIterator;

        public FlatteningIterator(Map<Object,Collection> map) {
            keyIterator = map.entrySet().iterator();
            if (keyIterator.hasNext()) {
                findValueIteratorAndKey();
            } else {
                valueIterator = Iterators.emptyIterator();
            }
        }

        void findValueIteratorAndKey() {
            final Map.Entry e = keyIterator.next();
            entry.key = e.getKey();
            Collection collection = (Collection) e.getValue();
            valueIterator = collection.iterator();
        }

        public boolean hasNext() {
            return valueIterator.hasNext() || keyIterator.hasNext();
        }

        /** note: current impl will always return the same Map.Entry object */
        public Map.Entry next() {
            if (!hasNext()) throw new NoSuchElementException();
            
            if (!valueIterator.hasNext())
                findValueIteratorAndKey();

            entry.value = valueIterator.next();
            
            return entry;
        }
    }
    


//     public synchronized void addIfContent(final String key, Object value) {
//         if (hasContent(value))
//             add(key, value);
//     }

//     public synchronized String addIfContent(final String desiredKey, Object value) {
//         if (hasContent(value))
//             return addProperty(desiredKey, value);
//         else
//             return null;
//     }
    

    /** No listeners will be updated until releaseChanges is called.  Multiple
     * overlapping holds are okay. */
    public synchronized void holdChanges() {
        if (DEBUG.RESOURCE && DEBUG.META) out("holding changes");
        mHoldingChanges = true;
    }

    /**
     * If any changes made since holdChanges, listeners will be notified.
     * It is crucial this is called at some point after holdChanges,
     * or listeners may never get updated. Tho ideally only one call to
     * this per holdChanges is made, Extra calls to this are okay 
     * -- it will at worst degrade update performance with rapid updates.
     */
    public synchronized void releaseChanges() {
        mHoldingChanges = false;
        if (mChanges > 0 && mTableModel != null) {
            if (DEBUG.RESOURCE && DEBUG.IMAGE) out("releasing changes " + mChanges);
            mTableModel.reload();
        }
        if (DEBUG.RESOURCE && DEBUG.IMAGE) out("released changes " + mChanges);
        mChanges = 0;
    }

    public synchronized java.util.Properties asProperties() {
        java.util.Properties props = new java.util.Properties();
        // todo: this not totally safe: values may not all be strings

        Util.printStackTrace("asProperties returning empty");
        
        //props.putAll(mData.asMap());
        //props.putAll(this);
        return props;
    }


    /* Do NOT synchronize getTableModel with the rest of the methods: this is because
     * when listeners get notified, one of the first things they're likely to do is ask
     * for the table model, but if two different threads are active on the other end,
     * we'll dead-lock.  (E.g., an ImageLoader thread and the AWT thread (via a user
     * LWSelection change) have both changed the current resource selection, and so the
     * meta-data pane is doing two synchronized updates (one from each thread) back to
     * back, but during one of the updates, it has to call back into the PropertyMap
     * here, which may already be locked on one of the threads, because it was from
     * there that a propertyMapChange was called.
     *
     * Eventually, the table model will probably want to move to the GUI and we can
     * avoid this special case.
     */
    
    public javax.swing.table.TableModel getTableModel() {
        // mTableModel is volatile, so as of Java 1.5, the double-checked locking
        // idiom used here should work.
        if (mTableModel == null) {
            synchronized (mTableModel_LOCK) {
                if (mTableModel == null)
                    mTableModel = new SortedMapModel();
            }
        }
        return mTableModel;
    }

    public synchronized void addListener(Listener l) {
        if (listeners == null)
            listeners = new java.util.ArrayList();
        listeners.add(l);
    }
    public synchronized void removeListener(Listener l) {
            
        // Note: we can DEADLOCK here in AWT obtaining the method entry lock if this
        // PropertyMap instance is already locked by an ImageLoader thread.
            
        if (listeners != null)
            listeners.remove(l);
    }

    private static int notifyCount = 0;
    
    public synchronized void notifyListeners()
    {
        if (listeners != null && listeners.size() > 0) {
            notifyCount++;
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifyListeners " + listeners.size() + " of " + super.toString());
            Iterator i = listeners.iterator();
            while (i.hasNext()) {
                Listener l = (Listener) i.next();
                if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifying: " + tufts.vue.gui.GUI.namex(l));
                
                // Note: we can DEADLOCK in MetaDataPane.propertyMapChanged (if it's
                // synchronized) waiting against AWT which is already locking
                // MetaDataPane, and waiting to lock this object (locked by an
                // ImageLoader thread) to enter removeListener above.
                
                l.tableBagChanged(this); 
            }
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("notifyListeners completed " + listeners.size());
        }
    }

    private void out(Object o) {

        Log.debug(String.format("@%x (#%d): %s",
                                System.identityHashCode(this),
                                notifyCount,
                                (o==null?"null":o.toString())));
    }

    public String toString() {
        return "PropertyMap@" + Integer.toHexString(hashCode()) + super.toString();
    }

    public int hashCode() {
        return mTableModel == null ? 0 : mTableModel.hashCode(); // doesn't change depending on contents
    }


    @Override
    public MetaMap clone() throws CloneNotSupportedException {
        final MetaMap clone = (MetaMap) super.clone();
        clone.listeners = null;
        clone.mChanges = 0;
        clone.mTableModel = null;
        return clone;
    }
    

    private static class Entry implements Comparable {
        final String key;
        final Object value;
        final boolean priority;
        
        Entry(Map.Entry e, boolean priority) {
            //this.key = tufts.Util.upperCaseWords((String) e.getKey());
            this.key = (String) e.getKey();
            this.value = e.getValue();
            this.priority = priority;
        }

        Entry(Map.Entry e) {
            this(e, false);
        }
        public int compareTo(Object o) {
            if (priority)
                return Short.MIN_VALUE;
            else if (((Entry)o).priority)
                return Short.MAX_VALUE;
            else
                return key.compareTo(((Entry)o).key);
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    // TODO: move this out to viewer
    
    private class SortedMapModel extends javax.swing.table.AbstractTableModel {

        private Entry[] mEntries;
        private int mEntryCount;
        
        SortedMapModel() {
            if (DEBUG.RESOURCE) out("new SortedMapModel");
            reload();
        }

        // make sure there is a sync on the HashMap before this is called
        private void reload() {
            //mEntries = new Entry[PropertyMap.this.size()];
            //mEntries = new Entry[mData.size()]; // TODO: need to track total size
            //mEntries = new Entry[1024]; // TODO: need to track total size
            //mEntries = new Entry[mTotalSize];
            mEntries = new Entry[MetaMap.this.size()];
            if (true||DEBUG.RESOURCE) out("SortedMapModel: reload " + mEntries.length + " items");

            //final Collection<Map.Entry<String,Object>> entries = mData.entries();
            //out("entries: " + Util.tags(entries) + "; size=" + entries.size());

            int ei = 0;
            //for (Map.Entry<String,?> e : entrySet()) {
            for (Map.Entry<String,?> e : entries()) {
                final String key = e.getKey().toLowerCase();

                //Log.info(String.format("sorting key %03d %-20s = %s", ei, Util.tags(key), Util.tags(e.getValue())));
                
                final boolean priority =
                       key.equals("title")
                    || key.equals("file")
                    || key.equals("url")
                    || key.equals("name")
                    ;

                mEntries[ei++] = new Entry(e, priority);
            }
            mEntryCount = ei - 1;

            //Arrays.sort(mEntries);
            
            /*
            mEntries = (Map.Entry[]) PropertyMap.this.entrySet().toArray(new Map.Entry[size()]);
            Arrays.sort(mEntries, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String k1 = (String) ((Map.Entry)o1).getKey();
                        String k2 = (String) ((Map.Entry)o2).getKey();
                        return k1.compareTo(k2);
                    }});
            */
            if (DEBUG.RESOURCE) {
                out("loaded " + mEntries.length + " entries");
                if (DEBUG.META)
                    out("model loaded " + Arrays.asList(mEntries));
            }
            if (DEBUG.RESOURCE || DEBUG.THREAD) out("fireTableDataChanged...");
            fireTableDataChanged();
            notifyListeners();
        }
        
        public int getRowCount() {
            return mEntryCount;
        }
        
        public Object getValueAt(int row, int col) {
            if (row > mEntryCount) {
                tufts.Util.printStackTrace("SortedMapModel has only " + mEntries.length + " entries, attempt to access row " + row);
                return "<empty>";
            }

            final Entry entry = mEntries[row];

            if (entry == null) {
                Log.warn(getClass().getName(), new Throwable("FYI: null entry at row " + row + "; col-request=" + col));
                return null;
            } else if (col == 0)
                return entry.key;
            else
                return entry.value;
        }

        public int getColumnCount() { return 2; }

        public String getColumnName(int col) {
            //return col == 0 ? "Field" : "Value";
            return null;
        }

        public Class getColumnClass(int colIndex) {
            return colIndex == 0 ? String.class : Object.class;
        }

        
        
    }
}