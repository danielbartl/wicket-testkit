package dev.jbaby.wicket.testkit.fixtures;

import org.apache.wicket.markup.html.link.Link;

/// A link that does nothing when clicked.
public class NoOpLink extends Link<Void> {

    public NoOpLink(String id) {
        super(id);
    }

    @Override
    public void onClick() {
    }
}
