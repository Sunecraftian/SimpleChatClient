package com.example.simplechatclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.util.Log;

import java.beans.PropertyChangeEvent;

import com.example.simplechatclient.databinding.ActivityMainBinding;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements AbstractView {

    public static final String TAG = "MainActivity";


    private ActivityMainBinding binding;

    private DefaultController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        /* Create Controller and Models */

        controller = new DefaultController();
        WebServiceModel model = new WebServiceModel();

        /* Register Activity View and Model with Controller */

        controller.addView(this);
        controller.addModel(model);

        /* Initialize Model to Default Values */

        model.initDefault();

        binding.clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controller.sendDeleteRequest();
            }
        });

        binding.postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = binding.textView.getText().toString();
                JSONObject json = new JSONObject();
                try{
                    json.put("name", WebServiceModel.NAME);
                    json.put("message", message);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                model.sendPostRequest(json.toString());
//                controller.sendPostRequest();
            }
        });

    }

    @Override
    public void modelPropertyChange(final PropertyChangeEvent evt) {

        String propertyName = evt.getPropertyName();
        String propertyValue = evt.getNewValue().toString();

        Log.i(TAG, "New " + propertyName + " Value from Model: " + propertyValue);

        if ( propertyName.equals(DefaultController.ELEMENT_OUTPUT_PROPERTY) ) {

            String oldPropertyValue = binding.output.getText().toString();

            if ( !oldPropertyValue.equals(propertyValue) ) {
                binding.output.setText(propertyValue);
            }

        }

    }

}
