package id.pineapple.calendarview;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

public class LabelGridView extends View {
	private final float cellSize;
	private final float cellMargin;
	private final float cellTextSize;
	private final float cellBorderWidth;
	private final float headerHeight;
	private float lastTouchX = 0.0f;
	private float lastTouchY = 0.0f;
	private float scale = 1.0f;
	private final Rect textBounds = new Rect();
	private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Adapter adapter;
	
	protected LabelGridView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.LabelGridView);
		cellSize = values.getDimension(R.styleable.LabelGridView_cellSize,
				context.getResources().getDimension(R.dimen.dot_size));
		cellMargin = values.getDimension(R.styleable.LabelGridView_cellMargin,
				context.getResources().getDimension(R.dimen.dot_margin));
		cellTextSize = values.getDimension(R.styleable.LabelGridView_textSize,
				context.getResources().getDimension(R.dimen.dot_text_size));
		cellBorderWidth = values.getDimension(R.styleable.LabelGridView_cellBorderWidth,
				context.getResources().getDimension(R.dimen.dot_border_width));
		headerHeight = values.getDimension(R.styleable.LabelGridView_headerHeight,
				context.getResources().getDimension(R.dimen.header_height));
		values.recycle();
	}
	
	protected LabelGridView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	protected LabelGridView(Context context) {
		this(context, null);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int columnCount = adapter != null ? adapter.getColumnCount() : 0;
		final int cellCount = adapter != null ? adapter.getItemCount() : 0;
		final int rowCount = cellCount > 0 ? (cellCount + columnCount - 1) / columnCount : 0;
		final float cellFullSize = cellSize + cellMargin * 2.0f;
		int width = (int) (cellFullSize * columnCount + cellBorderWidth * 2);
		int height = (int) (cellFullSize * rowCount + cellBorderWidth * 2);
		if (adapter != null && adapter.firstRowIsHeader()) {
			height -= cellFullSize - headerHeight;
		}
		final int targetWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		final int targetHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		float scaleX = 1.0f;
		switch (View.MeasureSpec.getMode(widthMeasureSpec)) {
			case MeasureSpec.AT_MOST:
				if (width <= targetWidth) break;
			case MeasureSpec.EXACTLY:
				scaleX = (float) targetWidth / width;
				width = targetWidth;
				break;
			case MeasureSpec.UNSPECIFIED:
		}
		height = (int) (height * scaleX);
		float scaleY = 1.0f;
		switch (View.MeasureSpec.getMode(heightMeasureSpec)) {
			case MeasureSpec.AT_MOST:
				if (height <= targetWidth) break;
			case MeasureSpec.EXACTLY:
				scaleY = scaleX * (float) targetHeight / height;
				height = targetHeight;
				break;
			case MeasureSpec.UNSPECIFIED:
		}
		scale = Math.min(scaleX, scaleY);
		setMeasuredDimension(width, height);
	}
	
	private int detectBackgroundColor() {
		View view = this;
		while (view != null) {
			final Drawable background = view.getBackground();
			if (background instanceof ColorDrawable) {
				return ((ColorDrawable) background).getColor();
			}
			ViewParent parent = view.getParent();
			view = parent instanceof View ? (View) parent : null;
		}
		return Color.TRANSPARENT;
	}
	
	private int mixColors(int foregroundColor, int backgroundColor, float factor) {
		int a1 = Color.alpha(foregroundColor);
		int a2 = Color.alpha(backgroundColor);
		int r1 = Color.red(foregroundColor);
		int r2 = Color.red(backgroundColor);
		int g1 = Color.green(foregroundColor);
		int g2 = Color.green(backgroundColor);
		int b1 = Color.blue(foregroundColor);
		int b2 = Color.blue(backgroundColor);
		final float f1 = factor;
		final float f2 = 1.0f - factor;
		return Color.argb(
				(int) (a1 * f1 + a2 * f2),
				(int) (r1 * f1 + r2 * f2),
				(int) (g1 * f1 + g2 * f2),
				(int) (b1 * f1 + b2 * f2)
		);
	}
	
	private int mixColors(int foregroundColor, int backgroundColor) {
		return mixColors(foregroundColor, backgroundColor,
				Color.alpha(foregroundColor) / 255.0f);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.scale(scale, scale);
		canvas.translate(cellBorderWidth, cellBorderWidth);
		
		final int backgroundColor = detectBackgroundColor();
		
		final int columnCount = adapter != null ? adapter.getColumnCount() : 0;
		final int cellCount = adapter != null ? adapter.getItemCount() : 0;
		final float cellFullSize = cellSize + cellMargin * 2.0f;
		float rowHeight = (adapter != null && adapter.firstRowIsHeader()) ?
				headerHeight : cellFullSize;
		float x = 0.0f;
		float y = 0.0f;
		textPaint.setTextSize(cellTextSize);
		for (int i = 0; i < cellCount; i++) {
			if (i > 0) {
				if (i % columnCount == 0) {
					x = 0.0f;
					y += rowHeight;
					rowHeight = cellFullSize;
				} else {
					x += cellFullSize;
				}
			}
			final String text = adapter.getItemText(i);
			if (text == null) continue;
			Adapter.BorderStyle[] borders = adapter.getItemBorders(i);
			final float cx = x + cellFullSize / 2.0f;
			final float cy = y + rowHeight / 2.0f;
			final int colorPrimary = adapter.getItemPrimaryColor(i);
			final int colorBackground = adapter.getItemBackgroundColor(i);
			float r = Math.min(cellSize, rowHeight) / 2.0f;
			for (Adapter.BorderStyle border : borders) {
				if (border == null) continue;
				if (border.width != Adapter.BorderStyle.FILLED) {
					r += border.width * cellBorderWidth;
				}
			}
			final boolean isCellFilled = borders.length > 0 && borders[0] != null &&
					borders[0].width == Adapter.BorderStyle.FILLED;
			boolean firstNonTransparentItem = true;
			for (int j = borders.length - 1; j >= 0; j--) {
				if (borders[j] == null) continue;
				if (borders[j].width != 0.0f &&
						(Color.alpha(borders[j].color) > 0 || !firstNonTransparentItem)) {
					firstNonTransparentItem = false;
					final int color = mixColors(borders[j].color, backgroundColor);
					shapePaint.setStyle(Paint.Style.FILL_AND_STROKE);
					shapePaint.setColor(color);
					if (borders[j].width == Adapter.BorderStyle.FILLED) {
						shapePaint.setStrokeWidth(cellBorderWidth);
					} else {
						shapePaint.setStrokeWidth(borders[j].width * cellBorderWidth);
					}
					canvas.drawCircle(cx, cy, r, shapePaint);
				}
				if (borders[j].width != Adapter.BorderStyle.FILLED) {
					r -= borders[j].width * cellBorderWidth;
				}
				if (borders[j].width != Adapter.BorderStyle.FILLED &&
						!isCellFilled && !firstNonTransparentItem) {
					shapePaint.setStyle(Paint.Style.FILL);
					shapePaint.setColor(backgroundColor);
					canvas.drawCircle(cx, cy, r, shapePaint);
				}
			}
			if (isCellFilled) {
				textPaint.setColor(backgroundColor);
			} else {
				textPaint.setColor(colorPrimary);
			}
			final int savedTextPaintFlags = textPaint.getFlags();
			textPaint.setFlags(savedTextPaintFlags | adapter.getItemExtraPaintFlags(i));
			textPaint.getTextBounds(text, 0, text.length(), textBounds);
			canvas.drawText(
					text,
					cx - textBounds.exactCenterX(),
					cy - textBounds.exactCenterY(),
					textPaint
			);
			textPaint.setFlags(savedTextPaintFlags);
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			lastTouchX = event.getX() / scale;
			lastTouchY = event.getY() / scale;
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public boolean performClick() {
		final int columnCount = adapter != null ? adapter.getColumnCount() : 0;
		final int cellCount = adapter != null ? adapter.getItemCount() : 0;
		final float cellFullSize = cellSize + cellMargin * 2.0f;
		int x = (int) (lastTouchX / cellFullSize);
		int y;
		if (adapter != null && adapter.firstRowIsHeader()) {
			if (lastTouchY < headerHeight) {
				y = 0;
			} else {
				y = (int) ((lastTouchY - headerHeight) / cellFullSize) + 1;
			}
		} else {
			y = (int) (lastTouchY / cellFullSize);
		}
		int index = y * columnCount + x;
		if (adapter != null && index < cellCount) {
			if (adapter.getItemText(index) != null) {
				if (adapter.onItemClicked(index)) {
					return true;
				}
			}
		}
		return super.performClick();
	}
	
	public Adapter getAdapter() {
		return adapter;
	}
	
	public void setAdapter(Adapter adapter) {
		this.adapter = adapter;
		invalidate();
	}
	
	public interface Adapter {
		boolean firstRowIsHeader();
		
		int getColumnCount();
		
		int getItemCount();
		
		BorderStyle[] getItemBorders(int index);
		
		int getItemPrimaryColor(int index);
		
		int getItemBackgroundColor(int index);
		
		int getItemExtraPaintFlags(int index);
		
		String getItemText(int index);
		
		boolean onItemClicked(int index);
		
		class BorderStyle {
			public static final float FILLED = -1.0f;
			
			public int color;
			public float width;
			
			public BorderStyle(int color, float width) {
				this.color = color;
				this.width = width;
			}
			
			public static BorderStyle filled(int color) {
				return new BorderStyle(color, FILLED);
			}
		}
	}
}
