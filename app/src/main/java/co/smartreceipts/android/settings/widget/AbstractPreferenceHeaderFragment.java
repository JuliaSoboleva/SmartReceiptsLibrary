package co.smartreceipts.android.settings.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.annotation.XmlRes;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import co.smartreceipts.android.R;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.utils.log.Logger;
import io.reactivex.disposables.CompositeDisposable;

public abstract class AbstractPreferenceHeaderFragment extends android.preference.PreferenceFragment implements UniversalPreferences {

    protected SettingsActivity mSettingsActivity;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    protected List<Preference> extraPreferences;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SettingsActivity) {
            mSettingsActivity = (SettingsActivity) activity;
        } else {
            throw new RuntimeException("AbstractPreferenceHeaderFragment requires a SettingsActivity");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        compositeDisposable.add(mSettingsActivity.getFragmentExtraSettingsVisibilityStream()
                .doOnNext(extraSettingsMustBeVisible -> {
                    Logger.debug(this, "ForYooz subscribed, got value extraSettingsMustBeVisible = " + extraSettingsMustBeVisible);
                    Logger.debug(this, "ForYooz isExtraSettingsVisible = " + isExtraSettingsVisible);
                    if (extraSettingsMustBeVisible && !isExtraSettingsVisible) {
                        showHiddenSettingsForYooz();
                    } else if (!extraSettingsMustBeVisible && isExtraSettingsVisible) {
                        hideSomeSettingsForYooz();
                    }
                })
                .subscribe());
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @XmlRes
    public abstract int getPreferencesResourceId();

    public abstract void configurePreferences();

    protected abstract int getHeaderKey();

    protected abstract List<Integer> getExtraSettingsKeys();

    private void initExtraPreferences() {
            extraPreferences = new ArrayList<>();

            for (Integer extraSettingsKey : getExtraSettingsKeys()) {
                final Preference preference = findPreference(extraSettingsKey);
                extraPreferences.add(preference);
            }
    }

    private void hideSomeSettingsForYooz() {
        isExtraSettingsVisible = false;

        final PreferenceCategory preferenceCategory = getPreferenceCategory();

        for (Preference extraPreference : extraPreferences) {
            preferenceCategory.removePreference(extraPreference);
        }
    }

    private void showHiddenSettingsForYooz() {
        isExtraSettingsVisible = true;

        final PreferenceCategory preferenceCategory = getPreferenceCategory();

        for (Preference pref : extraPreferences) {
            preferenceCategory.addPreference(pref);
        }
    }

    private boolean isExtraSettingsVisible = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsActivity.setFragmentHeaderIsShowing(true);
        setHasOptionsMenu(true); // Required to simulate up navigation
        getPreferenceManager().setSharedPreferencesName(UserPreferenceManager.PREFERENCES_FILE_NAME);

        addPreferencesFromResource(getPreferencesResourceId());
        configurePreferences();

        initExtraPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getArguments().getString(getString(R.string.pref_header_key)));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSettingsActivity.setFragmentHeaderIsShowing(false);
        extraPreferences = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSettingsActivity = null; //Garbage Collection
    }

    @Override
    public Preference findPreference(int stringId) {
        return findPreference(getString(stringId));
    }

    protected PreferenceCategory getPreferenceCategory() {
        return (PreferenceCategory) findPreference(getHeaderKey());
    }


}
