package com.example.sugertime;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Chat_screen extends AppCompatActivity {

    private MessageAdapter messageAdapter;
    private RecyclerView chat_LAY_chatList;
    private LinearLayoutManager linearLayoutManager;

    private DatabaseReference reference;

    private ImageView chat_IMG_back;
    private ImageView chat_IMG_send;
    private TextInputLayout chat_LBL_writeText;
    private TextView chat_LBL_name;

    private List<Message> messagesList;
    private String username;
    private String sendTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);

        username = getIntent().getStringExtra("sender");
        sendTo = getIntent().getStringExtra("sendTo");

        findView();
        initImage();
        initRecyclerView();
        initButton();

        readName();
        readMessageFromDB(username, sendTo);
    }

    private void initButton() {
        chat_IMG_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        chat_IMG_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = chat_LBL_writeText.getEditText().getText().toString();

                if (!text.equals("")) {
                    sendMessage(username, sendTo, text);
                } else {
                    Toast.makeText(getApplicationContext(), "You can't send empty message!", Toast.LENGTH_SHORT).show();

                }

                chat_LBL_writeText.getEditText().setText("");
            }
        });
    }

    private void initImage() {
        chat_IMG_back.setImageResource(R.drawable.ic_back);
        chat_IMG_send.setImageResource(R.drawable.ic_fork);
    }

    // Init recycle view for chat
    private void initRecyclerView() {
        chat_LAY_chatList.setHasFixedSize(true);
        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        chat_LAY_chatList.setLayoutManager(linearLayoutManager);
    }

    private void findView() {
        chat_LAY_chatList = findViewById(R.id.chat_LAY_chatList);
        chat_IMG_back = findViewById(R.id.chat_IMG_back);
        chat_IMG_send = findViewById(R.id.chat_IMG_send);
        chat_LBL_writeText = findViewById(R.id.chat_LBL_writeText);
        chat_LBL_name = findViewById(R.id.chat_LBL_name);

    }

    // Read previous massage from DB
    private void readMessageFromDB(final String firsUser, final String secondUser) {
        messagesList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats/");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();
                // Find previous massage between two users
                for (DataSnapshot snap : snapshot.getChildren()) {
                    if (snap.getValue(Message.class) != null) {
                        Message message = snap.getValue(Message.class);

                        if (message.getReceiver().equals(secondUser) && message.getSender().equals(firsUser) ||
                                message.getSender().equals(secondUser) && message.getReceiver().equals(firsUser)) {
                            messagesList.add(message);
                        }

                        // Show previous massage with recycle view
                        messageAdapter = new MessageAdapter(getApplicationContext(), messagesList, firsUser);
                        chat_LAY_chatList.setAdapter(messageAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Show the user I correspond with
    private void readName() {
        reference = FirebaseDatabase.getInstance().getReference("Users/").child(sendTo);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);

                // If user is seller show his store name
                if (user.getRole().equals("Seller")) {
                    chat_LBL_name.setText(user.getShopName());
                } else {
                    chat_LBL_name.setText(user.getUserName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Send massage
    private void sendMessage(final String sender, final String receiver, String message) {
        reference = FirebaseDatabase.getInstance().getReference();

        // Save information about massage in Hash Map
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);

        // Save Hash map in DB
        reference.child("Chats").push().setValue(hashMap);

        // Create chat list for user, save the users we talk to in DB
        reference = FirebaseDatabase.getInstance().getReference("ChatList/").child(sender).child(receiver);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    reference.child("id").setValue(receiver);

                    // Create the list for the other side as well
                    reference = FirebaseDatabase.getInstance().getReference("ChatList/").child(receiver).child(sender);

                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                reference.child("id").setValue(sender);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}