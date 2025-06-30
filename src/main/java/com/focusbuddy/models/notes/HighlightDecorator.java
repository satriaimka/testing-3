package com.focusbuddy.models.notes;

public class HighlightDecorator extends NoteDecorator {
    private String color;

    public HighlightDecorator(NoteComponent noteComponent, String color) {
        super(noteComponent);
        this.color = color;
    }

    @Override
    public String getFormattedContent() {
        return "<span style='background-color: " + color + "'>" +
                noteComponent.getFormattedContent() + "</span>";
    }
}
