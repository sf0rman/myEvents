package no.sforman.myevents;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity implements WarningDialogFragment.WarningListener {

    private static final String TAG = "CreateEventActivity";

    private Intent intent;
    private String eventId;

    // Connectivity
    private boolean connected = true;
    private NoticeFragment noInternetWarning;

    //UI
    private FrameLayout layout;
    private ProgressBar progressBar;
    private MapFragment mapFragment;
    private EditText name;
    private EditText description;
    private EditText startDate;
    private EditText startTime;
    private EditText endDate;
    private EditText endTime;
    private EditText location;
    private EditText reminderDate;
    private EditText reminderTime;
    private CheckBox isOnline;
    private CheckBox hasReminder;
    private TextView nameError;
    private TextView descriptionError;
    private TextView timeError;
    private TextView locationError;
    private TextView reminderError;
    private TextView isOnlineError;
    private FrameLayout onlineImg;
    private Button createEvent;
    private Button deleteEvent;

    // Places
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private LatLng placeLatLng;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    //Event variables
    private String eventOwnerId;
    private String eventName;
    private String eventDescription;
    private String eventLocation;
    private String eventAddress;
    private Calendar reminderCal = Calendar.getInstance();
    private long reminderKey;
    private Calendar startCal = Calendar.getInstance();
    private Calendar endCal = Calendar.getInstance();
    private Calendar today = Calendar.getInstance();
    private GeoPoint eventGeoPoint;

    private DateFormat dateFormat = new SimpleDateFormat("E, dd MMMM yyyy", Locale.getDefault());
    private DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private DateFormat dateTimeFormat = new SimpleDateFormat("E, dd MMMM yyyy @ HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: Lifecycle");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        noInternetWarning = new NoticeFragment(getString(R.string.error_no_internet));

        initUI();

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: Lifecycle");
        super.onStart();
        if (isOnline()) {
            initFirebase();
            initPlaces();
        } else {
            Log.e(TAG, "onCreate: No network");
        }

        intent = getIntent();
        if (intent.hasExtra("eventId")) {
            eventId = intent.getStringExtra("eventId");
            getEventData(eventId);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: Lifecycle");
        super.onResume();
    }

    private boolean isOnline() {
        Log.d(TAG, "isOnline: ");
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        connected = (networkInfo != null && networkInfo.isConnected());
        if (!connected) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.create_event_notice_container, noInternetWarning)
                    .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction().remove(noInternetWarning).commit();
        }
        return connected;
    }

    private void initFirebase() {
        Log.d(TAG, "initFirebase: ");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        eventOwnerId = currentUser.getUid();
    }

    private void initUI() {
        Log.d(TAG, "initUI: ");
        layout = findViewById(R.id.create_event_layout);
        progressBar = findViewById(R.id.create_event_progress);
        name = findViewById(R.id.create_event_name_text);
        startDate = findViewById(R.id.create_event_startdate_text);
        startTime = findViewById(R.id.create_event_starttime_text);
        endDate = findViewById(R.id.create_event_enddate_text);
        endTime = findViewById(R.id.create_event_endtime_text);
        reminderDate = findViewById(R.id.create_event_reminder_date);
        reminderTime = findViewById(R.id.create_event_reminder_time);
        location = findViewById(R.id.create_event_location_text);
        description = findViewById(R.id.create_event_description_text);

        isOnline = findViewById(R.id.create_event_is_online);
        onlineImg = findViewById(R.id.create_event_online_image);
        hasReminder = findViewById(R.id.create_event_add_reminder);

        nameError = findViewById(R.id.create_event_name_error);
        timeError = findViewById(R.id.create_event_time_error);
        reminderError = findViewById(R.id.create_event_reminder_time_error);
        isOnlineError = findViewById(R.id.create_event_is_online_error);
        locationError = findViewById(R.id.create_event_location_error);
        descriptionError = findViewById(R.id.create_event_description_error);

        hasReminder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    reminderDate.setVisibility(View.VISIBLE);
                    reminderTime.setVisibility(View.VISIBLE);
                    reminderError.setVisibility(View.VISIBLE);
                } else {
                    reminderDate.setVisibility(View.GONE);
                    reminderTime.setVisibility(View.GONE);
                    reminderError.setVisibility(View.GONE);
                }
            }
        });

        isOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mapFragment.getView().setVisibility(View.GONE);
                    onlineImg.setVisibility(View.VISIBLE);
                    location.setVisibility(View.GONE);
                    locationError.setVisibility(View.GONE);
                } else {
                    mapFragment.getView().setVisibility(View.VISIBLE);
                    onlineImg.setVisibility(View.GONE);
                    location.setVisibility(View.VISIBLE);
                    locationError.setVisibility(View.VISIBLE);
                }
            }
        });

        createEvent = findViewById(R.id.create_event_submit_button);
        deleteEvent = findViewById(R.id.create_event_delete_button);


    }

    private void initPlaces() {
        Log.d(TAG, "initPlaces: ");
        String mapKey = getString(R.string.events_maps_key);
        mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.create_event_map_fragment);
        Places.initialize(getApplicationContext(), mapKey);
        PlacesClient pClient = Places.createClient(this);
    }

    public void findLocation(View v) {
        Log.d(TAG, "findLocation: ");
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME,
                Place.Field.LAT_LNG, Place.Field.ADDRESS);

        Intent i = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(i, AUTOCOMPLETE_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult: ");
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "onActivityResult: Place" + place.getName() + ", " + place.getLatLng());
                placeLatLng = place.getLatLng();
                eventGeoPoint = new GeoPoint(placeLatLng.latitude, placeLatLng.longitude);
                eventLocation = place.getName();
                eventAddress = place.getAddress();
                if (eventAddress.contains(eventLocation)) {
                    // strip everything up to the first comma and the following space.
                    eventAddress = eventAddress.substring(eventAddress.indexOf(",") + 2);
                }
                // Add line breaks
                location.setText(eventLocation + " " + eventAddress);

                if (mapFragment != null) {
                    mapFragment.setLocationMarker(placeLatLng);
                }
            }
        }
    }

    public void onSubmitEvent(View v) {
        Log.d(TAG, "onSubmitEvent: Submit clicked");
        if (isOnline()) {

            progressBar.setVisibility(View.VISIBLE);
            if (validInput()) {
                if (intent.hasExtra("eventId")) {
                    editEvent();
                } else {
                    createEvent();
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }

        } else {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
        }


    }

    private boolean validInput() {
        Log.d(TAG, "validInput: ");
        clearErrors();
        eventName = name.getText().toString();
        eventDescription = description.getText().toString();
        boolean isOk = true;

        if (eventName.length() < 2) {
            nameError.setText(getString(R.string.error_invalid_input));
            isOk = false;
            Log.e(TAG, "validInput: Name not long enough");
        }
        if (startCal.after(endCal)) {
            timeError.setText(R.string.error_event_end_early);
            isOk = false;
            Log.e(TAG, "validInput: Event ends before it starts");
        }

        if (startCal.before(today)) {
            timeError.setText(R.string.error_event_early);
            isOk = false;
            Log.e(TAG, "validInput: Event starts before today");
        }

        if (!isOnline.isChecked()) {
            if (eventGeoPoint == null) {
                locationError.setText(getString(R.string.error_no_location));
                isOk = false;
                Log.e(TAG, "validInput: No location set");
            }
        }

        if (eventDescription.length() < 5) {
            descriptionError.setText(R.string.error_invalid_input);
            isOk = false;
            Log.e(TAG, "validInput: Description too short");
        }

        if (hasReminder.isChecked()) {
            if (reminderDate.getText().toString().matches("")) {
                reminderError.setText(R.string.error_no_reminder);
                isOk = false;
                Log.e(TAG, "validInput: No reminder set");
            } else if (reminderCal.before(today)) {
                reminderError.setText(R.string.error_reminder_early);
                isOk = false;
                Log.e(TAG, "validInput: Reminder too early " + reminderCal.toString());
            } else if (reminderCal.after(startCal)) {
                reminderError.setText(R.string.error_reminder_late);
                isOk = false;
                Log.e(TAG, "validInput: Reminder too late");
            }
        }

        return isOk;
    }

    private void clearErrors() {
        Log.d(TAG, "clearErrors: ");
        nameError.setText("");
        descriptionError.setText("");
        timeError.setText("");
        locationError.setText("");
        reminderError.setText("");
        isOnlineError.setText("");
    }

    public void setDateTime(View v) {
        Log.d(TAG, "setDateTime: ");
        switch (v.getId()) {
            case R.id.create_event_reminder_date:
                showDatePickerDialog(reminderDate, reminderCal);
                break;

            case R.id.create_event_reminder_time:
                showTimePickerDialog(reminderTime, reminderCal);
                break;

            case R.id.create_event_startdate_text:
                showDatePickerDialog(startDate, startCal);
                break;

            case R.id.create_event_enddate_text:
                showDatePickerDialog(endDate, endCal);
                break;

            case R.id.create_event_starttime_text:
                showTimePickerDialog(startTime, endCal);
                break;

            case R.id.create_event_endtime_text:
                showTimePickerDialog(endTime, endCal);
                break;
        }
    }

    private void showDatePickerDialog(final EditText text, final Calendar cal) {
        Log.d(TAG, "showDatePickerDialog: Open");
        DatePickerFragment datePicker = new DatePickerFragment();
        datePicker.setOnDateChosenListener(new DatePickerFragment.OnDateChosenListener() {
            @Override
            public void onDateChosen(int year, int month, int day) {
                //text.setText(String.format("%02d/%02d/%04d", day, month+1, year));
                cal.set(year, month, day);
                text.setText(dateFormat.format(cal.getTime()));

                if (text == startDate) {
                    showTimePickerDialog(startTime, startCal);
                } else if (text == endDate) {
                    showTimePickerDialog(endTime, endCal);
                } else if (text == reminderDate) {
                    showTimePickerDialog(reminderTime, reminderCal);
                }
            }
        });
        datePicker.show(getSupportFragmentManager(), "DatePicker");
    }

    private void showTimePickerDialog(final EditText text, final Calendar cal) {
        Log.d(TAG, "showTimePickerDialog: Open");
        TimePickerFragment timePicker = new TimePickerFragment();
        timePicker.setOnTimeChosenListener(new TimePickerFragment.OnTimeChosenListener() {
            @Override
            public void onTimeChosen(int hour, int min) {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                text.setText(timeFormat.format(cal.getTime()));

                if (text == startTime) {
                    endCal.setTimeInMillis(startCal.getTimeInMillis());
                    endCal.add(Calendar.HOUR_OF_DAY, 2);
                    endDate.setText(dateFormat.format(endCal.getTime()));
                    endTime.setText(timeFormat.format(endCal.getTime()));
                }

                Log.d(TAG, "onTimeChosen: start: " + startCal.getTime().toString());
                Log.d(TAG, "onTimeChosen: end  : " + endCal.getTime().toString());
                Log.d(TAG, "onTimeChosen: rem  : " + reminderCal.getTime().toString());
            }
        });
        timePicker.show(getSupportFragmentManager(), "TimePicker");
    }


    private void createEvent() {
        Log.d(TAG, "createEventObject: Started");
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create event object
        final Event event;
        final long rid = today.getTimeInMillis();
        if (isOnline.isChecked()) {
            event = new Event(eventName,
                    eventOwnerId,
                    eventDescription,
                    startCal,
                    endCal,
                    0,
                    0,
                    "none",
                    "none",
                    true,
                    rid);
        } else {
            event = new Event(eventName,
                    eventOwnerId,
                    eventDescription,
                    startCal,
                    endCal,
                    eventGeoPoint.getLatitude(),
                    eventGeoPoint.getLongitude(),
                    eventLocation,
                    eventAddress,
                    false,
                    rid);
        }

        // Add event to database
        db.collection("event")
                .add(event)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "onSuccess: Document written with ID: " + documentReference.getId());
                        final String eventId = documentReference.getId();
                        event.addID(eventId);
                        if (hasReminder.isChecked()) {
                            makeReminder(eventId, rid);
                        }


                        db.collection("user")
                                .document(eventOwnerId)
                                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: Got owner information");
                                    DocumentSnapshot userDoc = task.getResult();
                                    String userFirstname;
                                    String userSurname;
                                    String userImage;
                                    String userEmail;
                                    userFirstname = userDoc.getString(Keys.FIRSTNAME_KEY);
                                    userSurname = userDoc.getString(Keys.SURNAME_KEY);
                                    userImage = userDoc.getString(Keys.IMAGE_KEY);
                                    userEmail = userDoc.getString(Keys.EMAIL_KEY);

                                    // Create user map
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("firstname", userFirstname);
                                    userMap.put("surname", userSurname);
                                    userMap.put("email", userEmail);
                                    userMap.put("image", userImage);
                                    userMap.put("rsvp", "going");
                                    userMap.put("reminder", reminderCal);

                                    // Add user to event as going
                                    db.collection("event")
                                            .document(eventId)
                                            .collection("invited")
                                            .document(eventOwnerId)
                                            .set(userMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "onSuccess: Owner added as going");
                                                    Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();

                                                    Log.d(TAG, "onSuccess: SendEventId" + eventId);
                                                    addEventToSelf(eventId, event);

                                                    Intent i = new Intent(CreateEventActivity.this, EventActivity.class);
                                                    i.putExtra("eventId", eventId);
                                                    startActivity(i);
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    finish();
                                                }
                                            });

                                }
                            }
                        });

                    }
                });


    }

    private void addEventToSelf(String eventId, Event event) {
        Log.d(TAG, "addEventToSelf: Recieved eventId " + eventId);
        FirebaseFirestore eDb = FirebaseFirestore.getInstance();
        eDb.collection("user")
                .document(eventOwnerId)
                .collection("event")
                .document(eventId)
                .set(event)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Event added to self");
                        }
                    }
                });
    }

    private void makeReminder(String event, long reminder) {
        Log.d(TAG, "makeReminder: Started");

        Intent i = new Intent(getApplicationContext(), NotificationReceiver.class);
        i.putExtra("id", event);
        i.putExtra("reminder", reminder);
        i.putExtra("message", "You have an upcoming event on " + dateTimeFormat.format(startCal.getTime()));
        i.putExtra("name", eventName);
        i.putExtra("channel", "event");

        PendingIntent nIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                (int) reminder, i,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), nIntent);

        Log.d(TAG, "makeReminder: reminder set for " + reminderCal.getTime());
        Log.d(TAG, "makeReminder: reminderID: " + reminder);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> reminderMap = new HashMap<>();
        reminderMap.put("reminder", reminderCal);

        db.collection("event")
                .document(event)
                .collection("invited")
                .document(eventOwnerId)
                .update(reminderMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: User added as going with reminder");
                    }
                });

    }

    private void getEventData(String id) {
        Log.d(TAG, "getEventData: ");
        hasReminder.setVisibility(View.GONE);
        reminderError.setVisibility(View.GONE);
        deleteEvent.setVisibility(View.VISIBLE);
        createEvent.setText(R.string.btn_edit_event);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference eventRef = db.collection("event").document(id);
        eventRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot eventDoc = task.getResult();
                    if (eventDoc.exists()) {
                        name.setText(eventDoc.getString(Keys.NAME_KEY));
                        description.setText(eventDoc.getString(Keys.DESCRIPTION_KEY));
                        long startInMillis = eventDoc.getLong("start.timeInMillis");
                        startCal.setTimeInMillis(startInMillis);
                        startDate.setText(dateFormat.format(startCal.getTime()));
                        startTime.setText(timeFormat.format(startCal.getTime()));
                        long endInMillis = eventDoc.getLong("end.timeInMillis");
                        endCal.setTimeInMillis(endInMillis);
                        endDate.setText(dateFormat.format(endCal.getTime()));
                        endTime.setText(timeFormat.format(endCal.getTime()));
                        eventGeoPoint = eventDoc.getGeoPoint(Keys.GEOPONT_KEY);
                        eventAddress = eventDoc.getString(Keys.ADDRESS_KEY);
                        eventLocation = eventDoc.getString(Keys.LOCATION_KEY);
                        if (eventAddress.contains(eventLocation)) {
                            // strip everything up to the first comma and the following space.
                            eventAddress = eventAddress.substring(eventAddress.indexOf(",") + 2);
                        }
                        // Add line breaks
                        location.setText(eventLocation + " " + eventAddress);


                        isOnline.setChecked(eventDoc.getBoolean(Keys.ONLINE_KEY));
                        hasReminder.setChecked(false);
                        reminderKey = eventDoc.getLong(Keys.REMINDER_KEY);
                        placeLatLng = new LatLng(eventGeoPoint.getLatitude(), eventGeoPoint.getLongitude());
                        mapFragment.setLocationMarkerNoAnim(placeLatLng);

                    }
                }
            }
        });
    }

    private void editEvent() {
        Log.d(TAG, "createEventObject: Started");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Event event;
        if (isOnline.isChecked()) {
            event = new Event(eventName,
                    eventOwnerId,
                    eventDescription,
                    startCal,
                    endCal,
                    0,
                    0,
                    "none",
                    "none",
                    true,
                    reminderKey);
        } else {
            event = new Event(eventName,
                    eventOwnerId,
                    eventDescription,
                    startCal,
                    endCal,
                    eventGeoPoint.getLatitude(),
                    eventGeoPoint.getLongitude(),
                    eventLocation,
                    eventAddress,
                    false,
                    reminderKey);
        }

        db.collection("event").document(eventId).set(event).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: Document update. ID: " + eventId);
                addEventToSelf(eventId, event);
                Intent updated = new Intent(CreateEventActivity.this, EventActivity.class);
                updated.putExtra("eventId", eventId);
                progressBar.setVisibility(View.INVISIBLE);
                startActivity(updated);
                finish();
            }
        });
    }

    public void deleteEvent(View v) {
        Log.d(TAG, "deleteEvent: Started");

        WarningDialogFragment warning = new WarningDialogFragment(getString(R.string.msg_warning_delete_event), this);
        warning.show(getSupportFragmentManager(), "Warning");
    }

    @Override
    public void onCompleted(boolean b) {
        Log.d(TAG, "onCompleted: ");
        if (b) {
            Log.d(TAG, "onDialogPositiveClick: Accepted");
            final FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("event")
                    .document(eventId)
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: Deleted event id: " + eventId);

                            db.collection("event")
                                    .document(eventId)
                                    .collection("invited")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "onComplete: Got sub collection");

                                                for (QueryDocumentSnapshot subDoc : task.getResult()) {
                                                    String subDocId = subDoc.getId();
                                                    deleteSubCollection(subDocId);
                                                }

                                                Intent main = new Intent(CreateEventActivity.this, MainActivity.class);
                                                progressBar.setVisibility(View.INVISIBLE);
                                                Toast.makeText(CreateEventActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                                                startActivity(main);
                                                finish();
                                            }
                                        }
                                    });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Log.w(TAG, "onFailure: Couldn't delete event", e);
                }
            });
        }
    }

    @Override
    public void onCompleted(boolean b, String email) {
        Log.d(TAG, "onCompleted: ");
    }

    private void deleteSubCollection(final String id) {
        Log.d(TAG, "deleteSubCollection: ");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("event")
                .document(eventId)
                .collection("invited")
                .document(id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: deleted subcollection id: " + id);
                    }
                });
    }
}
