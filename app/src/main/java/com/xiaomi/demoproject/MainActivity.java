package com.xiaomi.demoproject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.demoproject.EPG.EPGActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	private RecyclerView mRecyclerView;
	private List<Integer> mIntegers;
	private Context mContext;
	private EditText mNum1;
	private EditText mNum2;
	private EditText mNum3;
	private EditText mNum4;
	int inputTime = 0;
	private LinearLayout mRightLeftGuidView;
	private ObjectAnimator mAlphaAnimator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;         // 屏幕宽度（像素）
		int height = dm.heightPixels;       // 屏幕高度（像素）
		LogUtil.i(this, "MainActivity.onCreate:" + height + ",item height:" + height / 5);

        float density = dm.density;         // 屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = dm.densityDpi;     // 屏幕密度dpi（120 / 160 / 240）
        // 屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (width / density);  // 屏幕宽度(dp)
        int screenHeight = (int) (height / density);// 屏幕高度(dp)

        LogUtil.i(this,"MainActivity.onCreate:"+screenHeight+",item height:"+screenHeight /5);
//        getFormatTime(1413432000L);
//        getFormatTime(1423726500L);
		LogUtil.i(this,"MainActivity.onCreate:"+dip2px(mContext,638));
		LogUtil.i(this,"MainActivity.onCreate.data:"+ getDate2String(System.currentTimeMillis(),"MMMM dd HH:mm:ss"));
		LogUtil.i(this,"MainActivity.onCreate.CurrentHour:"+ getCurrentHour());
		Intent intent = new Intent(MainActivity.this, EPGActivity.class);
		startActivity(intent);
//		initView();
//		initData();
//		setAdapter();
	}


	public static int getCurrentHour() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("HH", Locale.getDefault());
		return Integer.parseInt(format.format(date));
	}

	/**
	 *
	 * @param time  1541569323155
	 * @param pattern DD-MM-YY HH:mm:ss
	 */
	public  String getDate2String(long time, String pattern) {
		Date date = new Date(time);
		SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
		return format.format(date);
	}

	public static int px2dp(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}
	public int dip2px(Context context, int dp) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dp * scale + 0.5f);
	}
	@Override
	protected void onResume() {

		super.onResume();
	}

	/**
	 * 时间戳转时间
	 *
	 * @param time
	 * @return
	 */
	public String getFormatTime(Long time) {
		LogUtil.i(this, "MainActivity.getFormatTime.time:" + time);

		if (time == null) {
			return "";
		}
		time = time * 1000;
		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		String timeStr = format.format(time);

		LogUtil.i(this, "EPGUtils.getFormatTime.format time:" + timeStr);

		return timeStr;
	}

	private void setAdapter() {
		KeyBordAdapter adapter = new KeyBordAdapter(mContext, mIntegers);
		mRecyclerView.setAdapter(adapter);
		GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
		gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				//第10行显示两列，其它显示1列
				return position == 10 ? 2 : 1;
			}
		});
		mRecyclerView.setLayoutManager(gridLayoutManager);
		adapter.setOnItemClickListener(new KeyBordAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int num) {
				LogUtil.i(this, "FocusActivity.onItemClick:" + num);
				if (inputTime < 0) {
					inputTime = 0;
				}
				switch (inputTime) {
					case 0:
						mNum1.setText("" + num);
						break;
					case 1:
						mNum2.setText("" + num);
						break;
					case 2:
						mNum3.setText("" + num);
						break;
					case 3:
						mNum4.setText("" + num);
						break;
					default:
						break;

				}
				inputTime++;
			}

			@Override
			public void onDeleteClick() {
				LogUtil.i(this, "FocusActivity.onDeleteClick");
				if (inputTime > 4) {
					inputTime = 4;
				}
				switch (inputTime) {
					case 4:
						mNum4.setText("");
						break;
					case 3:
						mNum3.setText("");
						break;
					case 2:
						mNum2.setText("");
						break;
					case 1:
						mNum1.setText("");
						break;
					default:
						break;

				}
				inputTime--;
			}
		});
	}


	Handler handler = new Handler();

	private void initView() {
		mContext = this;
		setContentView(R.layout.activity_main);
		mRecyclerView = (RecyclerView) findViewById(R.id.key_recycler);
		mNum1 = (EditText) findViewById(R.id.num1);
		mNum2 = (EditText) findViewById(R.id.num2);
		mNum3 = (EditText) findViewById(R.id.num3);
		mNum4 = (EditText) findViewById(R.id.num4);
		mRightLeftGuidView = findViewById(R.id.right_left_guid);
		ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setMax(100);
		progressBar.setProgress(50);
		Button button = findViewById(R.id.btn_dialog);
		button.requestFocusFromTouch();
		Drawable image = getDrawable(R.drawable.ic_epg_play);
		image.setBounds(1, 1, 26, 33);
		button.setCompoundDrawables(image,null,null,null);
		TextView tv_title = findViewById(R.id.tv_title);
		tv_title.setMovementMethod(ScrollingMovementMethod.getInstance());
		ObjectAnimator.ofFloat(tv_title, "translationX", -200, 0).setDuration(1).start();
//		startTranslationAnimation(tv_title);
//		tv_title.scrollTo(-100,0);
		ImageView imageView = findViewById(R.id.loading_bg);
		startAlphaBreathAnimation(imageView);

		mAlphaAnimator.cancel();


		TextView rightLeftTextView = findViewById(R.id.right_left_text);
		String content = mContext.getString(R.string.left_right_guid);
		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(content);
		StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
		stringBuilder.setSpan(styleSpan, 6, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		styleSpan = new StyleSpan(Typeface.BOLD);
		stringBuilder.setSpan(styleSpan, 31, 37, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		rightLeftTextView.setText(stringBuilder);


		TextView confirmGuidView = findViewById(R.id.confirm_guid);

		String content1 = mContext.getString(R.string.ok_guid_text);
		SpannableStringBuilder stringBuilder1 = new SpannableStringBuilder(content1);
		StyleSpan styleSpan1 = new StyleSpan(Typeface.BOLD);
		stringBuilder1.setSpan(styleSpan1, 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		confirmGuidView.setText(stringBuilder1);

		((Button) findViewById(R.id.btn)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, InfoActivity.class);
				startActivity(intent);
			}
		});

		long currentTime = System.currentTimeMillis();
		long storeTime = SharedPreferencesUtils.getLong(mContext, "epg_show_time", 0);
		LogUtil.i(this, "MainActivity.initView.currentTime:" + currentTime + ",storeTime:" + storeTime);
		if (currentTime - storeTime > 1000) {
			showGuidView(mRightLeftGuidView);
			SharedPreferencesUtils.putLong(mContext, "epg_show_time", currentTime);
		}

	}

	/**
	 * 开启平移动画
	 */
	private void startTranslationAnimation(View view) {
		mAlphaAnimator = ObjectAnimator.ofFloat(view, "translationX", -100, 0);
		mAlphaAnimator.setDuration(500);
		mAlphaAnimator.start();
	}
	/**
	 * 开启透明度渐变呼吸动画
	 */
	private void startAlphaBreathAnimation(View view) {
		mAlphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 0.6f);
		mAlphaAnimator.setDuration(1500);
		mAlphaAnimator.setInterpolator(new BreatheInterpolator());//使用自定义的插值器
		mAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mAlphaAnimator.start();
	}

	/**
	 * 定义拟合呼吸变化的插值器
	 */

	public class BreatheInterpolator implements TimeInterpolator {
		@Override
		public float getInterpolation(float input) {

			float x = 6 * input;
			float k = 1.0f / 3;
			int t = 6;
			int n = 1;//控制函数周期，这里取此函数的第一个周期
			float PI = 3.1416f;
			float output = 0;

			if (x >= ((n - 1) * t) && x < ((n - (1 - k)) * t)) {
				output = (float) (0.5 * Math.sin((PI / (k * t)) * ((x - k * t / 2) - (n - 1) * t)) + 0.5);

			} else if (x >= (n - (1 - k)) * t && x < n * t) {
				output = (float) Math.pow((0.5 * Math.sin((PI / ((1 - k) * t)) * ((x - (3 - k) * t / 2) - (n - 1) * t)) + 0.5), 2);
			}
			return output;
		}
	}


	final static int COUNTS = 3;//点击次数
	final static long DURATION = 5 * 1000;//规定有效时间
	long[] mHits = new long[COUNTS];


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			/**
			 * src 拷贝的源数组
			 * srcPos 从源数组的那个位置开始拷贝.
			 * dst 目标数组
			 * dstPos 从目标数组的那个位子开始写数据
			 * length 拷贝的元素的个数
			 */
			System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
			//实现左移，然后最后一个位置更新距离开机的时间，如果最后一个时间和最开始时间小于DURATION，即连续5次点击
			mHits[mHits.length - 1] = SystemClock.uptimeMillis();
			if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
				String tips = "您已在[" + DURATION + "]ms内连续点击【" + mHits.length + "】次了！！！";
				Toast.makeText(MainActivity.this, tips, Toast.LENGTH_SHORT).show();
			}
		}

		return super.onKeyUp(keyCode, event);
	}

	private void showGuidView(View view) {
		handler.postDelayed(new Runnable() {
			@Override

			public void run() {
				/**
				 *要执行的操作
				 */
				view.setVisibility(View.VISIBLE);
				view.animate()
						.alpha(1f)
						.setDuration(2000)
						.setListener(null);
			}
		}, 3000);//3秒后执行Runnable中的run方法

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				/**
				 *要执行的操作
				 */
				view.setVisibility(View.VISIBLE);
				view.animate()
						.alpha(0f)
						.setDuration(2000)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								view.setVisibility(View.GONE);
							}
						});
			}
		}, 5000);//3秒后执行Runnable中的run方法
	}

	private void initData() {

		mIntegers = new ArrayList<>();
		for (int i = 1; i <= 9; i++) {
			mIntegers.add(i);
		}
		mIntegers.add(0);
		mIntegers.add(-1);
	}

}
