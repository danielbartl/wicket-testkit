package dev.jbaby.wicket.testkit.fixtures;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/// A panel to start on its own: Label `count` and AjaxLink `increment`, which adds one to it.
public class CounterPanel extends Panel {

    private int count;

    public CounterPanel(String id) {
        super(id);
        IModel<Integer> countModel = () -> count;
        var label = new Label("count", countModel);
        add(label.setOutputMarkupId(true));
        add(new AjaxLink<Void>("increment") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                count++;
                target.add(label);
            }
        });
    }
}
