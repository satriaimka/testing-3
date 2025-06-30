package com.focusbuddy.models.notes;

public abstract class NoteDecorator implements NoteComponent {
    protected NoteComponent noteComponent;

    public NoteDecorator(NoteComponent noteComponent) {
        this.noteComponent = noteComponent;
    }

    @Override
    public String getContent() {
        return noteComponent.getContent();
    }

    @Override
    public String getFormattedContent() {
        return noteComponent.getFormattedContent();
    }

    @Override
    public void setContent(String content) {
        noteComponent.setContent(content);
    }
}
