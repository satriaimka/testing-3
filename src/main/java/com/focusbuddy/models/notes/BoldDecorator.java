package com.focusbuddy.models.notes;

public class BoldDecorator extends NoteDecorator {

    public BoldDecorator(NoteComponent noteComponent) {
        super(noteComponent);
    }

    @Override
    public String getFormattedContent() {
        return "<b>" + noteComponent.getFormattedContent() + "</b>";
    }
}
