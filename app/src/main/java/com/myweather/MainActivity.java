package com.myweather;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.FIOHourly;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.reconinstruments.ReconSDK.*;
import com.reconinstruments.os.HUDOS;
import com.reconinstruments.ui.*;

import com.reconinstruments.ui.dialog.BaseDialog;
import com.reconinstruments.ui.dialog.DialogBuilder;
import com.reconinstruments.ui.list.SimpleListActivity;
import com.reconinstruments.webapi.ReconOSHttpClient;

import com.reconinstruments.os.connectivity.HUDConnectivityManager;
import com.reconinstruments.os.connectivity.IHUDConnectivity;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest;
import com.reconinstruments.os.connectivity.http.HUDHttpRequest.RequestMethod;
import com.reconinstruments.os.connectivity.http.HUDHttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//public class MainActivity extends Activity implements IHUDConnectivity {
public class MainActivity extends SimpleListActivity implements IHUDConnectivity {
//public class MainActivity extends Activity implements IReconDataReceiver, IHUDConnectivity {

	private LocationManager locationManager;
	private String provider;
	private MyLocationListener mylistener;
	private Criteria criteria;


	HUDConnectivityManager mHUDConnectivityManager = null;
	TextView mCurrentTemp;
    public boolean first;
	private TextView status;
	private TextView temperature,textressentie;
	private ImageView iconimage;
	public static String result;
	private static ReconOSHttpClient client;
	public double latitude,oldLatitude;
	public double longitude,oldLongitude;
	static String key = "28faca837266a521f823ab10d1a45050";
    public int testByte,pass;
    String language,unit,vitesse;
    String PreviousResult,temp,statusline;
    boolean UpOption,refreshInProgress,Mydebug;
	private String feel,press,wind,humid,un,tend,city;
	DialogBuilder builder;
	Button button_refresh;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		System.load("/system/lib/libreconinstruments_jni.so");
		mHUDConnectivityManager = (HUDConnectivityManager) HUDOS.getHUDService(HUDOS.HUD_CONNECTIVITY_SERVICE);

		TimeZone tz = TimeZone.getDefault();
		setContentView(R.layout.activity_main2);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		status = (TextView) findViewById(R.id.status);
//		textcondition = (TextView) findViewById(R.id.condition);
		temperature = (TextView) findViewById(R.id.Temperature);
		textressentie = (TextView) findViewById(R.id.textressentie);
    	iconimage = (ImageView) findViewById(R.id.icon);
	    button_refresh = (Button) findViewById(R.id.button_refresh);
	    statusline=""; city="";
	    UpOption = false;

	}

/*	private void createPopupDialog() {
		new DialogBuilder(this).setTitle("Warning").setSubtitle("Showing for 2 seconds").setWarningIcon().setDismissTimeout().createDialog().show();
	}
*/
/*	private void createProgressDialog() {
		new DialogBuilder(this).setTitle("Refresh").setSubtitle("(press select to finish)").showProgress().setOnKeyListener(new BaseDialog.OnKeyListener() {
			@Override
			public boolean onKey(BaseDialog dialog, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					ImageView icon = (ImageView) dialog.getView().findViewById(R.id.icon);
					icon.setImageResource(R.drawable.icon_checkmark);
					icon.setVisibility(View.VISIBLE);
					dialog.getView().findViewById(R.id.progress_bar).setVisibility(View.GONE);
					dialog.setDismissTimeout(2);
					return true;
				}
				return false;
			}
		}).createDialog().show();
	}
*/

	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		out("Keydown: (" + keyCode + ")");
	    switch (keyCode) {
	        case KeyEvent.KEYCODE_DPAD_DOWN :
	        {
	        	startActivity(new Intent(MainActivity.this, SettingsActivity.class));
	        	overridePendingTransition(R.anim.slideup_in, R.anim.slideup_out);
	        	break;
	        }

	        case KeyEvent.KEYCODE_DPAD_UP :
	        {
	        	if (UpOption) {
	        	startActivity(new Intent(MainActivity.this, HoursActivity.class));
	        	overridePendingTransition(R.anim.slidedown_in, R.anim.slidedown_out);
	        	}
	        	break;
	        }

	        case KeyEvent.KEYCODE_DPAD_CENTER :
	        {
	        	if (!refreshInProgress) {
//		        	button_refresh.setVisibility(View.INVISIBLE);
		        	refreshInProgress=true;
		        	doRefresh();
	        	}
	        	break;
	        }
	        case KeyEvent.KEYCODE_BACK :
	        {
    	        new AlertDialog.Builder(this)
    	        .setIcon(android.R.drawable.ic_dialog_alert)
    	        .setMessage(R.string.really_quit)
    	        .setPositiveButton(R.string.no, null)
    	        .setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
    	            @Override
    	            public void onClick(DialogInterface dialog, int which) {
    	                //Stop the activity
    	            	MainActivity.this.finish();    
    	            }
    	        })
    	        .show();
	        	break;
	        }
	    }
	    return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		mHUDConnectivityManager.unregister(this);
//		locationManager.removeUpdates(mylistener);
		SharedPreferences preferences = getSharedPreferences("com.myweather", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("latitude", String.valueOf(oldLatitude) );
		editor.putString("longitude", String.valueOf(oldLongitude));
		editor.putString("Language", String.valueOf(language) );
		editor.putString("Unit", String.valueOf(unit));
		editor.apply();
		super.onPause();
	}

	
    @Override
	protected void onDestroy() {
//		locationManager.removeUpdates(mylistener);
		SharedPreferences preferences = getSharedPreferences("com.myweather", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("latitude", String.valueOf(latitude) );
		editor.putString("longitude", String.valueOf(longitude));
		editor.putString("Language", String.valueOf(language) );
		editor.putString("Unit", String.valueOf(unit));
		editor.apply();
    	super.onDestroy();
	}
    
    @Override
	protected void onResume() {
		mHUDConnectivityManager.register(this);
    	SharedPreferences sharedpreferences = getSharedPreferences("com.myweather", Context.MODE_PRIVATE);
    	PreviousResult = sharedpreferences.getString("PreviousResult", "");
    	language = sharedpreferences.getString("Language", "en");
    	unit = sharedpreferences.getString("Unit", "F");
    	temp = sharedpreferences.getString("latitude", "0");
    	oldLatitude = Double.valueOf(temp);
    	temp = sharedpreferences.getString("longitude", "0");
    	oldLongitude = Double.valueOf(temp);
	    LayoutInflater inflater = getLayoutInflater();
    	View layout = inflater.inflate(R.layout.toast,(ViewGroup) findViewById(R.id.toast_layout_root));
    	ImageView image = (ImageView) layout.findViewById(R.id.image);
    	image.setImageResource(R.drawable.scroll2);
    	Toast toast = new Toast(getApplicationContext());
    	toast.setGravity(Gravity.RIGHT, 0, 0);
    	toast.setDuration(Toast.LENGTH_SHORT);
    	toast.setView(layout);
    	toast.show();
    	onDisplay(PreviousResult);
		doRefresh();
		super.onResume();
    }
    
    private void onDisplay(String data) {
    	if (data !=null & data!="") {
    		UpOption=true;
    		ForecastIO fio = new ForecastIO(key);
    		fio.getForecast(data);
    		FIOCurrently currently = new FIOCurrently(fio);
    		new FIOHourly(fio);
    		String icon =  currently.get().getByKey("icon").replace("\"", "");
    		String icon1 = "@drawable/"+icon.replace("-", "_");
    		Resources res = getResources();
    		int resourceId = res.getIdentifier(
    		   icon1, "drawable", getPackageName() );
    		iconimage.setImageResource( resourceId );
    		setTitle("MyWeather : currently");
    		if (language.equals("en")) {
    			feel = getString(R.string.feellike_en);
    			press = getString(R.string.pressure_en);
    			wind = getString(R.string.wind_en);
    			humid = getString(R.string.humid_en);
    			vitesse = "mi";
    			tend=getString(R.string.tend_en);
    		} else {
    			feel = getString(R.string.feellike_fr);
    			press = getString(R.string.pressure_fr);
    			wind = getString(R.string.wind_fr);
    			humid = getString(R.string.humid_fr);
    			vitesse = "km";
    			tend=getString(R.string.tend_fr);
    		}

    	    String [] f  = currently.get().getFieldsArray();
			String dir=headingToString2(Integer.valueOf(currently.get().getByKey("windBearing")));
    		temperature.setText(DoubleToI(currently.get().getByKey("temperature"))+"°");
    		textressentie.setText("("+DoubleToI(currently.get().getByKey("apparentTemperature"))+"°)");
			if (city !=null & city!="") { statusline=city; }
    		status.setText(statusline);
    		String substr=data.substring(data.indexOf("hourly\":{\"")+20);
    		substr=substr.substring(0, substr.indexOf("\""));
//    		button_refresh.setVisibility(View.VISIBLE);
			refreshInProgress=false;

    	} else {
    		String icon1 = "@drawable/unknown";
    		Resources res = getResources();
    		int resourceId = res.getIdentifier(
    		   icon1, "drawable", getPackageName() );
    		iconimage.setImageResource(resourceId);
    		out("nothing to display or bad json...");
    		out("data=" + data);
    		UpOption=false;
	        status.setText("nothing to display...");
//	        button_refresh.setVisibility(View.VISIBLE);
	        refreshInProgress=false;
    	}
    }
    
////////////////////////////////////////////////////


	private void doRefresh() {
		out("doRefresh()");
		result = "";
		statusline = "";
//		new DialogBuilder(this).setTitle("Refresh data").setSubtitle("(-)").showProgress().createDialog().show();
//		DialogBuilder builder = new DialogBuilder(this);
//		builder.setTitle("Refresh data").setSubtitle("(Please wait)").showProgress().setDismissTimeout(3);
//		builder.createDialog().show();
/*		BaseDialog dialog;
		ImageView icon = (ImageView)dialog.getView().findViewById(R.id.icon);
		icon.setImageResource(R.drawable.icon_checkmark);
		icon.setVisibility(View.VISIBLE);
		dialog.getView().findViewById(R.id.progress_bar).setVisibility(View.GONE);
		dialog.setDismissTimeout(2);
*/
//		builder.setDismissTimeout(5);
//		Toast.makeText(this, "Refresh", Toast.LENGTH_SHORT).show();
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider
		criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);   //default

		// user defines the criteria

		criteria.setCostAllowed(false);
		// get the best provider depending on the criteria
		provider = locationManager.getBestProvider(criteria, false);
		// the last known location of this provider
		Location location = locationManager.getLastKnownLocation(provider);

		mylistener = new MyLocationListener();

		if (location != null) {
			mylistener.onLocationChanged(location);

			// location updates: at least 1 meter and 200millsecs change
			locationManager.requestLocationUpdates(provider, 2000, 100, mylistener);
			String a = "" + location.getLatitude();
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();

				out("New Lat trouve:" + latitude + " / long:" + longitude);
				oldLatitude = latitude;
				oldLongitude = longitude;
//			statusline="(last refresh:";
			} else {
				out("no Lat trouve:");
				statusline = "No GPS. Previous location used";
				latitude = oldLatitude;
				longitude = oldLongitude;
			}
			//
			//	remplacement de la localisation pour test
			//
			//latitude=50.647392; longitude=3.130481; // my home
			latitude=45.092624; longitude=6.068348; // alpe d'huez
			//latitude=45.125263; longitude=6.127609; // Pic Blanc
			//latitude=41.919229; longitude=8.738635; //Ajaccio
			//latitude=46.192683; longitude=48.205964; //Russie
			//latitude=49.168602; longitude=25.351872; //bulgarie
			//latitude=36.752887; longitude=3.042048; //alger

			out("Fetching data...");
		        status.setText("Fetching data...");
				if (unit.equals("F")) {
					un = "us";
				} else {
					un = "ca";
				}
				city="";
			URL url = null;
			try {
				url = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=false");
				new WebRequestTask2().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
				url = new URL("https://api.forecast.io/forecast/" + key + "/" + latitude + "," + longitude + "?lang=" + language + "&units=" + un);
				new WebRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			if (!statusline.equals("")) {
				Toast.makeText(MainActivity.this, statusline, Toast.LENGTH_SHORT).show();
			}
		} else {
			out("No GPS found");
			Toast.makeText(this, "No GPS found", Toast.LENGTH_SHORT).show();
//			button_refresh.setVisibility(View.VISIBLE);
			refreshInProgress = false;
			// leads to the settings because there is no last known location
			//Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			//startActivity(intent);
		}
	}

	public String DoubleToP(String sourceDouble) {
		DecimalFormat df = new DecimalFormat("#");
		double db=Double.valueOf(sourceDouble);
		return df.format(db*100);
	}

	public String DoubleToI(String sourceDouble) {
		DecimalFormat df = new DecimalFormat("#");
		double db=Double.valueOf(sourceDouble);
		return df.format(db);
	}

	private class WebRequestTask extends AsyncTask<URL, Void, String> {

		@Override
		protected String doInBackground(URL... params) {
			URL url = params[0];
			HUDHttpRequest request = new HUDHttpRequest(RequestMethod.GET, url);
			HUDHttpResponse response;
			out("try get " + url);
			try {
				response = mHUDConnectivityManager.sendWebRequest(request);
				if ((response.getResponseCode() == 200) && (response.hasBody())) {
					out("Response.sendWebRequest = 200");
					return new String(response.getBody());
				}else {
//					status.setText("(TimeOut)");
//					button_refresh.setVisibility(View.VISIBLE);
					out("Response.sendWebRequest != 200");
					refreshInProgress=false;
					out("Error: " + response.getResponseMessage());
				}
			} catch (Exception e) {
				out("HUD not connected - No Internet");
				statusline="No Internet";
//				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
//				textView.setText("Data received...");
				PreviousResult=result;
				SharedPreferences preferences = getSharedPreferences("com.myweather", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString("PreviousResult", PreviousResult);
				editor.apply();
				oldLatitude = latitude; oldLongitude=longitude;
				out("Displaying data...");
				onDisplay(result);
				out("Data displayed...");
			}
			else {
				status.setText("No Internet");
				onDisplay(PreviousResult);
			}
		}

	}

	private class WebRequestTask2 extends AsyncTask<URL, Void, String> {

		@Override
		protected String doInBackground(URL... params) {
			URL url = params[0];
			HUDHttpRequest request = new HUDHttpRequest(RequestMethod.GET, url);
			HUDHttpResponse response;
			out("try get city " + url);
			try {
				response = mHUDConnectivityManager.sendWebRequest(request);
				if ((response.getResponseCode() == 200) && (response.hasBody())) {
					out("Response.sendWebRequest = 200");
					return new String(response.getBody());
				}else {
//					textView.setText("(TimeOut)");
//					button_refresh.setVisibility(View.VISIBLE);
					out("Response.sendWebRequest != 200");
					refreshInProgress=false;
					out("Error: " + response.getResponseMessage());
				}
			} catch (Exception e) {
				out("HUD not connected - No Internet");
				statusline="No Internet";
//				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				out("City received...");
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject = new JSONObject(result);
					city = ((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONArray("address_components").getJSONObject(2).getString("short_name");
					city=city+"("+((JSONArray)jsonObject.get("results")).getJSONObject(0).getJSONArray("address_components").getJSONObject(5).getString("short_name")+")";
				} catch (JSONException e) {
					e.printStackTrace();
				}
				out("city="+city);
				status.setText(city);
			}
			else {
			status.setText("No Internet");
			}
		}

	}

	public static String headingToString2(double x)
	{
		String directions[] = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
		return directions[ (int)Math.round((  ((double)x % 360) / 45)) ];
	}

	@Override
	public void onDeviceName(String deviceName) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onConnectionStateChanged(ConnectionState state) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onNetworkEvent(NetworkEvent networkEvent, boolean hasNetworkAccess) {
		if(!hasNetworkAccess) {
			out("HUD not connected (onNetworkEvent)- No Internet");
			status.setText("No Internet");
			statusline="No Internet";
			onDisplay(PreviousResult);
		}
	}

	public void out(String Trace) {
		System.out.println(Trace);
		File file = new File(Environment.getExternalStorageDirectory()+"/ReconApps/MyWeather2/" + "Mydebug");
		if (file.exists() == true) {
			try {
//				System.out.println("write to file");
				FileWriter fos = new FileWriter(Environment.getExternalStorageDirectory()+"/ReconApps/MyWeather2/" + "Mydebug",true);
				fos.write(Trace+"\n");
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// Initialize the location fields
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			out("Location OnStatusChanged, status="+status);
//			Toast.makeText(MainActivity.this, provider + "'s status changed to "+status +"!",
//					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			out("Location onProviderDisabled");
//			Toast.makeText(MainActivity.this, "Provider " + provider + " enabled!",
//					Toast.LENGTH_SHORT).show();

		}

		@Override
		public void onProviderDisabled(String provider) {
			out("Location onProviderDisabled");
//			Toast.makeText(MainActivity.this, "Provider " + provider + " disabled!",
//					Toast.LENGTH_SHORT).show();
		}
	}

}
