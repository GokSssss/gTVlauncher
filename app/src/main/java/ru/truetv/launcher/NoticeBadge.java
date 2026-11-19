package ru.truetv.launcher;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Same36dp image box as the other header glyphs; zero is an empty ring. */
final class NoticeBadge extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private int count=-1;
    NoticeBadge(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void count(int value){if(count!=value){count=value;invalidate();}}
    protected void onDraw(Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f;paint.setColor(Ui.ink(getContext()));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),1.7f));canvas.drawCircle(cx,cy,Ui.dp(getContext(),13),paint);
        if(count==0)return;String text=count<0?"—":count>99?"99+":String.valueOf(count);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Ui.dp(getContext(),text.length()>2?11:15));Paint.FontMetrics f=paint.getFontMetrics();canvas.drawText(text,cx,cy-(f.ascent+f.descent)/2f,paint);
    }
}
