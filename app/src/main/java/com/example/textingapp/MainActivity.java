package com.example.textingapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Activity activity = this;

    ArrayList<String> textList;
    RecyclerAdapter adapter;
    TextView stateDisplay;

    RecyclerView textLogs;

    SMSReceiver receiver;

    OrderMachine machine;

    String currMessage;

    LinearLayoutManager layoutManager;

    final int REQUEST_CODE_SEND = 14;
    final int REQUEST_CODE_RECEIVE = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateDisplay = findViewById(R.id.id_stateDisplay);

        textLogs = findViewById(R.id.id_textLogs);
        textList = new ArrayList<String>();

        adapter = new RecyclerAdapter(this, textList);
        textLogs.setAdapter(adapter);
        textLogs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        layoutManager = new LinearLayoutManager(this);
        textLogs.setLayoutManager(layoutManager);

        machine = new OrderMachine();

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
            receiver = new SMSReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            registerReceiver(receiver, filter);
        } else {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.RECEIVE_SMS}, REQUEST_CODE_RECEIVE);
        }
    }

    public void sendText (String message) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                SmsManager manager = SmsManager.getDefault();
                String msg = message;
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    manager.sendTextMessage("5556", null, message, null, null);
                    textList.add(message);
                    adapter.notifyItemInserted(textList.size()-1);
                    layoutManager.scrollToPosition(textList.size()-1);
                    if (machine != null) {
                        stateDisplay.setText("State " + machine.getCurrState());
                    }
                } else {
                    currMessage = message;
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_SEND);
                }
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(task, 2500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_SEND) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    sendText(currMessage);
                }
            }
        }
        if (requestCode == REQUEST_CODE_RECEIVE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                    receiver = new SMSReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
                    registerReceiver(receiver, filter);
                }
            }
        }
    }

    public class SMSReceiver extends BroadcastReceiver {

        private final String TAG = SMSReceiver.class.getSimpleName();
        public final String pdu_type = "pdus";

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            SmsMessage[] messages;
            String strMessage = " ";
            String format = bundle.getString("format");
            Object[] pdus = (Object[]) bundle.get(pdu_type);

            messages = new SmsMessage[pdus.length];

            for (int i = 0; i < messages.length; i++) {
                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.M) {
                    // If Android version M or newer:
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    // If Android version L or older:
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }

                strMessage += messages[i].getOriginatingAddress();
                strMessage += ": " + messages[i].getMessageBody() + "\n";

            }

            textList.add(strMessage);
            adapter.notifyItemInserted(textList.size()-1);
            layoutManager.scrollToPosition(textList.size()-1);
            machine.receiveInput(strMessage);
            if (machine != null) {
                stateDisplay.setText("State " + machine.getCurrState());
            }
        }
    }

    public class OrderMachine {

        public String GREETING = "1: Greeting", MAIN_ORDER = "2: Main Order", SIDE_ORDER = "3: Side Order", PRESENT_FOOD = "4: Present Food";

        public String CHEESEBURGER = "Cheeseburger", PIZZA = "Pizza", TACO = "Taco", HOT_DOG = "Hot Dog";
        public String FRIES = "French Fries", NUGGETS = "Chicken Nuggets", TORTILLA = "Tortilla Chips", ONIONRINGS = "Onion Rings";

        private String mainOrder, sideOrder;

        private String currState;

        public OrderMachine () {
            setState(GREETING);
        }

        public void setState (String state) {
            if (state.equals(GREETING)) {
                currState = GREETING;
                greeting();
            }
            else if (state.equals(MAIN_ORDER)) {
                currState = MAIN_ORDER;
                mainOrder();
            }
            else if (state.equals(SIDE_ORDER)) {
                currState = SIDE_ORDER;
                sideOrder();
            }
            else if (state.equals(PRESENT_FOOD)) {
                currState = PRESENT_FOOD;
                presentFood();
            }
        }

        public String getCurrState () {
            return this.currState;
        }

        public void greeting () {
            int randGreeting = (int)(Math.random()*4+1);
            switch (randGreeting) {
                case 1:
                    sendText("Hello! How are you?");
                    break;
                case 2:
                    sendText("Why hello there! How are you doing today?");
                    break;
                case 3:
                    sendText("Yo what's good, broski?");
                    break;
                case 4:
                    sendText("Greetings, fine gentleperson! How are you feeling on this fine day?");
                    break;
            }
        }

        public void mainOrder () {
            sendText("What would you like to order? Your options are:" +
                    "\n1. Cheeseburger" +
                    " 2. Pizza" +
                    " 3. Taco" +
                    " 4. Hot Dog");
        }

        public void sideOrder () {
            sendText("What would you like for your side order?" +
                    "\n1. French Fries" +
                    " 2. Chicken Nuggets" +
                    " 3. Tortilla Chips" +
                    " 4. Onion Rings");
        }

        public void presentFood () {
            int rand = (int)(Math.random()*4+1);
            String message = "";
            switch (rand) {
                case 1:
                    message = "Thank you for choosing our app! Bon appétit!";
                    break;
                case 2:
                    message = "Your meal is on its way. Enjoy!";
                    break;
                case 3:
                    message = "Thanks for choosing us. We hope you enjoy your meal!";
                    break;
                case 4:
                    message = "Your food is on its way. We know that you will love your meal!";
                    break;
            }
            sendText("Your order: " + mainOrder + " with a side of " + sideOrder + "." +
                    "\n" + message);
        }

        public void receiveInput (String input) {
            if (currState == GREETING) {
                receiveGreeting(input);
            }
            else if (currState == MAIN_ORDER) {
                receiveMainOrder(input);
            }
            else if (currState == SIDE_ORDER) {
                receiveSideOrder(input);
            }
        }

        public void receiveGreeting (String str) {
            String input = str.toLowerCase();
            if (input.contains("great") || input.contains("good") || input.contains("wonderful")) {
                int rand = (int)(Math.random()*2+1);
                if (rand == 1) {
                    sendText("That's good to hear!");
                }
                if (rand == 2) {
                    sendText("Wonderful! Glad you're feeling well!");
                }
                setState(MAIN_ORDER);
            }
            else if (input.contains("hello") || input.contains("hi") || input.contains("hey") || input.contains("yo")) {
                setState(MAIN_ORDER);
            }
            else if (input.contains("bad") || input.contains("terrible") || input.contains("horrible")) {
                int rand = (int)(Math.random()*2+1);
                if (rand == 1) {
                    sendText("So sorry to hear that!");
                }
                if (rand == 2) {
                    sendText("That's a bummer. I hope I can do something to make you feel better!");
                }
                setState(MAIN_ORDER);
            } else {
                sendText("Sorry, I didn't quite understand what you were saying.");
            }
        }

        public void receiveMainOrder (String str) {
            String input = str.toLowerCase();
            if (input.contains("cheeseburger") || input.contains("burger")) {
                sendText("You ordered a cheeseburger! Yummm....");
                mainOrder = CHEESEBURGER;
                setState(SIDE_ORDER);
            }
            else if (input.contains("pizza")) {
                sendText("You ordered a slice of pizza. Mmmmm, so good and tasty!");
                mainOrder = PIZZA;
                setState(SIDE_ORDER);
            }
            else if (input.contains("taco")) {
                sendText("You ordered a taco. Sounds delicious!");
                mainOrder = TACO;
                setState(SIDE_ORDER);
            }
            else if (input.contains("hot dog") || input.contains("dog")) {
                sendText("You ordered a hot dog. Mmmmm... so good!");
                mainOrder = HOT_DOG;
                setState(SIDE_ORDER);
            } else {
                sendText("Sorry, I didn't quite catch that. Could you please repeat what you said?");
            }
        }

        public void receiveSideOrder (String str) {
            String input = str.toLowerCase();
            if (input.contains("fries") || input.contains("french fries")) {
                sendText("Oooh, French Fries! We'll give you some ketchup with those too!");
                sideOrder = FRIES;
                setState(PRESENT_FOOD);
            }
            else if (input.contains("nuggets") || input.contains("chicken nuggets")) {
                sendText(mainOrder + " with a side of chicken nuggies? I'm jealous...");
                sideOrder = NUGGETS;
                setState(PRESENT_FOOD);
            }
            else if (input.contains("tortilla") || input.contains("chips")) {
                sendText("Don't forget to get some hot salsa with your chips!");
                sideOrder = TORTILLA;
                setState(PRESENT_FOOD);
            }
            else if (input.contains("onion") || input.contains("rings")) {
                sendText("Onion Rings... my mouth is watering just thinking about it!");
                sideOrder = ONIONRINGS;
                setState(PRESENT_FOOD);
            } else {
                sendText("Sorry, I didn't quite catch that. Could you please repeat what you said?");
            }
        }

    }
}