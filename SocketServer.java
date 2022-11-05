import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.PrintStream;

// SocketServer can be an acceptor or a learner

public class SocketServer extends Thread {
    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private Acceptor acceptor;
    private Learner learner = null;
    private String learner_ip;
    private int learner_port;

    // constructor for an acceptor
        // passing in acceptor to contact acceptor obj for paxos logic
        // passing in port to start the server on
        // passing in learner ip and port to contact learner server once a value has been accepted
    public SocketServer(Acceptor acceptor, int port, String learner_ip, int learner_port) {
        this.port = port;
        this.acceptor = acceptor;
        this.learner_ip = learner_ip;
        this.learner_port = learner_port;
    }

    // constructor for a learner
    public SocketServer(Learner learner, int port) {
        this.port = port;
        this.learner = learner;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
            System.out.println("Server started on port: " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                System.out.println("Listening for a connection");

                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();

                // if learner is not null, this server is for learner so it will be handled by a LearnerHandler
                // otherwise it is for an acceptor and will be handled by a AcceptorHandler
                if (learner != null) {
                    LearnerHandler learnerHandler = new LearnerHandler(socket, learner);
                    learnerHandler.start();
                } else {
                    // Pass the socket to the RequestHandler thread for processing
                    AcceptorHandler acceptorHandler = new AcceptorHandler(socket, acceptor, learner_ip, learner_port);
                    acceptorHandler.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class AcceptorHandler extends Thread {
    private Socket socket;
    private Acceptor acceptor;
    private String learner_ip;
    private int learner_port;

    AcceptorHandler(Socket socket, Acceptor acceptor, String learner_ip, int learner_port) {
        this.socket = socket;
        this.acceptor = acceptor;
        this.learner_ip = learner_ip;
        this.learner_port = learner_port;
    }

    @Override
    public void run() {
        try {
            System.out.println("Server received a connection");

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            int id = Integer.parseInt(in.readLine());
            String uid = in.readLine();

            ProposalID proposalID = new ProposalID(id, uid);
            Promise promise;

            synchronized(this){
                promise = this.acceptor.receivePrepare(uid, proposalID);
            }
            
            if (promise != null) {
                String p_acceptorUID = promise.acceptorUID;
                String p_proposal_number = String.valueOf(promise.proposalID.getNumber());
                String p_proposal_uid = promise.proposalID.getUID();
                String p_previous_number = null;
                String p_previous_uid = null;
                if (promise.previousID != null){
                    p_previous_number = String.valueOf(promise.previousID.getNumber());
                    p_previous_uid = promise.previousID.getUID();
                }
                String p_acceptedValue = String.valueOf(promise.acceptedValue);

                out.println(p_acceptorUID);
                out.println(p_proposal_number);
                out.println(p_proposal_uid);
                out.println(p_previous_number);
                out.println(p_previous_uid);
                out.println(p_acceptedValue);
                out.flush();
            } else {
                System.out.println("prepare fail");
            }

            int a_proposal_number = Integer.parseInt(in.readLine());
            String a_proposal_uid = in.readLine();
            ProposalID a_proposalID = new ProposalID(a_proposal_number, a_proposal_uid);
            int a_value = Integer.parseInt(in.readLine());
            
            AcceptRequest accepted;
            synchronized(this){
                accepted = acceptor.receiveAcceptRequest(a_proposal_uid, a_proposalID, a_value);
            }

            Socket l_socket;
            if (accepted!=null){
                // create a socket to inform learner of the accepted value
                l_socket = new Socket(learner_ip, learner_port);
                // Create input and output streams to read from and write to the server
                PrintStream l_out = new PrintStream(l_socket.getOutputStream());
                // BufferedReader l_in = new BufferedReader(new InputStreamReader(l_socket.getInputStream()));
                l_out.println(acceptor.getAcceptorUID());
                l_out.println(accepted.proposalID.getNumber());
                l_out.println(accepted.proposalID.getUID());
                l_out.println(accepted.proposedValue);
            } else {
                System.out.println("not accepted");
            }

            // Close our connection
            in.close();
            out.close();
            socket.close();

            System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class LearnerHandler extends Thread {
    private Socket socket;
    private Learner learner;

    LearnerHandler(Socket socket, Learner learner) {
        this.socket = socket;
        this.learner = learner;
    }

    @Override
    public void run() {
        try {
            System.out.println("Server received a connection");

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            String acceptor_uid = in.readLine();
            int number = Integer.parseInt(in.readLine());
	        String uid = in.readLine();
            int acceptedValue = Integer.parseInt(in.readLine());

            ProposalID acceptedProposalID = new ProposalID(number, uid);

            System.out.println("learner: " + number);
            System.out.println("learner: " + uid);
            System.out.println("learner: " + acceptedValue);

            AcceptRequest resolution;
            synchronized(this){
                resolution = learner.receiveAccepted(acceptor_uid, acceptedProposalID, acceptedValue);
            }

            if (resolution != null) {
                System.out.println("resolution: " + resolution.proposalID.getNumber());
                System.out.println("resolution: " + resolution.proposalID.getUID());
                System.out.println("resolution: " + resolution.proposedValue);
            } else {
                System.out.println("majority not reached");
            }

            // Close our connection
            in.close();
            out.close();
            socket.close();

            System.out.println("Connection closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}