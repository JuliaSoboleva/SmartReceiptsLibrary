package co.smartreceipts.android.settings.widget;

import java.util.Arrays;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderReceiptsFragment extends AbstractPreferenceHeaderFragment {

    public static List<Integer> EXTRA_SETTINGS_KEYS = Arrays.asList(R.string.pref_receipt_reimbursable_only_key,
            R.string.pref_receipt_reimbursable_default_key, R.string.pref_receipt_default_to_report_start_date_key,
            R.string.pref_receipt_show_id_key, R.string.pref_receipt_minimum_receipts_price_key,
            R.string.pref_receipt_use_payment_methods_key, R.string.pref_receipt_payment_methods_key);

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_receipts;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesReceipts(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_receipt_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return EXTRA_SETTINGS_KEYS;
    }
}
