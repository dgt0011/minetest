package net.minetest.minetest;

import android.app.NativeActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.content.res.AssetFileDescriptor;
import java.io.OutputStreamWriter;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MtNativeActivity extends NativeActivity {

    TextToSpeech t1 = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		m_MessagReturnCode = -1;
		m_MessageReturnValue = "";
		makeFullScreen();
		writeSystemInfo();
	}
	
	private void writeSystemInfo()
	{
		
		String currentKeyboard =  Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);

		String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/eidy/android.info";

		try {

			File myFile = new File(filename);
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append("Keyboard = \"" + currentKeyboard + "\"");
			myOutWriter.append("\n");
			myOutWriter.close();
			fOut.close();
			
        } catch (Exception e) {
              Log.e("error", "Could not create " + filename + " : ",e);
        } 

	}

	private String getConfigSetting(String settingName, String theFile)
	{
		try
		{
			String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/eidy/" + theFile;

			File file = new File(filename);

			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line = reader.readLine();
			while (line != null)
			{

				line = reader.readLine();
				if(line.startsWith(settingName + " =") || line.startsWith(settingName + " ="))
				{
					return line.split("=")[1].trim();
				}

			}
			reader.close();
		}
		catch (IOException e1)
		{
			Log.e("error","Error trying to retrieve language from minetest.conf");
			e1.printStackTrace();
		}
		finally
		{
			return "";
		}

	}
	
	private Locale GetLocale()
	{

		// Look for marker file (Paranoia)
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/eidy/.eidysw";
		File file = new File(filename);
		if (file.exists() )
		{
			return  new Locale("sw");
		}

		try
		{
			String language = getConfigSetting("language", "minetest.conf");
			if ( language != "" )
			{
				Log.i("info", "Language is set to <" + language + "> in minetest.conf");
				return new Locale(language);
			}
		}
		catch (Exception e)
		{
			Log.e("error","Error trying to retrieve language from minetest.conf");
			e.printStackTrace();
		}
		finally
		{
			Log.e("warning", "TTS - Default Locale Selected!");
			return Locale.US;
		}
	}
	
	
	private void initSpeech()
	{
		String speechEngine = getConfigSetting("speech_engine", "minetest.conf");

		// Look for marker file (Paranoia)
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/eidy/.filtetts";
		File file = new File(filename);
		if (file.exists() )
		{
			speechEngine = "edu.cmu.cs.speech.tts.flite";
		}


 		if (speechEngine != "")
		{
			try
			{
				t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener()
				{
					@Override
					public void onInit(int status)
					{
						if (status == TextToSpeech.SUCCESS)
						{
							int result = t1.setLanguage(GetLocale());
							if (result == TextToSpeech.LANG_MISSING_DATA ||
									result == TextToSpeech.LANG_NOT_SUPPORTED)
							{
								Log.e("error", "This Language is not supported");
							}

						} else
							Log.e("error", "Speech initialisation Failed!");
					}

				}, speechEngine);
			} catch (Exception e)
			{
				Log.e("error", "Speech Engine " + speechEngine + " Failed!");
			}
		}
		else
		{
			t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener()
			{
				@Override
				public void onInit(int status)
				{
					if (status == TextToSpeech.SUCCESS)
					{
						int result = t1.setLanguage(GetLocale());
						if (result == TextToSpeech.LANG_MISSING_DATA ||
								result == TextToSpeech.LANG_NOT_SUPPORTED)
						{
							Log.e("error", "This Language is not supported");
						}

					} else
						Log.e("error", "Default Initialisation Failed!");
				}

			});
		}

	}
	
	public void speakText(String someText) {
		if (t1 == null)
		{
			initSpeech();
		}
		if (t1 != null)
		{
	     t1.speak(someText, TextToSpeech.QUEUE_FLUSH, null);
		}
	}	

	 
	public void makeFullScreen() {
        if (Build.VERSION.SDK_INT >= 19) {
            this.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            makeFullScreen();
        }
    }
 
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void copyAssets() {
		
		Intent intent = new Intent(this, MinetestAssetCopy.class);
		startActivity(intent);
	}

	public void showDialog(String acceptButton, String hint, String current,
			int editType) {
	 
		Intent intent = new Intent(this, MinetestTextEntry.class);
		Bundle params = new Bundle();
		params.putString("acceptButton", acceptButton);
		params.putString("hint", hint);
		params.putString("current", current);
		params.putInt("editType", editType);
		intent.putExtras(params);
		startActivityForResult(intent, 101);
		m_MessageReturnValue = "";
		m_MessagReturnCode   = -1;
	}
	
	public void showBrowser(String url) {
		Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        startActivity(intent);
	}	

	public static native void putMessageBoxResult(String text);

	/* ugly code to workaround putMessageBoxResult not beeing found */
	public int getDialogState() {
		return m_MessagReturnCode;
	}

	public String getDialogValue() {
		m_MessagReturnCode = -1;
		return m_MessageReturnValue;
	}

	public float getDensity() {
		return getResources().getDisplayMetrics().density;
	}

	public int getDisplayWidth() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	public int getDisplayHeight() {
		return getResources().getDisplayMetrics().heightPixels;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (requestCode == 101) {
			if (resultCode == RESULT_OK) {
				String text = data.getStringExtra("text");
				m_MessagReturnCode = 0;
				m_MessageReturnValue = text;
			}
			else {
				m_MessagReturnCode = 1;
			}
		}
	}

	static {
		System.loadLibrary("openal");
		System.loadLibrary("ogg");
		System.loadLibrary("vorbis");
		System.loadLibrary("ssl");
		System.loadLibrary("crypto");
		System.loadLibrary("gmp");
		System.loadLibrary("iconv");
		System.loadLibrary("libintl");

		// We don't have to load libminetest.so ourselves,
		// but if we do, we get nicer logcat errors when
		// loading fails.
		System.loadLibrary("minetest");
	}

	private int m_MessagReturnCode;
	private String m_MessageReturnValue;
}
