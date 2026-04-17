package com.webdev.greenify.greenaction.constant;

import java.util.List;

public final class ActionTypeConstants {

    private ActionTypeConstants() {}

    public static final List<String> REPORTER_ACTION_NAMES = List.of(
            "Báo cáo môi trường",
            "Report illegal dumping/polluted spots");

    public static final List<String> REVIEWER_ACTION_NAMES = List.of(
            "Duyệt bài hợp lệ với tư cách CTV",
            "Review posts as a Contributor");
}
