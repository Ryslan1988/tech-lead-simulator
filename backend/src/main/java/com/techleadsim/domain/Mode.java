package com.techleadsim.domain;

public enum Mode {
    CLASSIC(10),
    HARDCORE(20);

    private final int questionCount;
    Mode(int questionCount) { this.questionCount = questionCount; }
    public int questionCount() { return questionCount; }
}
