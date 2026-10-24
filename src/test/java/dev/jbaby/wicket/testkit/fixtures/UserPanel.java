package dev.jbaby.wicket.testkit.fixtures;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/// Shows a user's name.
public class UserPanel extends Panel {

    public UserPanel(String id, IModel<String> name) {
        super(id, name);
        add(new Label("name", name));
    }
}
