package com.dunksoftware.seminoletix;

import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterActivity extends Activity {

	private EditText EditCardNumber,
		EditPIN,
		EditEmail,
		EditPassword,
		EditConfirmPass;
	
	private String CardNumber,
		PIN,
		Email,
		Password;
	
	private TextView ErrorMessage;
	
	// This variable handles user registration after form validation
	private UserControl.RegisterUser registerUser;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		// Link all UI widgets to reference variables
		EditCardNumber = (EditText)findViewById(R.id.UI_EditCardNum);
		EditPIN = (EditText)findViewById(R.id.UI_EditPIN);
		EditEmail = (EditText)findViewById(R.id.UI_EditEmail);
		EditPassword = (EditText)findViewById(R.id.UI_EditPassword);
		EditConfirmPass = (EditText)findViewById(R.id.UI_EditConfirmPassword);
		
		// link message box to output error message
		ErrorMessage = (TextView)findViewById(R.id.UI_TextMessage);
		
		// Listener for submit button. Validates form data
		((Button)findViewById(R.id.UI_ButtonConfirm)).setOnClickListener(
				new OnClickListener() {

			boolean formOK = true;
			
			@Override
			public void onClick(View arg0) {
				if( verifyEntries() ) {
					
					// clear error message
					ErrorMessage.setText("");
					
					// compare two passwords for equality
					String confirmPass = EditConfirmPass.getText().toString();
					if( !EditPassword.getText().toString().equals(confirmPass) ) {
						formOK = false;
						
						ErrorMessage.setText("Password entries do not match");
						
						EditPassword.setText("");
						EditConfirmPass.setText("");
					}
					
					// check email for format (contains '@', .com)
					String email = EditEmail.getText().toString();
					if( !email.contains("@")) {
						formOK = false;
						
						ErrorMessage.setText("Expected email format: \"user@server_name.com\"");
					}
					
					// check card number length
					/*if(EditCardNumber.getText().length() != 
							Constants.CARD_NUMBER_LENGTH) {
						formOK = false;
						
						ErrorMessage.setText("Incorrect length on FSU Card Number");
					} Commented out for testing purposes only */
						
					if( formOK ) {
						// capture data from the form
						CardNumber = EditCardNumber.getText().toString();
						PIN = EditPIN.getText().toString();
						Email = EditEmail.getText().toString();
						Password = EditPassword.getText().toString();
						
						// . . . then POST to site
						registerUser = new UserControl.RegisterUser(CardNumber, PIN, Email, Password);
						
						registerUser.execute();
						
						try {
							ErrorMessage.setText( registerUser.get() );
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else {
					
					ErrorMessage.setText("Error: All fields are required to have a value.");
				}
			}
		});
		
		// Listener for clear button. Simply clears out all of the form data
		((Button)findViewById(R.id.UI_ButtonClear)).setOnClickListener(
				new OnClickListener() {
					
					@Override
					public void onClick(View arg0) {
						EditCardNumber.setText("");
						EditPIN.setText("");
						EditEmail.setText("");
						EditPassword.setText("");
						EditConfirmPass.setText("");
						
						// return focus to the first edittext(CardNumber)
						EditCardNumber.requestFocus();
						
						// clear error message
						ErrorMessage.setText("");
					}
				});
		
	}
	
	/***
	 * This function simply checks each EditText box to ensure that
	 * the element does not contain an empty string
	 * @return false -> if at least one box is empty;
	 * true -> is all box entries have some data
	 */
	private boolean verifyEntries() {
		if(EditCardNumber.getText().length() <= 0)
			return false;
		if(EditPIN.getText().length() <= 0)
			return false;
		if(EditEmail.getText().length() <= 0)
			return false;
		if(EditPassword.getText().length() <= 0)
			return false;
		if(EditConfirmPass.getText().length() <= 0)
			return false;
		
		
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_register, menu);
		return true;
	}

}