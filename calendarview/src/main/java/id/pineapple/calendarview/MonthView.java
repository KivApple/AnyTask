package id.pineapple.calendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static id.pineapple.calendarview.WeekView.WEEK_DAY_COUNT;

public class MonthView extends LabelGridView implements LabelGridView.Adapter {
	private String[] weekDaySymbols =
			DateFormatSymbols.getInstance(Locale.getDefault()).getShortWeekdays();
	private int firstDayOfWeekIndex = Calendar.getInstance().getFirstDayOfWeek() - 1;
	private Calendar calendar = Calendar.getInstance();
	private Date date;
	private int dotCount;
	private OnDayClickedListener onDayClickedListener;
	private boolean persistentHeight;
	private boolean exactMonth;
	private LabelGridView.Adapter.BorderStyle[][] itemBorders =
			new LabelGridView.Adapter.BorderStyle[49][];
	private String[] itemTexts = new String[49];
	private int[] itemColors = new int[49];
	private Date startDate;
	private Date stopDate;
	private Date selectedDate;
	private int colorPrimary;
	private int colorForeground;
	private int colorDimmed;
	private int firstMonthItem;
	private int lastMonthItem;
	private int selectedItem;
	private int nowItem;
	private DateColorAdapter dateColorAdapter;
	
	public MonthView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.MonthView);
		persistentHeight = values.getBoolean(R.styleable.MonthView_persistentHeight, false);
		exactMonth = values.getBoolean(R.styleable.MonthView_exactMonth, false);
		colorPrimary = values.getColor(R.styleable.MonthView_colorPrimary, Color.BLACK);
		colorForeground = values.getColor(R.styleable.MonthView_colorForeground, Color.WHITE);
		colorDimmed = values.getColor(R.styleable.MonthView_textColor, Color.GRAY);
		values.recycle();
		setDate(new Date());
		setSelectedDate(date);
		setAdapter(this);
	}
	
	public MonthView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public MonthView(Context context) {
		this(context, null);
	}
	
	public void setDate(Date date) {
		this.date = date;
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		final Date now = calendar.getTime();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		final int currentMonth = calendar.get(Calendar.MONTH);
		int i = 0;
		while (i < 7) {
			itemTexts[i] = weekDaySymbols[(i + firstDayOfWeekIndex) % WEEK_DAY_COUNT + 1];
			itemBorders[i] = new BorderStyle[0];
			itemColors[i] = 0;
			i++;
		}
		while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.getFirstDayOfWeek()) {
			calendar.add(Calendar.DAY_OF_MONTH, -1);
		}
		startDate = calendar.getTime();
		while (calendar.get(Calendar.MONTH) != currentMonth) {
			itemTexts[i] = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
			itemBorders[i] = new BorderStyle[0];
			itemColors[i] = 0;
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			i++;
		}
		firstMonthItem = i;
		nowItem = -1;
		while (calendar.get(Calendar.MONTH) == currentMonth) {
			final Date current = calendar.getTime();
			if (current.equals(now)) {
				nowItem = i;
			}
			itemTexts[i] = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
			if (dateColorAdapter != null) {
				itemColors[i] = dateColorAdapter.getDateColor(current);
			} else {
				itemColors[i] = 0;
			}
			if (exactMonth) {
				if (current.equals(now)) {
					itemBorders[i] = new BorderStyle[] {
							new BorderStyle(getItemPrimaryColor(i), 1.0f)
					};
				} else {
					itemBorders[i] = new BorderStyle[] {
							new BorderStyle(getItemPrimaryColor(i), 0.0f)
					};
				}
			} else {
				int color = getItemPrimaryColor(i);
				if (current.equals(now)) {
					itemBorders[i] = new BorderStyle[] {
							new BorderStyle(color, itemColors[i] != 0 ? BorderStyle.FILLED : 1.0f),
							new BorderStyle(
									Color.argb(
											Color.alpha(color) / 2,
											Color.red(color),
											Color.green(color),
											Color.blue(color)
									),
									2.0f
							),
							new BorderStyle(color, 0.0f)
					};
				} else {
					itemBorders[i] = new BorderStyle[] {
							new BorderStyle(color, itemColors[i] != 0 ? BorderStyle.FILLED : 1.0f),
							new BorderStyle(Color.TRANSPARENT, 2.0f),
							new BorderStyle(color, 0.0f)
					};
				}
			}
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			i++;
		}
		lastMonthItem = i - 1;
		while (calendar.get(Calendar.DAY_OF_WEEK) != calendar.getFirstDayOfWeek()) {
			itemTexts[i] = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
			itemBorders[i] = new BorderStyle[0];
			itemColors[i] = 0;
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			i++;
		}
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		stopDate = calendar.getTime();
		dotCount = i;
		while (i < itemBorders.length) {
			itemBorders[i] = new BorderStyle[0];
			itemTexts[i] = "";
			itemColors[i] = 0;
			i++;
		}
		setSelectedDate(selectedDate);
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setSelectedDate(Date date) {
		if (selectedItem >= 0 && itemBorders[selectedItem].length > 0) {
			if (exactMonth) {
				itemBorders[selectedItem][itemBorders[selectedItem].length - 1].width =
					nowItem == selectedItem ? 1.0f : 0.0f;
			} else {
				itemBorders[selectedItem][itemBorders[selectedItem].length - 1].width = 0.0f;
			}
		}
		selectedDate = date;
		if (date == null) {
			selectedItem = -1;
		} else {
			final int index = (int) TimeUnit.DAYS.convert(date.getTime() - startDate.getTime(),
					TimeUnit.MILLISECONDS) + WEEK_DAY_COUNT;
			if (index >= firstMonthItem && index <= lastMonthItem) {
				selectedItem = index;
				itemBorders[selectedItem][itemBorders[selectedItem].length - 1].width =
						exactMonth ? BorderStyle.FILLED : 2.0f;
			} else {
				selectedItem = -1;
			}
		}
		invalidate();
	}
	
	public void setOnDayClickedListener(OnDayClickedListener onDayClickedListener) {
		this.onDayClickedListener = onDayClickedListener;
		if (onDayClickedListener != null) {
			setClickable(true);
		}
	}
	
	@Override
	public boolean firstRowIsHeader() {
		return true;
	}
	
	@Override
	public int getColumnCount() {
		return WEEK_DAY_COUNT;
	}
	
	@Override
	public int getItemCount() {
		return !persistentHeight ? dotCount : itemTexts.length;
	}
	
	@Override
	public BorderStyle[] getItemBorders(int index) {
		return itemBorders[index];
	}
	
	@Override
	public String getItemText(int index) {
		if (exactMonth) {
			if (index >= 7 && index < firstMonthItem || index > lastMonthItem) {
				return null;
			}
		}
		return itemTexts[index];
	}
	
	@Override
	public int getItemPrimaryColor(int index) {
		return itemColors[index] == 0 ? colorDimmed : itemColors[index];
	}
	
	@Override
	public int getItemBackgroundColor(int index) {
		return colorForeground;
	}
	
	@Override
	public int getItemExtraPaintFlags(int index) {
		return 0;
	}
	
	@Override
	public boolean onItemClicked(int index) {
		if (onDayClickedListener != null) {
			if (index >= WEEK_DAY_COUNT) {
				calendar.setTime(startDate);
				calendar.add(Calendar.DAY_OF_MONTH, index - WEEK_DAY_COUNT);
				onDayClickedListener.onDayClicked(calendar.getTime());
				return true;
			}
		}
		return false;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public Date getStopDate() {
		return stopDate;
	}
	
	public DateColorAdapter getDateColorAdapter() {
		return dateColorAdapter;
	}
	
	public void setDateColorAdapter(DateColorAdapter dateColorAdapter) {
		if (this.dateColorAdapter != null) {
			this.dateColorAdapter.monthViews.remove(this);
		}
		this.dateColorAdapter = dateColorAdapter;
		if (dateColorAdapter != null) {
			dateColorAdapter.monthViews.add(this);
		}
		setDate(date);
	}
	
	public interface OnDayClickedListener {
		void onDayClicked(Date day);
	}
	
	public abstract static class DateColorAdapter {
		private Set<MonthView> monthViews = new HashSet<>();
		
		public abstract int getDateColor(Date date);
		
		public void notifyDataSetChanged() {
			for (MonthView monthView : monthViews) {
				int selectedItem = monthView.selectedItem;
				monthView.setDate(monthView.getDate());
				monthView.selectedItem = selectedItem;
			}
		}
	}
}
