package net.minetest.minetest;

import android.app.NativeActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.net.Uri;

import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MtNativeActivity extends NativeActivity {

    TextToSpeech t1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		m_MessagReturnCode = -1;
		m_MessageReturnValue = "";
		makeFullScreen();
		initSpeech();
	
		
	}
	private void initSpeech()
	{
		t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
         @Override
         public void onInit(int status) {
            if(status != TextToSpeech.ERROR) {
               t1.setLanguage(Locale.UK);
            }
         }
        });
	
	}
	
	public void speakText(String someText) {
	     t1.speak(someText, TextToSpeech.QUEUE_FLUSH, null);
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
		speakText("Welcome to eidy.  I am now copying assets.  Please stand by.");
		Intent intent = new Intent(this, MinetestAssetCopy.class);
		startActivity(intent);
	}

	public void showDialog(String acceptButton, String hint, String current,
			int editType) {
		speakText("Enter some text.");
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

		// We don't have to load libminetest.so ourselves,
		// but if we do, we get nicer logcat errors when
		// loading fails.
		System.loadLibrary("minetest");
	}

	private int m_MessagReturnCode;
	private String m_MessageReturnValue;
}
