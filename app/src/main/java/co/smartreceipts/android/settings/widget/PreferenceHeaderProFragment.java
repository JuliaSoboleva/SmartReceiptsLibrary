package co.smartreceipts.android.settings.widget;

import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderProFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_pro;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePlusPreferences(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_pro_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return Collections.emptyList();
    }
}
