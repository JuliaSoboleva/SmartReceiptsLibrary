package co.smartreceipts.android.settings.widget;

import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderUserFragment
        extends AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_user;
    }

    @Override
    public void configurePreferences() {
        /* no-op */
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_user_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return Collections.emptyList();
    }
}
