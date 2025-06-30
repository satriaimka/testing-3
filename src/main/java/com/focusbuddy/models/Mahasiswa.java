package com.focusbuddy.models;

public class Mahasiswa extends User {
    private String studentId;
    private String major;
    
    public Mahasiswa() {
        super();
    }
    
    public Mahasiswa(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }
    
    @Override
    public String getUserType() {
        return "MAHASISWA";
    }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
}
