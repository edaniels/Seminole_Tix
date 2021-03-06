package com.dunksoftware.seminoletix;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class ListActivity extends Activity {

	public static String userName = "";

	private JSONObject[] GameObjects = null;
	private JSONArray gamesArray = null;
	private AdditionDetailsListener mDetailsListener = null;

	private GetGames games;
	private GetCurrentUser getCurrentUser;
	private String response = "ERROR!";

	/*
	 *  A variable that lets functions know which game has been inquired
	 *  about. When a "More details" button is clicked, that JSONArray element
	 *  location will be recorded into this variable. ( -1 is default value ) 
	 */
	private int inquiredGame = -1;

	private String homeTeam,
	awayTeam,
	sportType,
	date,
	selectedGameID;

	CharSequence finalDetailString;

	private int remainingSeats = 0;

	private TableLayout mainTable;

	private final int MESSAGE = 200, 
			DETAILS_POPUP = 250,
			NO_MORE_SEATS_POPUP = 300;

	@SuppressLint("DefaultLocale")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);

		TextView welcomeMsg = (TextView)findViewById(R.id.UI_GreetingText);
		
		((Button)findViewById(R.id.ViewReservedBtn)).setOnClickListener(
				new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				
				startActivity(new Intent(getApplicationContext(),
						ShowGamesList.class));
			}
		});

		// set up logout button
		((Button)findViewById(R.id.UI_LogoutBtn)).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						UserControl UC = new UserControl();
						UserControl.Logout logout = UC.new Logout();

						// begin the logout process
						logout.execute();

						// wait for the server's results
						try {
							JSONObject json = new JSONObject(logout.get());

							String message = json.getString("success");

							// check to see if user has successfully logged out
							if(message.equals("true")) {
								Toast.makeText(getApplicationContext(), 
										"You have been logged out.", Toast.LENGTH_LONG)
										.show();
							}
							else {
								Toast.makeText(getApplicationContext(), 
										"You are not logged in.", Toast.LENGTH_LONG)
										.show();
							}

							finish();

							startActivity(new Intent(getApplicationContext(),
									LoginActivity.class));

						} catch (JSONException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				});

		homeTeam = awayTeam = sportType = date = "";

		// general initialization
		mainTable = (TableLayout)findViewById(R.id.UI_MainTableLayout);
		mDetailsListener = new AdditionDetailsListener();

		// get the name of the user that is currently logged in
		getCurrentUser = new GetCurrentUser();
		getCurrentUser.execute();

		try {
			JSONObject userInfoObject = new JSONObject(getCurrentUser.get());

			userName = userInfoObject.getJSONObject("name").getString("first") 
					+ " " + 
					userInfoObject.getJSONObject("name").getString("last");

			String constructHeader = "Welcome, " + userName;

			welcomeMsg.setText(constructHeader);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		games = new GetGames();

		try {
			games.execute();
			response = games.get();

			// Show the pop-up box (for display testing)
			//showDialog(MESSAGE);

			gamesArray = new JSONArray(response);

			// Allocate space for all JSON objects embedded in the JSON array
			GameObjects = new JSONObject[gamesArray.length()];

			// Transfer each object in this JSONArray into its own object
			for(int i=0;i<gamesArray.length();i++)
				GameObjects[i] = gamesArray.getJSONObject(i);

			/*
			 *  give every game a button for displaying additional
			 *  details about that game. (This button will also be
			 *  used to register for games
			 */

			Button[] detailsButtons = 
					new Button[ gamesArray.length()];


			for(int i = 0; i < gamesArray.length(); i++) {
				TableRow gameTableRow = new TableRow(this);
				LinearLayout list = new LinearLayout(this);

				// Initialize each button and give it a reference id (i)
				detailsButtons[i] = new Button(this);
				detailsButtons[i].setText("More Details");
				detailsButtons[i].setId(i);
				detailsButtons[i].setOnClickListener(mDetailsListener);

				TextView[] info = new TextView[4];

				// set the list to a top down look
				list.setOrientation(LinearLayout.VERTICAL);


				info[0] = new TextView(this);
				info[0].setText("\tSport:\t\t" + GameObjects[i].getString("sport"));

				list.addView(info[0]);

				info[1] = new TextView(this);

				//Format the date so that it is appropriate
				String dateTime = GameObjects[i].getString("date");

				String date = FormatDate(dateTime);

				info[1].setText("\tGame Date:\t\t" + date);

				list.addView(info[1]);

				info[2] = new TextView(this);
				info[2].setText("\tHome Team:\t\t" + GameObjects[i].getJSONObject("teams")
						.getString("home").toUpperCase());

				list.addView(info[2]);

				info[3] = new TextView(this);
				info[3].setText("\tAgainst:\t\t" + GameObjects[i].getJSONObject("teams")
						.getString("away").toUpperCase());

				list.addView(info[3]);

				// add the button to the display details for each game
				// might have to add tag to button
				list.addView(detailsButtons[i]);

				list.setPadding(0, 5, 0, 20);
				gameTableRow.addView(list);
				gameTableRow.setBackgroundResource(R.drawable.img_gloss_background);

				mainTable.addView(gameTableRow);
			}
		} catch(InterruptedException ex) {
			Log.w("List Activity - mGetTable.execute()", ex.getMessage());
		} catch(ExecutionException ex) {
			Log.w("List Activity - mGetTable.execute()", ex.getMessage());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	String FormatDate(String Date) {
		String[] splits = Date.split("T");

		splits = splits[0].split("-");

		Date d = new Date(Integer.parseInt(splits[0]) - 1900, 
				Integer.parseInt(splits[1]), Integer.parseInt(splits[2])); 
		DateFormat newDate = DateFormat.getDateInstance(DateFormat.LONG); 
		newDate.format(d);

		return DateFormat.getDateInstance(DateFormat.LONG).format(d);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_list, menu);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog.Builder builder = null;

		switch( id ) {

		case MESSAGE: {
			builder = new AlertDialog.
					Builder(this);

			builder.setCancelable(false).setTitle("Page Result").
			setMessage(response).setNeutralButton("Close", 
					new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					/* onclick closes the dialog by default, unless code is
					 * added here
					 */
				}
			});
			break;
		}

		case DETAILS_POPUP:
			builder = new AlertDialog.Builder(this);

			// set up the URL for the map (map uses browser)
			TextView mapURL = new TextView(this);
			mapURL.setText(Html.fromHtml("<a href=http://tinyurl.com/bwvhpsv>" +
					"<i>View a Map of the Stadium</i></a><br /><br />"));
			mapURL.setTextSize(20);

			// make URL active
			mapURL.setMovementMethod(LinkMovementMethod.getInstance());

			builder.setCancelable(false).setTitle(
					Html.fromHtml("<b>Ticket Details</b>")).
					setMessage(finalDetailString).setNeutralButton("Close", 
							new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							/* onclick closes the dialog by default, unless code is
							 * added here
							 */
						}
					}).setPositiveButton("Reserve It!", 
							new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							/* 
							 * if all seats are gone, show error. Else, reserve
							 * the selected ticket
							 * 
							 * (remaining seats was set in the "ShowDetails"
							 * function)
							 */
							if( remainingSeats == 0)
								showDialog(NO_MORE_SEATS_POPUP);
							else {
								ReserveTicket reserve = new ReserveTicket();

								reserve.execute(selectedGameID);

								// navigate to the list of currently reserved tickets
								startActivity(new Intent(getApplication(),
										ShowGamesList.class));

							}
						}
					}).setView(mapURL);	// add link at the bottom
			break;

		case NO_MORE_SEATS_POPUP:
			builder = new AlertDialog.Builder(this);

			builder.setCancelable(false)
			.setTitle("An Error Occurred During Reservation")
			.setNeutralButton("Return to List", null)
			.setMessage("It appears that no more seats are" +
					" available at the moment.\n\nPlease choose" +
					" another game.");
			break;
		}

		if( builder != null) {
			// now show the dialog box once it's completed
			builder.create().show();
		}
		return super.onCreateDialog(id);
	}

	/**
	 * This function will set the data for the necessary variables
	 * needed to correctly display a details dialog box for the chosen
	 * game.
	 * @param gameIndex - Signifies which game (index) was chosen
	 */
	@SuppressWarnings("deprecation")
	public void showDetails() {

		// get the corresponding object for the desired game
		JSONObject selectedGame = GameObjects[inquiredGame];

		//set up the information variables
		try {
			selectedGameID = selectedGame.getString("_id");

			homeTeam = selectedGame.getJSONObject("teams")
					.getString("home");

			awayTeam = selectedGame.getJSONObject("teams")
					.getString("away");			

			sportType = selectedGame.getString("sport");

			date = selectedGame.getString("date");

			// format the date
			date = FormatDate(date);

			remainingSeats = selectedGame.getInt("seatsLeft");

		} catch (JSONException e) {
			e.printStackTrace();
		}

		// format the resulting info string using HTML :) (New skill acquired)
		finalDetailString = Html.fromHtml(
				"<img src=img_" + homeTeam + ">\t\t" + 
						"<b>V.S</b>\t\t" +
						"<img src=img_" + awayTeam + "> " +
						"<br />" + "<br />" +
						"<b>Sport: </b>" + sportType +
						"<br />" + "<br />" +
						"<b>Seats Remaining: </b>" + remainingSeats +
						"<br />" + "<br />" +
						"<b>Event Date: </b>" + date

						,new ImageGetter() {
					@Override public Drawable getDrawable(String source) {
						Drawable drawFromPath;
						int path =
								ListActivity.this.getResources().getIdentifier(source, "drawable",
										"com.dunksoftware.seminoletix");
						drawFromPath = (Drawable) ListActivity.this.getResources()
								.getDrawable(path);

						drawFromPath.setBounds(0, 0, 
								drawFromPath.getIntrinsicWidth(),
								drawFromPath.getIntrinsicHeight());

						return drawFromPath;
					}
				}, null);// link to map of game arena
		showDialog(DETAILS_POPUP);
	}

	/**
	 * After determining what game was inquired about, this class
	 * will draw up a Dialog box that will give a full list of info
	 * concerning that game. After reviewing the terms of the game, users
	 * can either return to the list of games or reserve a ticket to the
	 * current game being viewed
	 */
	class AdditionDetailsListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {

			Button btn_viewGame = (Button)v;

			// find out which list array element was inquired about and set it
			inquiredGame = btn_viewGame.getId();

			showDetails();
		}
	}

	class GetCurrentUser extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {

			// Create a new HttpClient and Post Header
			MyHttpClient client = new MyHttpClient(null);

			//sets cookie
			client.setCookieStore(UserControl.mCookie);

			// Prepare a request object
			HttpGet httpget = new HttpGet(Constants.CurrentUserAddress); 

			// Execute the request
			HttpResponse response=null;

			// return string
			String returnString = null;

			try {
				// Open the web page.
				response = client.execute(httpget);
				returnString = EntityUtils.toString(response.getEntity());

			}
			catch (IOException  ex) {
				// Connection was not established
				returnString = "Connection failed; " + ex.getMessage();
			}
			return returnString;
		}
	}

	class GetGames extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {

			// Create a new HttpClient and Post Header
			MyHttpClient client=new MyHttpClient(null);

			//sets cookie
			client.setCookieStore(UserControl.mCookie);
			// Prepare a request object
			HttpGet httpget = new HttpGet(Constants.GamesAddress); 

			// Execute the request
			HttpResponse response=null;

			// return string
			String returnString = null;

			try {
				// Open the web page.
				response = client.execute(httpget);
				returnString = EntityUtils.toString(response.getEntity());

			}
			catch (IOException  ex) {
				// Connection was not established
				returnString = "Connection failed; " + ex.getMessage();
			}
			return returnString;
		}
	}
}
