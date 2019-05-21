package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderDistanceFragment extends
        AbstractPreferenceHeaderFragment {

    public static final List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_distance_include_price_in_report_key,
            R.string.pref_distance_print_table_key, R.string.pref_distance_print_daily_key, R.string.pref_distance_as_price_key,
            R.string.pref_distance_category_name_key, R.string.pref_distance_category_code_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_distance;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesDistance(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_distance_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }
}
