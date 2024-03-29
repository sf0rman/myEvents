package no.sforman.myevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;

class EventsFragment extends Fragment {

    private static final String TAG = "EventsFragment";
    private final Calendar today = Calendar.getInstance();

    //UI
    private FloatingActionButton fab;
    private ConstraintLayout layout;
    private ProgressBar progressBar;
    private TextView subAll;
    private TextView subMine;

    //RecyclerView
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ArrayList<Event> eventList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Lifecycle");
        if (container == null) {
            return null;
        }
        layout = (ConstraintLayout) inflater.inflate(R.layout.fragment_event, container, false);

        progressBar = layout.findViewById(R.id.event_fragment_progressbar);
        subAll = layout.findViewById(R.id.event_fragment_submenu_all);
        subMine = layout.findViewById(R.id.event_fragment_submenu_mine);

        initFab();

        return layout;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: Lifecycle");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: Lifecycle");
        super.onResume();
        initFire();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop: Lifecycle");
        super.onStop();
    }


    private void initFire() {
        Log.d(TAG, "initFire: ");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            getEvents();
        } else {
            Intent noUser = new Intent(getContext(), LoginActivity.class);
            startActivity(noUser);
        }
        Log.d(TAG, "initFire: Got User ID: " + userId);
    }

    private void getEvents() {
        Log.d(TAG, "getEvents: ");
        eventList.clear();
        progressBar.setVisibility(View.VISIBLE);
        db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(userId)
                .collection("event")
                .orderBy(Keys.START_KEY, Query.Direction.ASCENDING)
                .startAt(today.getTimeInMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            eventList.clear();
                            Log.d(TAG, "onComplete: Got events");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "onComplete: event" + document.toString());
                                String id = document.getId();
                                String name = document.getString("name");
                                String owner = document.getString("owner");
                                String description = document.getString("description");
                                Calendar start = Calendar.getInstance();
                                Calendar end = Calendar.getInstance();
                                long stime = document.getLong("start");
                                long etime = document.getLong("end");
                                start.setTimeInMillis(stime);
                                end.setTimeInMillis(etime);
                                GeoPoint geoPoint = document.getGeoPoint("geoPoint");
                                String location = document.getString("location");
                                String address = document.getString("address");
                                boolean isOnline = document.getBoolean("online");
                                long reminderKey = document.getLong("reminderKey");

                                Log.d(TAG, "onComplete: Creating event id: " + id);
                                Event e = new Event(name,
                                        owner,
                                        description,
                                        stime,
                                        etime,
                                        geoPoint.getLatitude(),
                                        geoPoint.getLongitude(),
                                        location,
                                        address,
                                        isOnline,
                                        reminderKey);

                                e.addID(id);
                                eventList.add(e);
                            }
                            Log.d(TAG, "Events" + eventList.toString());
                            initRecyclerView();
                            progressBar.setVisibility(View.INVISIBLE);
                        }

                    }
                });

    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: ");
        recyclerView = layout.findViewById(R.id.events_fragment_recycler_view);
        adapter = new EventsAdapter(getContext(), eventList);
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
    }

    private void initFab() {
        Log.d(TAG, "initFab: ");
        fab = layout.findViewById(R.id.event_add_event_fab);

    }

    public void getMyEvents() {
        Log.d(TAG, "getMyEvents: ");
        subAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        subAll.setTextColor(ContextCompat.getColor(getContext(), R.color.lightSecondary));
        subMine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        subMine.setTextColor(ContextCompat.getColor(getContext(), R.color.lightPrimary));
        eventList.clear();
        progressBar.setVisibility(View.VISIBLE);
        db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(userId)
                .collection("event")
                .whereEqualTo(Keys.OWNER_KEY, userId)
                .orderBy(Keys.START_KEY, Query.Direction.ASCENDING)
                .startAt(today.getTimeInMillis())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Got events");
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "onComplete: event" + document.toString());
                                String id = document.getId();
                                String name = document.getString("name");
                                String owner = document.getString("owner");
                                String description = document.getString("description");
                                Calendar start = Calendar.getInstance();
                                Calendar end = Calendar.getInstance();
                                long stime = document.getLong("start");
                                long etime = document.getLong("end");
                                start.setTimeInMillis(stime);
                                end.setTimeInMillis(etime);
                                GeoPoint geoPoint = document.getGeoPoint("geoPoint");
                                String location = document.getString("location");
                                String address = document.getString("address");
                                boolean isOnline = document.getBoolean("online");
                                long reminderKey = document.getLong("reminderKey");

                                Log.d(TAG, "onComplete: Creating event id: " + id);
                                Event e = new Event(name,
                                        owner,
                                        description,
                                        stime,
                                        etime,
                                        geoPoint.getLatitude(),
                                        geoPoint.getLongitude(),
                                        location,
                                        address,
                                        isOnline,
                                        reminderKey);

                                e.addID(id);
                                eventList.add(e);
                            }
                            Log.d(TAG, "Events" + eventList.toString());
                            initRecyclerView();
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
    }

    public void getAllEvents() {
        Log.d(TAG, "getAllEvents: ");
        subAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        subAll.setTextColor(ContextCompat.getColor(getContext(), R.color.lightPrimary));
        subMine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        subMine.setTextColor(ContextCompat.getColor(getContext(), R.color.lightSecondary));
        getEvents();
    }


}
