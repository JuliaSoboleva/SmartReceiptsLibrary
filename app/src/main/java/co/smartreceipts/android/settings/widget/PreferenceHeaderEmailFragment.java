package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderEmailFragment extends
        AbstractPreferenceHeaderFragment {

    public static final List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_email_default_email_subject_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_email;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesEmail(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_email_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }
}
