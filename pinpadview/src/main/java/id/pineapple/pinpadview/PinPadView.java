package id.pineapple.pinpadview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PinPadView extends GridLayout implements View.OnClickListener {
	private static final int BUTTON_COUNT = 10;
	private static final int BUTTON_COLUMN_COUNT = 3;
	private static final int DEFAULT_PIN_CODE_LENGTH = 4;
	
	private static final int CANCEL_BUTTON_TAG = -1;
	private static final int BACKSPACE_BUTTON_TAG = -2;
	
	private final LinearLayout dotsLayout;
	private DotView[] dots;
	private final int dotsTheme;
	private final Button[] buttons = new Button[BUTTON_COUNT];
	private final Button cancelButton;
	private final ImageButton backspaceButton;
	private boolean shuffleButtons;
	private String pinCode = "";
	private int pinCodeLength;
	private boolean errorState = false;
	private OnPinCodeEnteredListener onPinCodeEnteredListener = null;
	private int dotsDrawableResId = 0;
	
	public PinPadView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PinPadView);
		shuffleButtons = a.getBoolean(R.styleable.PinPadView_shuffleButtons, false);
		pinCodeLength = a.getInteger(R.styleable.PinPadView_pinCodeLength,
				DEFAULT_PIN_CODE_LENGTH);
		final int buttonTheme = a.getResourceId(R.styleable.PinPadView_buttonsTheme,
				android.R.style.Widget_Button);
		dotsTheme = a.getResourceId(R.styleable.PinPadView_dotsTheme, R.style.PinPadDotBase);
		dotsDrawableResId = a.getResourceId(R.styleable.PinPadView_dotsDrawable, 0);
		final String cancelButtonTitle = a.getString(R.styleable.PinPadView_cancelButtonTitle);
		a.recycle();
		Context buttonContext = new ContextThemeWrapper(context, buttonTheme);
		setColumnCount(BUTTON_COLUMN_COUNT);
		GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
		layoutParams.columnSpec = spec(0, BUTTON_COLUMN_COUNT, CENTER);
		layoutParams.setGravity(Gravity.CENTER);
		dotsLayout = new LinearLayout(context);
		addView(dotsLayout, layoutParams);
		int rowCount = (BUTTON_COUNT + BUTTON_COLUMN_COUNT - 1) / BUTTON_COLUMN_COUNT;
		int row = 0;
		cancelButton = new Button(buttonContext, null, buttonTheme);
		backspaceButton = new ImageButton(buttonContext, null, buttonTheme);
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new Button(buttonContext, null, buttonTheme);
			buttons[i].setOnClickListener(this);
			if (row == rowCount - 1 && i % BUTTON_COLUMN_COUNT == 0) {
				addView(cancelButton, makeLayoutParams(buttonContext));
			}
			addView(buttons[i], makeLayoutParams(buttonContext));
			if (i == buttons.length - 1) {
				addView(backspaceButton, makeLayoutParams(buttonContext));
			}
			if ((i + 1) % BUTTON_COLUMN_COUNT == 0) {
				row++;
			}
		}
		cancelButton.setTag(CANCEL_BUTTON_TAG);
		cancelButton.setText(cancelButtonTitle);
		cancelButton.setOnClickListener(this);
		backspaceButton.setTag(BACKSPACE_BUTTON_TAG);
		backspaceButton.setImageResource(R.drawable.ic_backspace_black_24dp);
		Drawable drawable = backspaceButton.getDrawable().mutate();
		drawable.setColorFilter(cancelButton.getTextColors().getDefaultColor(),
				PorterDuff.Mode.SRC_IN);
		backspaceButton.setImageDrawable(drawable);
		backspaceButton.setOnClickListener(this);
		setupDots();
		setupButtons();
	}
	
	public PinPadView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public PinPadView(Context context) {
		this(context, null);
	}
	
	@SuppressLint("SetTextI18n")
	private void setupButtons() {
		final List<Integer> digits = new ArrayList<>(buttons.length);
		for (int i = 0; i < buttons.length; i++) {
			digits.add((i + 1) % 10);
		}
		if (shuffleButtons) {
			Collections.shuffle(digits);
		}
		for (int i = 0; i < buttons.length; i++) {
			int digit = digits.get(i);
			buttons[i].setText(Integer.toString(digit));
			buttons[i].setTag(digit);
		}
	}
	
	private void setupDots() {
		dotsLayout.removeAllViews();
		dots = new DotView[pinCodeLength];
		Context dotsContext = new ContextThemeWrapper(getContext(), dotsTheme);
		for (int i = 0; i < dots.length; i++) {
			dots[i] = new DotView(dotsContext, null, dotsTheme);
			LinearLayout.LayoutParams layoutParams =
					new LinearLayout.LayoutParams(dotsContext, null);
			if (dotsDrawableResId != 0) {
				dots[i].setBackgroundResource(dotsDrawableResId);
			}
			dotsLayout.addView(dots[i], layoutParams);
		}
		updateDots();
	}
	
	private void updateDots() {
		for (int i = 0; i < dots.length; i++) {
			if (!errorState) {
				dots[i].setState(i < pinCode.length() ? DotState.CHECKED : DotState.NORMAL);
			} else {
				dots[i].setState(DotState.ERROR);
			}
		}
	}
	
	public void showFilledDots() {
		for (DotView dot : dots) {
			dot.setState(DotState.CHECKED);
		}
	}
	
	public boolean isShuffleButtons() {
		return shuffleButtons;
	}
	
	public void setShuffleButtons(boolean shuffleButtons) {
		if (shuffleButtons != this.shuffleButtons) {
			this.shuffleButtons = shuffleButtons;
			setupButtons();
		}
	}
	
	public boolean isErrorState() {
		return errorState;
	}
	
	public void setErrorState(boolean errorState) {
		if (this.errorState != errorState) {
			this.errorState = errorState;
			setupDots();
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			for (Button button : buttons) {
				button.setEnabled(enabled);
			}
			cancelButton.setEnabled(enabled);
			backspaceButton.setEnabled(enabled);
			Drawable drawable = backspaceButton.getDrawable().mutate();
			drawable.setColorFilter(cancelButton.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
			backspaceButton.setImageDrawable(drawable);
		}
		super.setEnabled(enabled);
	}
	
	public int getPinCodeLength() {
		return pinCodeLength;
	}
	
	public void setPinCodeLength(int pinCodeLength) {
		pinCode = "";
		if (pinCodeLength != this.pinCodeLength) {
			this.pinCodeLength = pinCodeLength;
			setupDots();
		} else {
			updateDots();
		}
	}
	
	public void clearPinCode() {
		pinCode = "";
		updateDots();
	}
	
	public void setOnPinCodeEnteredListener(OnPinCodeEnteredListener onPinCodeEnteredListener) {
		this.onPinCodeEnteredListener = onPinCodeEnteredListener;
	}
	
	@Override
	public void onClick(View view) {
		Object tagObject = view.getTag();
		if (tagObject instanceof Integer) {
			int tag = (Integer) tagObject;
			if (tag >= 0 && tag < 10 && pinCode.length() < pinCodeLength) {
				pinCode += tag;
				updateDots();
				if (pinCode.length() == pinCodeLength && onPinCodeEnteredListener != null) {
					onPinCodeEnteredListener.onPinCodeEntered(pinCode);
				}
			} else if (tag == BACKSPACE_BUTTON_TAG && pinCode.length() > 0) {
				pinCode = pinCode.substring(0, pinCode.length() - 1);
				updateDots();
			} else if (tag == CANCEL_BUTTON_TAG && onPinCodeEnteredListener != null) {
				onPinCodeEnteredListener.onPinCodeCancel();
			}
		}
	}
	
	private static GridLayout.LayoutParams makeLayoutParams(Context context) {
		GridLayout.LayoutParams params = new GridLayout.LayoutParams(context, null);
		params.setGravity(Gravity.FILL);
		return params;
	}
	
	public interface OnPinCodeEnteredListener {
		void onPinCodeEntered(String pinCode);
		void onPinCodeCancel();
	}
	
	private static class DotView extends View {
		private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
		private static final int[] ERROR_STATE_SET = { android.R.attr.state_activated };
		
		private DotState state = DotState.NORMAL;
		
		private DotView(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			Drawable background = getBackground();
			int width;
			int height;
			if (background != null) {
				width = background.getIntrinsicWidth();
				height = background.getIntrinsicHeight();
			} else {
				width = -1;
				height = -1;
			}
			if (width < 0) {
				width = (int) getContext().getResources().getDimension(R.dimen.PinPadView_dot_size);
			}
			if (height < 0) {
				height = (int) getContext().getResources().getDimension(R.dimen.PinPadView_dot_size);
			}
			int targetWidth = MeasureSpec.getSize(widthMeasureSpec);
			int targetHeight = MeasureSpec.getSize(heightMeasureSpec);
			switch (MeasureSpec.getMode(widthMeasureSpec)) {
				case MeasureSpec.AT_MOST:
					if (width < targetWidth) break;
				case MeasureSpec.EXACTLY:
					width = targetWidth;
					break;
				case MeasureSpec.UNSPECIFIED:
					break;
			}
			switch (MeasureSpec.getMode(heightMeasureSpec)) {
				case MeasureSpec.AT_MOST:
					if (height < targetHeight) break;
				case MeasureSpec.EXACTLY:
					height = targetHeight;
					break;
				case MeasureSpec.UNSPECIFIED:
					break;
			}
			setMeasuredDimension(width, height);
		}
		
		public void setState(DotState state) {
			if (state != this.state) {
				this.state = state;
				refreshDrawableState();
				invalidate();
			}
		}
		
		@Override
		protected int[] onCreateDrawableState(int extraSpace) {
			int[] states = super.onCreateDrawableState(extraSpace + 1);
			if (state != null) {
				switch (state) {
					case NORMAL:
						break;
					case CHECKED:
						mergeDrawableStates(states, CHECKED_STATE_SET);
						break;
					case ERROR:
						mergeDrawableStates(states, ERROR_STATE_SET);
						break;
				}
			}
			return states;
		}
	}
	
	enum DotState {
		NORMAL,
		CHECKED,
		ERROR
	}
}
