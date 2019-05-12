package co.smartreceipts.android.settings.widget;

import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderHelpFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_help;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesHelp(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_help_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return Collections.emptyList();
    }
}
