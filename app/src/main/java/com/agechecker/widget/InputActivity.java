package com.agechecker.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

public class InputActivity extends Activity {
    private static final int SPEECH = 12;
    private EditText dob;
    private TextView resultTitle, resultAge;
    private LinearLayout resultCard;
    private RibbonView ribbons;
    private Button submit;
    private boolean formatting;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setFinishOnTouchOutside(true);
        buildUi();
        String saved = Themes.prefs(this).getString("dob", "");
        if (state == null && !saved.isEmpty()) dob.setText(saved);
        if (!saved.isEmpty()) showSavedResult(saved);
        if (state == null && getIntent().getBooleanExtra("start_speech", false)) dob.postDelayed(this::listen, 300);
    }

    @Override public void onStart() {
        super.onStart();
        Window w = getWindow();
        WindowManager.LayoutParams p = w.getAttributes();
        p.width = Math.min(dp(520), getResources().getDisplayMetrics().widthPixels - dp(24));
        p.height = WindowManager.LayoutParams.WRAP_CONTENT;
        p.gravity = Gravity.CENTER;
        p.dimAmount = .62f;
        w.setAttributes(p);
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private void buildUi() {
        FrameLayout shell = new FrameLayout(this);
        shell.setPadding(dp(1), dp(1), dp(1), dp(1));
        ribbons = new RibbonView(this);
        shell.addView(ribbons, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(22), dp(24), dp(22));
        scroll.addView(body);

        LinearLayout heading = row();
        TextView title = text("▣  Age Checker", 25, Color.WHITE, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button themes = button("COLORS", dp(82), 40);
        themes.setOnClickListener(v -> showThemeDialog());
        heading.addView(themes);
        Button close = button("×", dp(46), 40);
        close.setContentDescription("Close"); close.setOnClickListener(v -> finish());
        heading.addView(close);
        body.addView(heading);

        TextView hint = text("Date of birth", 14, 0xFFBCC4D8, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin = dp(16); body.addView(hint, hp);

        LinearLayout entry = row();
        entry.setPadding(dp(14), 0, dp(6), 0);
        entry.setBackground(round(0xD91A2138, 26, 0x554F608B, 1));
        dob = new EditText(this);
        dob.setSingleLine(); dob.setHint("MM/DD/YYYY"); dob.setTextSize(18); dob.setTextColor(Color.WHITE);
        dob.setHintTextColor(0xFF99A3B8); dob.setBackgroundColor(Color.TRANSPARENT);
        dob.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); dob.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dob.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        entry.addView(dob, new LinearLayout.LayoutParams(0, dp(58), 1));
        Button mic = button("MIC", dp(58), 44); mic.setContentDescription("Speak date of birth"); mic.setOnClickListener(v -> listen()); entry.addView(mic);
        submit = button("→", dp(58), 48); submit.setTextSize(27); submit.setOnClickListener(v -> calculate()); entry.addView(submit);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(60)); ep.topMargin = dp(7); body.addView(entry, ep);

        dob.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (formatting) return;
                String f = DobParser.formatTyping(s.toString());
                if (!f.equals(s.toString())) { formatting = true; dob.setText(f); dob.setSelection(f.length()); formatting = false; }
            }
            public void afterTextChanged(Editable e) { }
        });
        dob.setOnEditorActionListener((v, action, event) -> { if (action == EditorInfo.IME_ACTION_DONE) { calculate(); return true; } return false; });

        resultCard = row(); resultCard.setGravity(Gravity.CENTER_VERTICAL); resultCard.setPadding(dp(17), dp(14), dp(17), dp(14));
        resultCard.setBackground(round(0xD91B2036, 23, 0x55606B8C, 1));
        TextView statusIcon = text("○", 34, 0xFFB6C0D5, true); statusIcon.setId(android.R.id.icon); resultCard.addView(statusIcon, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
        resultTitle = text("READY TO CHECK", 20, Color.WHITE, true);
        resultAge = text("Enter DOB or use the microphone", 14, 0xFFCAD0DE, false);
        labels.addView(resultTitle); labels.addView(resultAge); resultCard.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.topMargin = dp(14); body.addView(resultCard, rp);
        TextView privacy = text("Your date is calculated locally on this device.", 12, 0xFF9099AD, false); privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2); pp.topMargin = dp(11); body.addView(privacy, pp);
        shell.addView(scroll);
        setContentView(shell);
        applyAccent();
    }

    private void calculate() {
        DobParser.Parse parsed = DobParser.parseManual(dob.getText().toString(), LocalDate.now());
        if (parsed.error != null) { showError(parsed.error); return; }
        int age = AgeCalc.completedYears(parsed.date, LocalDate.now());
        Themes.prefs(this).edit().putString("dob", parsed.date.format(DobParser.DISPLAY)).apply();
        showResult(age);
        Themes.onAgeChecked(this);
        ribbons.setColors(Themes.colors(this)); applyAccent(); AgeWidget.refresh(this);
    }

    private void showSavedResult(String saved) {
        try { showResult(AgeCalc.completedYears(LocalDate.parse(saved, DobParser.DISPLAY), LocalDate.now())); }
        catch (RuntimeException ignored) { }
    }

    private void showResult(int age) {
        boolean ok = age >= AgeCalc.THRESHOLD; int color = ok ? 0xFF63E6A0 : 0xFFFF7777;
        ((TextView)resultCard.findViewById(android.R.id.icon)).setText(ok ? "✓" : "×");
        ((TextView)resultCard.findViewById(android.R.id.icon)).setTextColor(color);
        resultTitle.setText(ok ? "21 OR ABOVE" : "BELOW 21"); resultTitle.setTextColor(color);
        resultAge.setText("Age: " + age + " years");
        resultCard.setBackground(round(ok ? 0xDB192A31 : 0xDB391D2A, 23, ok ? 0x9963E6A0 : 0x99FF7777, 1));
    }

    private void showError(String message) {
        TextView icon = resultCard.findViewById(android.R.id.icon); icon.setText("!"); icon.setTextColor(0xFFFF7777);
        resultTitle.setText("CHECK THE DATE"); resultTitle.setTextColor(0xFFFF7777); resultAge.setText(message);
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your birth date");
        try { startActivityForResult(i, SPEECH); }
        catch (RuntimeException e) { Toast.makeText(this, "Speech recognition is unavailable. Please type the date.", Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onActivityResult(int request, int code, Intent data) {
        super.onActivityResult(request, code, data);
        if (request != SPEECH || code != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null) return;
        for (String heard : results) {
            LocalDate date = DobParser.parseSpoken(heard);
            if (date != null && !date.isAfter(LocalDate.now())) {
                dob.setText(date.format(DobParser.DISPLAY));
                resultTitle.setText("DATE RECOGNIZED"); resultTitle.setTextColor(0xFFE7E9F2);
                resultAge.setText(date.format(DobParser.DISPLAY) + " — tap the arrow to confirm"); return;
            }
        }
        showError("I couldn't interpret that date. Please try again or type it.");
    }

    private void showThemeDialog() {
        final int originalTheme = Themes.selected(this), originalA = Themes.customA(this), originalB = Themes.customB(this);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(8), dp(18), 0);
        TextView preview = text("ABSTRACT FLOW PREVIEW", 16, Color.WHITE, true); preview.setGravity(Gravity.CENTER); preview.setPadding(0, dp(18), 0, dp(18));
        int[] now = Themes.colors(this); preview.setBackground(gradient(now)); box.addView(preview, new LinearLayout.LayoutParams(-1, dp(78)));
        GridLayout grid = new GridLayout(this); grid.setColumnCount(2);
        for (int i = 0; i < Themes.NAMES.length; i++) {
            final int choice = i; Button b = new Button(this); b.setText(Themes.NAMES[i]); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackground(gradient(Themes.PRESETS[i]));
            b.setOnClickListener(v -> { Themes.applyPreset(this, choice); updateThemePreview(preview); });
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams(); gp.width = 0; gp.height = dp(58); gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); gp.setMargins(dp(3),dp(3),dp(3),dp(3)); grid.addView(b, gp);
        }
        box.addView(grid);
        Button custom = new Button(this); custom.setText("Custom Theme — choose two colors"); custom.setAllCaps(false); custom.setOnClickListener(v -> showCustomPicker(preview)); box.addView(custom);
        Button surprise = new Button(this); surprise.setText("Surprise Me"); surprise.setAllCaps(false); surprise.setOnClickListener(v -> { Themes.applyPreset(this, Themes.randomPreset(Themes.selected(this))); updateThemePreview(preview); }); box.addView(surprise);
        Spinner auto = new Spinner(this); String[] modes = {"Auto Change: Off", "Auto Change: Every age check", "Auto Change: Daily (inexact)"}; auto.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes)); auto.setSelection(Themes.autoMode(this)); box.addView(auto);
        ScrollView sc = new ScrollView(this); sc.addView(box);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Color theme").setView(sc).setNegativeButton("Cancel", null).setPositiveButton("Apply", null).create();
        d.setOnCancelListener(x -> Themes.apply(this, originalTheme, originalA, originalB));
        d.setOnShowListener(x -> {
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> { Themes.apply(this, originalTheme, originalA, originalB); d.dismiss(); });
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                Themes.setAutoMode(this, auto.getSelectedItemPosition()); ThemeAlarmReceiver.schedule(this);
                ribbons.setColors(Themes.colors(this)); applyAccent(); AgeWidget.refresh(this); d.dismiss();
            });
        }); d.show();
    }

    private void showCustomPicker(TextView parentPreview) {
        LinearLayout l = new LinearLayout(this); l.setPadding(dp(20), dp(8), dp(20), 0); l.setOrientation(LinearLayout.VERTICAL);
        TextView p = text("CUSTOM GRADIENT", 16, Color.WHITE, true); p.setGravity(Gravity.CENTER); l.addView(p, new LinearLayout.LayoutParams(-1, dp(70)));
        SeekBar a = hueBar(Themes.customA(this)), b = hueBar(Themes.customB(this)); l.addView(text("Color 1",14,Color.DKGRAY,true)); l.addView(a); l.addView(text("Color 2",14,Color.DKGRAY,true)); l.addView(b);
        Runnable redraw = () -> p.setBackground(gradient(Themes.colorsFor(Themes.CUSTOM, hue(a), hue(b))));
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int x,boolean f){redraw.run();} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} }; a.setOnSeekBarChangeListener(listener); b.setOnSeekBarChangeListener(listener); redraw.run();
        new AlertDialog.Builder(this).setTitle("Custom Theme").setView(l).setNegativeButton("Cancel",null).setPositiveButton("Use colors", (d,w) -> { Themes.apply(this, Themes.CUSTOM, hue(a), hue(b)); updateThemePreview(parentPreview); }).show();
    }

    private SeekBar hueBar(int color) { SeekBar s = new SeekBar(this); float[] hsv = new float[3]; Color.colorToHSV(color, hsv); s.setMax(359); s.setProgress(Math.round(hsv[0])); return s; }
    private int hue(SeekBar s) { return Color.HSVToColor(new float[]{s.getProgress(), .78f, .94f}); }
    private void updateThemePreview(TextView p) { p.setBackground(gradient(Themes.colors(this))); p.setText(Themes.name(Themes.selected(this)).toUpperCase(Locale.US)); }
    private void applyAccent() { int c = Themes.buttonColor(Themes.colors(this)); submit.setBackgroundTintList(ColorStateList.valueOf(c)); submit.setTextColor(Themes.onColor(c)); }
    private GradientDrawable gradient(int[] cs) { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cs); g.setCornerRadius(dp(14)); return g; }
    private GradientDrawable round(int fill, int radius, int stroke, int sw) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); g.setStroke(dp(sw), stroke); return g; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String s, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); if (bold) t.setTypeface(null, 1); return t; }
    private Button button(String s, int width, int height) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(12); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(dp(4),0,dp(4),0); b.setBackgroundTintList(ColorStateList.valueOf(0x553D4865)); b.setLayoutParams(new LinearLayout.LayoutParams(width, dp(height))); return b; }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
}
