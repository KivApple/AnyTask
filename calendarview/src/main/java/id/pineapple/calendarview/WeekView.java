package id.pineapple.calendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class WeekView extends LabelGridView implements LabelGridView.Adapter {
	public static final int WEEK_DAY_COUNT = 7;
	private String[] weekDaySymbols =
			DateFormatSymbols.getInstance(Locale.getDefault()).getShortWeekdays();
	private int firstDayOfWeekIndex = Calendar.getInstance().getFirstDayOfWeek() - 1;
	private boolean[] weekDayStates = new boolean[WEEK_DAY_COUNT];
	private int colorPrimary;
	private int colorForeground;
	private int colorDimmed;
	private boolean checkable;
	private boolean dimmed;
	private int highlightedWeekday = -1;
	private OnWeekDayClickListener onWeekDayClickListener = null;
	
	public WeekView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.WeekView);
		colorPrimary = values.getColor(R.styleable.WeekView_colorPrimary, Color.BLACK);
		colorForeground = values.getColor(R.styleable.WeekView_colorForeground, Color.WHITE);
		colorDimmed = values.getColor(R.styleable.WeekView_textColor, Color.GRAY);
		setCheckable(values.getBoolean(R.styleable.WeekView_checkable, false));
		setDimmed(values.getBoolean(R.styleable.WeekView_dimmed, false));
		values.recycle();
		setAdapter(this);
	}
	
	public WeekView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public WeekView(Context context) {
		this(context, null);
	}
	
	public void setWeekDayState(int weekDay, boolean state) {
		weekDayStates[(weekDay - 1) % WEEK_DAY_COUNT] = state;
		invalidate();
	}
	
	public boolean getWeekDayState(int weekDay) {
		return weekDayStates[(weekDay - 1) % WEEK_DAY_COUNT];
	}
	
	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
		if (checkable) {
			setClickable(true);
		}
	}
	
	public boolean isCheckable() {
		return checkable;
	}
	
	public void setDimmed(boolean dimmed) {
		this.dimmed = dimmed;
		invalidate();
	}
	
	public boolean isDimmed() {
		return dimmed;
	}
	
	public void setOnWeekDayClickListener(OnWeekDayClickListener onWeekDayClickListener) {
		this.onWeekDayClickListener = onWeekDayClickListener;
		if (onWeekDayClickListener != null) {
			setClickable(true);
		}
	}
	
	@Override
	public boolean firstRowIsHeader() {
		return false;
	}
	
	@Override
	public int getItemCount() {
		return WEEK_DAY_COUNT;
	}
	
	@Override
	public int getColumnCount() {
		return WEEK_DAY_COUNT;
	}
	
	@Override
	public String getItemText(int index) {
		return weekDaySymbols[indexToWeekDay(index)];
	}
	
	@Override
	public BorderStyle[] getItemBorders(int index) {
		int color = getItemPrimaryColor(index);
		return new BorderStyle[] {
				new BorderStyle(color,
						weekDayStates[indexToWeekDay(index) - 1] ? BorderStyle.FILLED : 1.0f),
				(highlightedWeekday == indexToWeekDay(index)) ? new BorderStyle(
						Color.argb(
								Color.alpha(color) / 2,
								Color.red(color),
								Color.green(color),
								Color.blue(color)
						),
						3.0f
				) : null
		};
	}
	
	@Override
	public int getItemPrimaryColor(int index) {
		if (!dimmed) {
			return weekDayStates[indexToWeekDay(index) - 1] ? colorPrimary : colorDimmed;
		} else {
			return colorDimmed;
		}
	}
	
	@Override
	public int getItemBackgroundColor(int index) {
		return colorForeground;
	}
	
	@Override
	public int getItemExtraPaintFlags(int index) {
		return weekDayStates[indexToWeekDay(index) - 1] ? Paint.FAKE_BOLD_TEXT_FLAG : 0;
	}
	
	@Override
	public boolean onItemClicked(int index) {
		int weekDay = indexToWeekDay(index);
		if (checkable) {
			setWeekDayState(weekDay, !getWeekDayState(weekDay));
		}
		if (onWeekDayClickListener != null) {
			onWeekDayClickListener.onClick(weekDay);
		}
		return checkable || onWeekDayClickListener != null;
	}
	
	private int indexToWeekDay(int index) {
		return (index + firstDayOfWeekIndex) % WEEK_DAY_COUNT + 1;
	}
	
	public int getHighlightedWeekday() {
		return highlightedWeekday;
	}
	
	public void setHighlightedWeekday(int highlightedWeekday) {
		this.highlightedWeekday = (highlightedWeekday - 1) % WEEK_DAY_COUNT + 1;
	}
	
	public interface OnWeekDayClickListener {
		void onClick(int weekDay);
	}
}
