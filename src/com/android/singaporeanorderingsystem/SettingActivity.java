package com.android.singaporeanorderingsystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.R;
import com.android.bean.FoodHttpBean;
import com.android.common.Constants;
import com.android.common.HttpHelper;
import com.android.common.MyApp;
import com.android.dao.FoodHttpBeanDao;
import com.android.dialog.DialogBuilder;
import com.android.handler.RemoteDataHandler;
import com.android.handler.RemoteDataHandler.Callback;
import com.android.model.ResponseData;

public class SettingActivity extends Activity {

	private EditText language_set;
	private EditText print_one_edit;
	private EditText shop_set;
	private EditText take_price_edit;
	private boolean is_chinese;
	private SharedPreferences sharedPrefs;
	private PopupWindow popupWindow;
	private View view;
	private ImageView menu;
	private Button synchronization_menu;
	private Button synchronization_shop;
	private Button btu_discount;
	public static String type;
	private Button print_one_btu;
	private MyApp myApp;
	private TextView admin_set;
	private RelativeLayout r_set_admin_lay;
	private RelativeLayout layout_exit;
	private SharedPreferences spf;
	private FoodHttpBeanDao fhb_dao;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
   	 .detectDiskReads()
   	 .detectDiskWrites()
   	 .detectNetwork() // 这里可以替换为detectAll() 就包括了磁盘读写和网络I/O
   	 .penaltyLog() //打印logcat，当然也可以定位到dropbox，通过文件保存相应的log
   	 .build());
   	 StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
   	 .detectLeakedSqlLiteObjects() //探测SQLite数据库操作
   	.penaltyLog() //打印logcat
   	 .penaltyDeath()
   	 .build());
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.setting);	
		myApp = (MyApp) SettingActivity.this.getApplication();
		Intent intent = this.getIntent();
		Bundle bundle = intent.getExtras();
		type = bundle.getString("type");
		language_set = (EditText) findViewById(R.id.language_set);
		print_one_edit = (EditText) findViewById(R.id.print_one_edit);
		take_price_edit = (EditText) findViewById(R.id.take_price_edit);
		admin_set = (TextView) findViewById(R.id.admin_set);
		r_set_admin_lay =(RelativeLayout) findViewById(R.id.r_set_admin_lay);
		layout_exit =(RelativeLayout) findViewById(R.id.layout_exit);
		shop_set = (EditText) findViewById(R.id.shop_set);
		menu = (ImageView) findViewById(R.id.menu_btn);
		print_one_btu = (Button) findViewById(R.id.print_one_btu);
		btu_discount = (Button) findViewById(R.id.btu_discount);
		synchronization_menu = (Button) findViewById(R.id.synchronization_menu_brn);
		synchronization_shop = (Button) findViewById(R.id.synchronization_shop_brn);
		sharedPrefs = getSharedPreferences("language", Context.MODE_PRIVATE);
		String type = sharedPrefs.getString("type", "");
		if(myApp.getU_type().equals("SUPERADMIN")){
			admin_set.setVisibility(View.VISIBLE);
			r_set_admin_lay.setVisibility(View.VISIBLE);
		}
		menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				initPopupWindow();
				popupWindow.setFocusable(true);
				popupWindow.setBackgroundDrawable(new BitmapDrawable());
				popupWindow.showAsDropDown(menu, 0, -5);
			}
		});
		print_one_edit.setText(myApp.getIp_str());
		take_price_edit.setText(myApp.getDiscount());
		shop_set.setText(myApp.getSettingShopId());
		if (type == null) {
			type = "en";
		}
		if (type.equals("zh")) {
			is_chinese = true;
		} else {
			is_chinese = false;
		}
		if (!is_chinese) {
			language_set.setText("English");
		} else {
			language_set.setText("中文");
		}
		layout_exit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CreatedDialog().create().show();
			}
		});
		print_one_btu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String ip = print_one_edit.getText().toString();
				myApp.setIp_str(ip);
				myApp.getPrinter().reconnect();
				Toast.makeText(SettingActivity.this, getString(R.string.toast_setting_succ), Toast.LENGTH_SHORT).show();
			}
		});
		btu_discount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String text_discount=take_price_edit.getText().toString();
				myApp.setDiscount(text_discount);
				Toast.makeText(SettingActivity.this, getString(R.string.toast_setting_succ), Toast.LENGTH_SHORT).show();
			}
		});

		// 设置摊位ID,第一次超管必须设置好
		synchronization_shop.setOnClickListener(new OnClickListener() {;
			@Override
			public void onClick(View v) {
				String shop_id = shop_set.getText().toString();
				myApp.setSettingShopId(shop_id);
				Toast.makeText(SettingActivity.this, getString(R.string.toast_setting_succ), Toast.LENGTH_SHORT)
						.show();
			}
		});
		synchronization_menu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("myApp.getSettingShopId()-->"+myApp.getSettingShopId());
				if(myApp.getSettingShopId() == null || myApp.getSettingShopId().equals("") ||
						myApp.getSettingShopId().equals("null")||
						myApp.getSettingShopId().equals("0")){
					Toast.makeText(SettingActivity.this, getString(R.string.setting_tanwei_id), Toast.LENGTH_SHORT).show();
				return ;
				}
				String url = Constants.URL_FOODSLIST_PATH+myApp.getSettingShopId();
				RemoteDataHandler.asyncGet(url, new Callback() {
					@Override
					public void dataLoaded(ResponseData data) {
						if(data.getCode() == 1){
							String json =data.getJson();
							ArrayList<FoodHttpBean> datas=FoodHttpBean.newInstanceList(json);
							fhb_dao =FoodHttpBeanDao.getInatance(SettingActivity.this);
							fhb_dao.delete();
							for(int i=0 ; i< datas.size() ; i ++){
								FoodHttpBean food_h_bean=datas.get(i);
								System.out.println("f-->"+food_h_bean.getPicture()+",i--->"+i);
								try {
									System.out.println("11111111111111");
									String image_file=Constants.CACHE_IMAGE+"/"+"food_image_"+i+".png";
									HttpHelper.download(food_h_bean.getPicture(),new File(image_file));
									food_h_bean.setPicture(image_file);
									fhb_dao.save(food_h_bean);
								} catch (IOException e) {
									e.printStackTrace();
								}
								
							}
						}else if(data.getCode() == -1){
							
						}
					}
				});
			}
		});

		// 语言设置
		language_set.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Toast.makeText(SettingActivity.this, getString(R.string.toast_setting_language_succ), Toast.LENGTH_SHORT).show();
				DialogBuilder builder=new DialogBuilder(SettingActivity.this);
				builder.setTitle(R.string.message_title);
				builder.setMessage(R.string.message_2);
				builder.setPositiveButton(R.string.message_ok, new android.content.DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						//Toast.makeText(DailyPayActivity.this, "你点击了确定", Toast.LENGTH_SHORT).show();
						if(!is_chinese){
						updateLange(Locale.SIMPLIFIED_CHINESE);
						language_set.setText("中文");
						Editor editor = sharedPrefs.edit();
						editor.putString("type", "zh");
						editor.commit();
						is_chinese=true;
						}else{
							updateLange(Locale.ENGLISH);
							language_set.setText("English");
							Editor editor = sharedPrefs.edit();
							editor.putString("type", "en");
							editor.commit();
							is_chinese=false;
						}
					}});
				builder.setNegativeButton(R.string.message_cancle, new android.content.DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						//Toast.makeText(DailyPayActivity.this, "你点击了取消", Toast.LENGTH_SHORT).show();
					}});
				builder.create().show();
				
			}});
	}
	public void initPopupWindow() {
		if (popupWindow == null) {
			view = this.getLayoutInflater().inflate(R.layout.popupwindow, null);
			popupWindow = new PopupWindow(view, LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			popupWindow.setOutsideTouchable(true);
			TextView popu_setting = (TextView) view
					.findViewById(R.id.popu_setting);
			TextView popu_exit = (TextView) view.findViewById(R.id.popu_exit);
			TextView popu_daily = (TextView) view.findViewById(R.id.popu_daily);
			TextView popu_diancai = (TextView) view
					.findViewById(R.id.popu_diancai);
			popu_diancai.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (popupWindow.isShowing()) {
						popupWindow.dismiss();
					}
					Intent intent = new Intent(SettingActivity.this,
							MainActivity.class);
					SettingActivity.this.startActivity(intent);
					// overridePendingTransition(R.anim.in_from_right,R.anim.out_to_left);
					SettingActivity.this.finish();
				}
			});
			;
			popu_setting.setVisibility(View.GONE);

			popu_daily.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					if (popupWindow.isShowing()) {
						popupWindow.dismiss();
					}
					Intent intent = new Intent(SettingActivity.this,
							DailyPayActivity.class);
					overridePendingTransition(R.anim.in_from_right,
							R.anim.out_to_left);
					SettingActivity.this.startActivity(intent);
					SettingActivity.this.finish();
				}
			});
			popu_exit.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (popupWindow.isShowing()) {
						popupWindow.dismiss();
					}
					CreatedDialog().create().show();
				}
			});
		}
		if (popupWindow.isShowing()) {
			popupWindow.dismiss();
		}
	}

	public void updateActivity() {
		Intent intent = new Intent();
		intent.setClass(this, SettingActivity.class);// 当前Activity重新打开
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Bundle bundle = new Bundle();
		bundle.putString("type", type);
		intent.putExtras(bundle);
		startActivity(intent);
		this.finish();

	}

	public DialogBuilder CreatedDialog() {
		DialogBuilder builder = new DialogBuilder(this);
		builder.setTitle(R.string.message_title);
		builder.setMessage(R.string.message_exit);
		builder.setPositiveButton(R.string.message_ok,
				new android.content.DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_MAIN);
						intent.addCategory(Intent.CATEGORY_HOME);
						intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intent);
						System.exit(0);
					}
				});
		builder.setNegativeButton(R.string.message_cancle,
				new android.content.DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
					}
				});
		return builder;
	}

	private void updateLange(Locale locale) {
		Resources res = getResources();
		Configuration config = res.getConfiguration();
		config.locale = locale;
		DisplayMetrics dm = res.getDisplayMetrics();
		res.updateConfiguration(config, dm);
		Toast.makeText(this, "Locale in " + locale + " !", Toast.LENGTH_LONG)
				.show();
		updateActivity();

	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		 InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		 imm.hideSoftInputFromWindow(print_one_edit.getWindowToken(), 0); //强制隐藏键盘 
		 imm.hideSoftInputFromWindow(shop_set.getWindowToken(), 0); //强制隐藏键盘 
		 imm.hideSoftInputFromWindow(take_price_edit.getWindowToken(), 0); //强制隐藏键盘 
		return super.onTouchEvent(event);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (type.equals("1")) {
				Intent intent = new Intent();
				intent.setClass(this, MainActivity.class);
				startActivity(intent);
				this.finish();
			} else {
				Intent intent = new Intent();
				intent.setClass(this, DailyPayActivity.class);
				startActivity(intent);
				this.finish();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

}
class MyAsynaTask extends AsyncTask<String,Void,String>{
	private String dates;
	private int i;

	public MyAsynaTask(String dates,int i){
		this.dates=dates;
		this.i=i;
	}
	@Override
	protected String doInBackground(String... params) {
		if(dates!=null){
		if(dates!=null && !"".equals(dates) && !"null".equals(dates)){
			return dates;
		}
		}
		return null;
	}
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		if(result!=null && !"".equals(result)){
			//加载远程图片
			try {
				System.out.println("1->"+result);
				HttpHelper.download(result,new File( Constants.CACHE_IMAGE+"/"
						+"food_image_"+i+".png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
