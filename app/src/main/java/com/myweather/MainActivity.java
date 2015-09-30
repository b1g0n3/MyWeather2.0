package com.myweather;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import com.github.dvdme.ForecastIOLib.FIOCurrently;
import com.github.dvdme.ForecastIOLib.FIOHourly;
import com.github.dvdme.ForecastIOLib.ForecastIO;
import com.reconinstruments.ReconSDK.*;
import com.reconinstruments.os.HUDOS;
import com.reconinstruments.webapi.IReconHttpCallback;
import com.reconinstruments.webapi.ReconHttpRequest;
import com.reconinstruments.webapi.ReconHttpResponse;
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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class MainActivity extends Activity implements IHUDConnectivity {
//public class MainActivity extends Activity implements IReconDataReceiver, IHUDConnectivity {

	private LocationManager locationManager;
	private String provider;
	private MyLocationListener mylistener;
	private Criteria criteria;


	HUDConnectivityManager mHUDConnectivityManager = null;
	TextView mCurrentTemp;
    public boolean first;
//	private TextView textView;
	private TextView textcondition,temperature,textressentie;
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
	private String feel,press,wind,humid,un,tend;
	Button button_refresh;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		System.load("/system/lib/libreconinstruments_jni.so");
		mHUDConnectivityManager = (HUDConnectivityManager) HUDOS.getHUDService(HUDOS.HUD_CONNECTIVITY_SERVICE);

		TimeZone tz = TimeZone.getDefault();
		setContentView(R.layout.activity_main);
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
//		textView = (TextView) findViewById(R.id.status);
		textcondition = (TextView) findViewById(R.id.condition);
		temperature = (TextView) findViewById(R.id.Temperature);
		textressentie = (TextView) findViewById(R.id.textressentie);
    	iconimage = (ImageView) findViewById(R.id.icon);
	    button_refresh = (Button) findViewById(R.id.button_refresh);
	    statusline="(last source : ";
	    UpOption = false;
		File file = new File("/storage/sdcard0/ReconApps/MyWeather2/"+"Mydebug");
		if (file.exists() == true) { Mydebug=true; } else { Mydebug=false;}
		System.out.println("Mydebug: (" + Mydebug + ")");
	}
	
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		System.out.println("Keydown: (" + keyCode + ")");
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
		        	button_refresh.setVisibility(View.INVISIBLE);
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
		locationManager.removeUpdates(mylistener);
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
//    	System.out.println("(Main onResume) Je lis values:"+oldLatitude+" / "+oldLongitude+" / "+language+" / "+unit+" / >"+PreviousResult+"<");
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
    		textressentie.setText(feel+" "+DoubleToI(currently.get().getByKey("apparentTemperature"))+"°");
//    		textView.setText(statusline+currently.get().getByKey("time")+")");
    		String substr=data.substring(data.indexOf("hourly\":{\"")+20);
    		substr=substr.substring(0, substr.indexOf("\""));
    		button_refresh.setVisibility(View.VISIBLE);
    		refreshInProgress=false;
    	} else {
    		String icon1 = "@drawable/unknown";
    		Resources res = getResources();
    		int resourceId = res.getIdentifier(
    		   icon1, "drawable", getPackageName() );
    		iconimage.setImageResource(resourceId);
    		System.out.println("nothing to display or bad json...");
    		System.out.println("data="+data);
    		UpOption=false;
//	        textView.setText("nothing to display...");
//	        button_refresh.setVisibility(View.VISIBLE);
	        refreshInProgress=false;
    	}
    }
    
////////////////////////////////////////////////////
    
	private void doRefresh() {
		System.out.println("doRefresh()");
		result=""; statusline="";
		Toast.makeText(this, "Refresh", Toast.LENGTH_SHORT).show();

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
			String a=""+location.getLatitude();
			if (location != null)
			{
				latitude=location.getLatitude();
				longitude=location.getLongitude();

				System.out.println("New Lat trouve:"+latitude+" / long:"+longitude);
				oldLatitude=latitude; oldLongitude=longitude;
//			statusline="(last refresh:";
			} else {
				System.out.println("no Lat trouve:");
				statusline="No GPS. Previous location used";
				latitude=oldLatitude; longitude=oldLongitude;
			}
			//
			//	remplacement de la localisation pour test
			//
			//latitude=50.647392; longitude=3.130481; // my home
			//latitude=45.092624; longitude=6.068348; // alpe d'huez
			//latitude=45.125263; longitude=6.127609; // Pic Blanc
			//latitude=41.919229; longitude=8.738635; //Ajaccio
			//latitude=46.192683; longitude=48.205964; //Russie
			//latitude=49.168602; longitude=25.351872; //bulgarie
			//latitude=36.752887; longitude=3.042048; //alger

			System.out.println("Fetching data...");
//		        textView.setText("Fetching data...");
			try {
				if (unit.equals("F")) { un = "us"; } else { un = "ca"; }
				URL url = new URL("https://api.forecast.io/forecast/"+key+"/"+latitude+","+longitude+"?lang="+language+"&units="+un);
				new WebRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
			} catch (MalformedURLException e) {
				System.out.println("MalformedURLException...");
			}
			if (!statusline.equals("")) { Toast.makeText(MainActivity.this, statusline,Toast.LENGTH_SHORT).show(); }
		} else {
			System.out.println("No GPS found");
			Toast.makeText(this, "No GPS found", Toast.LENGTH_SHORT).show();

			// leads to the settings because there is no last known location
			//Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			//startActivity(intent);
		}

/*
//		pass=1;
		System.out.println("init DataManager()");
		ReconSDKManager mDataManager   = ReconSDKManager.Initialize(this);
		System.out.println("DataManager receivedata");
		mDataManager.receiveData(this, ReconEvent.TYPE_LOCATION);
		System.out.println("close DataManager()");
		mDataManager.unregisterListener(ReconEvent.TYPE_LOCATION);
*/
	}
		
/*		public void onReceiveCompleted(int status, ReconDataResult result)
		{
//			if (pass==1) {
				
			    if (status != ReconSDKManager.STATUS_OK)
			    {
			        System.out.println("Communication Failure with Transcend Service");
			        return;
			    }
//			    pass++;
			    ReconLocation rloc = (ReconLocation)result.arrItems.get(0);
			    Location loc = rloc.GetLocation();
			    rloc.GetPreviousLocation();
			    if (loc != ReconLocation.INVALID_LOCATION)
			    {
			        latitude=loc.getLatitude();
			        longitude=loc.getLongitude();
			        
			        System.out.println("New Lat trouve:"+latitude+" / long:"+longitude);
					oldLatitude=latitude; oldLongitude=longitude;
//					statusline="(last refresh:";
			    } else {
			    	statusline="No GPS. Previous location used";
			    	latitude=oldLatitude; longitude=oldLongitude;

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

			System.out.println("Fetching data...");
//		        textView.setText("Fetching data...");
				try {
					if (unit.equals("F")) { un = "us"; } else { un = "ca"; }
					URL url = new URL("https://api.forecast.io/forecast/"+key+"/"+latitude+","+longitude+"?lang="+language+"&units="+un);
					new WebRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
				} catch (MalformedURLException e) {
					System.out.println("MalformedURLException...");
				}
//			}
		}

		@Override
		public void onFullUpdateCompleted(int arg0, ArrayList<ReconDataResult> arg1) {
			// TODO Auto-generated method stub
			System.out.println("boucleX");
		}
*/
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
			System.out.println("try get "+url);
			try {
				response = mHUDConnectivityManager.sendWebRequest(request);
				if ((response.getResponseCode() == 200) && (response.hasBody())) {
					System.out.println("Response.sendWebRequest = 200");
					return new String(response.getBody());
				}else {
//					textView.setText("(TimeOut)");
//					button_refresh.setVisibility(View.VISIBLE);
					System.out.println("Response.sendWebRequest != 200");
					refreshInProgress=false;
					System.out.println("Error: " +  response.getResponseMessage());
				}
			} catch (Exception e) {
				System.out.println("HUD not connected - No Internet");
//				Toast.makeText(MainActivity.this, "HUD not connected - No Internet", Toast.LENGTH_LONG).show();
				statusline="HUD not connected - No Internet";
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
				editor.putString("PreviousResult",PreviousResult);
				editor.apply();
				oldLatitude=latitude; oldLongitude=longitude;
				System.out.println("Displaying data...");
				onDisplay(result);
			}
			else {
//				textView.setText("No Internet");
				onDisplay(PreviousResult);
			}
		}

	}

/*	private IReconHttpCallback clientCallback = new IReconHttpCallback() {
		@Override
		public void onReceive(int requestId, ReconHttpResponse response) {
//			textView.setText("Data received...");
			result=new String(response.getBody());
			PreviousResult=result;
			SharedPreferences preferences = getSharedPreferences("com.myweather", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("PreviousResult",PreviousResult);
			editor.apply();
			oldLatitude=latitude; oldLongitude=longitude;
			System.out.println("Displaying data...");
			onDisplay(result);
		}
			
		@Override
		public void onError(int requestId, ERROR_TYPE type, String message) {
//			textView.setText("(TimeOut)");
			button_refresh.setVisibility(View.VISIBLE);
			refreshInProgress=false;
			System.out.println("Error: " + type.toString() + "(" + message + ")");
		}
	};
*/
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
		if(!hasNetworkAccess){
			System.out.println("HUD not connected (onNetworkEvent)- No Internet");
			Toast.makeText(MainActivity.this, "HUD not connected - No Internet", Toast.LENGTH_LONG).show();
//			textView.setText("No Internet");
			statusline="HUD not connected - No Internet";
			onDisplay(PreviousResult);
		}
	}

	private class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// Initialize the location fields
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			System.out.println("Location OnStatusChanged, status="+status);
			Toast.makeText(MainActivity.this, provider + "'s status changed to "+status +"!",
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			System.out.println("Location onProviderDisabled");
			Toast.makeText(MainActivity.this, "Provider " + provider + " enabled!",
					Toast.LENGTH_SHORT).show();

		}

		@Override
		public void onProviderDisabled(String provider) {
			System.out.println("Location onProviderDisabled");
			Toast.makeText(MainActivity.this, "Provider " + provider + " disabled!",
					Toast.LENGTH_SHORT).show();
		}
	}

}
