package com.ict.lms.service;

/** Builds the starting password for a registered student: first word of the email name + "@22". */
public final class DefaultPassword {

    private DefaultPassword() {}

    /** yasiru.s@gmail.com -> yasiru@22 ; falls back to the first word of the name. */
    public static String from(String email, String fullName) {
        String w = firstWord(email == null ? "" : email.split("@")[0]);
        if (w.isEmpty()) w = firstWord(fullName == null ? "" : fullName.trim().split("\\s+")[0]);
        if (w.isEmpty()) w = "student";
        return w + "@22";
    }

    private static String firstWord(String s) {
        String[] parts = s.toLowerCase().split("[^a-z]+");
        for (String p : parts) if (!p.isEmpty()) return p;
        return "";
    }
}
