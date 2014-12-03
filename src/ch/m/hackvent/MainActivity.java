package ch.m.hackvent;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.zxing.integration.android.*;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity {

	class BallGridAdapter extends BaseAdapter {
		private MainActivity mContext;

		public BallGridAdapter(Context c) { mContext = (MainActivity)c; }

		public int getCount() { return 24; }

		public Object getItem(int position) { return null; }

		public long getItemId(int position) { return 0; }

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView = null;
			if (convertView == null) {
				imageView = new ImageView(mContext);
				int size = mContext.getResources().getConfiguration().screenHeightDp / 7;
				imageView.setLayoutParams(new GridView.LayoutParams(size,size+16));
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(0,8,0,8);
			} else {
				imageView = (ImageView) convertView;
			}

			imageView.setImageResource(mContext.balls[position]);
			return imageView;
		}

	}
	private class UpdateBallsTask extends AsyncTask<MainActivity,Void,MainActivity> {
		protected MainActivity doInBackground(MainActivity ... update) {
			int[] newBalls = new int[24];
			for(int i=0; i<24; i++) newBalls[i] = R.drawable.ball_empty;

			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet("http://hackvent.hacking-lab.com/load.php?service=balls&name="+URLEncoder.encode(update[0].user,"UTF-8"));
				HttpResponse resp = httpclient.execute(httpget);
				JSONArray json = new JSONArray(EntityUtils.toString(resp.getEntity()));
				for(int i=0; i<json.length(); i++) {
					JSONObject obj = json.getJSONObject(i);
					int ball = R.drawable.ball_empty;
					switch(obj.getInt("pointsFull")*10+obj.getInt("points")) {
					case 21: ball = R.drawable.ball_green; break;
					case 22: ball = R.drawable.ball_green_plus; break;
					case 43: ball = R.drawable.ball_yellow; break;
					case 44: ball = R.drawable.ball_yellow_plus; break;
					case 54: ball = R.drawable.ball_orange; break;
					case 55: ball = R.drawable.ball_orange_plus; break;
					case 65: ball = R.drawable.ball_red; break;
					case 66: ball = R.drawable.ball_red_plus; break;
					}
					newBalls[obj.getInt("ball")-1] = ball;
					System.out.println(obj.getInt("ball"));
					System.out.println(obj.getInt("pointsFull")*10+obj.getInt("points"));
				}
				
				update[0].balls = newBalls;
			} catch(Exception e){ e.printStackTrace(); }
			return update[0];
		}
		protected void onPostExecute(MainActivity a) {
			((GridView)(a.findViewById(R.id.ballGrid))).invalidateViews();
		}
	}	
	private String user = "";
	private String token = "";
	int[] balls = {
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty,
			R.drawable.ball_empty
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment())
			.commit();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		GridView grid = (GridView)findViewById(R.id.ballGrid);
		grid.setAdapter(new BallGridAdapter(this));
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		if(pref.getString("user", "").isEmpty()) {
			requestLogin();
		} else {
			setLogin(pref.getString("user", ""),pref.getString("token", ""));
		}
	}

	private void setLogin(String user, String token) {
		this.user = user;
		this.token = token;
		TextView status = (TextView)findViewById(R.id.loginStatus);
		status.setText("Welcome, "+user);

		TextView subStatus = (TextView)findViewById(R.id.loginSubStatus);
		subStatus.setText("Ticket = "+token);
		
		SharedPreferences.Editor pref = getPreferences(MODE_PRIVATE).edit();
		pref.putString("user",user);
		pref.putString("token",token);
		pref.apply();

		new UpdateBallsTask().execute(this);
		findViewById(R.id.scan).setEnabled(true);
	}

	private void requestLogin() {
		LoginDialog dlg = new LoginDialog();
		dlg.show(getFragmentManager(),"foo");
	}

	public void setUser(String user) {
		TextView status = (TextView)findViewById(R.id.loginStatus);
		status.setText("Logging in as "+user);

		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://hackvent.hacking-lab.com/register.php");

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("regcode", user));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = httpclient.execute(httppost);
			Header[] cookies = response.getHeaders("Set-Cookie");
			System.out.println(cookies[0].getValue());
			System.out.println(cookies[1].getValue());
			Pattern name = Pattern.compile("HACKvent_User=(.*);");
			Matcher m = name.matcher(cookies[0].getValue());
			if(!m.find()) throw new Exception("wrong user cookie");
			String newuser = m.group(1);

			Pattern ticket = Pattern.compile("HACKvent_Ticket=(.*);");
			m = ticket.matcher(cookies[1].getValue());
			if(!m.find()) throw new Exception("wrong ticket cookie");
			String newticket = m.group(1);

			setLogin(newuser, newticket);
		} catch (Exception e) {
			e.printStackTrace();
			requestLogin();
		}
	}

	public void scanClick(View v) {
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.initiateScan();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://hackvent.hacking-lab.com/load.php?service=solution");
			httppost.addHeader("Cookie","HACKvent_User="+user+"; HACKvent_Ticket="+token);

			try {
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("name", user));
				nameValuePairs.add(new BasicNameValuePair("ticket", token));
				nameValuePairs.add(new BasicNameValuePair("code", scanResult.getContents()));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse resp = httpclient.execute(httppost);
				JSONObject json = new JSONObject(EntityUtils.toString(resp.getEntity()));

				Toast toast = Toast.makeText(getApplicationContext(), json.getString("txt"), Toast.LENGTH_LONG);
				toast.show();
				new UpdateBallsTask().execute(this);
			} catch (Exception e) {
				Toast toast = Toast.makeText(getApplicationContext(), "Submission FAILED!", Toast.LENGTH_LONG);
				toast.show();
			}

			/*TextView foo = (TextView)findViewById(R.id.loginStatus);
			foo.setText(scanResult.getContents());*/
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id==R.id.logout) {
			requestLogin();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}
}
