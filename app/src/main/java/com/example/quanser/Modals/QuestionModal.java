package com.example.quanser.Modals;

public class QuestionModal {
    String que;
    String ans;

    public QuestionModal() {
    }

    public QuestionModal(String que, String ans) {
        this.que = que;
        this.ans = ans;
    }

    public String getQue() {
        return que;
    }

    public void setQue(String que) {
        this.que = que;
    }

    public String getAns() {
        return ans;
    }

    public void setAns(String ans) {
        this.ans = ans;
    }
}
