package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderGeneralFragment extends
        AbstractPreferenceHeaderFragment {

    public static final List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_general_default_date_separator_key,
            R.string.pref_general_date_format_key, R.string.pref_general_track_cost_center_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_general;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesGeneral(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_general_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }

}
