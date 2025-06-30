package com.focusbuddy.models.notes;

public class BasicNote implements NoteComponent {
    private String content;
    
    public BasicNote(String content) {
        this.content = content;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public String getFormattedContent() {
        return content;
    }
    
    @Override
    public void setContent(String content) {
        this.content = content;
    }
}
