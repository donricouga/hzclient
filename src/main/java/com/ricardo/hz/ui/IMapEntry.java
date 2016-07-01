package com.ricardo.hz.ui;

import javafx.beans.property.SimpleStringProperty;

public class IMapEntry {

    private SimpleStringProperty key;
	private SimpleStringProperty value;

    public IMapEntry() {
    }

	public IMapEntry(String s1, String s2) {

        key = new SimpleStringProperty(s1);
        value = new SimpleStringProperty(s2);
    }

    public String getKey() {
	
        return key.get();
    }
    public void setKey(String s) {
	
        key.set(s);
    }
	
    public String getValue() {
	
        return value.get();
    }
    public void setValue(String s) {
	
        value.set(s);
    }
	
    @Override
    public String toString() {
	
        return (key.get() + ", by " + value.get());
    }
}