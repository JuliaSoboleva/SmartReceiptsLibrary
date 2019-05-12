package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderAboutFragment extends
        AbstractPreferenceHeaderFragment {

    public static final List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_about_about_key, R.string.pref_about_terms_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_about;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesAbout(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_about_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }
}
