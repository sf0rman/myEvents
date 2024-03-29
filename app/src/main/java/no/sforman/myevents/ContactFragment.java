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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ContactFragment extends Fragment implements UserAdapter.ResponseListener {

    private static final String TAG = "ContactFragment";

    // UI
    private FloatingActionButton fab;
    private ConstraintLayout layout;
    private ProgressBar progressBar;

    private TextView subContacts;
    private TextView subRequests;

    // RecyclerView
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private final ArrayList<User> userList = new ArrayList<>();

    // Firebase
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

        layout = (ConstraintLayout) inflater.inflate(R.layout.fragment_contact, container, false);

        progressBar = layout.findViewById(R.id.contacts_progress_bar);
        subContacts = layout.findViewById(R.id.contacts_sub_menu_contacts);
        subRequests = layout.findViewById(R.id.contacts_sub_menu_requests);


        initFab();
        initFire();

        return layout;
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: Lifecycle");
        super.onStart();
        userList.clear();
        getContacts();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: Lifecycle");
        super.onResume();
    }

    private void initFire() {
        Log.d(TAG, "initFire: ");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "initFire: No user");
            Intent i = new Intent(getContext(), LoginActivity.class);
            startActivity(i);
        } else {
            userId = currentUser.getUid();
        }

    }

    private void initFab() {
        Log.d(TAG, "initFab: ");
        fab = layout.findViewById(R.id.contacts_add_contact_fab);
    }

    private void getContacts() {
        Log.d(TAG, "getContacts: ");
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore contactDb = FirebaseFirestore.getInstance();

        Log.d(TAG, "getContacts: Getting contacts...");
        // Get all friends
        contactDb.collection("user")
                .document(userId)
                .collection(Keys.FRIEND_KEY)
                .orderBy(Keys.FIRSTNAME_KEY)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            Log.d(TAG, "onComplete: Got contacts");

                            // For each person, add to RecyclerView
                            for (QueryDocumentSnapshot userDoc : task.getResult()) {
                                String id = userDoc.getId();
                                Log.d(TAG, "onComplete: Got document: " + id);
                                String firstname = userDoc.getString(Keys.FIRSTNAME_KEY);
                                String surname = userDoc.getString(Keys.SURNAME_KEY);
                                String email = userDoc.getString(Keys.EMAIL_KEY);
                                String image = userDoc.getString(Keys.IMAGE_KEY);

                                User u = new User(id,
                                        firstname,
                                        surname,
                                        email,
                                        image);

                                userList.add(u);
                            }

                            initRecyclerView();
                            progressBar.setVisibility(View.INVISIBLE);

                        } else {
                            Log.e(TAG, "onComplete: Couldn't get contacts", task.getException());
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });

    }

    private void getRequests() {
        Log.d(TAG, "getRequests: ");
        progressBar.setVisibility(View.VISIBLE);
        final FirebaseFirestore requestDb = FirebaseFirestore.getInstance();

        Log.d(TAG, "getRequests: Getting requests...");
        requestDb.collection(Keys.REQUEST_KEY)
                .whereEqualTo(Keys.RECIEVER_KEY, userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                final String senderId = doc.getString(Keys.SENDER_KEY);
                                final String requestId = doc.getId();

                                Log.d(TAG, "onComplete: Got sender key: " + senderId);
                                FirebaseFirestore senderRef = FirebaseFirestore.getInstance();
                                senderRef.collection(Keys.USER_KEY)
                                        .document(senderId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot senderDoc = task.getResult();
                                                    String firstname = senderDoc.getString(Keys.FIRSTNAME_KEY);
                                                    String surname = senderDoc.getString(Keys.SURNAME_KEY);
                                                    String email = senderDoc.getString(Keys.EMAIL_KEY);
                                                    String image = senderDoc.getString(Keys.IMAGE_KEY);

                                                    User u = new User(senderId, firstname, surname, email, image);
                                                    userList.add(u);
                                                    initRequestView(requestId);
                                                    Log.d(TAG, "onComplete: Input RequestId: " + requestId);
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                } else {
                                                    Log.e(TAG, "onComplete: Something went wrong", task.getException());
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.e(TAG, "onComplete: Something went wrong", task.getException());
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }


    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: ");
        recyclerView = layout.findViewById(R.id.contacts_recycler_view);
        adapter = new UserAdapter(getContext(), userList);
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
    }

    private void initRequestView(String requestId) {
        Log.d(TAG, "initRequestView: ");
        recyclerView = layout.findViewById(R.id.contacts_recycler_view);
        adapter = new UserAdapter(getContext(), userList, "request", requestId, this);
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
    }

    public void getMyContacts() {
        Log.d(TAG, "getMyContacts: ");
        subContacts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        subContacts.setTextColor(ContextCompat.getColor(getContext(), R.color.lightPrimary));
        subRequests.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        subRequests.setTextColor(ContextCompat.getColor(getContext(), R.color.lightSecondary));

        Log.d(TAG, "getMyContacts: Started");

        userList.clear();
        getContacts();
    }

    public void getMyRequests() {
        Log.d(TAG, "getMyRequests: ");
        subContacts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        subContacts.setTextColor(ContextCompat.getColor(getContext(), R.color.lightSecondary));
        subRequests.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        subRequests.setTextColor(ContextCompat.getColor(getContext(), R.color.lightPrimary));

        Log.d(TAG, "getRequests: Started");

        userList.clear();
        getRequests();
    }

    public void addContact() {
        Log.d(TAG, "addContact: ");
        Intent add = new Intent(getContext(), AddContactActivity.class);
        startActivity(add);
    }

    @Override
    public void respondToRequest(final String rId, boolean wasAccepted) {
        Log.d(TAG, "respondToRequest: ");
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (wasAccepted) {
            Log.d(TAG, "respondToRequest: Accepted: " + rId);
            db.collection(Keys.REQUEST_KEY)
                    .document(rId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot doc = task.getResult();
                                String sender = doc.getString(Keys.SENDER_KEY);
                                Log.d(TAG, "onComplete: Got sender id: " + sender);
                                addFriend(rId, sender);
                            }
                        }
                    });


        } else {

            Log.d(TAG, "respondToRequest: Removed: " + rId);
            db.collection(Keys.REQUEST_KEY)
                    .document(rId)
                    .delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "onComplete: Removed request!");
                                getMyRequests();
                            }
                        }
                    });
        }
    }

    private void addFriend(final String rId, final String id) {
        Log.d(TAG, "addFriend: ");
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        final Map<Object, String> self = new HashMap<>();
        final Map<Object, String> sender = new HashMap<>();

        db.collection(Keys.USER_KEY)
                .document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            sender.put(Keys.FIRSTNAME_KEY, doc.getString(Keys.FIRSTNAME_KEY));
                            sender.put(Keys.SURNAME_KEY, doc.getString(Keys.SURNAME_KEY));
                            sender.put(Keys.EMAIL_KEY, doc.getString(Keys.EMAIL_KEY));
                            sender.put(Keys.IMAGE_KEY, doc.getString(Keys.IMAGE_KEY));
                            Log.d(TAG, "addFriend: sender " + sender);


                            db.collection(Keys.USER_KEY)
                                    .document(userId)
                                    .collection(Keys.FRIEND_KEY)
                                    .document(id)
                                    .set(sender)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "onComplete: Friend added to self");

                                            }
                                        }
                                    });
                        }
                    }
                });

        db.collection(Keys.USER_KEY)
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot doc = task.getResult();
                            self.put(Keys.FIRSTNAME_KEY, doc.getString(Keys.FIRSTNAME_KEY));
                            self.put(Keys.SURNAME_KEY, doc.getString(Keys.SURNAME_KEY));
                            self.put(Keys.EMAIL_KEY, doc.getString(Keys.EMAIL_KEY));
                            self.put(Keys.IMAGE_KEY, doc.getString(Keys.IMAGE_KEY));
                            Log.d(TAG, "addFriend: self: " + self);


                            db.collection(Keys.USER_KEY)
                                    .document(id)
                                    .collection(Keys.FRIEND_KEY)
                                    .document(userId)
                                    .set(self)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "onComplete: self added to " + id + " friends");
                                            // Using decline method to delete request.
                                            respondToRequest(rId, false);
                                        }
                                    });
                        }
                    }
                });


    }

    @Override
    public void selectedUsers(ArrayList<User> users) {
        Log.d(TAG, "selectedUsers: ");
        // Select users from contact list.
    }
}
