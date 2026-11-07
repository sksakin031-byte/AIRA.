package com.aira.agentwrapper;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.chaquo.python.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final int INK = Color.rgb(26, 43, 47);
    private static final int MUTED = Color.rgb(94, 112, 115);
    private static final int GREEN = Color.rgb(20, 108, 90);
    private static final int PAGE = Color.rgb(242, 247, 245);

    private LinearLayout messageList;
    private ScrollView scrollView;
    private TextView statusView;
    private EditText messageInput;
    private Button sendButton;
    private PyObject bridge;
    private boolean ready = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(15, 61, 54));
        getWindow().setNavigationBarColor(Color.rgb(15, 61, 54));
        buildInterface();
        addMessage("AIRA", "স্বাগতম। এই অ্যাপটি repository-র Python agent ব্যবহার করে। Chat চালাতে Settings-এ নিজের Gemini API key যোগ করুন।", false);
        addMessage("নিরাপত্তা", "অনুমোদন-চাওয়া file action স্বয়ংক্রিয়ভাবে প্রত্যাখ্যান করা হয়; /write ও /overwrite এখানে বন্ধ।", false);
        initializePython();
    }

    private void buildInterface() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(12));
        page.setBackgroundColor(PAGE);
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("AIRA");
        title.setTextColor(INK);
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settingsButton = new Button(this);
        settingsButton.setText("API key");
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setAllCaps(false);
        settingsButton.setBackground(rounded(GREEN, dp(18)));
        settingsButton.setOnClickListener(v -> openApiSettings());
        header.addView(settingsButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(46)));
        page.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusView = new TextView(this);
        statusView.setText("Python runtime চালু হচ্ছে…");
        statusView.setTextColor(MUTED);
        statusView.setTextSize(13);
        statusView.setPadding(0, dp(2), 0, dp(10));
        page.addView(statusView);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        messageList = new LinearLayout(this);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(0, dp(6), 0, dp(12));
        scrollView.addView(messageList, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM);
        composer.setPadding(0, dp(8), 0, 0);

        messageInput = new EditText(this);
        messageInput.setHint("বার্তা লিখুন…");
        messageInput.setTextColor(INK);
        messageInput.setHintTextColor(MUTED);
        messageInput.setTextSize(16);
        messageInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        messageInput.setMinHeight(dp(50));
        messageInput.setMaxLines(4);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageInput.setBackground(rounded(Color.WHITE, dp(16)));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.rightMargin = dp(8);
        composer.addView(messageInput, inputParams);

        sendButton = new Button(this);
        sendButton.setText("পাঠান");
        sendButton.setTextColor(Color.WHITE);
        sendButton.setAllCaps(false);
        sendButton.setBackground(rounded(GREEN, dp(16)));
        sendButton.setOnClickListener(v -> sendMessage());
        composer.addView(sendButton, new LinearLayout.LayoutParams(dp(86), dp(50)));
        page.addView(composer);

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean send = actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP);
            if (send) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void initializePython() {
        WORKER.execute(() -> {
            try {
                if (!Python.isStarted()) {
                    Python.start(new AndroidPlatform(getApplicationContext()));
                }
                PyObject module = Python.getInstance().getModule("android_bridge");
                module.callAttr("initialize", getFilesDir().getAbsolutePath());
                boolean hasKey = module.callAttr("has_api_key").toBoolean();
                runOnUiThread(() -> {
                    bridge = module;
                    ready = true;
                    updateStatus(hasKey);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    statusView.setText("Python runtime চালু হয়নি");
                    statusView.setTextColor(Color.rgb(175, 45, 45));
                    addMessage("ত্রুটি", ex.getClass().getSimpleName() + ": " + safeMessage(ex), false);
                });
            }
        });
    }

    private void sendMessage() {
        if (!ready || bridge == null) {
            statusView.setText("অ্যাপ প্রস্তুত হওয়া পর্যন্ত অপেক্ষা করুন।");
            return;
        }
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        messageInput.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
        addMessage("আপনি", text, true);
        sendButton.setEnabled(false);
        statusView.setText("AIRA উত্তর তৈরি করছে…");
        View waiting = addMessage("AIRA", "ভাবছি…", false);

        WORKER.execute(() -> {
            String answer;
            try {
                answer = bridge.callAttr("send_message", text).toString();
                if (answer.trim().isEmpty()) answer = "কোনো উত্তর পাওয়া যায়নি।";
            } catch (Exception ex) {
                answer = "ত্রুটি: " + ex.getClass().getSimpleName() + ": " + safeMessage(ex);
            }
            final String result = answer;
            runOnUiThread(() -> {
                messageList.removeView(waiting);
                addMessage("AIRA", result, false);
                sendButton.setEnabled(true);
                statusView.setText("প্রস্তুত");
            });
        });
    }

    private void openApiSettings() {
        if (!ready || bridge == null) {
            statusView.setText("অ্যাপ প্রস্তুত হওয়া পর্যন্ত অপেক্ষা করুন।");
            return;
        }
        EditText keyInput = new EditText(this);
        keyInput.setHint("নিজের Gemini API key লিখুন");
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setPadding(dp(12), dp(10), dp(12), dp(10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Gemini API key")
                .setMessage("Keyটি এই ডিভাইসের app-private storage-এ থাকবে; APK বা repository-তে যোগ হবে না।")
                .setView(keyInput)
                .setPositiveButton("সংরক্ষণ", null)
                .setNeutralButton("মুছুন", null)
                .setNegativeButton("বন্ধ", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String key = keyInput.getText().toString().trim();
                if (key.isEmpty()) {
                    keyInput.setError("API key লিখুন");
                    return;
                }
                dialog.dismiss();
                WORKER.execute(() -> {
                    try {
                        bridge.callAttr("save_api_key", key);
                        runOnUiThread(() -> {
                            updateStatus(true);
                            addMessage("সেটিংস", "API key এই ডিভাইসে সংরক্ষিত হয়েছে।", false);
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() -> addMessage("ত্রুটি", safeMessage(ex), false));
                    }
                });
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                dialog.dismiss();
                WORKER.execute(() -> {
                    try {
                        bridge.callAttr("clear_api_key");
                        runOnUiThread(() -> {
                            updateStatus(false);
                            addMessage("সেটিংস", "সংরক্ষিত API key মুছে ফেলা হয়েছে।", false);
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() -> addMessage("ত্রুটি", safeMessage(ex), false));
                    }
                });
            });
        });
        dialog.show();
    }

    private void updateStatus(boolean hasKey) {
        if (hasKey) {
            statusView.setText("Gemini API key সংরক্ষিত • প্রস্তুত");
            statusView.setTextColor(GREEN);
        } else {
            statusView.setText("Chat চালাতে API key যোগ করুন");
            statusView.setTextColor(MUTED);
        }
    }

    private View addMessage(String speaker, String content, boolean fromUser) {
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(11));
        bubble.setBackground(rounded(fromUser ? Color.rgb(220, 241, 234) : Color.WHITE, dp(16)));
        bubble.setElevation(dp(1));

        TextView label = new TextView(this);
        label.setText(speaker);
        label.setTextColor(fromUser ? GREEN : MUTED);
        label.setTextSize(12);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.addView(label);

        TextView body = new TextView(this);
        body.setText(content);
        body.setTextColor(INK);
        body.setTextSize(15);
        body.setTextIsSelectable(true);
        body.setPadding(0, dp(3), 0, 0);
        bubble.addView(body);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(9);
        params.gravity = fromUser ? Gravity.END : Gravity.START;
        messageList.addView(bubble, params);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        return bubble;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) return "অজানা সমস্যা";
        return message.length() > 250 ? message.substring(0, 250) : message;
    }
}
