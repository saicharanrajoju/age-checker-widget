package com.agechecker.widget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

public class InputActivity extends Activity {
    EditText dob; TextView result; DotView background;
    static final int SPEECH = 12;
    static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        FrameLayout root = new FrameLayout(this);
        background = new DotView(); root.addView(background);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(24),dp(46),dp(24),dp(20));
        TextView title = label("◷  Age Checker", 27, Color.WHITE); title.setTypeface(null,1); panel.addView(title);
        TextView caption = label("Date of birth", 15, 0xffbbbbbb); LinearLayout.LayoutParams cap = new LinearLayout.LayoutParams(-1,-2); cap.topMargin=dp(32); panel.addView(caption,cap);
        LinearLayout entry = new LinearLayout(this); entry.setOrientation(LinearLayout.HORIZONTAL);
        dob = new EditText(this); dob.setSingleLine(true); dob.setTextColor(Color.WHITE); dob.setHintTextColor(0xffaaaaaa); dob.setHint("MM/DD/YYYY"); dob.setTextSize(19); dob.setInputType(2); dob.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffd48b39));
        entry.addView(dob,new LinearLayout.LayoutParams(0,dp(58),1));
        Button mic = new Button(this); mic.setText("🎙"); mic.setContentDescription("Speak date of birth"); mic.setTextSize(21); entry.addView(mic,new LinearLayout.LayoutParams(dp(68),dp(58)));
        mic.setOnClickListener(v->listen());
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1,-2); ep.topMargin=dp(10); panel.addView(entry,ep);
        dob.addTextChangedListener(new TextWatcher() { boolean busy; public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){if(busy)return;String digits=s.toString().replaceAll("\\D",""); if(digits.length()>8)digits=digits.substring(0,8); String out=digits.length()>4?digits.substring(0,2)+"/"+digits.substring(2,4)+"/"+digits.substring(4):digits.length()>2?digits.substring(0,2)+"/"+digits.substring(2):digits; if(!out.equals(s.toString())){busy=true;dob.setText(out);dob.setSelection(out.length());busy=false;}} public void afterTextChanged(Editable e){} });
        Button check = new Button(this); check.setText("CHECK AGE  →"); check.setTextColor(Color.BLACK); check.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffe3a453)); check.setOnClickListener(v->calculate());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,dp(56)); cp.topMargin=dp(22); panel.addView(check,cp);
        result=label("Enter DOB or tap the microphone",20,0xffffd6a4); result.setGravity(Gravity.CENTER); result.setPadding(dp(12),dp(18),dp(12),dp(18));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(25);panel.addView(result,rp);
        root.addView(panel); setContentView(root);
        String saved=getSharedPreferences("age",MODE_PRIVATE).getString("dob",""); if(!saved.isEmpty()){dob.setText(saved);calculate();}
    }
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    void calculate(){
        try {LocalDate birth=LocalDate.parse(dob.getText().toString(),FORMAT); LocalDate now=LocalDate.now();if(birth.isAfter(now)||birth.getYear()<1900)throw new Exception();
            int age=Period.between(birth,now).getYears();boolean ok=age>=21;
            result.setText((ok?"✓  21 OR ABOVE":"✕  BELOW 21")+"\nAge: "+age+" years");result.setTextColor(ok?0xff65e9a2:0xffff8181);
            getSharedPreferences("age",MODE_PRIVATE).edit().putString("dob",birth.format(FORMAT)).apply();AgeWidget.refresh(this);
        }catch(Exception ex){result.setText("Check the date: MM/DD/YYYY");result.setTextColor(0xffff8181);}
    }
    void listen(){
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say a date, e.g. December seven two thousand");
        try{startActivityForResult(i,SPEECH);}catch(Exception e){Toast.makeText(this,"Google speech recognition is unavailable. Use keyboard voice typing.",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);if(request==SPEECH&&code==RESULT_OK&&data!=null){ArrayList<String> a=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty()){String parsed=parseSpoken(a.get(0));if(parsed!=null){dob.setText(parsed);result.setText("Recognized: "+parsed+"\nTap CHECK AGE to confirm");result.setTextColor(0xffffd6a4);}else{result.setText("Heard: "+a.get(0)+"\nPlease type or repeat the date");}}}}
    String parseSpoken(String raw){
        String s=raw.toLowerCase(Locale.US).replaceAll("(st|nd|rd|th)\\b","").replaceAll("[,.-]"," ").trim();
        String[] months={"january","february","march","april","may","june","july","august","september","october","november","december"};
        for(int m=0;m<12;m++){Matcher match=Pattern.compile("\\b"+months[m]+"\\s+(\\d{1,2})\\s+(\\d{4})\\b").matcher(s);if(match.find())return normalize(m+1,Integer.parseInt(match.group(1)),Integer.parseInt(match.group(2)));}
        Matcher numeric=Pattern.compile("\\b(\\d{1,2})[ /](\\d{1,2})[ /](\\d{4})\\b").matcher(s);if(numeric.find())return normalize(Integer.parseInt(numeric.group(1)),Integer.parseInt(numeric.group(2)),Integer.parseInt(numeric.group(3)));
        // Recognizer can transcribe the year as separate words; avoid guessing ambiguous dates.
        return null;
    }
    String normalize(int m,int d,int y){try{return LocalDate.of(y,m,d).format(FORMAT);}catch(Exception e){return null;}}
    class DotView extends View {
        Paint p=new Paint(3);long start=System.currentTimeMillis();
        DotView(){super(InputActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.BLACK);float w=getWidth(),h=getHeight(),time=(System.currentTimeMillis()-start)/1000f;
            float spacing=dp(16);for(float y=dp(32);y<h-dp(35);y+=spacing)for(float x=dp(16);x<w-dp(10);x+=spacing){float wave=(float)Math.sin(time*1.3+x*.009+y*.007);float fall=(float)Math.max(.12,1-Math.abs(x-w*.57)/w-Math.abs(y-h*.46)/h);float r=(1.1f+wave*.7f)*getResources().getDisplayMetrics().density*fall;p.setColor(Color.argb((int)(140*fall),225,135,42));c.drawCircle(x,y,Math.max(.35f,r),p);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1)/2f);p.setColor(0xff77716b);Path web=new Path();float ox=w-dp(10),oy=dp(15);for(int i=0;i<6;i++){float angle=(float)(Math.PI+i*Math.PI/9);web.moveTo(ox,oy);web.lineTo(ox+(float)Math.cos(angle)*dp(130),oy-(float)Math.sin(angle)*dp(130));}for(int j=1;j<5;j++){float rad=dp(j*27);web.moveTo(ox-rad,oy);web.quadTo(ox-rad*.55f,oy+rad*.6f,ox,oy+rad);}c.drawPath(web,p);
            float spiderY=dp(90)+(float)Math.sin(time*.9)*dp(25);float sx=w-dp(62);c.drawLine(sx,oy,sx,spiderY,p);p.setStyle(Paint.Style.FILL);p.setColor(0xff9c928b);c.drawCircle(sx,spiderY,dp(3),p);p.setStyle(Paint.Style.STROKE);invalidate();
        }
    }
}
