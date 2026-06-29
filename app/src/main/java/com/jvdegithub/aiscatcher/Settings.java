/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022-2023 jvde.github@gmail.com.
 *     Copyright (C)  2025 MastChain – trimmed to RTL-SDR + TCP only
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jvdegithub.aiscatcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

import com.jvdegithub.aiscatcher.tools.InputFilterIP;
import com.jvdegithub.aiscatcher.tools.InputFilterMinMax;

import java.util.Objects;

public class Settings extends AppCompatActivity {

    static boolean is_enabled = true;
    public static void setEnabled(boolean e) {
        is_enabled = e;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    static void setDefault(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferences.edit().putString("sSHARINGKEY", "").commit();
        preferences.edit().putBoolean("sSHARING", false).commit();
        preferences.edit().putBoolean("sAUTOSTART", false).commit();
        preferences.edit().putBoolean("sKEEPSCREENON", false).commit();

        preferences.edit().putString("oCGF_WIDE", "Default").commit();
        preferences.edit().putString("oMODEL_TYPE", "Default").commit();
        preferences.edit().putBoolean("oFP_DS", false).commit();

        preferences.edit().putString("rRATE", "288K").commit();
        preferences.edit().putBoolean("rRTLAGC", false).commit();
        preferences.edit().putString("rTUNER", "Auto").commit();
        preferences.edit().putBoolean("rBIASTEE", false).commit();
        preferences.edit().putString("rFREQOFFSET", "0").commit();
        preferences.edit().putBoolean("rBANDWIDTH", false).commit();

        preferences.edit().putString("tPROTOCOL", "RTLTCP").commit();
        preferences.edit().putString("tRATE", "240K").commit();
        preferences.edit().putString("tTUNER", "Auto").commit();
        preferences.edit().putString("tHOST", "localhost").commit();
        preferences.edit().putString("tPORT", "12345").commit();

        preferences.edit().putBoolean("u1SWITCH", true).commit();
        preferences.edit().putString("u1HOST", "127.0.0.1").commit();
        preferences.edit().putString("u1PORT", "10110").commit();
        preferences.edit().putBoolean("u1JSON", false).commit();

        preferences.edit().putBoolean("s1SWITCH", false).commit();
        preferences.edit().putString("s1PORT", "5012").commit();

        // MastChain / HTTP output defaults
        preferences.edit().putBoolean("hENABLE", false).commit();
        if (!preferences.contains("hURL"))
            preferences.edit().putString("hURL", "https://api.mastchain.io/api/upload").commit();
        if (!preferences.contains("hUSERNAME"))
            preferences.edit().putString("hUSERNAME", "wallieminer@protonmail.com").commit();
        if (!preferences.contains("hPASSWORD"))
            preferences.edit().putString("hPASSWORD", "6mlgmE9UhB5iAa4mOyFdCaZmiWG5t39K5yOC0/H92Hk=").commit();
        if (!preferences.contains("hSTATIONID"))
            preferences.edit().putString("hSTATIONID", "WallieM3").commit();
        if (!preferences.contains("hINTERVAL"))
            preferences.edit().putString("hINTERVAL", "5").commit();
        if (!preferences.contains("hPROTOCOL"))
            preferences.edit().putString("hPROTOCOL", "NMEA").commit();
    }

    static boolean setDefaultOnFirst(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean pref_set = preferences.getBoolean("pref_set", false);
        if (!pref_set) setDefault(context);
        preferences.edit().putBoolean("pref_set", true).commit();
        return !pref_set;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            ((EditTextPreference) getPreferenceManager().findPreference("tPORT")).setOnBindEditTextListener(validatePort);
            ((EditTextPreference) getPreferenceManager().findPreference("tHOST")).setOnBindEditTextListener(validateIP);
            ((EditTextPreference) getPreferenceManager().findPreference("u1HOST")).setOnBindEditTextListener(validateIP);
            ((EditTextPreference) getPreferenceManager().findPreference("u1PORT")).setOnBindEditTextListener(validatePort);
            ((EditTextPreference) getPreferenceManager().findPreference("s1PORT")).setOnBindEditTextListener(validatePort);
            ((EditTextPreference) getPreferenceManager().findPreference("rFREQOFFSET")).setOnBindEditTextListener(validatePPM);

            // HTTP output preference input validators
            EditTextPreference hIntervalPref = findPreference("hINTERVAL");
            if (hIntervalPref != null) {
                hIntervalPref.setOnBindEditTextListener(validatePort); // reuses 0-65536 range, good enough
            }
            EditTextPreference hUrlPref = findPreference("hURL");
            if (hUrlPref != null) {
                hUrlPref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                    editText.selectAll();
                });
            }

            setSummaries();
            updateHttpOutputSummaries();
        }

        static public int getModelType(Context context)
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.getInt("oMODEL_TYPE", 0);
        }

        private void setSummaries() {
            setSummaryText(new String[]{"tPORT","tHOST","u1HOST","u1PORT", "s1PORT", "rFREQOFFSET", "sSHARINGKEY"});
            setSummaryList(new String[]{"rTUNER","rRATE","tRATE","tPROTOCOL","tTUNER","oMODEL_TYPE","oCGF_WIDE"});
        }

        /**
         * Update preference summaries for the MastChain / HTTP Output section.
         */
        private void updateHttpOutputSummaries() {
            String[] textKeys = {"hURL", "hUSERNAME", "hSTATIONID", "hINTERVAL"};
            for (String k : textKeys) {
                EditTextPreference pref = findPreference(k);
                if (pref != null) {
                    String val = pref.getText();
                    if (val != null && !val.isEmpty()) {
                        pref.setSummary(val);
                    } else {
                        switch (k) {
                            case "hURL":        pref.setSummary("https://api.mastchain.io/api/upload"); break;
                            case "hUSERNAME":   pref.setSummary("wallieminer@protonmail.com"); break;
                            case "hSTATIONID":  pref.setSummary("WallieM3"); break;
                            case "hINTERVAL":   pref.setSummary("5"); break;
                        }
                    }
                }
            }
            // Password – show masked summary
            EditTextPreference passPref = findPreference("hPASSWORD");
            if (passPref != null) {
                String val = passPref.getText();
                if (val != null && !val.isEmpty()) {
                    passPref.setSummary("********");
                } else {
                    passPref.setSummary("Not set");
                }
            }
            // Protocol dropdown
            ListPreference protoPref = findPreference("hPROTOCOL");
            if (protoPref != null) {
                protoPref.setSummary(protoPref.getEntry());
            }
        }

        private void setSummaryText(String[] settings) {

            for (String s : settings) {
                EditTextPreference e = findPreference(s);
                e.setSummary(e.getText());
            }
        }

        private void setSummaryList(String[] settings) {
            for (String s : settings) {
                ListPreference e = findPreference(s);
                e.setSummary(e.getEntry());
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
            if(!is_enabled)
                Toast.makeText(getContext(), "Settings disabled during run", Toast.LENGTH_SHORT).show();

            PreferenceScreen preferenceScreen = getPreferenceScreen();
            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                Preference preference = preferenceScreen.getPreference(i);
                preference.setEnabled(is_enabled);
            }

            final Preference tPROTOCOL = findPreference("tPROTOCOL");

            tPROTOCOL.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ("TXT".equals(newValue.toString())) {
                        findPreference("tRATE").setEnabled(false);
                        findPreference("tTUNER").setEnabled(false);
                    } else {
                        findPreference("tRATE").setEnabled(is_enabled & true);
                        findPreference("tTUNER").setEnabled(is_enabled & true);
                    }
                    return true;
                }
            });

            String currentProtocolValue = tPROTOCOL.getSharedPreferences().getString("tPROTOCOL", "");
            if ("TXT".equals(currentProtocolValue)) {
                findPreference("tRATE").setEnabled(false);
                findPreference("tTUNER").setEnabled(false);
            } else {
                findPreference("tRATE").setEnabled(is_enabled & true);
                findPreference("tTUNER").setEnabled(is_enabled & true);            }
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);

            Preference tPROTOCOL = findPreference("tPROTOCOL");
            if (tPROTOCOL != null) {
                tPROTOCOL.setOnPreferenceChangeListener(null); // Remove the listener
            }
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummaries();
            updateHttpOutputSummaries();
        }

        EditTextPreference.OnBindEditTextListener validatePPM = editText -> {
            editText.selectAll();
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(-150,150)});
        };

        EditTextPreference.OnBindEditTextListener validatePort = editText -> {
            editText.selectAll();
            editText.setInputType(InputType.TYPE_CLASS_NUMBER );
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(0,65536)});
        };

        EditTextPreference.OnBindEditTextListener validateInteger = editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.selectAll();
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        };

        EditTextPreference.OnBindEditTextListener validateIP = editText -> {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            editText.selectAll();
            editText.setFilters(new InputFilter[]{new InputFilterIP()});
        };
    }

    static public boolean Apply(Context context) {

        if (!SetDevice(new String[]{"rRATE", "rTUNER", "rFREQOFFSET", "tRATE", "tPROTOCOL","tTUNER", "tHOST", "tPORT"}, context))
            return false;
        if (!SetDeviceBoolean(new String[]{"rRTLAGC", "rBIASTEE"}, "ON", "OFF", context))
            return false;

        if(!SetRTLbandwidth(context)) return false;

        if (!SetUDPoutput("u1", context)) return false;

        if (!SetTCPListener(context)) return false;

        if(!SetSharing(context))  return false;

        // MastChain / HTTP output – start or stop based on preferences
        SetHttpOutput(context);

        return true;
    }

    static public int getModelType(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String set = preferences.getString("oMODEL_TYPE", "Default");

        if(set.equals("Default")) return 0;
        return 1;
    }

    static public int getCGFSetting(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String set = preferences.getString("oCGF_WIDE", "Default");
        if(set.equals("Default")) return 1;
        if(set.equals("Narrow")) return 0;
        return 1;
    }

    static public boolean getAutoStart(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("sAUTOSTART",false);
    }

    static public boolean getKeepScreenOn(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("sKEEPSCREENON",false);
    }

    static public boolean getFixedPointDownsampling(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("oFP_DS", false);
    }

    static private boolean SetDevice(String[] settings, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String s : settings) {
            String p = preferences.getString(s, "");
            if (Objects.equals(p, "")) return false;
            if (AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), p) != 0)
                return false;
        }
        return true;
    }

    static private boolean SetRTLbandwidth(Context context)
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean b = preferences.getBoolean("rBANDWIDTH", false);
        if(b) {
            if (AisCatcherJava.applySetting("r", "BW", "192000") != 0)
                return false;
        }
        else {
            if (AisCatcherJava.applySetting("r", "BW", "0") != 0)
                return false;
        }
        return true;
    }

    static private boolean SetDeviceBoolean(String[] settings, String st, String sf, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String s : settings) {
            boolean b = preferences.getBoolean(s, true);
            if (AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), b ? st : sf) != 0)
                return false;
        }
        return true;
    }

    static private boolean SetUDPoutput(String s, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean b = preferences.getBoolean(s + "SWITCH", true);
        if (b) {
            String host = preferences.getString(s + "HOST", "");
            String port = preferences.getString(s + "PORT", "");
            boolean JSON = preferences.getBoolean(s + "JSON", false);

            return AisCatcherJava.createUDP(host, port, JSON) == 0;

        }
        return true;
    }

    static private boolean SetTCPListener(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean b = preferences.getBoolean("s1SWITCH", true);
        if (b) {
            String port = preferences.getString("s1PORT", "");

            return AisCatcherJava.createTCPlistener( port) == 0;

        }
        return true;
    }

    static private boolean SetSharing(Context context) {
        String defaultKey = "a6392e08-c57e-4e7a-a4fb-d73bfc7619ae";

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean b = preferences.getBoolean("sSHARING", false);
        if (b) {
            String key = preferences.getString("sSHARINGKEY", defaultKey);
            if (key.equals("")) key = defaultKey;
            return AisCatcherJava.createSharing(b, key) == 0;

        }
        else
            AisCatcherJava.createSharing(b, defaultKey);
        return true;
    }

    /**
     * Apply HTTP output settings – start or stop the HttpOutputManager
     * based on current preferences. Called from {@link #Apply(Context)}.
     */
    static private void SetHttpOutput(Context context) {
        boolean enabled = HttpOutputManager.isEnabled(context);
        if (enabled) {
            HttpOutputManager.getInstance().start(context);
        } else {
            HttpOutputManager.getInstance().stop();
        }
    }

    /**
     * Stop HTTP output (called when the receiver stops).
     */
    static public void StopHttpOutput() {
        HttpOutputManager.getInstance().stop();
    }

    /**
     * Enqueue a NMEA sentence for HTTP output.
     * Thread-safe; called from the native NMEA callback.
     */
    static public void feedHttpOutput(String nmea) {
        HttpOutputManager.getInstance().enqueueNmea(nmea);
    }
}