public class Acceptor {

	protected Messenger messenger;
	protected String acceptorUID;
	protected ProposalID promisedID;
	protected ProposalID acceptedID = null;
	protected int acceptedValue = -1;

	protected ProposalID finalProposalID = null;
	protected int finalValue = -1;

	public Acceptor(String acceptorUID) {
		this.acceptorUID = acceptorUID;
	}

	public void setMessenger (Messenger messenger){
		this.messenger = messenger;
	}

	public ProposalID getFinalProposalID () {
		return finalProposalID;
	}

	public int getFinalValue () {
		return finalValue;
	}

	public void start (){
		this.messenger.startListening();
	}

	// acceptor’s prepare(n) handler:
	public Promise receivePrepare(String fromUID, ProposalID proposalID) {

		// System.out.println("receive prepare");

		Promise promise = new Promise(acceptorUID, proposalID, acceptedID, acceptedValue);
		if (this.promisedID != null && proposalID.equals(promisedID)) { // duplicate message
			return promise;
		} else if (this.promisedID == null || proposalID.isGreaterThan(promisedID)) { // if n > n_p
			// n_p = n
			promisedID = proposalID;
			// reply prepare_ok(n, n_a, v_a)
			
			return promise;
		}
		return null;
	}

	// acceptor’s accept(n, v) handler:
	public AcceptRequest receiveAcceptRequest(String fromUID, ProposalID proposalID, int value) { // if n >= n_p
		if (promisedID == null || proposalID.isGreaterThan(promisedID) || proposalID.equals(promisedID)) {
			promisedID = proposalID; // n_p = n
			acceptedID = proposalID; // n_a = n
			acceptedValue = value; // v_a = v

			AcceptRequest accepted = new AcceptRequest(acceptedID, acceptedValue);
			return accepted; // reply accept_ok(n)
		} else {
			return null;
		}
	}

	public void resolution (int id, String uid, int value) {
		finalProposalID = new ProposalID(id, uid);
		finalValue = value;
	}

	public Messenger getMessenger() {
		return messenger;
	}

	public ProposalID getPromisedID() {
		return promisedID;
	}

	public ProposalID getAcceptedID() {
		return acceptedID;
	}

	public int getAcceptedValue() {
		return acceptedValue;
	}

	public String getAcceptorUID() {
		return acceptorUID;
	}

}
