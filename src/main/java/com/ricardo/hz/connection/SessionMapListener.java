package com.ricardo.hz.connection;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.MapClearedListener;
import com.hazelcast.map.listener.MapEvictedListener;
import com.ricardo.hz.ui.Main;
import com.ricardo.hz.ui.IMapEntry;

/**
 * Created by ricardo on 6/30/16.
 */
public class SessionMapListener implements EntryAddedListener<String, String>,
        EntryRemovedListener<String, String>,
        EntryUpdatedListener<String, String>,
        EntryEvictedListener<String, String>,
        MapEvictedListener,
        MapClearedListener {

    @Override
    public void entryAdded( EntryEvent<String, String> event ) {
        System.out.println( "Entry Added:" + event );
        Main.data.add(new IMapEntry(event.getKey(), event.getValue()));
    }

    @Override
    public void entryRemoved( EntryEvent<String, String> event ) {
        System.out.println( "Entry Removed:" + event );
    }

    @Override
    public void entryUpdated( EntryEvent<String, String> event ) {
        System.out.println( "Entry Updated:" + event );
    }

    @Override
    public void entryEvicted( EntryEvent<String, String> event ) {
        Main.data.remove(new IMapEntry(event.getKey(), event.getValue()));
    }

    @Override
    public void mapEvicted( MapEvent event ) {
        Main.data.clear();
    }

    @Override
    public void mapCleared( MapEvent event ) {
        Main.data.clear();
    }



}
