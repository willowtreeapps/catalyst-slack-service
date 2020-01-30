package org.catalyst.slackservice.util;

import play.i18n.Lang;

public class SlackLocale {
    public static final String DEFAULT_LOCALE = "en";

    private Lang _lang;
    private String _code;

    public SlackLocale() {
        initDefaults();
    }

    public SlackLocale(String code) {
        _lang = Lang.forCode(code);
        _code = code;
        if (_lang == null) {
            initDefaults();
        }
    }

    private void initDefaults() {
        _lang = Lang.forCode(DEFAULT_LOCALE);
        _code = DEFAULT_LOCALE;
    }

    public Lang getLang() {
        return _lang;
    }

    public String getCode() {
        return _code;
    }
}
