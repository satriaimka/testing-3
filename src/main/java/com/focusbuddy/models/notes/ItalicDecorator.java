package com.focusbuddy.models.notes;

public class ItalicDecorator extends NoteDecorator {

    public ItalicDecorator(NoteComponent noteComponent) {
        super(noteComponent);
    }

    @Override
    public String getFormattedContent() {
        return "<i>" + noteComponent.getFormattedContent() + "</i>";
    }
}
