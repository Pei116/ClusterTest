package com.unoceros.cluster;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import io.scalecube.cluster.Cluster;
import io.scalecube.transport.Message;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initCluster();
    }

    private void initCluster() {
        new Thread(() -> {

            // Start seed node
            Cluster seedNode = Cluster.joinAwait();

            // Listen for gossips on member A
            seedNode.listenGossips().subscribe(gossip ->
                    Log.d("Seed", "Heard gossip: " + gossip.data()));

            // Join to cluster another member B
            Cluster clusterA = Cluster.joinAwait(seedNode.address());
            Log.d("Member A", "Joined");
            Cluster clusterB = Cluster.joinAwait(seedNode.address());
            Log.d("Member B", "Joined");

            // Spread gossip from another member
            clusterB.spreadGossip(
                    Message.builder()
                            .data(new Greetings("Greetings from ClusterMember B"))
                            .build()
            );

            // Listen for greetings messages
            clusterA.listen().filter(message-> {
                return message.data() instanceof Greetings;
            }).subscribe(message-> {
                Log.d("Member A", "Received: " + message.data());
            });

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.getLocalizedMessage();
            }

            // Send greetings message to other members
            Message greetingsMessage = Message.builder()
                    .data(new Greetings("Greetings from Member B"))
                    .build();

            clusterB.otherMembers().forEach(member->{
                clusterB.send(member, greetingsMessage);
            });
            Log.d("Member B", "Sent greetings: " + clusterB.otherMembers().size());

            Cluster.joinAwait().listenMembership()
                    .subscribe(event ->
                            Log.d("Seed", "Alice received membership: " + event)
                    );

        }).start();
    }

    public class Greetings {
        String message;

        public Greetings(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
