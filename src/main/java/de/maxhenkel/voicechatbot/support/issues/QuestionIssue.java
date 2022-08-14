package de.maxhenkel.voicechatbot.support.issues;

import java.util.List;

public class QuestionIssue extends BaseIssue {

    public QuestionIssue() {
        super("issue_general_question", "General question");
    }

    @Override
    protected List<String> getQuestionsInternal() {
        List<String> questions = super.getQuestionsInternal();
        questions.set(0, "What is your question?");
        return questions;
    }

}
