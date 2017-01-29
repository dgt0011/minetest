package net.minetest.minetest;

import android.app.NativeActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
              Log.e("eidy", "Could not create " + filename + " : ",e);
        } 

	}


	private String getConfigSetting(String settingName, String theFile)
	{

		String retvalue = "";

		BufferedReader reader = null;
		FileReader fileReader = null;

		try
		{
			String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/eidy/" + theFile;

			File file = new File(filename);

			fileReader = new FileReader(file);
			reader = new BufferedReader(fileReader);
			String line = "";
			while ((line = reader.readLine()) != null)
			{

				if(line.startsWith(settingName + " =") || line.startsWith(settingName + "="))
				{
					retvalue = line.split("=")[1].trim();
					break;
				}
			}

		}
		catch (IOException e1)
		{
			Log.e("eidy","Error trying to retrieve language from " + theFile);
			e1.printStackTrace();
		}
		finally
		{
			try
			{
				if (reader != null) reader.close();
				if (fileReader != null) fileReader.close();
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}

		Log.i("eidy", "Read config value <" + retvalue + "> for " + settingName + " in " + theFile);

		return retvalue;
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


	static final int REQUEST_IMAGE_CAPTURE = 1;

	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			 startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			 }
	}

	public void showBrowser(String url) {

		if (url == "about:takephotowiththecameraapp")
		{
			dispatchTakePictureIntent();
		}
		else if (url.endsWith(".mp4") && !url.contains(":"))
		{
			// Default to camera folder
			if (!url.contains("/"))
			{
				url = "/DCIM/Camera/" + url;
			}

			String filename = Environment.getExternalStorageDirectory().getAbsolutePath()
					+ url;
			File filecheck = new File(filename);
			if (filecheck.exists())
			{
				Intent intent = new Intent(MtNativeActivity.this, VideoPlayerActivity.class);
				intent.putExtra("videofilename", filename);
				startActivity(intent);
			}
			else
			{
				Log.e("eidy", "Cannot find video file " + filename);
			}
		}
		else
		{
			/*Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
			startActivity(intent);*/
			Intent intent = new Intent(MtNativeActivity.this, WebViewActivity.class);
			intent.putExtra("url", url);
			startActivity(intent);
		}
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
		//System.loadLibrary("intl"); //-- Doesn't seem to work...

		// We don't have to load libminetest.so ourselves,
		// but if we do, we get nicer logcat errors when
		// loading fails.
		System.loadLibrary("minetest");
	}

	private int m_MessagReturnCode;
	private String m_MessageReturnValue;
}
