import java.util.HashSet;

public class Proposer {

	protected Messenger messenger; // implemeted using Sockets
	protected String proposerUID; // proposerUIDs will be the councilors' member order - M1 will have UID 1, etc.
	protected final int quorumSize; // quorumSize is 5 because we have 9 Acceptors so 5 is majority

	protected ProposalID proposalID; // all Proposal will have their unique identifier start from 0
	protected int proposedValue = -1; // proposedValue will be each proposing councilor's UID since they are proposing
										// themselves to be elected as president
	protected ProposalID lastAcceptedID = null;
	protected HashSet<String> promisesReceived = new HashSet<String>(); // using HeshSet here to ensure a unique value
																		// list

	public Proposer(String proposerUID, int quorumSize) {
		this.proposerUID = proposerUID;
		this.quorumSize = quorumSize;
		this.proposalID = new ProposalID(0, proposerUID); // proposal id is a combo of a locally incrementing identifier
															// and the proposer UID
	}

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}

	public void setProposal(int value) {
		if (proposedValue == -1)
			proposedValue = value;
	}

	public void prepare(String[] ips, int[] ports) {
		promisesReceived.clear(); // promises from previous proposals do not carry over, so reset promisesReceived

		// choose n > n_p
		proposalID.incrementNumber();

		// send prepare(n) to all servers including self
		for (int i = 0; i < ips.length; i++) {
			messenger.sendPrepare(this, proposalID, ips[i], ports[i]);
		}
	}

	// Acceptors returns a prevAcceptedID and a prevAcceptedValue if it has accepted
	// a proposal. Else these will be null
	public AcceptRequest receivePromise(String fromUID, ProposalID proposalID, ProposalID prevAcceptedID, int prevAcceptedValue) {
		if (!proposalID.equals(this.proposalID) || promisesReceived.contains(fromUID)) {
			return null;
		}

		promisesReceived.add(fromUID);

		// v’ = v_a with highest n_a; choose own v otherwise
		// proposedValue = prevAcceptedValue with highest prevAcceptedID; choose own
		// proposedValue otherwise
		if (lastAcceptedID == null) {
			lastAcceptedID = prevAcceptedID; // lastAcceptedID stores the highest accepted id returned in the promises
			
			if (prevAcceptedID != null){
				if (prevAcceptedID.isGreaterThan(lastAcceptedID)){
					if (prevAcceptedValue != -1) {
						proposedValue = prevAcceptedValue;
					} // if true this means prevAcceptedID.isGreaterThan(lastAcceptedID), so set
						// proposedValue to preAcceptedValue
				}
			}
		}

		// if receivePromise(fromUID, proposalID, prevAcceptedID, prevAcceptedValue)
		// from majority:
		if (promisesReceived.size() == quorumSize) {
			if (proposedValue != -1) {
				// send accept(n, v’) to all
				AcceptRequest req = new AcceptRequest(this.proposalID, proposedValue);
				return req;
			}
		}
		return null;
	}

	public Messenger getMessenger() {
		return messenger;
	}

	public String getProposerUID() {
		return proposerUID;
	}

	public int getQuorumSize() {
		return quorumSize;
	}

	public ProposalID getProposalID() {
		return proposalID;
	}

	public int getProposedValue() {
		return proposedValue;
	}

	public ProposalID getLastAcceptedID() {
		return lastAcceptedID;
	}

	public int numPromises() {
		return promisesReceived.size();
	}
}
