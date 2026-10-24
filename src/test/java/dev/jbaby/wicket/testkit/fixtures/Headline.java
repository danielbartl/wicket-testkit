package dev.jbaby.wicket.testkit.fixtures;

import java.io.Serializable;
import org.apache.wicket.markup.html.basic.Label;

/// A label subclass, to show that locating by type includes subtypes.
public class Headline extends Label {

    public Headline(String id, Serializable text) {
        super(id, text);
    }
}
