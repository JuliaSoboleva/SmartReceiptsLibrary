package co.smartreceipts.android.settings.widget;

import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderLayoutFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_layout;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesLayoutCustomizations(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_layout_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return Collections.emptyList();
    }
}
