package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderReportOutputFragment
        extends AbstractPreferenceHeaderFragment {

    public static List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_output_custom_csv_key,
            R.string.pref_output_custom_pdf_key, R.string.pref_output_receipts_landscape_key,
            R.string.pref_output_pdf_page_size_key, R.string.pref_output_preferred_language_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_output;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesOutput(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_output_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }
}
