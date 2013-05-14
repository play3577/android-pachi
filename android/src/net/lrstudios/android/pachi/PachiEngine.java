package net.lrstudios.android.pachi;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import lrstudios.games.ego.lib.ExternalGtpEngine;
import lrstudios.games.ego.lib.Utils;
import lrstudios.util.android.AndroidUtils;

import java.io.*;


public class PachiEngine extends ExternalGtpEngine {

    private static final String TAG = "PachiEngine";

    /**
     * Increment this counter each time you update the pachi executable
     * (this will force the app to extract it again - I didn't find a better way
     * to know if an android resource has been updated).
     */
    protected static final int EXE_VERSION = 1;

    private static final String ENGINE_NAME = "Pachi";
    private static final String ENGINE_VERSION = "10.00";

    protected static final String PREF_KEY_VERSION = "pachi_exe_version";


    public PachiEngine(Context context) {
        super(context);
        long totalRam = AndroidUtils.getTotalRam(context);
        int treeSize = 192;
        if (totalRam > 0) {
            // The amount of RAM used by pachi (adjustable with max_tree_size) should not
            // be too high compared to the total RAM available, because Android can kill a
            // process at any time if it uses too much memory.
            treeSize = (int) Math.round(totalRam / 1024.0 / 1024.0 * 0.5);
            Log.v(TAG, "Set max_tree_size = " + treeSize);
        }

        setProcessArgs(new String[]{"-t", "_600", "max_tree_size=" + treeSize});
    }


    @Override
    protected File getEngineFile() {
        File dir = _context.getDir("engines", Context.MODE_PRIVATE);
        File file = new File(dir, "pachi");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
        int version = prefs.getInt(PREF_KEY_VERSION, 0);
        if (version < EXE_VERSION) {
            if (file.exists())
                file.delete();
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file), 4096);
                inputStream = new BufferedInputStream(_context.getResources().openRawResource(R.raw.pachi), 4096);
                Utils.copyStream(inputStream, outputStream, 4096);

                try {
                    new ProcessBuilder("chmod", "744", file.getAbsolutePath()).start().waitFor();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(PREF_KEY_VERSION, EXE_VERSION);
                    editor.commit();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            catch (IOException e) { // TODO handle file extracting errors
                e.printStackTrace();
            }
            finally {
                Utils.closeObject(inputStream);
                Utils.closeObject(outputStream);
            }
        }

        return file;
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public String getVersion() {
        return ENGINE_VERSION;
    }
}