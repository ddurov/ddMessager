package com.eviger;

import static com.eviger.z_globals.*;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.regex.Pattern;

public class changeName extends AppCompatActivity {

    boolean inAnotherActivity = false, activatedMethodUserLeaveHint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_name);

        EditText newName = findViewById(R.id.newName_changeName);
        Button checkEmail = findViewById(R.id.checkEmail_changeName);

        checkEmail.setOnClickListener(v -> {

            if (!hasConnection(getApplicationContext())) {
                Toast.makeText(getApplicationContext(), "Отсутствует подключение к интернету", Toast.LENGTH_LONG).show();
                return;
            }

            if (newName.getText().toString().length() <= 6 || newName.getText().toString().length() >= 128) {
                Toast.makeText(getApplicationContext(), "Имя должен быть больше 6 и меньше 128 символов", Toast.LENGTH_LONG).show();
                return;
            }

            if (Pattern.compile("^e?id+[\\d]+").matcher(newName.getText().toString()).find()) {
                Toast.makeText(getApplicationContext(), "Имя не должно содержать в себе id или eid", Toast.LENGTH_LONG).show();
                return;
            }

            try {

                JSONObject parametersPostRequest = new JSONObject();
                parametersPostRequest.put("newName", newName.getText().toString());
                parametersPostRequest.put("email", z_globals.myProfile.getString("email"));

                JSONObject postResponse_changeName = new JSONObject(executeApiMethodPost("user", "changeName", parametersPostRequest));

                if (postResponse_changeName.getString("status").equals("ok")) {

                    JSONObject postResponse_requestEmailCode = new JSONObject(requestEmailCode(z_globals.myProfile.getString("email")));

                    if (postResponse_requestEmailCode.getString("status").equals("ok") || postResponse_requestEmailCode.getJSONObject("response").getString("message").equals("code has already been requested")) {

                        inAnotherActivity = true;
                        Intent in = new Intent(changeName.this, emailConfirm.class);
                        in.putExtra("type", "changeName");
                        in.putExtra("newName", newName.getText().toString().trim());
                        in.putExtra("email", z_globals.myProfile.getString("email"));
                        in.putExtra("hashCode", postResponse_requestEmailCode.getJSONObject("response").getString("hash"));
                        startActivity(in);

                    } else {

                        switch (postResponse_requestEmailCode.getJSONObject("response").getString("message")) {
                            default:
                                Toast.makeText(getApplicationContext(), postResponse_requestEmailCode.getJSONObject("response").getString("message"), Toast.LENGTH_LONG).show();
                                break;
                        }

                    }

                } else {

                    switch (postResponse_changeName.getJSONObject("response").getString("message")) {
                        case "user with provided newName already registered":
                            Toast.makeText(getApplicationContext(), "Такое имя уже зарегистрировано", Toast.LENGTH_LONG).show();
                            break;

                        case "email not found":
                            Toast.makeText(getApplicationContext(), "Почта не зарегестрирована", Toast.LENGTH_LONG).show();
                            break;

                        default:
                            Toast.makeText(getApplicationContext(), postResponse_changeName.getJSONObject("response").getString("message"), Toast.LENGTH_LONG).show();
                            break;
                    }

                }

            } catch (Exception ex) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                    writeErrorInLog(ex);
                });
            }

        });

    }

    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!inAnotherActivity) {
            sendingOnline = false;
            activatedMethodUserLeaveHint = true;
        }
    }

    protected void onResume() {
        super.onResume();
        if (activatedMethodUserLeaveHint) {
            setOnline();
            sendingOnline = true;
            inAnotherActivity = false;
            activatedMethodUserLeaveHint = false;
        }
    }

}