package de.maxhenkel.voicechatbot.support.issues;

import javax.annotation.Nullable;
import java.util.List;

public class Issues {

    public static final List<Issue> ISSUES;

    static {
        ISSUES = List.of(
                new NotConnectedIssue(),
                new MicNotWorkingIssue(),
                new ConfigIssue(),
                new NoPermissionsIssue(),
                new PortInUseIssue(),
                new VoiceChoppyIssue(),
                new OtherIssue(),
                new CrashIssue(),
                new QuestionIssue()
        );
    }

    @Nullable
    public static Issue byId(String id) {
        return ISSUES.stream().filter(issue -> issue.getId().equals(id)).findAny().orElse(null);
    }

}
