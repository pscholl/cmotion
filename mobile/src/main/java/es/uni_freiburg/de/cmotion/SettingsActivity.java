package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;

import java.io.File;

import es.uni_freiburg.de.cmotion.shared_ui.SettingsConsts;
import es.uni_freiburg.de.cmotion.ui.DirectoryChooserDialog;
import es.uni_freiburg.de.cmotion.ui.OnTextChangedListener;
import es.uni_freiburg.de.cmotion.ui.TextEditDialog;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference, Object defaultValue) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.

        if (preference instanceof SwitchPreference)
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), (Boolean) defaultValue));
        else {
            String value = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), (String) defaultValue);
            preference.setSummary(value);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
        }
    }

    /**
     * TODO this codes needs some serious refactoring
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        addPreferencesFromResource(R.xml.pref_general);
        bindPreferenceSummaryToValue(findPreference(SettingsConsts.PREF_KEY_OUTPUTDIR), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        bindPreferenceSummaryToValue(findPreference(SettingsConsts.PREF_KEY_RSYNC_OUTPUT), Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
        bindPreferenceSummaryToValue(findPreference(SettingsConsts.PREF_KEY_AUTOPLAY), true);
        bindPreferenceSummaryToValue(findPreference(SettingsConsts.PREF_KEY_RSYNC), false);
//        bindPreferenceSummaryToValue(findPreference(PREF_KEY_FILENAME), de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getDefaultFileName());

        findPreference(SettingsConsts.PREF_KEY_OUTPUTDIR).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                chooseDirectory(preference);
                return false;
            }
        });

        findPreference(SettingsConsts.PREF_KEY_RSYNC).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                findPreference(SettingsConsts.PREF_KEY_RSYNC_OUTPUT).setEnabled(((SwitchPreference) preference).isChecked());
                return false;
            }
        });


        findPreference(SettingsConsts.PREF_KEY_RSYNC_OUTPUT).setEnabled(((SwitchPreference) findPreference(SettingsConsts.PREF_KEY_RSYNC)).isChecked());
        findPreference(SettingsConsts.PREF_KEY_RSYNC_OUTPUT).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {

                String currentOutput = PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                        .getString(SettingsConsts.PREF_KEY_RSYNC_OUTPUT,
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());

                new TextEditDialog(preference.getContext())
                        .build(preference.getContext(), "Enter Destination", currentOutput, null, new OnTextChangedListener() {
                            @Override
                            public void onTextChanged(Object tag, String newText) {
                                PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                                        .edit()
                                        .putString(SettingsConsts.PREF_KEY_RSYNC_OUTPUT, newText)
                                        .commit();
                                preference.setSummary(newText);
                            }
                        })
                        .show();
                return false;
            }
        });


        findPreference(SettingsConsts.PREF_KEY_DELETE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                shodDeleteAllRecordingsDialog(preference);
                return false;
            }
        });

//        findPreference(PREF_KEY_FILENAME).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//            @Override
//            public boolean onPreferenceClick(final Preference preference) {
//
//                DigitEditDialog.build(SettingsActivity.this, "Enter Filename", PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(PREF_KEY_FILENAME, de.uni_freiburg.es.sensorrecordingtool.RecorderCommands.getDefaultFileName()), null, null).show();
//                return false;
//            }
//        });
    }

    private void shodDeleteAllRecordingsDialog(final Preference preference) {
        AlertDialog dialog = new AlertDialog.Builder(preference.getContext())
                .setTitle(R.string.delete_recordings_head)
                .setMessage(R.string.delete_recordings_text)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRecordings(preference.getContext());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void deleteRecordings(Context context) {
        File outputFolder = new File(getOutputPath(context));
        for(File file : outputFolder.listFiles()) {
            if(file.isFile() && file.getName().endsWith(".mkv"))
                file.delete();
        }
    }

    private void chooseDirectory(final Preference preference) {
        DirectoryChooserDialog directoryChooserDialog =
                new DirectoryChooserDialog(SettingsActivity.this,
                        new DirectoryChooserDialog.ChosenDirectoryListener() {
                            @Override
                            public void onChosenDir(String chosenDir) {
                                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString(SettingsConsts.PREF_KEY_OUTPUTDIR, chosenDir).commit();
                                preference.setSummary(chosenDir);
                            }
                        });
        directoryChooserDialog.setNewFolderEnabled(true);
        directoryChooserDialog.chooseDirectory(PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(SettingsConsts.PREF_KEY_OUTPUTDIR, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()));

    }

    private String getOutputPath(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("output_directory",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName);
    }

    public static void open(Activity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }


}
