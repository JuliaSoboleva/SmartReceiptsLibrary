package co.smartreceipts.android.settings.widget;

import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.R;

public class PreferenceHeaderCameraFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_camera;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesCamera(this);
    }

    @Override
    protected int getHeaderKey() {
        return R.string.pref_camera_header_key;
    }

    @Override
    protected List<Integer> getExtraSettingsKeys() {
        return Collections.emptyList();
    }
}
